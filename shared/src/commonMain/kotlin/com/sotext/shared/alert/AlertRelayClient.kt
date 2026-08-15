package com.sotext.shared.alert

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import com.sotext.shared.network.createPlatformHttpClient

class AlertRelayClient private constructor(
    private val httpClient: HttpClient,
    private val baseUrl: String
) {
    companion object {
        fun create(baseUrl: String = AlertRelay.DEFAULT_BASE_URL): AlertRelayClient =
            AlertRelayClient(
                httpClient = createPlatformHttpClient(),
                baseUrl = baseUrl
            )
    }

    suspend fun sendAlert(request: AlertRequest): AlertResponse {
        // Allow a mock base for UI testing (no network)
        if (baseUrl.equals("mock", ignoreCase = true)) {
            return AlertResponse(status = "mocked", relayId = "local", estimatedFanOut = request.recipients.size)
        }

        val endpoint = baseUrl.trimEnd('/') + "/alertRelayHttp"
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
        AlertRelayClient.create(baseUrl)
}
