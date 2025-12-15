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
        // Always launch a fresh task straight into the inbox to avoid landing on Home/Trusted Contacts.
        val inboxIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                Intent.FLAG_ACTIVITY_TASK_ON_HOME
            putExtra("open_sms_inbox", true)
        }
        startActivity(inboxIntent)
        finish()
    }
}
