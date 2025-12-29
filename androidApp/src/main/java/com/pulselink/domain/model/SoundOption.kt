package com.pulselink.domain.model

data class SoundOption(
    val key: String,
    val label: String,
    val resId: Int,
    val category: SoundCategory
)

enum class SoundCategory { SIREN, CHIME }
