package com.pulselink.beacon.data

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object SotextProtocol {

    private const val PREFIX = "SOTEXT|DEL"

    data class DeleteCommand(
        val messageId: Long,
        val timestamp: Long,
        val bodySnippet: String
    )

    fun encodeDeleteCommand(
        messageId: Long,
        timestamp: Long,
        bodySnippet: String
    ): String {
        val encodedSnippet = URLEncoder.encode(
            bodySnippet,
            StandardCharsets.UTF_8.toString()
        )
        return "$PREFIX|$messageId|$timestamp|$encodedSnippet"
    }

    fun parseDeleteCommand(body: String): DeleteCommand? {
        if (!body.startsWith(PREFIX)) return null

        val parts = body.split('|')
        if (parts.size < 5) return null

        val messageId = parts[2].toLongOrNull() ?: return null
        val timestamp = parts[3].toLongOrNull() ?: return null

        val decodedSnippet = try {
            URLDecoder.decode(parts[4], StandardCharsets.UTF_8.toString())
        } catch (e: IllegalArgumentException) {
            return null
        }

        return DeleteCommand(
            messageId = messageId,
            timestamp = timestamp,
            bodySnippet = decodedSnippet
        )
    }
}
