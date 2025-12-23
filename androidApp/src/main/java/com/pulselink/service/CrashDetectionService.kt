package com.pulselink.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pulselink.R
import com.pulselink.domain.model.EscalationTier
import com.pulselink.ui.EmergencyPopupActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.sqrt

@AndroidEntryPoint
class CrashDetectionService : Service(), SensorEventListener {

    @Inject
    lateinit var sensorManager: SensorManager

    private var accelerometer: Sensor? = null
    private var lastCrashTimestamp = 0L

    override fun onCreate() {
        super.onCreate()
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        startForeground(NOTIFICATION_ID, createNotification())

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]

                val gForce = sqrt(x.toDouble() * x.toDouble() + y.toDouble() * y.toDouble() + z.toDouble() * z.toDouble()) / 9.80665

                if (gForce > CRASH_THRESHOLD_G && (System.currentTimeMillis() - lastCrashTimestamp > 5000)) {
                    lastCrashTimestamp = System.currentTimeMillis()
                    triggerCrashAlert()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun triggerCrashAlert() {
        val intent = EmergencyPopupActivity.newIntent(
            this,
            "Crash Detected",
            EscalationTier.EMERGENCY.name
        ).apply {
            putExtra("EXTRA_TRIGGER_SOURCE", "CRASH")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "crash_alert_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Crash Alerts", NotificationManager.IMPORTANCE_HIGH)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle("CRASH DETECTED")
            .setContentText("Tap to cancel alert")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID_ALERT, notification)
    }

    private fun createNotification(): Notification {
        val channelId = "crash_detection_channel"
        val channelName = "Crash Detection"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Safety Sensors Active")
            .setContentText("Monitoring for potential accidents")
            .setSmallIcon(R.drawable.ic_logo)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_ID_ALERT = 1002
        private const val CRASH_THRESHOLD_G = 4.0
    }
}
