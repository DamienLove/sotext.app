package com.pulselink.data.contacts

import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceContact(
    val id: Long,
    val displayName: String,
    val phoneNumber: String
)

@Singleton
class DeviceContactsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun listPhoneContacts(limit: Int = 500): List<DeviceContact> {
        if (!hasContactsPermission()) return emptyList()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val cursor = runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                "${ContactsContract.CommonDataKinds.Phone.NUMBER} IS NOT NULL",
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )
        }.getOrNull() ?: return emptyList()

        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val results = ArrayList<DeviceContact>()
            val seen = HashSet<String>()
            while (c.moveToNext() && results.size < limit) {
                val id = c.getLong(idIdx)
                val name = c.getString(nameIdx) ?: ""
                val number = c.getString(numberIdx) ?: ""
                if (number.isBlank()) continue
                val key = "${name.trim()}|${normalize(number)}"
                if (seen.add(key)) {
                    results.add(DeviceContact(id, name, number))
                }
            }
            return results
        }
    }

    private fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

    private fun normalize(input: String): String {
        if (input.isBlank()) return ""
        val digits = buildString {
            input.forEach { ch ->
                if (ch.isDigit()) append(ch)
            }
        }
        return if (input.trim().startsWith("+")) "+$digits" else digits
    }
}
