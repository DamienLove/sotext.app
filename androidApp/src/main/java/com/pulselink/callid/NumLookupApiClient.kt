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
) {
    suspend fun lookup(rawNumber: String): CallerIdLookupResult? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            Log.w(TAG, "Numlookup API key missing; skipping lookup.")
            return@withContext null
        }

        val normalized = rawNumber.filter { it.isDigit() || it == '+' }
        if (normalized.isBlank()) return@withContext null

        val encoded = URLEncoder.encode(normalized, "UTF-8")
        val request = Request.Builder()
            .url("${BuildConfig.NUMLOOKUP_API_BASE}/validate/$encoded")
            .addHeader("apikey", apiKey)
            .get()
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Lookup failed for $normalized with code ${response.code}")
                    return@withContext null
                }
                val body = response.body?.string() ?: return@withContext null
                val payload = json.decodeFromString<NumLookupApiResponse>(body)
                CallerIdLookupResult(
                    number = payload.internationalFormat ?: payload.intlFormat ?: payload.number ?: normalized,
                    carrier = payload.carrier,
                    location = payload.location ?: payload.countryName,
                    lineType = payload.lineType,
                    valid = payload.valid ?: false
                )
            }
        }.getOrElse { error ->
            Log.w(TAG, "Lookup error for $normalized: ${error.message}")
            null
        }
    }

    companion object {
        private const val TAG = "NumLookupApiClient"
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
