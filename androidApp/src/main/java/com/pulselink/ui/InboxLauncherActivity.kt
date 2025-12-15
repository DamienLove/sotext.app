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
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                Intent.FLAG_ACTIVITY_TASK_ON_HOME
            putExtra("open_sms_inbox", true)
        }
        startActivity(intent)
        finish()
    }
}

