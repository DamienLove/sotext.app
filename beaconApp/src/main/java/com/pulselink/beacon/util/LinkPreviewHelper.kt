package com.pulselink.beacon.util

import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

data class LinkPreviewData(
    val url: String,
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val siteName: String? = null
)

object LinkPreviewHelper {
    private val cache = LruCache<String, LinkPreviewData>(100)
    // Regex updated to include () but exclude trailing punctuation like dot, comma etc.
    // Logic: Matches http/https, then non-whitespace chars.
    // Then we manually trim trailing punctuation in the function logic or improved regex.
    // Improved regex: https?:// [not whitespace]+ (but not ending in .,;) )
    private val urlRegex = Pattern.compile("https?://[^\\s]+(?<![.,;)])")

    fun extractUrl(text: String): String? {
        val matcher = urlRegex.matcher(text)
        return if (matcher.find()) matcher.group(0) else null
    }

    suspend fun fetchPreview(url: String): LinkPreviewData = withContext(Dispatchers.IO) {
        cache.get(url)?.let { return@withContext it }

        var title: String? = null
        var description: String? = null
        var imageUrl: String? = null
        var siteName: String? = null

        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            connection.setRequestProperty("Accept", "text/html")

            if (connection.responseCode == 200) {
                // Read only first 15KB to avoid downloading huge files
                val buffer = CharArray(15 * 1024)
                val reader = connection.inputStream.bufferedReader()
                val readCount = reader.read(buffer)
                val html = if (readCount > 0) String(buffer, 0, readCount) else ""

                // Simple regex extraction for OGP tags
                title = extractMeta(html, "og:title") ?: extractTag(html, "title")
                description = extractMeta(html, "og:description") ?: extractMeta(html, "description")
                imageUrl = extractMeta(html, "og:image")
                siteName = extractMeta(html, "og:site_name")
            }
            connection.disconnect()
        } catch (e: Exception) {
            // Ignore errors
        }

        val data = LinkPreviewData(url, title, description, imageUrl, siteName)
        cache.put(url, data)
        return@withContext data
    }

    private fun extractMeta(html: String, property: String): String? {
        // Matches <meta ... property="og:title" ... content="Title" ... >
        // Supports single or double quotes for attributes.

        // 1. Find the meta tag containing the property
        // property=['"]value['"]
        val propertyRegex = "(?:property|name)=[\"']$property[\"']"
        val tagPattern = Pattern.compile("<meta[^>]+$propertyRegex[^>]*>", Pattern.CASE_INSENSITIVE)
        val tagMatcher = tagPattern.matcher(html)

        if (tagMatcher.find()) {
            val tag = tagMatcher.group(0)
            // 2. Extract content from that tag
            val contentPattern = Pattern.compile("content=[\"']([^\"']+)[\"']")
            val contentMatcher = contentPattern.matcher(tag)
            if (contentMatcher.find()) return contentMatcher.group(1)
        }
        return null
    }

    private fun extractTag(html: String, tagName: String): String? {
        val pattern = Pattern.compile("<$tagName>(.*?)</$tagName>", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        return if (matcher.find()) matcher.group(1) else null
    }
}
