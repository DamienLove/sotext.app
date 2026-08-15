package com.sotext.callid

import android.util.Log
import com.sotext.BuildConfig
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

@Singleton
class RapidPhoneLookupClient @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    private val keyProvider: RapidLookupKeyProvider,
    @Named("RapidLookupApiHost") private val apiHost: String
) : CallerIdProvider {

    override val providerName: String = "RapidLookup"
    override val priority: Int = 4

    override suspend fun lookup(phoneNumber: String): CallerIdLookupResult? = withContext(Dispatchers.IO) {
        val apiKey = keyProvider.currentKey()
        if (apiKey.isBlank()) {
            Log.w(TAG, "[$providerName] API key missing; skipping")
            return@withContext null
        }

        val encoded = URLEncoder.encode(phoneNumber, "UTF-8")
        val request = Request.Builder()
            .url("${BuildConfig.RAPID_LOOKUP_BASE}?number=$encoded")
            .addHeader("X-RapidAPI-Key", apiKey)
            .addHeader("X-RapidAPI-Host", apiHost)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "[$providerName] failed code=${response.code}")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val payload = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return@withContext null

            val valid = payload.bool("valid") ?: payload.bool("is_valid") ?: true
            val number = payload.str("international_format")
                ?: payload.str("phone")
                ?: payload.str("number")
                ?: phoneNumber
            val callerName = payload.str("name") ?: payload.str("caller_name")
            val carrier = payload.str("carrier")
            val lineType = payload.str("line_type") ?: payload.str("type")
            val location = listOfNotNull(
                payload.str("location"),
                payload.str("region"),
                payload.str("country_name"),
                payload.str("country_code")
            ).joinToString(", ").ifBlank { null }
            val spamScore = payload["spam_score"]?.jsonPrimitive?.intOrNull
            val isSpam = (spamScore ?: 0) >= 80 || valid.not()

            val summary = buildString {
                append(number)
                if (!callerName.isNullOrBlank()) append(" | ").append(callerName)
                if (!carrier.isNullOrBlank()) append(" | ").append(carrier)
                if (!lineType.isNullOrBlank()) append(" | ").append(lineType)
            }.ifBlank { number }

            CallerIdLookupResult(
                number = number,
                carrier = carrier,
                location = location,
                lineType = lineType,
                valid = valid,
                spamScore = spamScore,
                isLikelySpam = isSpam,
                source = providerName.lowercase(),
                summary = summary
            )
        }
    }

    private fun JsonObject.str(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun JsonObject.bool(key: String): Boolean? =
        this[key]?.jsonPrimitive?.booleanOrNull

    private val kotlinx.serialization.json.JsonPrimitive.intOrNull: Int?
        get() = this.contentOrNull?.toIntOrNull()

    companion object {
        private const val TAG = "RapidPhoneLookupClient"
    }
}
