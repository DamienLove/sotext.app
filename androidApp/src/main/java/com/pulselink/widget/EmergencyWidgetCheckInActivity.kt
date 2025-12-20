package com.pulselink.widget

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.pulselink.ui.AssistantShortcutActivity

class EmergencyWidgetCheckInActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val forwardIntent = Intent(this, AssistantShortcutActivity::class.java).apply {
            action = AssistantShortcutActivity.ACTION_ASSISTANT_CHECK_IN
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(forwardIntent)
        finish()
    }
}
