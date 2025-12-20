package com.pulselink.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Secondary launcher that jumps straight into the SMS inbox (Beacon).
 * Visibility is controlled at runtime via PackageManager to show/hide the icon.
 */
class InboxLauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Always launch the dedicated Beacon inbox task; never route through PulseLink screens.
        val intent = Intent(this, BeaconInboxActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        finish()
    }
}

