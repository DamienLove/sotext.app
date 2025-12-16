package com.pulselink.callid

import android.util.Log
import com.pulselink.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

@Singleton
class RealCallLookupClient @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json
) : CallerIdProvider {

    override val providerName: String = "RealCall"
    override val priority: Int = 5

    override suspend fun lookup(phoneNumber: String): CallerIdLookupResult? = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(phoneNumber, "UTF-8")
        val request = Request.Builder()
            .url("${BuildConfig.REALCALL_LOOKUP_BASE}$encoded")
            .header("User-Agent", USER_AGENT)
            .header("Accept-Language", "en-US,en;q=0.9")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "[$providerName] failed code=${response.code}")
                return@withContext null
            }
            val body = response.body?.string() ?: return@withContext null

            val dataBlob = extractNextData(body) ?: body
            val root = runCatching { json.parseToJsonElement(dataBlob) }.getOrNull()
            val statsSource: JsonObject? = when (root) {
                is JsonObject -> findLikelyResultObject(root)
                else -> null
            }

            var name = statsSource?.str("name")
                ?: statsSource?.str("caller_name")
                ?: findString(root, "name", "callerName", "caller_name", "owner")
            var carrier = statsSource?.str("carrier")
                ?: findString(root, "carrier", "carrier_name")
            var lineType = statsSource?.str("line_type")
                ?: statsSource?.str("type")
                ?: findString(root, "line_type", "type", "phone_type")
            var location = statsSource?.str("location")
                ?: findString(root, "location", "city", "region", "state", "country", "country_name")
            val spamScore = findInt(root, "spam_score", "spamScore", "spamRisk")
            var number = statsSource?.str("number")
                ?: statsSource?.str("phone")
                ?: statsSource?.str("international_format")
                ?: findString(root, "phone_number", "phone", "number", "international_format")
                ?: phoneNumber

            // Fallback to HTML scraping if JSON-like data is absent or empty.
            if (name.isNullOrBlank() && carrier.isNullOrBlank() && location.isNullOrBlank()) {
                val htmlParsed = parseHtmlFallback(body)
                if (htmlParsed != null) {
                    name = htmlParsed.name ?: name
                    lineType = htmlParsed.lineType ?: lineType
                    carrier = htmlParsed.carrier ?: carrier
                    location = htmlParsed.location ?: location
                    number = htmlParsed.number ?: number
                }
            }

            if (name.isNullOrBlank() && carrier.isNullOrBlank() && location.isNullOrBlank()) {
                return@withContext null
            }

            val isSpam = (spamScore ?: 0) >= 80
            val summary = buildString {
                append(number)
                if (!name.isNullOrBlank()) append(" | ").append(name)
                if (!carrier.isNullOrBlank()) append(" | ").append(carrier)
                if (!lineType.isNullOrBlank()) append(" | ").append(lineType)
            }.ifBlank { number }

            CallerIdLookupResult(
                number = number,
                carrier = carrier,
                location = location,
                lineType = lineType,
                valid = true,
                spamScore = spamScore,
                isLikelySpam = isSpam,
                source = providerName.lowercase(),
                summary = summary
            )
        }
    }

    private fun extractNextData(html: String): String? {
        val regex = Regex("""<script id="__NEXT_DATA__" type="application/json">(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
        return regex.find(html)?.groupValues?.getOrNull(1)
    }

    private fun findLikelyResultObject(obj: JsonObject): JsonObject? {
        // Heuristic: look for nested object containing carrier and name.
        fun JsonObject.hasInterestingKeys(): Boolean =
            keys.any { it.contains("name", ignoreCase = true) || it.contains("carrier", ignoreCase = true) }

        val queue = ArrayDeque<JsonObject>()
        queue.add(obj)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current.hasInterestingKeys()) return current
            current.values.forEach { el ->
                if (el is JsonObject) queue.add(el)
            }
        }
        return null
    }

    private fun findString(element: JsonElement?, vararg keys: String): String? =
        when (element) {
            is JsonObject -> {
                keys.firstNotNullOfOrNull { key ->
                    element[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                } ?: element.values.firstNotNullOfOrNull { child ->
                    findString(child, *keys)
                }
            }
            is kotlinx.serialization.json.JsonArray -> element.firstNotNullOfOrNull { child ->
                findString(child, *keys)
            }
            else -> null
        }

    private fun findInt(element: JsonElement?, vararg keys: String): Int? =
        when (element) {
            is JsonObject -> {
                keys.firstNotNullOfOrNull { key ->
                    element[key]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                } ?: element.values.firstNotNullOfOrNull { child ->
                    findInt(child, *keys)
                }
            }
            is kotlinx.serialization.json.JsonArray -> element.firstNotNullOfOrNull { child ->
                findInt(child, *keys)
            }
            else -> null
        }

    private fun JsonObject.str(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun parseHtmlFallback(html: String): HtmlParseResult? {
        fun extract(label: String): String? {
            val regex = Regex(
                """${Regex.escape(label)}\s*:</span>\s*<span[^>]*>(.*?)</span>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            )
            val raw = regex.find(html)?.groupValues?.getOrNull(1) ?: return null
            return cleanHtmlText(raw)
        }
        val name = extract("Display Name")
        val lineType = extract("Number Type")
        val carrier = extract("Number Carrier")
        val location = extract("Location")
        if (name.isNullOrBlank() && carrier.isNullOrBlank() && location.isNullOrBlank()) return null
        return HtmlParseResult(
            name = name?.takeIf { it.isNotBlank() },
            lineType = lineType?.takeIf { it.isNotBlank() },
            carrier = carrier?.takeIf { it.isNotBlank() },
            location = location?.takeIf { it.isNotBlank() },
            number = null
        )
    }

    private fun cleanHtmlText(text: String): String =
        text.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<.*?>"), "")
            .replace("&nbsp;", " ")
            .trim()

    private data class HtmlParseResult(
        val name: String?,
        val lineType: String?,
        val carrier: String?,
        val location: String?,
        val number: String?
    )

    companion object {
        private const val TAG = "RealCallLookupClient"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}
