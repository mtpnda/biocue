package com.mtpnda.biocue.wear

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.health.services.client.data.DataTypeAvailability
import com.mtpnda.biocue.core.domain.model.SensorWindow
import com.mtpnda.biocue.core.ml.DutyCycleEngine
import com.mtpnda.biocue.core.ml.DutyCycleState
import com.mtpnda.biocue.core.ml.HrvBaselineProfiler
import com.mtpnda.biocue.core.ml.HrvCalculator
import com.mtpnda.biocue.wear.link.BleLinkServer
import com.mtpnda.biocue.wear.link.PhoneTarget
import com.mtpnda.biocue.wear.link.PhoneTargetStore
import com.mtpnda.biocue.wear.location.LocationTracker
import com.mtpnda.biocue.wear.profile.HrvProfileStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

class SensorCollectionService : Service(), SensorEventListener {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var dataSender: WearDataSender
    private lateinit var profileStore: HrvProfileStore
    private lateinit var profiler: HrvBaselineProfiler
    private val hrvCalculator = HrvCalculator()
    private val dutyCycleEngine = DutyCycleEngine()

    private var sensorManager: SensorManager? = null
    private var accelSensor: Sensor? = null
    private var offbodySensor: Sensor? = null

    // Vitals and motion state
    private val recentHrReadings = ArrayDeque<Double>()
    private var latestHr: Double = 72.0
    private var latestAccMag: Double = 0.05
    private var latestRepetitiveness: Double = 0.0
    private var isExercising: Boolean = false
    private var isOffBody: Boolean = false

    private var ppgRegistered: Boolean = false
    private var dutyCycleJob: Job? = null
    private var lastWindowSentMs: Long = 0L

    /** Expose the sender's reachability state so the Activity can observe it. */
    val isPhoneReachable: StateFlow<Boolean>
        get() = dataSender.isPhoneReachable

    inner class LocalBinder : Binder() {
        val service: SensorCollectionService get() = this@SensorCollectionService
        val currentProfiler: HrvBaselineProfiler get() = profiler
        val currentDutyState: DutyCycleState get() = dutyCycleEngine.currentState
    }
    private val binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        dataSender = WearDataSender(this)
        profileStore = HrvProfileStore(this)
        profiler = profileStore.getProfiler()
        createNotificationChannel()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        offbodySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT)

        // iPhone link: the phone can ask for a GPS fix during an SOS; answer over the same link.
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
            val flags = if (BuildConfig.DEBUG) Context.RECEIVER_EXPORTED else Context.RECEIVER_NOT_EXPORTED
            registerReceiver(testModeReceiver, filter, flags)
        } else {
            registerReceiver(testModeReceiver, filter)
        }
    }

    private val testModeReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: android.content.Intent?) {
            if (intent?.action == "com.mtpnda.biocue.wear.SET_TEST_MODE") {
                val enable = intent.getBooleanExtra("enabled", true)
                setTestMode(enable)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = Notification.Builder(this, "sensor_channel")
            .setContentTitle("BioCue")
            .setContentText("Monitoring vitals (battery optimized)")
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
        // Send initial baseline window immediately so phone UI displays connected metrics
        scope.launch {
            val initialWindow = SensorWindow(
                timestamp = System.currentTimeMillis(),
                hrMean = profiler.profile.baseline.hrMean,
                rmssd = profiler.profile.baseline.hrvMean,
                accMag = 0.05,
                repetitiveness = 0.0,
                hrSlope = 0.0,
                isOffBody = false,
                isExercising = false
            )
            dataSender.sendSensorWindow(
                hrMean = initialWindow.hrMean,
                rmssd = initialWindow.rmssd,
                accMag = initialWindow.accMag,
                repetitiveness = initialWindow.repetitiveness,
                hrSlope = initialWindow.hrSlope,
                isOffBody = initialWindow.isOffBody,
                isExercising = initialWindow.isExercising
            )
            lastWindowSentMs = initialWindow.timestamp
        }

        val requested = runCatching { java.io.File(filesDir, "test_mode").readText().trim() == "true" }.getOrDefault(false)
        setTestMode(com.mtpnda.biocue.core.domain.TestModePolicy.effective(BuildConfig.DEBUG, requested))

        // Register hardware low-power motion and off-body sensors
        sensorManager?.let { sm ->
            accelSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
            offbodySensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        }

        // Start smart duty-cycle controller
        startDutyCycleLoop()
    }

    private fun startDutyCycleLoop() {
        dutyCycleJob?.cancel()
        dutyCycleJob = scope.launch {
            while (isActive) {
                when (dutyCycleEngine.currentState) {
                    DutyCycleState.OFF_BODY -> {
                        ensurePpgRegistered(false)
                        delay(5000)
                    }
                    DutyCycleState.ACTIVE_HIGH -> {
                        ensurePpgRegistered(true)
                        processAndEmitWindow(immediate = true)
                        delay(3000)
                    }
                    DutyCycleState.CALM_DUTY -> {
                        // 1. Active sampling phase: 30 seconds
                        ensurePpgRegistered(true)
                        val sampleEndTime = System.currentTimeMillis() + (dutyCycleEngine.calmActiveSeconds * 1000L)
                        while (System.currentTimeMillis() < sampleEndTime && dutyCycleEngine.currentState == DutyCycleState.CALM_DUTY) {
                            delay(5000)
                        }

                        // Compute, profile, and sync batched window
                        processAndEmitWindow(immediate = false)

                        // 2. Battery-saving sleep phase: 90 seconds (optical PPG LED off)
                        if (dutyCycleEngine.currentState == DutyCycleState.CALM_DUTY) {
                            ensurePpgRegistered(false)
                            val sleepEndTime = System.currentTimeMillis() + (dutyCycleEngine.calmSleepSeconds * 1000L)
                            while (System.currentTimeMillis() < sleepEndTime && dutyCycleEngine.currentState == DutyCycleState.CALM_DUTY) {
                                delay(3000)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ensurePpgRegistered(register: Boolean) {
        if (register == ppgRegistered) return
        val measureClient = HealthServices.getClient(this).measureClient
        if (register) {
            val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.BODY_SENSORS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
                try {
                    measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, hrCallback)
                    ppgRegistered = true
                    Log.d("SensorCollection", "PPG registered (Duty cycle active)")
                } catch (e: Exception) {
                    Log.e("SensorCollection", "Failed to register PPG", e)
                }
            }
        } else {
            try {
                measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, hrCallback)
                ppgRegistered = false
                Log.d("SensorCollection", "PPG unregistered (Duty cycle sleep / off-body)")
            } catch (e: Exception) {
                Log.e("SensorCollection", "Failed to unregister PPG", e)
            }
        }
    }

    private suspend fun processAndEmitWindow(immediate: Boolean) {
        val now = System.currentTimeMillis()
        val rmssd = hrvCalculator.computeRmssdFromHrList(recentHrReadings.toList())
        val hrSlope = hrvCalculator.computeHrSlope(now)

        val window = SensorWindow(
            timestamp = now,
            hrMean = latestHr,
            rmssd = rmssd,
            accMag = latestAccMag,
            repetitiveness = latestRepetitiveness,
            hrSlope = hrSlope,
            isOffBody = isOffBody,
            isExercising = isExercising
        )

        // Adapt profile and update local baseline
        profiler.incorporateWindow(window)
        profileStore.persistThrottled(profiler, force = false)

        val tier = profiler.profile.latestTier
        dutyCycleEngine.onStressEvaluated(tier, hrSlope, latestAccMag)

        // Sync with phone:
        // Immediate sync if stress elevated or if calm duty cycle interval has elapsed
        val elapsedSinceLast = now - lastWindowSentMs
        if (immediate || elapsedSinceLast >= 25_000L) {
            dataSender.sendSensorWindow(
                hrMean = window.hrMean,
                rmssd = window.rmssd,
                accMag = window.accMag,
                repetitiveness = window.repetitiveness,
                hrSlope = window.hrSlope,
                isOffBody = window.isOffBody,
                isExercising = window.isExercising
            )
            lastWindowSentMs = now
            Log.d("SensorCollection", "Synced window: HR=${window.hrMean.toInt()}, RMSSD=${window.rmssd.toInt()}ms, Stress=${profiler.profile.latestStressPercent}%, Tier=$tier")
        }
    }

    private val hrCallback = object : MeasureCallback {
        override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {
            Log.d("SensorCollection", "HR Availability: $availability")
            if (availability is DataTypeAvailability) {
                val off = availability == DataTypeAvailability.UNAVAILABLE
                if (off != isOffBody) {
                    isOffBody = off
                    dutyCycleEngine.onBodyChanged(!off)
                }
            }
        }

        override fun onDataReceived(data: DataPointContainer) {
            val hrData = data.getData(DataType.HEART_RATE_BPM)
            if (hrData.isNotEmpty()) {
                val hr = hrData.last().value
                latestHr = hr
                val now = System.currentTimeMillis()
                hrvCalculator.addHrReading(now, hr)

                synchronized(recentHrReadings) {
                    recentHrReadings.addLast(hr)
                    if (recentHrReadings.size > 30) recentHrReadings.removeFirst()
                }

                // If in active elevated state, evaluate trigger quickly
                if (dutyCycleEngine.currentState == DutyCycleState.ACTIVE_HIGH) {
                    scope.launch {
                        processAndEmitWindow(immediate = true)
                    }
                }
            }
        }
    }

    // ---- SensorEventListener (Accelerometer & Off-Body) -----------------------------------

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT -> {
                val onBody = event.values[0] == 1.0f
                isOffBody = !onBody
                dutyCycleEngine.onBodyChanged(onBody)
                Log.d("SensorCollection", "Hardware off-body event: onBody=$onBody")
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val mag = sqrt((x * x + y * y + z * z).toDouble())
                val netAcc = abs(mag - 9.81)

                // Smooth acceleration magnitude
                latestAccMag = 0.8 * latestAccMag + 0.2 * netAcc
                isExercising = latestAccMag > 1.2 && latestHr > 105.0

                // If asleep in CALM_DUTY and high motion detected, wake PPG
                if (!ppgRegistered && latestAccMag > 0.8 && dutyCycleEngine.currentState == DutyCycleState.CALM_DUTY) {
                    Log.d("SensorCollection", "Motion spike during duty sleep: waking PPG early")
                    dutyCycleEngine.onStressEvaluated(tier = 0, hrSlope = 1.0, accMag = latestAccMag)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun startSyntheticDataLoop() {
        syntheticJob = scope.launch {
            while (isActive) {
                delay(5000)
                val randomHr = 65.0 + (Math.random() * 30.0)
                val randomRmssd = 45.0 + (Math.random() * 10.0)
                val window = SensorWindow(
                    timestamp = System.currentTimeMillis(),
                    hrMean = randomHr,
                    rmssd = randomRmssd,
                    accMag = 0.1,
                    repetitiveness = 0.2,
                    hrSlope = (Math.random() * 2) - 1.0,
                    isOffBody = false,
                    isExercising = false
                )
                profiler.incorporateWindow(window)
                dataSender.sendSensorWindow(
                    hrMean = window.hrMean,
                    rmssd = window.rmssd,
                    accMag = window.accMag,
                    repetitiveness = window.repetitiveness,
                    hrSlope = window.hrSlope,
                    isOffBody = window.isOffBody,
                    isExercising = window.isExercising
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(testModeReceiver) } catch (_: Exception) {}
        sensorManager?.unregisterListener(this)
        ensurePpgRegistered(false)
        dutyCycleJob?.cancel()
        syntheticJob?.cancel()

        // Force flush profile to disk on termination
        profileStore.persistThrottled(profiler, force = true)

        if (PhoneTargetStore(this).get() == PhoneTarget.IPHONE) BleLinkServer.get(this).stop()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "sensor_channel",
                "Sensor Monitoring",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
