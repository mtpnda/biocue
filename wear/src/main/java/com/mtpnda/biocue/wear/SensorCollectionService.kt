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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = Notification.Builder(this, "sensor_channel")
            .setContentTitle("BioCue")
            .setContentText("Monitoring vitals")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
            
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
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
        setTestMode(true)

        val measureClient = HealthServices.getClient(this).measureClient
        
        scope.launch {
            try {
                measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, hrCallback)
                Log.d("SensorCollection", "Registered HR callback")
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
