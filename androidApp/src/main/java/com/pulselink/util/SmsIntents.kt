package com.pulselink.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsManager
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Sends an MMS attachment directly via SmsManager so the user stays in-app.
 * Requires default SMS role + SEND_SMS permission.
 */
fun sendAttachmentViaSms(
    context: Context,
    address: String,
    uri: Uri
): Boolean {
    // Must be default SMS app to send MMS
    val defaultPackage = Telephony.Sms.getDefaultSmsPackage(context)
    if (defaultPackage != null && defaultPackage != context.packageName) {
        Toast.makeText(
            context,
            "Set PulseLink as default SMS to send attachments",
            Toast.LENGTH_LONG
        ).show()
        return false
    }

    val resolver = context.contentResolver
    val mimeType = resolver.getType(uri) ?: "*/*"

    // Copy into our FileProvider scope so the Mms service can read it
    val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
    val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)?.ifBlank { "bin" } ?: "bin"
    val destFile = File(exportDir, "mms_${UUID.randomUUID()}.$ext")
    runCatching {
        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output -> input.copyTo(output) }
        }
    }.onFailure {
        Toast.makeText(context, "Unable to read attachment", Toast.LENGTH_LONG).show()
        return false
    }
    destFile.deleteOnExit()

    val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.export", destFile)

    // Grant Telephony stack permission to read the file
    listOf(
        "com.android.mms.service",
        "com.android.providers.telephony",
        context.packageName
    ).forEach { pkg ->
        try {
            context.grantUriPermission(pkg, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // best-effort; some packages may not exist on OEM builds
        }
    }

    val smsManager = SmsManager.getDefault()
    val config = Bundle().apply { putString("address", address) }

    return runCatching {
        smsManager.sendMultimediaMessage(context, contentUri, null, config, null)
        Toast.makeText(context, "Sending attachment…", Toast.LENGTH_SHORT).show()
        true
    }.getOrElse { error ->
        Toast.makeText(
            context,
            "Failed to send attachment: ${error.message ?: "unknown error"}",
            Toast.LENGTH_LONG
        ).show()
        false
    }
}
