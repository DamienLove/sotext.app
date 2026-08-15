package com.sotext.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.sotext.ui.InboxLauncherActivity
import java.util.Locale

object BeaconIconManager {
    private const val TAG = "BeaconIconManager"

    private val themeVariantMap = mapOf(
        "default_light" to "com.sotext.BeaconInboxThemeDefaultLight",
        "midnight_oled" to "com.sotext.BeaconInboxThemeMidnightOled",
        "ocean_deep" to "com.sotext.BeaconInboxThemeOceanDeep",
        "rose_petal" to "com.sotext.BeaconInboxThemeRosePetal",
        "sunset_fade" to "com.sotext.BeaconInboxThemeSunsetFade",
        "citrus_pop" to "com.sotext.BeaconInboxThemeCitrusPop",
        "forest_trail" to "com.sotext.BeaconInboxThemeForestTrail",
        "lavender_haze" to "com.sotext.BeaconInboxThemeLavenderHaze",
        "slate_mono" to "com.sotext.BeaconInboxThemeSlateMono",
        "aurora" to "com.sotext.BeaconInboxThemeAurora",
        "desert_clay" to "com.sotext.BeaconInboxThemeDesertClay",
        "nord_frost" to "com.sotext.BeaconInboxThemeNordFrost",
        "neon_noir" to "com.sotext.BeaconInboxThemeNeonNoir",
        "paperback" to "com.sotext.BeaconInboxThemePaperback",
        "mint_breeze" to "com.sotext.BeaconInboxThemeMintBreeze",
        "amethyst_night" to "com.sotext.BeaconInboxThemeAmethystNight"
    )

    private fun normalizeVariant(raw: String): String {
        return raw.trim()
            .lowercase(Locale.US)
            .replace("-", "_")
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]+"), "")
            .replace(Regex("_+"), "_")
            .trim('_')
            .ifBlank { "default" }
    }

    /**
     * Apply beacon icon variant settings.
     * @param unifiedModeActive If true, skip all operations as UnifiedLauncherManager handles icon state
     */
    fun apply(context: Context, variant: String, enabled: Boolean = true, unifiedModeActive: Boolean = false) {
        // When unified navigation is enabled, UnifiedLauncherManager handles all launcher icon states.
        // Skip beacon icon management to avoid race conditions.
        if (unifiedModeActive) {
            Log.d(TAG, "Skipping beacon icon management - unified mode is active")
            return
        }

        Log.d(TAG, "apply() called - variant=$variant, enabled=$enabled")

        val pm = context.packageManager
        val pkg = context.packageName
        val defaultComp = ComponentName(context, InboxLauncherActivity::class.java)
        val logoComp = ComponentName(pkg, "com.sotext.BeaconInboxLogo")
        val proComp = ComponentName(pkg, "com.sotext.BeaconInboxPro")
        val themeComps = themeVariantMap.values.map { ComponentName(pkg, it) }
        val allComps = listOf(defaultComp, logoComp, proComp) + themeComps

        if (!enabled) {
            Log.d(TAG, "Disabling all beacon icons")
            allComps.forEach {
                pm.setComponentEnabledSetting(it, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }
            return
        }

        val key = normalizeVariant(variant)
        val target = when {
            key == "logo" -> logoComp
            key == "pro" -> proComp
            key in themeVariantMap -> ComponentName(pkg, themeVariantMap.getValue(key))
            else -> defaultComp
        }

        Log.d(TAG, "Enabling beacon icon variant: $key (${target.className})")

        allComps.forEach { component ->
            val state = if (component == target) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            pm.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
        }

        Log.d(TAG, "Beacon icon management complete")
    }
}
