package com.pulselink.beacon.data

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ThemeSyncManager(private val repository: ThemePreferencesRepository) {
    private var registration: ListenerRegistration? = null
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    fun startSync(scope: CoroutineScope) {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            registration?.remove()
            if (user != null) {
                registration = db.collection("users").document(user.uid)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.w("ThemeSync", "Listen failed.", e)
                            return@addSnapshotListener
                        }
                        if (snapshot != null && snapshot.exists()) {
                            val data = snapshot.data
                            val themePrefs = data?.get("themePreferences") as? Map<String, Any>
                            if (themePrefs != null) {
                                scope.launch(Dispatchers.IO) {
                                    val palette = mapToThemePalette(themePrefs)
                                    repository.saveGlobal(palette)
                                }
                            }
                        }
                    }
            }
        }
    }

    private fun mapToThemePalette(map: Map<String, Any>): ThemePalette {
        val default = ThemePalette.default()
        
        fun parseColor(key: String, defaultVal: Long): Long {
            val hex = map[key] as? String ?: return defaultVal
            return try {
                android.graphics.Color.parseColor(hex).toLong()
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                defaultVal
            }
        }

        fun parseFloat(key: String, defaultVal: Float): Float {
            val value = map[key]
            return when (value) {
                is Number -> value.toFloat()
                is String -> value.toFloatOrNull() ?: defaultVal
                else -> defaultVal
            }
        }
        
        val webFont = map["fontStyle"] as? String
        val font = when(webFont) {
            "Serif" -> ThemeFont.Serif
            "Cursive" -> ThemeFont.Rounded // Mapping Cursive to Rounded (closest match)
            "Monospace" -> ThemeFont.Mono
            else -> ThemeFont.System
        }

        val webIcon = map["inboxIconVariant"] as? String
        // Clean up web icon names to match enum names if possible
        // Web uses: "default_light", "midnight_oled", etc. usually.
        // But the definition in App.jsx shows enum-like strings for *variant*?
        // Actually App.jsx presets have: "inboxIconVariant": "midnight_oled"
        // But ThemeModels.kt has: Beacon, Bubble, Shield, Minimal.
        // I might need a robust mapper or just default.
        // Let's rely on name matching if possible, or default.
        val iconVariant = InboxIconVariant.values().firstOrNull { 
            it.name.equals(webIcon, ignoreCase = true) || it.label.equals(webIcon, ignoreCase = true)
        } ?: InboxIconVariant.Beacon

        return default.copy(
            accent = parseColor("primaryColor", default.accent),
            outgoing = parseColor("bubbleOutgoing", default.outgoing),
            incoming = parseColor("bubbleIncoming", default.incoming),
            inboxBackground = parseColor("backgroundColor", default.inboxBackground),
            threadBackground = parseColor("backgroundColor", default.threadBackground), // Sync thread bg with app bg
            frame = parseColor("topBarColor", default.frame),
            bubbleRadius = parseFloat("bubbleCornerRadius", default.bubbleRadius),
            font = font,
            iconVariant = iconVariant
        )
    }
}
