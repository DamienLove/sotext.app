package com.pulselink.data.alert

import android.content.Context
import com.pulselink.R
import com.pulselink.domain.model.SoundCategory
import com.pulselink.domain.model.SoundOption
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundCatalog @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val soundOptions: List<SoundOption> by lazy { discoverSounds() }

    fun emergencyOptions(): List<SoundOption> =
        soundOptions.filter { it.category == SoundCategory.SIREN }

    fun checkInOptions(): List<SoundOption> =
        soundOptions.filter { it.category == SoundCategory.CHIME }
            .ifEmpty { emergencyOptions() }

    fun resolve(key: String?, fallbackCategory: SoundCategory): SoundOption? {
        key?.let { cachedOptions[it] }?.let { return it }
        return when (fallbackCategory) {
            SoundCategory.SIREN -> emergencyOptions().firstOrNull()
            SoundCategory.CHIME -> checkInOptions().firstOrNull()
        }
    }

    fun defaultKeyFor(category: SoundCategory): String? =
        resolve(null, category)?.key

    private val cachedOptions: Map<String, SoundOption> by lazy {
        soundOptions.associateBy { it.key }
    }

    private fun discoverSounds(): List<SoundOption> {
        val rawClass = R.raw::class.java
        val fields = rawClass.fields.orEmpty()
        return fields.mapNotNull { field ->
            val name = field.name
            val resId = runCatching { field.getInt(null) }.getOrNull() ?: return@mapNotNull null
            when {
                name.startsWith(SIREN_PREFIX) -> {
                    val label = toLabel(name.removePrefix(SIREN_PREFIX), "Standard Siren")
                    SoundOption(
                        key = name,
                        label = label,
                        resId = resId,
                        category = SoundCategory.SIREN
                    )
                }
                name.startsWith(CHIME_PREFIX) -> {
                    val suffix = name.removePrefix(CHIME_PREFIX)
                    val label = toLabel(suffix.trimStart('_'), "Gentle Chime")
                    SoundOption(
                        key = name,
                        label = label,
                        resId = resId,
                        category = SoundCategory.CHIME
                    )
                }
                else -> null
            }
        }.sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    private fun toLabel(raw: String, fallback: String): String {
        if (raw.isBlank()) return fallback
        return raw
            .replace("__", "_")
            .replace('-', '_')
            .split('_')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
    }

    companion object {
        private const val SIREN_PREFIX = "alert_siren"
        private const val CHIME_PREFIX = "alert_chime"
    }
}
