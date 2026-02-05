package com.pulselink.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.File

class MmsSentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        if (filePath != null) {
            val file = File(filePath)
            if (file.exists()) {
                val deleted = file.delete()
                if (deleted) {
                    Log.d(TAG, "MMS temporary file deleted: $filePath")
                } else {
                    Log.w(TAG, "Failed to delete MMS temporary file: $filePath")
                }
            }
        }
    }

    companion object {
        const val TAG = "MmsSentReceiver"
        const val EXTRA_FILE_PATH = "com.pulselink.data.sms.EXTRA_FILE_PATH"
    }
}
