package com.pulselink.ui.branding

import com.pulselink.R

/**
 * Returns the appropriate logo based on whether pro/premium branding should be shown.
 * Pro branding is used for Pro builds and for Free users with an active premium subscription.
 */
fun brandLogoRes(useProBranding: Boolean): Int =
    if (useProBranding) R.drawable.ic_pulselink_pro else R.drawable.ic_logo
