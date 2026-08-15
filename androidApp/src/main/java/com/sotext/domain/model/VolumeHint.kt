package com.sotext.domain.model

/**
 * Optional volume guidance sent with cross-device alerts/messages.
 *
 * The receiver maps these to fractions of max ring/notification volume.
 */
enum class VolumeHint {
    LOW,
    MEDIUM,
    HIGH,
    MAX
}
