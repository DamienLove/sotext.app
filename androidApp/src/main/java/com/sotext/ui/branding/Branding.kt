package com.sotext.ui.branding

import com.sotext.R

/**
 * Returns the appropriate logo based on whether premium/pro branding should be shown.
 * Gold logo is used for Pro builds and for users with an active premium or pro unlock.
 */
fun brandLogoRes(usePremiumBranding: Boolean): Int =
    brandLogoRes(usePremiumBranding, isUnifiedMode = false)

/**
 * Returns the appropriate logo based on branding mode and unified experience setting.
 * - Unified mode: Returns unified_logo regardless of premium status
 * - Premium/Pro mode: Returns ic_pulselink_pro
 * - Default: Returns standard ic_logo
 */
fun brandLogoRes(usePremiumBranding: Boolean, isUnifiedMode: Boolean): Int = when {
    isUnifiedMode -> R.drawable.unified_logo
    usePremiumBranding -> R.drawable.ic_pulselink_pro
    else -> R.drawable.ic_logo
}

fun pulseBrandName(isPremium: Boolean, isPro: Boolean): String = when {
    isPremium -> "SoText Premium"
    isPro -> "SoText Pro"
    else -> "SoText"
}

fun beaconBrandName(isPremium: Boolean, isPro: Boolean): String = when {
    isPremium -> "SoText Premium"
    isPro -> "SoText Pro"
    else -> "SoText"
}

/**
 * Returns the unified brand name when merged experience is enabled.
 * Format: "Beacon · SoText" with appropriate premium/pro suffix
 */
fun unifiedBrandName(isPremium: Boolean, isPro: Boolean): String = when {
    isPremium -> "Beacon · SoText Premium"
    isPro -> "Beacon · SoText Pro"
    else -> "Beacon · SoText"
}
