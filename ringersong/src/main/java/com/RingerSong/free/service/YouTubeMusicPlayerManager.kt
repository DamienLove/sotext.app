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
class YouTubeMusicPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val webViewPlayer: WebViewPlayer
) {
    companion object {
        private const val TAG = "YouTubeMusicPlayer"
        private const val YOUTUBE_MUSIC_PACKAGE = "com.google.android.apps.youtube.music"
    }

    /**
     * Attempts to play a YouTube Music track.
     * Tries to use a hidden WebView first (if permission granted),
     * otherwise falls back to launching the app via deep link.
     */
    suspend fun playTrack(song: SongEntry): Boolean = withContext(Dispatchers.Main) {
        try {
            // Extract video ID from uri "youtube:video:VIDEO_ID"
            val videoId = song.uri.removePrefix("youtube:video:")

            // Check for overlay permission
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
                android.provider.Settings.canDrawOverlays(context)) {

                Log.d(TAG, "Overlay permission granted. Using WebViewPlayer.")
                // https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1
                val embedUrl = "https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1"
                webViewPlayer.play(embedUrl)
                return@withContext true
            }

            Log.d(TAG, "Overlay permission missing. Falling back to Intent.")

            // Construct Deep Link
            val uri = Uri.parse("https://music.youtube.com/watch?v=$videoId")

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(YOUTUBE_MUSIC_PACKAGE)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val packageManager = context.packageManager
            if (intent.resolveActivity(packageManager) != null) {
                context.startActivity(intent)
                Log.d(TAG, "YouTube Music intent launched")
                return@withContext true
            } else {
                Log.e(TAG, "YouTube Music app not installed")
                return@withContext false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error playing YouTube Music", e)
            return@withContext false
        }
    }

    suspend fun stop() {
        webViewPlayer.stop()
    }
}
