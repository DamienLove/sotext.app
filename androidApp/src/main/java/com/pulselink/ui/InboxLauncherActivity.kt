package com.pulselink.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Launcher shortcut that opens the SMS inbox inside PulseLink when enabled
 * for pro/premium tiers.
 */
class InboxLauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("open_sms_inbox", true)
        }
        startActivity(intent)
        finish()
    }
}
