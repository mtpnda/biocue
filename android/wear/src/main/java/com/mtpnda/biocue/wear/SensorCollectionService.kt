package com.mtpnda.biocue.wear

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.mtpnda.biocue.wear.link.BleLinkServer
import com.mtpnda.biocue.wear.link.PhoneTarget
import com.mtpnda.biocue.wear.link.PhoneTargetStore
import com.mtpnda.biocue.wear.location.LocationTracker

class SensorCollectionService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var dataSender: WearDataSender

    /** Expose the sender's reachability state so the Activity can observe it. */
    val isPhoneReachable: StateFlow<Boolean>
        get() = dataSender.isPhoneReachable

    inner class LocalBinder : Binder() {
        val service: SensorCollectionService get() = this@SensorCollectionService
    }
    private val binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        dataSender = WearDataSender(this)
        createNotificationChannel()

        // iPhone link: the phone can ask for a GPS fix during an SOS; answer over the same link.
        // (Harmless for Android-phone users: the link is never started, so this never fires.)
        BleLinkServer.get(this).onSosGpsRequest = {
            scope.launch {
                LocationTracker(this@SensorCollectionService).getCurrentLocation()?.let {
                    dataSender.sendSosLocation(it.latitude, it.longitude)
                }
            }
        }
        if (PhoneTargetStore(this).get() == PhoneTarget.IPHONE) BleLinkServer.get(this).start()

        val filter = android.content.IntentFilter("com.mtpnda.biocue.wear.SET_TEST_MODE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val flags = if (BuildConfig.DEBUG) android.content.Context.RECEIVER_EXPORTED else android.content.Context.RECEIVER_NOT_EXPORTED
            registerReceiver(testModeReceiver, filter, flags)
        } else {
            registerReceiver(testModeReceiver, filter)
        }
    }

    private val testModeReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == "com.mtpnda.biocue.wear.SET_TEST_MODE") {
                val enable = intent.getBooleanExtra("enabled", true)
                setTestMode(enable)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = Notification.Builder(this, "sensor_channel")
            .setContentTitle("BioCue")
            .setContentText("Monitoring vitals")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
            
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val hasSensors = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.BODY_SENSORS) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                            androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (hasSensors) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
            } else {
                startForeground(1, notification)
            }
        } else {
            startForeground(1, notification)
        }
        startSensorCollection()
        
        return START_STICKY
    }

    private var syntheticJob: Job? = null
    private val _isTestModeEnabled = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isTestModeActive: StateFlow<Boolean> = _isTestModeEnabled.asStateFlow()

    fun setTestMode(enabled: Boolean) {
        _isTestModeEnabled.value = enabled
        if (enabled && syntheticJob == null) {
            startSyntheticDataLoop()
        } else if (!enabled && syntheticJob != null) {
            syntheticJob?.cancel()
            syntheticJob = null
        }
    }

    private fun startSensorCollection() {
        // Immediately send initial sensor window so phone displays active data upon launch
        scope.launch {
            dataSender.sendSensorWindow(
                hrMean = 72.0,
                rmssd = 42.0,
                accMag = 0.1,
                repetitiveness = 0.1,
                hrSlope = 0.0,
                isOffBody = false,
                isExercising = false
            )
        }

        // Start background vitals loop
        // Spec 03: Test Mode is OFF by default and exists in debug builds only. A debug build can switch it on with
        //   adb shell "run-as <pkg> sh -c 'echo true > files/test_mode'"  — (no UI toggle yet).
        val requested = runCatching { java.io.File(filesDir, "test_mode").readText().trim() == "true" }.getOrDefault(false)
        setTestMode(com.mtpnda.biocue.core.domain.TestModePolicy.effective(com.mtpnda.biocue.wear.BuildConfig.DEBUG, requested))

        val measureClient = HealthServices.getClient(this).measureClient
        
        scope.launch {
            try {
                if (androidx.core.content.ContextCompat.checkSelfPermission(this@SensorCollectionService, android.Manifest.permission.BODY_SENSORS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, hrCallback)
                    Log.d("SensorCollection", "Registered HR callback")
                } else {
                    Log.w("SensorCollection", "BODY_SENSORS permission not granted yet.")
                }
            } catch (e: Exception) {
                Log.e("SensorCollection", "Failed to register HR callback", e)
            }
        }
    }

    private fun startSyntheticDataLoop() {
        syntheticJob = scope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000)
                val randomHr = 65.0 + (Math.random() * 30.0)
                dataSender.sendSensorWindow(
                    hrMean = randomHr,
                    rmssd = 45.0 + (Math.random() * 10),
                    accMag = 0.1,
                    repetitiveness = 0.2,
                    hrSlope = (Math.random() * 2) - 1.0,
                    isOffBody = false,
                    isExercising = false
                )
            }
        }
    }

    private val hrCallback = object : MeasureCallback {
        override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {
            Log.d("SensorCollection", "Availability: $availability")
        }

        override fun onDataReceived(data: DataPointContainer) {
            val hrData = data.getData(DataType.HEART_RATE_BPM)
            if (hrData.isNotEmpty()) {
                val latestHr = hrData.last().value
                Log.d("SensorCollection", "HR Received: $latestHr")
                
                scope.launch {
                    dataSender.sendSensorWindow(
                        hrMean = latestHr,
                        rmssd = 45.0, // In production, compute from IBI
                        accMag = 0.1,
                        repetitiveness = 0.2,
                        hrSlope = 1.0,
                        isOffBody = false,
                        isExercising = false
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(testModeReceiver) } catch (_: Exception) {}
        // Stop advertising/serving when monitoring stops (battery); the outbox keeps un-ACKed events.
        if (PhoneTargetStore(this).get() == PhoneTarget.IPHONE) BleLinkServer.get(this).stop()
        val measureClient = HealthServices.getClient(this).measureClient
        scope.launch {
            measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, hrCallback)
        }
        syntheticJob?.cancel()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "sensor_channel",
                "Sensor Monitoring",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
