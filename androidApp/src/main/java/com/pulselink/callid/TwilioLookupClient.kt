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
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

@Singleton
class TwilioLookupClient @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    @Named("TwilioAccountSid") private val accountSid: String,
    @Named("TwilioAuthToken") private val authToken: String
) : CallerIdProvider {

    override val providerName: String = "TwilioLookup"
    override val priority: Int = 0

    override suspend fun lookup(phoneNumber: String): CallerIdLookupResult? = withContext(Dispatchers.IO) {
        if (accountSid.isBlank() || authToken.isBlank()) {
            Log.w(TAG, "[$providerName] credentials missing; skipping")
            return@withContext null
        }
        val encoded = URLEncoder.encode(phoneNumber, "UTF-8")
        val request = Request.Builder()
            .url("${BuildConfig.TWILIO_LOOKUP_BASE}/$encoded?Fields=caller_name")
            .header("Authorization", Credentials.basic(accountSid, authToken))
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "[$providerName] failed code=${response.code}")
                return@withContext null
            }
            val body = response.body?.string() ?: return@withContext null
            val payload = json.decodeFromString<TwilioLookupResponse>(body)
            val hasError = (payload.callerName?.errorCode != null) || (payload.carrier?.errorCode != null)
            if (hasError) return@withContext null

            val normalizedNumber = payload.phoneNumber ?: payload.nationalFormat ?: phoneNumber
            val callerName = payload.callerName?.callerName
            val carrierName = payload.carrier?.name
            val lineType = payload.carrier?.type
            val summary = buildString {
                append(normalizedNumber)
                if (!callerName.isNullOrBlank()) append(" | ").append(callerName)
                if (!carrierName.isNullOrBlank()) append(" | ").append(carrierName)
            }.ifBlank { normalizedNumber }

            CallerIdLookupResult(
                number = normalizedNumber,
                carrier = carrierName,
                location = payload.countryCode,
                lineType = lineType,
                valid = true,
                spamScore = null,
                isLikelySpam = false,
                source = providerName.lowercase(),
                summary = summary
            )
        }
    }

    @Serializable
    private data class TwilioLookupResponse(
        @SerialName("caller_name") val callerName: CallerName? = null,
        val carrier: Carrier? = null,
        @SerialName("country_code") val countryCode: String? = null,
        @SerialName("national_format") val nationalFormat: String? = null,
        @SerialName("phone_number") val phoneNumber: String? = null
    )

    @Serializable
    private data class CallerName(
        @SerialName("caller_name") val callerName: String? = null,
        @SerialName("caller_type") val callerType: String? = null,
        @SerialName("error_code") val errorCode: Int? = null
    )

    @Serializable
    private data class Carrier(
        val name: String? = null,
        val type: String? = null,
        @SerialName("mobile_country_code") val mobileCountryCode: String? = null,
        @SerialName("mobile_network_code") val mobileNetworkCode: String? = null,
        @SerialName("error_code") val errorCode: Int? = null
    )

    companion object {
        private const val TAG = "TwilioLookupClient"
    }
}
