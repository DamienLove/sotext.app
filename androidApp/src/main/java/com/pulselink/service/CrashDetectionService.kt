package com.pulselink.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.pulselink.R
import com.pulselink.domain.model.EscalationTier
import com.pulselink.ui.EmergencyPopupActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.sqrt

@AndroidEntryPoint
class CrashDetectionService : Service(), SensorEventListener, LocationListener {

    @Inject
    lateinit var sensorManager: SensorManager

    private lateinit var locationManager: LocationManager
    private var accelerometer: Sensor? = null
    private var lastCrashTimestamp = 0L

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        startForeground(NOTIFICATION_ID, createNotification())

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                // Request location to justify foreground service type "location" and context
                locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 1000L, 10f, this)
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                     locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 20f, this)
                }
            } catch (e: SecurityException) {
                // Ignore
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        locationManager.removeUpdates(this)
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

    // LocationListener methods
    override fun onLocationChanged(location: Location) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

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
            .setFullScreenIntent(pendingIntent, true)
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
