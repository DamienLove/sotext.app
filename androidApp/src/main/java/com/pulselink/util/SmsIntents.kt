package com.pulselink.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast

/**
 * Launches the platform SMS/MMS chooser with an attachment for the given address.
 * Used by compose/new-message and in-thread attachment flows.
 */
fun sendAttachmentViaSms(
    context: Context,
    address: String,
    uri: Uri
): Boolean {
    val mimeType = context.contentResolver.getType(uri) ?: "*/*"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        data = Uri.parse("smsto:$address")
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra("address", address)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val resolved = context.packageManager.queryIntentActivities(
        intent,
        PackageManager.MATCH_DEFAULT_ONLY
    )
    resolved.forEach { resolveInfo ->
        context.grantUriPermission(
            resolveInfo.activityInfo.packageName,
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }

    return try {
        context.startActivity(Intent.createChooser(intent, "Send attachment via SMS/MMS"))
        true
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            context,
            "No messaging app found to send attachments",
            Toast.LENGTH_LONG
        ).show()
        false
    }
}
