package com.sotext.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

/**
 * Switches the app's own home-screen launcher icon between the color options offered in
 * Settings, each backed by its own manifest activity-alias (com.sotext.AppIcon*), plus the
 * default com.sotext.PulseLinkLauncher. Exactly one of these is enabled at a time.
 *
 * Gold/Steel/Iridescent/Rainbow/Bronze are wired with real PNG art. Matte Black/Reverse/Glass/
 * Sapphire/Crimson had hand-drawn vector placeholders that were pulled pending real PNG art;
 * their manifest aliases were removed too, so any variant key below without a matching alias
 * falls through to [apply]'s default via `variantMap[variant] == null` - safe even if a device
 * still has one of those old variant strings persisted in settings from before this cleanup.
 *
 * Distinct from [BeaconIconManager], which switches the separate, optional Beacon-inbox
 * shortcut icon - the two never touch the same components.
 */
object AppIconManager {
    private const val TAG = "AppIconManager"

    private val variantMap = mapOf(
        "gold" to "com.sotext.AppIconGold",
        "steel" to "com.sotext.AppIconSteel",
        "iridescent" to "com.sotext.AppIconIridescent",
        "rainbow" to "com.sotext.AppIconRainbow",
        "bronze" to "com.sotext.AppIconBronze"
    )

    /**
     * @param variant one of [variantMap]'s keys, or "default"/anything else for the standard
     * PulseLinkLauncher icon.
     * @param unifiedModeActive if true, skip entirely - UnifiedLauncherManager owns launcher
     * icon state while unified navigation is on (it explicitly disables PulseLinkLauncher, and
     * every AppIcon* alias targets the same MainActivity that PulseLinkLauncher does, so
     * fighting over enabled state here would risk showing two home-screen icons at once).
     */
    fun apply(context: Context, variant: String, unifiedModeActive: Boolean = false) {
        if (unifiedModeActive) {
            Log.d(TAG, "Skipping app icon management - unified mode is active")
            return
        }

        val pm = context.packageManager
        val pkg = context.packageName
        val defaultComp = ComponentName(pkg, "com.sotext.PulseLinkLauncher")
        val variantComps = variantMap.values.map { ComponentName(pkg, it) }
        val allComps = listOf(defaultComp) + variantComps

        val target = variantMap[variant]?.let { ComponentName(pkg, it) } ?: defaultComp

        Log.d(TAG, "Enabling app icon variant: $variant (${target.className})")
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
