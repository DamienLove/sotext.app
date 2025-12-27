package com.pulselink.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.pulselink.ui.InboxLauncherActivity
import java.util.Locale

object BeaconIconManager {
    private val themeVariantMap = mapOf(
        "default_light" to "com.pulselink.BeaconInboxThemeDefaultLight",
        "midnight_oled" to "com.pulselink.BeaconInboxThemeMidnightOled",
        "ocean_deep" to "com.pulselink.BeaconInboxThemeOceanDeep",
        "rose_petal" to "com.pulselink.BeaconInboxThemeRosePetal",
        "sunset_fade" to "com.pulselink.BeaconInboxThemeSunsetFade",
        "citrus_pop" to "com.pulselink.BeaconInboxThemeCitrusPop",
        "forest_trail" to "com.pulselink.BeaconInboxThemeForestTrail",
        "lavender_haze" to "com.pulselink.BeaconInboxThemeLavenderHaze",
        "slate_mono" to "com.pulselink.BeaconInboxThemeSlateMono",
        "aurora" to "com.pulselink.BeaconInboxThemeAurora",
        "desert_clay" to "com.pulselink.BeaconInboxThemeDesertClay",
        "nord_frost" to "com.pulselink.BeaconInboxThemeNordFrost",
        "neon_noir" to "com.pulselink.BeaconInboxThemeNeonNoir",
        "paperback" to "com.pulselink.BeaconInboxThemePaperback",
        "mint_breeze" to "com.pulselink.BeaconInboxThemeMintBreeze",
        "amethyst_night" to "com.pulselink.BeaconInboxThemeAmethystNight"
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

    fun apply(context: Context, variant: String, enabled: Boolean = true) {
        val pm = context.packageManager
        val pkg = context.packageName
        val defaultComp = ComponentName(context, InboxLauncherActivity::class.java)
        val logoComp = ComponentName(pkg, "com.pulselink.BeaconInboxLogo")
        val proComp = ComponentName(pkg, "com.pulselink.BeaconInboxPro")
        val themeComps = themeVariantMap.values.map { ComponentName(pkg, it) }
        val allComps = listOf(defaultComp, logoComp, proComp) + themeComps

        if (!enabled) {
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

        allComps.forEach { component ->
            val state = if (component == target) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            pm.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
        }
    }
}
