package com.pulselink.util

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultSmsHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isDefaultSms(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true) {
                return true
            }
        }
        val current = Telephony.Sms.getDefaultSmsPackage(context)
        return current == context.packageName
    }

    suspend fun checkDefaultSmsWithRetry(maxAttempts: Int = 5, initialDelayMs: Long = 300): Boolean {
        var currentDelay = initialDelayMs
        repeat(maxAttempts) { attempt ->
            if (isDefaultSms()) {
                return true
            }
            android.util.Log.d("DefaultSmsHelper", "Default SMS check attempt ${attempt + 1} failed. Retrying in ${currentDelay}ms...")
            kotlinx.coroutines.delay(currentDelay)
            currentDelay *= 2
        }
        return isDefaultSms()
    }

    fun buildRoleRequestIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager?.isRoleAvailable(RoleManager.ROLE_SMS) == true) {
                roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
            } else {
                null
            }
        } else {
            Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
            }
        }
    }
}
