package com.RingerSong.free.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.RingerSong.free.data.SongEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TidalPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "TidalPlayerManager"
        private const val TIDAL_PACKAGE = "com.aspiro.tidal"
    }

    /**
     * Attempts to play a Tidal track by launching the app with a deep link.
     */
    suspend fun playTrack(song: SongEntry): Boolean = withContext(Dispatchers.Main) {
        try {
            // Song URI is expected to be a Tidal URL or URI
            // e.g., "https://tidal.com/track/12345" or "tidal://track/12345"
            // or maybe "tidal:track:12345" if we normalize it

            var uriString = song.uri

            // Basic cleanup/normalization if needed
            if (!uriString.startsWith("http") && !uriString.startsWith("tidal")) {
                // If just ID?
                uriString = "https://tidal.com/track/$uriString"
            }

            val uri = Uri.parse(uriString)

            Log.d(TAG, "Attempting to launch Tidal with URI: $uri")

            // Check for overlay permission
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
                !android.provider.Settings.canDrawOverlays(context)) {
                Log.w(TAG, "Cannot launch Tidal: Missing Overlay Permission")
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(TIDAL_PACKAGE)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val packageManager = context.packageManager
            if (intent.resolveActivity(packageManager) != null) {
                context.startActivity(intent)
                Log.d(TAG, "Tidal intent launched")
                return@withContext true
            } else {
                Log.e(TAG, "Tidal app not installed")
                // Fallback: try removing package and letting system resolve (might open browser)
                // But we want Ringer functionality, opening browser might be annoying.
                // However, user asked to "activate users paid memberships", so app is preferred.
                return@withContext false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error launching Tidal", e)
            return@withContext false
        }
    }
}
