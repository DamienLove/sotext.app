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

        val main = ComponentName(pkg, "com.pulselink.ui.MainActivity")
        val inbox = ComponentName(pkg, "com.pulselink.ui.InboxLauncherActivity")
        val inboxLogo = ComponentName(pkg, "com.pulselink.BeaconInboxLogo")
        val inboxPro = ComponentName(pkg, "com.pulselink.BeaconInboxPro")

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
            // Enable chosen unified launcher AND keep main PulseLink launcher enabled
            enable(pm, targetUnified)
            enable(pm, main)  // Keep PulseLink enabled in unified mode
            // Don't disable inbox - let BeaconIconManager handle it
            disable(pm, inboxLogo)
            disable(pm, inboxPro)
            unifiedTargets.values.filterNot { it == targetUnified }.forEach { disable(pm, it) }
        } else {
            // Restore default PulseLink launcher, keep unified aliases hidden
            enable(pm, main)
            disable(pm, inboxLogo)
            disable(pm, inboxPro)
            disable(pm, unifiedMessages)
            disable(pm, unifiedPulseLink)
            disable(pm, unifiedBeacon)
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
