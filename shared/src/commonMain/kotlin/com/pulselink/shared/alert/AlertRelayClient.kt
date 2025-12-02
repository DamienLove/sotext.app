package com.pulselink.shared.alert

import com.pulselink.shared.network.createPlatformHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AlertRelayClient(
    private val httpClient: HttpClient = createPlatformHttpClient(),
    private val baseUrl: String
) {
    suspend fun sendAlert(request: AlertRequest): AlertResponse {
        // Allow a mock base for UI testing (no network)
        if (baseUrl.equals("mock", ignoreCase = true)) {
            return AlertResponse(status = "mocked", relayId = "local", estimatedFanOut = request.recipients.size)
        }

        val endpoint = baseUrl.trimEnd('/') + "/alertRelay"
        val response: HttpResponse = httpClient.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        return if (response.status.isSuccess()) {
            response.body()
        } else {
            AlertResponse(
                status = "error:${response.status.value}",
                relayId = null,
                estimatedFanOut = null
            )
        }
    }
}

object AlertRelay {
    const val DEFAULT_BASE_URL = "https://us-central1-pulselink-prod.cloudfunctions.net"

    fun create(baseUrl: String = DEFAULT_BASE_URL): AlertRelayClient =
        AlertRelayClient(baseUrl = baseUrl)
}
