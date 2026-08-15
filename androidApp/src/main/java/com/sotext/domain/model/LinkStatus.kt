package com.sotext.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class LinkStatus {
    NONE,
    OUTBOUND_PENDING,
    INBOUND_REQUEST,
    LINKED
}
