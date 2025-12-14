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
class CallerIdLookupClient @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    @Named("NumlookupApiKey") private val numlookupApiKey: String,
    @Named("IpqsApiKey") private val ipqsApiKey: String
) {

    suspend fun lookup(rawNumber: String): CallerIdLookupResult? = withContext(Dispatchers.IO) {
        val normalized = rawNumber.filter { it.isDigit() || it == '+' }
        if (normalized.isBlank()) return@withContext null
        val encoded = URLEncoder.encode(normalized, "UTF-8")

        // Primary: Numlookup
        runCatching { lookupNumlookup(encoded, normalized) }.getOrNull()?.let { return@withContext it }

        // Fallback: IPQualityScore
        runCatching { lookupIpqs(encoded, normalized) }.getOrNull()?.let { return@withContext it }

        null
    }

    private fun lookupNumlookup(encodedNumber: String, normalized: String): CallerIdLookupResult? {
        if (numlookupApiKey.isBlank()) {
            Log.w(TAG, "Numlookup API key missing; skipping primary lookup.")
            return null
        }
        val request = Request.Builder()
            .url("${BuildConfig.NUMLOOKUP_API_BASE}/validate/$encodedNumber")
            .addHeader("apikey", numlookupApiKey)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "Numlookup failed code=${response.code}")
                return null
            }
            val body = response.body?.string() ?: return null
            val payload = json.decodeFromString<NumLookupApiResponse>(body)
            val number = payload.internationalFormat ?: payload.intlFormat ?: payload.number ?: normalized
            val valid = payload.valid ?: false
            val result = CallerIdLookupResult(
                number = number,
                carrier = payload.carrier,
                location = payload.location ?: payload.countryName,
                lineType = payload.lineType,
                valid = valid,
                spamScore = null,
                isLikelySpam = valid.not(),
                source = "numlookup",
                summary = buildString {
                    append(number)
                    payload.carrier?.let { append(" · $it") }
                    payload.lineType?.let { append(" · $it") }
                }.ifBlank { number }
            )
            return result
        }
    }

    private fun lookupIpqs(encodedNumber: String, normalized: String): CallerIdLookupResult? {
        if (ipqsApiKey.isBlank()) {
            Log.w(TAG, "IPQS API key missing; skipping fallback lookup.")
            return null
        }
        val url = "${BuildConfig.IPQS_API_BASE}/$ipqsApiKey/$encodedNumber"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "IPQS failed code=${response.code}")
                return null
            }
            val body = response.body?.string() ?: return null
            val payload = json.decodeFromString<IpqsResponse>(body)
            val number = payload.formattedPhone ?: payload.phone ?: normalized
            val location = listOfNotNull(payload.city, payload.region, payload.country)
                .joinToString(separator = ", ").ifBlank { null }
            val fraudScore = payload.fraudScore
            val spam = (payload.spammer == true) || (payload.recentAbuse == true) || (fraudScore ?: 0) >= 85 || payload.valid == false
            return CallerIdLookupResult(
                number = number,
                carrier = payload.carrier,
                location = location,
                lineType = payload.lineType,
                valid = payload.valid ?: false,
                spamScore = fraudScore,
                isLikelySpam = spam,
                source = "ipqualityscore",
                summary = buildString {
                    append(number)
                    payload.carrier?.let { append(" · $it") }
                    location?.let { append(" · $it") }
                    fraudScore?.let { append(" · risk $it") }
                }.ifBlank { number }
            )
        }
    }

    companion object {
        private const val TAG = "CallerIdLookupClient"
    }
}

data class CallerIdLookupResult(
    val number: String,
    val carrier: String?,
    val location: String?,
    val lineType: String?,
    val valid: Boolean,
    val spamScore: Int?,
    val isLikelySpam: Boolean,
    val source: String,
    val summary: String
)

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

@Serializable
private data class IpqsResponse(
    val success: Boolean? = null,
    val valid: Boolean? = null,
    @SerialName("formatted_phone") val formattedPhone: String? = null,
    val phone: String? = null,
    val carrier: String? = null,
    @SerialName("line_type") val lineType: String? = null,
    val city: String? = null,
    val region: String? = null,
    val country: String? = null,
    @SerialName("fraud_score") val fraudScore: Int? = null,
    val spammer: Boolean? = null,
    @SerialName("recent_abuse") val recentAbuse: Boolean? = null
)
