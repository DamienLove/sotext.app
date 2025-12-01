package com.pulselink.domain.model

/**
 * Semantic urgency for manual (non-system) messages routed through PulseLink.
 *
 * - EMERGENCY: treat like full emergency/siren, include location and maximum override.
 * - URGENT: high priority, DND break-through, loud but can respect volume hints.
 * - STANDARD: still routed through PulseLink (not plain SMS), lower intensity but can still
 *   request DND bypass and volume hints.
 */
enum class MessageUrgency {
    EMERGENCY,
    URGENT,
    STANDARD
}
