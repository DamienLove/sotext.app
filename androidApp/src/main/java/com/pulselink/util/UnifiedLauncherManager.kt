package com.pulselink.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object UnifiedLauncherManager {
    fun apply(
        context: Context,
        unifiedEnabled: Boolean,
        preferredLabel: String?
    ) {
        val pm = context.packageManager
        val pkg = context.packageName
        val pulseLinkLauncher = ComponentName(pkg, "com.pulselink.PulseLinkLauncher")
        val main = ComponentName(pkg, "com.pulselink.ui.MainActivity")
        val inbox = ComponentName(pkg, "com.pulselink.ui.InboxLauncherActivity")
        val inboxLogo = ComponentName(pkg, "com.pulselink.BeaconInboxLogo")
        val inboxPro = ComponentName(pkg, "com.pulselink.BeaconInboxPro")
        val beaconVariants = listOf(
            inboxLogo,
            inboxPro,
            ComponentName(pkg, "com.pulselink.BeaconInboxThemeDefaultLight"),
            ComponentName(pkg, "com.pulselink.BeaconInboxThemeMidnightOled"),
            ComponentName(pkg, "com.pulselink.BeaconInboxThemeOceanDeep"),
            ComponentName(pkg, "com.pulselink.BeaconInboxThemeRosePetal"),
            ComponentName(pkg, "com.pulselink.BeaconInboxThemeSunsetFade"),
            ComponentName(pkg, "com.pulselink.BeaconInboxThemeCitrusPop"),
            ComponentName(pkg, "com.pulselink.BeaconInboxThemeForestTrail"),
            ComponentName(pkg, "com.pulselink.BeaconInboxThemeLavenderHaze"),
            ComponentName(pkg, "com.pulselink.BeaconInboxThemeSlateMono"),
            ComponentName(pkg, "com.pulselink.BeaconInboxThemeDesertClay"),
            ComponentName(pkg, "com.pulselink.BeaconInboxThemeNordFrost"),
            ComponentName(pkg, "com.pulselink.BeaconInboxThemeNeonNoir"),
            ComponentName(pkg, "com.pulselink.BeaconInboxThemePaperback"),
            ComponentName(pkg, "com.pulselink.BeaconInboxThemeMintBreeze"),
            ComponentName(pkg, "com.pulselink.BeaconInboxThemeAmethystNight"),
            ComponentName(pkg, "com.pulselink.BeaconInboxThemeAurora")
        )

        val unifiedMessages = ComponentName(pkg, "com.pulselink.UnifiedLauncherMessages")
        val unifiedPulseLink = ComponentName(pkg, "com.pulselink.UnifiedLauncherPulseLinkPremium")
        val unifiedBeacon = ComponentName(pkg, "com.pulselink.UnifiedLauncherBeaconPremium")

        val unifiedTargets = mapOf(
            "messages" to unifiedMessages,
            "pulselink premium" to unifiedPulseLink,
            "beacon premium" to unifiedBeacon
        )

        val desired = preferredLabel?.trim()?.lowercase()
        val targetUnified = unifiedTargets[desired] ?: unifiedMessages

        if (unifiedEnabled) {
            // Enable unified launcher; keep MainActivity enabled so the alias can open
            enable(pm, targetUnified)
            enable(pm, main)
            disable(pm, pulseLinkLauncher)
            disable(pm, inbox)
            beaconVariants.forEach { disable(pm, it) }
            unifiedTargets.values.filterNot { it == targetUnified }.forEach { disable(pm, it) }
        } else {
            // Restore default PulseLink launcher, keep unified aliases hidden
            enable(pm, main)
            enable(pm, pulseLinkLauncher)
            enable(pm, inbox)
            // Re-enable Beacon launchers so the icon returns when unified is off
            beaconVariants.forEach { enable(pm, it) }
            unifiedTargets.values.forEach { disable(pm, it) }
            // Inbox launcher follows its existing runtime flag elsewhere; leave it untouched
        }
    }

    private fun enable(pm: PackageManager, component: ComponentName) {
        pm.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun disable(pm: PackageManager, component: ComponentName) {
        pm.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
