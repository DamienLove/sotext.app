package com.sotext.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class MessageChannel(val priority: Int) {
    FIREBASE(1),
    SMS(2),
    EMAIL(3)
}
