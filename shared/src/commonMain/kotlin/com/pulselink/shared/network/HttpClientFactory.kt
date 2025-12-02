package com.pulselink.shared.network

import io.ktor.client.HttpClient

/**
 * Platform-aware HTTP client with JSON support.
 */
expect fun createPlatformHttpClient(): HttpClient
