package com.mtpnda.biocue.mockemitter

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.mtpnda.biocue.core.domain.model.DataLayerSchema
import com.mtpnda.biocue.core.domain.model.SensorWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EmissionRecord(
    val id: Long,
    val time: String,
    val hr: Double,
    val rmssd: Double,
    val motion: Double,
    val label: String,
    val channels: String
)

class MockEmitterEngine(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val tag = "MockEmitterEngine"
    private val dataClient by lazy { Wearable.getDataClient(context) }

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _streamIntervalMs = MutableStateFlow(2000L)
    val streamIntervalMs: StateFlow<Long> = _streamIntervalMs.asStateFlow()

    private val _emissionHistory = MutableStateFlow<List<EmissionRecord>>(emptyList())
    val emissionHistory: StateFlow<List<EmissionRecord>> = _emissionHistory.asStateFlow()

    private val _totalEmissions = MutableStateFlow(0)
    val totalEmissions: StateFlow<Int> = _totalEmissions.asStateFlow()

    private var streamingJob: Job? = null
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun setInterval(ms: Long) {
        _streamIntervalMs.value = ms
    }

    fun startStreaming(windowProvider: () -> SensorWindow) {
        if (_isStreaming.value) return
        _isStreaming.value = true
        streamingJob = scope.launch {
            while (_isStreaming.value) {
                emitSensorWindow(windowProvider())
                delay(_streamIntervalMs.value)
            }
        }
    }

    fun stopStreaming() {
        _isStreaming.value = false
        streamingJob?.cancel()
        streamingJob = null
    }

    suspend fun emitSensorWindow(window: SensorWindow): Boolean {
        var dataLayerSuccess = false
        var broadcastSuccess = false

        // 1. Send via Google Play Services Wearable Data Layer (Cross-device Wear <-> Phone sync)
        try {
            val putDataReq = PutDataMapRequest.create(DataLayerSchema.PATH_SENSOR_WINDOW).apply {
                dataMap.putLong(DataLayerSchema.KEY_TIMESTAMP, window.timestamp)
                dataMap.putDouble(DataLayerSchema.KEY_HR_MEAN, window.hrMean)
                dataMap.putDouble(DataLayerSchema.KEY_RMSSD, window.rmssd)
                dataMap.putDouble(DataLayerSchema.KEY_ACC_MAG, window.accMag)
                dataMap.putDouble(DataLayerSchema.KEY_REPETITIVENESS, window.repetitiveness)
                dataMap.putDouble(DataLayerSchema.KEY_HR_SLOPE, window.hrSlope)
                dataMap.putBoolean(DataLayerSchema.KEY_IS_OFF_BODY, window.isOffBody)
                dataMap.putBoolean(DataLayerSchema.KEY_IS_EXERCISING, window.isExercising)
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(putDataReq).await()
            dataLayerSuccess = true
        } catch (e: Exception) {
            Log.w(tag, "Wearable Data Layer item skipped/failed (expected if Google Play Services Wear unavailable): ${e.message}")
        }

        // 2. Send via Intent Broadcast directly to com.mtpnda.biocue (Local Same-Device / Emulator bypass)
        try {
            val intent = Intent("com.mtpnda.biocue.MOCK_SENSOR_WINDOW").apply {
                setPackage("com.mtpnda.biocue")
                putExtra("timestamp", window.timestamp)
                putExtra("hrMean", window.hrMean)
                putExtra("rmssd", window.rmssd)
                putExtra("accMag", window.accMag)
                putExtra("repetitiveness", window.repetitiveness)
                putExtra("hrSlope", window.hrSlope)
                putExtra("isOffBody", window.isOffBody)
                putExtra("isExercising", window.isExercising)
            }
            context.sendBroadcast(intent)
            broadcastSuccess = true
        } catch (e: Exception) {
            Log.e(tag, "Failed to send local broadcast", e)
        }

        // Record history
        val channels = when {
            dataLayerSuccess && broadcastSuccess -> "Wearable + Broadcast"
            dataLayerSuccess -> "Wearable Data Layer"
            broadcastSuccess -> "Broadcast"
            else -> "Failed"
        }

        val record = EmissionRecord(
            id = System.nanoTime(),
            time = timeFormat.format(Date(window.timestamp)),
            hr = window.hrMean,
            rmssd = window.rmssd,
            motion = window.accMag,
            label = if (window.isOffBody) "Off Body" else if (window.isExercising) "Exercising" else "${window.hrMean.toInt()} BPM",
            channels = channels
        )

        _totalEmissions.value += 1
        _emissionHistory.value = (listOf(record) + _emissionHistory.value).take(30)

        return dataLayerSuccess || broadcastSuccess
    }

    suspend fun emitEventStart(eventId: String = java.util.UUID.randomUUID().toString(), isManual: Boolean = true): String {
        try {
            val req = PutDataMapRequest.create(DataLayerSchema.PATH_EVENT_START).apply {
                dataMap.putString(DataLayerSchema.KEY_EVENT_ID, eventId)
                dataMap.putLong(DataLayerSchema.KEY_TIMESTAMP, System.currentTimeMillis())
                dataMap.putString(DataLayerSchema.KEY_EVENT_TRIGGER_TYPE, if (isManual) "MANUAL" else "AUTO")
            }.asPutDataRequest().setUrgent()
            dataClient.putDataItem(req).await()
        } catch (_: Exception) {}

        try {
            val intent = Intent("com.mtpnda.biocue.MOCK_MANUAL_EVENT").apply {
                setPackage("com.mtpnda.biocue")
                putExtra("trigger", if (isManual) "MANUAL" else "AUTO")
            }
            context.sendBroadcast(intent)
        } catch (_: Exception) {}

        return eventId
    }

    suspend fun emitEventLabel(eventId: String, label: String) {
        try {
            val req = PutDataMapRequest.create(DataLayerSchema.PATH_EVENT_LABEL).apply {
                dataMap.putString(DataLayerSchema.KEY_EVENT_ID, eventId)
                dataMap.putLong(DataLayerSchema.KEY_TIMESTAMP, System.currentTimeMillis())
                dataMap.putString(DataLayerSchema.KEY_EVENT_LABEL, label)
            }.asPutDataRequest().setUrgent()
            dataClient.putDataItem(req).await()
        } catch (_: Exception) {}
    }

    suspend fun emitEventEnd(eventId: String) {
        try {
            val req = PutDataMapRequest.create(DataLayerSchema.PATH_EVENT_END).apply {
                dataMap.putString(DataLayerSchema.KEY_EVENT_ID, eventId)
                dataMap.putLong(DataLayerSchema.KEY_TIMESTAMP, System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            dataClient.putDataItem(req).await()
        } catch (_: Exception) {}
    }

    fun setWatchTestMode(enabled: Boolean) {
        try {
            val intent = Intent("com.mtpnda.biocue.wear.SET_TEST_MODE").apply {
                setPackage("com.mtpnda.biocue")
                putExtra("enabled", enabled)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(tag, "Failed to broadcast SET_TEST_MODE to watch", e)
        }
    }
}
