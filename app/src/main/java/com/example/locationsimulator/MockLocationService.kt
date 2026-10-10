package com.example.locationsimulator

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat

class MockLocationService : Service() {

    companion object {
        const val ACTION_START =
            "com.example.locationsimulator.START"

        const val ACTION_STOP =
            "com.example.locationsimulator.STOP"

        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"

        private const val CHANNEL_ID = "gps_simulation"
        private const val NOTIFICATION_ID = 1001
        private const val UPDATE_INTERVAL_MS = 1000L
        private const val TAG = "MockLocationService"
    }

    private lateinit var locationManager: LocationManager

    private val handler = Handler(Looper.getMainLooper())

    private var latitude = -6.1754
    private var longitude = 106.8272
    private var simulationRunning = false

    private val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER
    )

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!simulationRunning) return

            try {
                updateMockLocation()

                handler.postDelayed(
                    this,
                    UPDATE_INTERVAL_MS
                )
            } catch (e: Exception) {
                Log.e(TAG, "Gagal memperbarui lokasi", e)

                reportError(
                    e.message ?: "Gagal memperbarui lokasi"
                )

                stopSimulation()
                stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        locationManager = getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager

        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_START -> {
                val newLatitude = intent.getDoubleExtra(
                    EXTRA_LATITUDE,
                    Double.NaN
                )

                val newLongitude = intent.getDoubleExtra(
                    EXTRA_LONGITUDE,
                    Double.NaN
                )

                if (
                    !newLatitude.isFinite() ||
                    !newLongitude.isFinite() ||
                    newLatitude !in -90.0..90.0 ||
                    newLongitude !in -180.0..180.0
                ) {
                    reportError("Koordinat tidak valid")
                    stopSimulation()
                    stopSelf()
                    return START_NOT_STICKY
                }

                latitude = newLatitude
                longitude = newLongitude

                try {
                    startForegroundNotification()
                    startSimulation()
                } catch (e: Exception) {
                    Log.e(TAG, "Gagal memulai simulasi", e)

                    reportError(
                        e.message ?: "Gagal memulai simulasi"
                    )

                    stopSimulation()
                    stopSelf()
                }
            }

            ACTION_STOP -> {
                stopSimulation()
                stopSelf()
            }

            else -> {
                stopSimulation()
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startSimulation() {
        handler.removeCallbacks(updateRunnable)

        if (!simulationRunning) {
            providers.forEach { provider ->
                try {
                    addMockProvider(provider)
                } catch (e: IllegalArgumentException) {
                    Log.d(
                        TAG,
                        "Test provider sudah tersedia: $provider",
                        e
                    )
                }

                locationManager.setTestProviderEnabled(
                    provider,
                    true
                )
            }
        }

        updateMockLocation()

        simulationRunning = true

        // Status aktif baru dikirim setelah
        // lokasi berhasil diperbarui.
        SimulationState.setRunning(
            true,
            latitude,
            longitude
        )

        handler.postDelayed(
            updateRunnable,
            UPDATE_INTERVAL_MS
        )

        Log.d(
            TAG,
            "Simulasi aktif: $latitude, $longitude"
        )
    }

    @Suppress("DEPRECATION")
    private fun addMockProvider(provider: String) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            val properties = ProviderProperties.Builder()
                .setHasAltitudeSupport(true)
                .setHasSpeedSupport(true)
                .setHasBearingSupport(true)
                .setPowerUsage(
                    ProviderProperties.POWER_USAGE_LOW
                )
                .setAccuracy(
                    ProviderProperties.ACCURACY_FINE
                )
                .build()

            locationManager.addTestProvider(
                provider,
                properties
            )

        } else {

            locationManager.addTestProvider(
                provider,
                false,
                false,
                false,
                false,
                true,
                true,
                true,
                ProviderProperties.POWER_USAGE_LOW,
                ProviderProperties.ACCURACY_FINE
            )
        }
    }

    private fun updateMockLocation() {
        providers.forEach { provider ->

            val location = Location(provider).apply {
                latitude = this@MockLocationService.latitude
                longitude = this@MockLocationService.longitude

                accuracy = 3f
                altitude = 0.0
                time = System.currentTimeMillis()

                elapsedRealtimeNanos =
                    SystemClock.elapsedRealtimeNanos()
            }

            locationManager.setTestProviderLocation(
                provider,
                location
            )
        }
    }

    private fun reportError(message: String) {
        SimulationState.setError(message)
        Log.e(TAG, message)
    }

    private fun stopSimulation() {
        simulationRunning = false

        handler.removeCallbacks(updateRunnable)

        if (::locationManager.isInitialized) {
            providers.forEach { provider ->
                try {
                    locationManager.removeTestProvider(provider)
                } catch (e: Exception) {
                    Log.d(
                        TAG,
                        "Provider tidak perlu dibersihkan: $provider",
                        e
                    )
                }
            }
        }

        // Beri tahu UI bahwa layanan berhenti.
        SimulationState.setRunning(
            false,
            latitude,
            longitude
        )

        stopForeground(STOP_FOREGROUND_REMOVE)

        Log.d(TAG, "Simulasi berhenti")
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "GPS Simulation",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description =
                "Menampilkan status simulasi lokasi Android"
        }

        val manager = getSystemService(
            NotificationManager::class.java
        )

        manager.createNotificationChannel(channel)
    }

    private fun startForegroundNotification() {
        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }
    }

    private fun createNotification(): Notification {

        val openAppIntent = Intent(
            this,
            MainActivity::class.java
        )

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(
            this,
            MockLocationService::class.java
        ).apply {
            action = ACTION_STOP
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setSmallIcon(
                android.R.drawable.ic_menu_mylocation
            )
            .setContentTitle("GPS Simulation Active")
            .setContentText(
                "Lat: $latitude, Lon: $longitude"
            )
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop Simulation",
                stopPendingIntent
            )
            .build()
    }

    override fun onDestroy() {
        stopSimulation()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
