package com.sotext.shared.alert

fun createAlertRelayClient(baseUrl: String = AlertRelay.DEFAULT_BASE_URL): AlertRelayClient =
    AlertRelay.create(baseUrl)

object AlertRelayFactory {
    fun build(baseUrl: String = AlertRelay.DEFAULT_BASE_URL): AlertRelayClient =
        AlertRelay.create(baseUrl)
}
