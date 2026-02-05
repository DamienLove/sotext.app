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
class AppleMusicPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val webViewPlayer: WebViewPlayer
) {
    companion object {
        private const val TAG = "AppleMusicPlayer"
        private const val APPLE_MUSIC_PACKAGE = "com.apple.android.music"
    }

    suspend fun playTrack(song: SongEntry): Boolean = withContext(Dispatchers.Main) {
        try {
            val uriString = song.uri

            // Normalize URI to a valid URL string if possible
            val fullUrl = if (uriString.all { it.isDigit() }) {
                "https://music.apple.com/us/song/$uriString"
            } else {
                uriString
            }

            // Check for overlay permission
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
                android.provider.Settings.canDrawOverlays(context)) {

                Log.d(TAG, "Overlay permission granted. Using WebViewPlayer.")

                // Convert to embed URL for better web playback support
                val embedUrl = if (fullUrl.contains("music.apple.com")) {
                    fullUrl.replace("music.apple.com", "embed.music.apple.com")
                } else {
                    fullUrl
                }

                webViewPlayer.play(embedUrl)
                return@withContext true
            }

            Log.d(TAG, "Overlay permission missing. Falling back to Intent.")

            // Use the normalized URL for parsing Uri
            val uri = Uri.parse(fullUrl)

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(APPLE_MUSIC_PACKAGE)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val packageManager = context.packageManager
            if (intent.resolveActivity(packageManager) != null) {
                context.startActivity(intent)
                Log.d(TAG, "Apple Music intent launched")
                return@withContext true
            } else {
                Log.e(TAG, "Apple Music not installed")
                return@withContext false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error playing Apple Music", e)
            return@withContext false
        }
    }

    suspend fun stop() {
        webViewPlayer.stop()
    }
}
