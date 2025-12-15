package com.pulselink.callid

import android.util.Log
import com.pulselink.BuildConfig
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
class NumLookupApiClient @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    @Named("NumlookupApiKey") private val apiKey: String
) : CallerIdProvider {

    override val providerName: String = "NumLookupApi"
    override val priority: Int = 1

    override suspend fun lookup(phoneNumber: String): CallerIdLookupResult? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            Log.w(TAG, "[$providerName] API key missing; skipping")
            return@withContext null
        }
        val encoded = URLEncoder.encode(phoneNumber, "UTF-8")
        val request = Request.Builder()
            .url("${BuildConfig.NUMLOOKUP_API_BASE}/validate/$encoded")
            .addHeader("apikey", apiKey)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "[$providerName] failed code=${response.code}")
                return@withContext null
            }
            val body = response.body?.string() ?: return@withContext null
            val payload = json.decodeFromString<NumLookupApiResponse>(body)
            val number = payload.internationalFormat ?: payload.intlFormat ?: payload.number ?: phoneNumber
            val valid = payload.valid ?: false
            val summary = buildString {
                append(number)
                payload.carrier?.let { append(" | ").append(it) }
                payload.lineType?.let { append(" | ").append(it) }
            }.ifBlank { number }
            CallerIdLookupResult(
                number = number,
                carrier = payload.carrier,
                location = payload.location ?: payload.countryName,
                lineType = payload.lineType,
                valid = valid,
                spamScore = null,
                isLikelySpam = valid.not(),
                source = providerName.lowercase(),
                summary = summary
            )
        }
    }

    @Serializable
    private data class NumLookupApiResponse(
        val valid: Boolean? = null,
        val number: String? = null,
        @SerialName("local_format") val localFormat: String? = null,
        @SerialName("international_format") val internationalFormat: String? = null,
        @SerialName("intl_format") val intlFormat: String? = null,
        @SerialName("country_prefix") val countryPrefix: String? = null,
        @SerialName("country_code") val countryCode: String? = null,
        @SerialName("country_name") val countryName: String? = null,
        val location: String? = null,
        val carrier: String? = null,
        @SerialName("line_type") val lineType: String? = null
    )

    companion object {
        private const val TAG = "NumLookupApiClient"
    }
}
