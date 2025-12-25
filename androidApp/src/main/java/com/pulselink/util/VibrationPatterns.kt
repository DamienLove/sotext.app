package com.pulselink.util

data class VibrationPatternOption(
    val key: String,
    val label: String,
    val pattern: LongArray
)

object VibrationPatterns {
    const val MESSAGE_DEFAULT = "message_default"
    const val ALERT_DEFAULT = "alert_default"

    val messageOptions: List<VibrationPatternOption> = listOf(
        VibrationPatternOption(
            key = MESSAGE_DEFAULT,
            label = "Default",
            pattern = longArrayOf(0, 250, 200, 250)
        ),
        VibrationPatternOption(
            key = "message_short",
            label = "Short",
            pattern = longArrayOf(0, 150, 100, 150)
        ),
        VibrationPatternOption(
            key = "message_double",
            label = "Double tap",
            pattern = longArrayOf(0, 180, 120, 180)
        ),
        VibrationPatternOption(
            key = "message_strong",
            label = "Strong",
            pattern = longArrayOf(0, 300, 150, 300, 200, 450)
        )
    )

    val alertOptions: List<VibrationPatternOption> = listOf(
        VibrationPatternOption(
            key = ALERT_DEFAULT,
            label = "Default",
            pattern = longArrayOf(0, 250, 250, 250, 500, 250)
        ),
        VibrationPatternOption(
            key = "alert_urgent",
            label = "Urgent",
            pattern = longArrayOf(0, 200, 100, 200, 100, 400, 200, 600)
        ),
        VibrationPatternOption(
            key = "alert_strong",
            label = "Strong",
            pattern = longArrayOf(0, 400, 200, 400, 200, 500)
        ),
        VibrationPatternOption(
            key = "alert_sos",
            label = "S.O.S.",
            pattern = longArrayOf(0, 120, 80, 120, 80, 120, 200, 300, 200, 300, 200, 300)
        )
    )

    fun messageOption(key: String?): VibrationPatternOption =
        messageOptions.firstOrNull { it.key == key } ?: messageOptions.first()

    fun alertOption(key: String?): VibrationPatternOption =
        alertOptions.firstOrNull { it.key == key } ?: alertOptions.first()
}
