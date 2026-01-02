package com.RingerSong.free.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.ContactsContract
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.RingerSong.free.R
import android.util.Log
import com.RingerSong.free.data.AppStateStore
import com.RingerSong.free.data.ProgressionEngine
import com.RingerSong.free.data.SegmentPlay
import com.RingerSong.free.data.SpotifyRemoteManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RingerPlaybackService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var player: MediaPlayer? = null
    private var stopJob: Job? = null
    private var isSpotifyPlaying = false
    private var spotifyAppRemote: com.spotify.android.appremote.api.SpotifyAppRemote? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var originalRingVolume: Int = -1
    private lateinit var prefs: SharedPreferences

    companion object {
        private const val TAG = "RingerPlayback"
        const val ACTION_PLAY_SEGMENT = "com.RingerSong.free.ACTION_PLAY_SEGMENT"
        const val ACTION_STOP_PLAYBACK = "com.RingerSong.free.ACTION_STOP_PLAYBACK"
        const val EXTRA_PHONE_NUMBER = "phone_number"
        private const val NOTIFICATION_ID = 9002
        private const val CHANNEL_ID = "ringer_playback"
        private const val PREFS_NAME = "ringer_prefs"
        private const val KEY_ORIGINAL_VOLUME = "original_ring_volume"
        private const val SPOTIFY_SEEK_DELAY_MS = 500L
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Failsafe: Check if we crashed while muted and restore volume
        val savedVolume = prefs.getInt(KEY_ORIGINAL_VOLUME, -1)
        if (savedVolume != -1) {
            Log.w(TAG, "Found saved volume $savedVolume from previous crash. Restoring...")
            originalRingVolume = savedVolume
            restoreSystemRinger()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action}")
        when (intent?.action) {
            ACTION_PLAY_SEGMENT -> {
                val number = intent.getStringExtra(EXTRA_PHONE_NUMBER)
                Log.d(TAG, "ACTION_PLAY_SEGMENT for number: $number")
                startForeground(NOTIFICATION_ID, buildNotification("Playing your progression segment"))
                scope.launch {
                    playNextSegment(number)
                }
            }
            ACTION_STOP_PLAYBACK -> {
                Log.d(TAG, "ACTION_STOP_PLAYBACK")
                stopPlayback()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopPlayback()
        scope.cancel() // Prevent memory leaks
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun playNextSegment(phoneNumber: String?) {
        Log.d(TAG, "playNextSegment for phoneNumber: $phoneNumber")
        val store = AppStateStore(this)
        val contactId = phoneNumber?.let { lookupContactId(it) }
        Log.d(TAG, "Contact ID: $contactId")

        val current = store.stateFlow.first()
        Log.d(TAG, "Current state - songs: ${current.songs.size}, enabled: ${current.settings.enabled}")

        val callerKey = contactId ?: phoneNumber
        val (updated, segment) = ProgressionEngine.advance(current, contactId, callerKey)
        store.update { updated }

        if (segment == null) {
            Log.w(TAG, "No segment to play - stopping")
            stopPlayback()
            return
        }

        Log.d(TAG, "Playing segment: song=${segment.song.title}, uri=${segment.song.uri}, start=${segment.startMs}, duration=${segment.durationMs}")
        playSegment(segment)
    }

    private fun requestAudioFocus(): Boolean {
        val manager = audioManager ?: run {
            Log.e(TAG, "AudioManager is null!")
            return false
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA) // Consistent with playback
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setWillPauseWhenDucked(false)
                .build()
            audioFocusRequest = request
            manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC, // Request focus for MEDIA stream
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }

        Log.d(TAG, "Audio focus request result: $result")
        return result
    }

    private fun abandonAudioFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { manager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(null)
        }
    }

    private fun muteSystemRinger() {
        val manager = audioManager ?: return
        try {
            if (originalRingVolume == -1) {
                originalRingVolume = manager.getStreamVolume(AudioManager.STREAM_RING)
                // Persist to SharedPreferences for crash recovery
                prefs.edit().putInt(KEY_ORIGINAL_VOLUME, originalRingVolume).apply()
            }
            // Attempt to mute the ringer stream to prevent the default ringtone from playing over the music
            manager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
            Log.d(TAG, "Muted system ringer (Original volume: $originalRingVolume)")
        } catch (e: SecurityException) {
            Log.e(TAG, "Could not mute ringer - Do Not Disturb policy access required or permission missing", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error muting ringer", e)
        }
    }

    private fun restoreSystemRinger() {
        val manager = audioManager ?: return
        if (originalRingVolume != -1) {
            try {
                manager.setStreamVolume(AudioManager.STREAM_RING, originalRingVolume, 0)
                Log.d(TAG, "Restored system ringer to volume: $originalRingVolume")
                originalRingVolume = -1
                // Clear from SharedPreferences
                prefs.edit().remove(KEY_ORIGINAL_VOLUME).apply()
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring ringer volume", e)
            }
        }
    }

    private fun playSegment(segment: SegmentPlay) {
        Log.d(TAG, "playSegment called for URI: ${segment.song.uri}")
        stopPlayback()

        // Mute the system ringer immediately to prevent conflict
        muteSystemRinger()

        // Request audio focus to try to duck/stop other media/system sounds
        if (!requestAudioFocus()) {
            Log.w(TAG, "Failed to get audio focus, but continuing anyway")
        }

        if (segment.song.uri.startsWith("spotify:")) {
            Log.d(TAG, "Spotify URI detected - Using Spotify App Remote")

            // DIRECT STREAMING: Skip download check and use Spotify App Remote directly.
            // This approach prioritizes streaming over file downloads, relying on the user's
            // active Spotify session. If the user wants offline support, they would need to
            // pre-download tracks, but this feature enforces streaming for better user experience.
            scope.launch {
                runCatching {
                    val remote = SpotifyRemoteManager.connect(this@RingerPlaybackService)
                    spotifyAppRemote = remote
                    remote.playerApi.play(segment.song.uri)
                    // Seek after a short delay to ensure playback has initialized.
                    // We need a delay because the Spotify app needs time to start the track before seeking works reliably.
                    delay(SPOTIFY_SEEK_DELAY_MS)
                    remote.playerApi.seekTo(segment.startMs)
                    isSpotifyPlaying = true
                    Log.d(TAG, "Spotify App Remote playback started")
                }.onFailure { e ->
                    Log.e(TAG, "Spotify App Remote failed", e)
                    // Provide specific user feedback about the failure reason
                    val errorMessage = when {
                        e.message?.contains("not installed", ignoreCase = true) == true ->
                            "Spotify app not installed"
                        e.message?.contains("not logged in", ignoreCase = true) == true ->
                            "Not logged into Spotify"
                        e.message?.contains("premium", ignoreCase = true) == true ->
                            "Spotify Premium required for playback"
                        else -> "Spotify playback failed. Please check app & premium status."
                    }
                    updateNotification(errorMessage)
                    stopPlayback()
                }
            }
        } else if (segment.song.uri.startsWith("youtube:")) {
            Log.w(TAG, "YouTube URI not supported yet")
            stopPlayback()
        } else {
            // Local file playback (for non-Spotify URIs)
            Log.d(TAG, "Local file URI: ${segment.song.uri}")
            val uri = Uri.parse(segment.song.uri)
            val mediaPlayer = MediaPlayer()
            player = mediaPlayer
            runCatching {
                mediaPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA) // Use MEDIA for music, not RINGTONE, as we muted RINGTONE
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                mediaPlayer.setDataSource(this, uri)
                Log.d(TAG, "Preparing MediaPlayer")
                mediaPlayer.prepare()
                mediaPlayer.seekTo(segment.startMs.toInt())
                mediaPlayer.start()
                Log.d(TAG, "MediaPlayer started successfully")
            }.onFailure { e ->
                Log.e(TAG, "MediaPlayer failed", e)
                stopPlayback()
                return
            }

            mediaPlayer.setOnCompletionListener {
                Log.d(TAG, "MediaPlayer completed")
                stopPlayback()
            }
        }

        // TODO: Consider monitoring Spotify playback state with PlayerStateCallback
        // to enforce duration limits more reliably and handle unexpected track changes
        stopJob = scope.launch {
            delay(segment.durationMs)
            Log.d(TAG, "Segment duration elapsed, stopping")
            stopPlayback()
        }
    }

    private fun stopPlayback() {
        abandonAudioFocus()
        restoreSystemRinger() // Restore ringer volume!

        stopJob?.cancel()
        stopJob = null
        player?.runCatching {
            stop()
            release()
        }
        player = null

        if (isSpotifyPlaying) {
             scope.launch {
                runCatching {
                    // Check if we're already connected before reconnecting to prevent memory leaks
                    if (spotifyAppRemote?.isConnected == true) {
                        spotifyAppRemote?.playerApi?.pause()
                    } else {
                        // Only reconnect if needed
                        val remote = SpotifyRemoteManager.connect(this@RingerPlaybackService)
                        remote.playerApi.pause()
                    }
                }
            }
            isSpotifyPlaying = false
            spotifyAppRemote = null
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification(message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(message))
    }

    private fun lookupContactId(phoneNumber: String): String? {
        val permission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_CONTACTS
        )
        if (permission != PackageManager.PERMISSION_GRANTED) return null
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup._ID),
            null,
            null,
            null
        ).use { cursor ->
            return if (cursor != null && cursor.moveToFirst()) {
                cursor.getString(0)
            } else {
                null
            }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "RingerSong playback",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(message: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }
}
