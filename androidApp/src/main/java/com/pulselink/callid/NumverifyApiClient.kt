package com.pulselink.callid

import android.util.Log
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

@Singleton
class NumverifyApiClient @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    @Named("NumverifyApiKey") private val apiKey: String
) : CallerIdProvider {

    override val providerName: String = "Numverify"
    override val priority: Int = 2

    override suspend fun lookup(phoneNumber: String): CallerIdLookupResult? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            Log.w(TAG, "[$providerName] API key missing; skipping")
            return@withContext null
        }
        val encoded = URLEncoder.encode(phoneNumber, "UTF-8")
        val url = "http://apilayer.net/api/validate?access_key=$apiKey&number=$encoded&format=1"
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "[$providerName] failed code=${response.code}")
                return@withContext null
            }
            val body = response.body?.string() ?: return@withContext null
            val payload = json.decodeFromString<NumverifyResponse>(body)
            if (payload.valid != true) return@withContext null
            val location = listOfNotNull(payload.location, payload.countryName)
                .joinToString(", ").ifBlank { null }
            val summary = buildString {
                append(payload.internationalFormat ?: phoneNumber)
                payload.carrier?.let { append(" | ").append(it) }
                payload.lineType?.let { append(" | ").append(it) }
            }.ifBlank { phoneNumber }
            CallerIdLookupResult(
                number = payload.internationalFormat ?: payload.localFormat ?: phoneNumber,
                carrier = payload.carrier,
                location = location,
                lineType = payload.lineType,
                valid = true,
                spamScore = null,
                isLikelySpam = false,
                source = providerName.lowercase(),
                summary = summary
            )
        }
    }

    @Serializable
    private data class NumverifyResponse(
        val valid: Boolean? = null,
        val number: String? = null,
        @SerialName("local_format") val localFormat: String? = null,
        @SerialName("international_format") val internationalFormat: String? = null,
        @SerialName("country_prefix") val countryPrefix: String? = null,
        @SerialName("country_code") val countryCode: String? = null,
        @SerialName("country_name") val countryName: String? = null,
        val location: String? = null,
        val carrier: String? = null,
        @SerialName("line_type") val lineType: String? = null
    )

    companion object {
        private const val TAG = "NumverifyApiClient"
    }
}
