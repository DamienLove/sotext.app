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
class IpQualityScoreClient @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    @Named("IpQualityScoreApiKey") private val apiKey: String
) : CallerIdProvider {

    override val providerName: String = "IPQualityScore"
    override val priority: Int = 3

    override suspend fun lookup(phoneNumber: String): CallerIdLookupResult? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            Log.w(TAG, "[$providerName] API key missing; skipping")
            return@withContext null
        }
        val encoded = URLEncoder.encode(phoneNumber, "UTF-8")
        val request = Request.Builder()
            .url("${BuildConfig.IPQS_API_BASE}/$apiKey/$encoded")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "[$providerName] failed code=${response.code}")
                return@withContext null
            }
            val body = response.body?.string() ?: return@withContext null
            val payload = json.decodeFromString<IpqsResponse>(body)
            val number = payload.formattedPhone ?: payload.phone ?: phoneNumber
            val location = listOfNotNull(payload.city, payload.region, payload.country)
                .joinToString(", ").ifBlank { null }
            val fraudScore = payload.fraudScore
            val spam = (payload.spammer == true) || (payload.recentAbuse == true) || (fraudScore ?: 0) >= 75 || payload.valid == false
            val summary = buildString {
                append(number)
                payload.carrier?.let { append(" | ").append(it) }
                location?.let { append(" | ").append(it) }
                fraudScore?.let { append(" | risk ").append(it) }
            }.ifBlank { number }
            CallerIdLookupResult(
                number = number,
                carrier = payload.carrier,
                location = location,
                lineType = payload.lineType,
                valid = payload.valid ?: false,
                spamScore = fraudScore,
                isLikelySpam = spam,
                source = providerName.lowercase(),
                summary = summary
            )
        }
    }

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

    companion object {
        private const val TAG = "IpQualityScoreClient"
    }
}
