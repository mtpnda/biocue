package com.mtpnda.biocue.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WearDataSender(context: Context) {
    private val dataClient = Wearable.getDataClient(context)
    private val nodeClient = Wearable.getNodeClient(context)
    private val capabilityClient = Wearable.getCapabilityClient(context)

    private val _isPhoneReachable = MutableStateFlow(true)
    val isPhoneReachable: StateFlow<Boolean> = _isPhoneReachable.asStateFlow()

    fun startMonitoring(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            while (true) {
                checkReachability()
                delay(3000)
            }
        }
    }

    suspend fun checkReachability(): Boolean {
        return try {
            try {
                capabilityClient.addLocalCapability("biocue_sensor").await()
            } catch (_: Exception) {}

            val nodes = nodeClient.connectedNodes.await()
            val reachable = nodes.isNotEmpty()
            _isPhoneReachable.value = reachable
            reachable
        } catch (e: Exception) {
            _isPhoneReachable.value = false
            false
        }
    }

    suspend fun sendSensorWindow(
        hrMean: Double,
        rmssd: Double,
        accMag: Double,
        repetitiveness: Double,
        hrSlope: Double,
        isOffBody: Boolean,
        isExercising: Boolean
    ): Boolean {
        return try {
            checkReachability()

            val putDataReq = PutDataMapRequest.create(com.mtpnda.biocue.core.domain.model.DataLayerSchema.PATH_SENSOR_WINDOW).apply {
                dataMap.putLong(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_TIMESTAMP, System.currentTimeMillis())
                dataMap.putDouble(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_HR_MEAN, hrMean)
                dataMap.putDouble(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_RMSSD, rmssd)
                dataMap.putDouble(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_ACC_MAG, accMag)
                dataMap.putDouble(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_REPETITIVENESS, repetitiveness)
                dataMap.putDouble(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_HR_SLOPE, hrSlope)
                dataMap.putBoolean(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_IS_OFF_BODY, isOffBody)
                dataMap.putBoolean(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_IS_EXERCISING, isExercising)
            }.asPutDataRequest().setUrgent()
            
            val result = dataClient.putDataItem(putDataReq).await()
            Log.d("WearDataSender", "Data item sent: ${result.uri}")
            true
        } catch (e: Exception) {
            _isPhoneReachable.value = false
            Log.e("WearDataSender", "Failed to send data item", e)
            false
        }
    }

    suspend fun sendEventStart(eventId: String, isManual: Boolean): Boolean {
        try {
            val req = PutDataMapRequest.create(com.mtpnda.biocue.core.domain.model.DataLayerSchema.PATH_EVENT_START).apply {
                dataMap.putString(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_EVENT_ID, eventId)
                dataMap.putLong(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_TIMESTAMP, System.currentTimeMillis())
                dataMap.putString(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_EVENT_TRIGGER_TYPE, if (isManual) "MANUAL" else "AUTO")
            }.asPutDataRequest().setUrgent()
            dataClient.putDataItem(req).await()
            return true
        } catch(e: Exception) { return false }
    }

    suspend fun sendEventLabel(eventId: String, label: String): Boolean {
        try {
            val req = PutDataMapRequest.create(com.mtpnda.biocue.core.domain.model.DataLayerSchema.PATH_EVENT_LABEL).apply {
                dataMap.putString(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_EVENT_ID, eventId)
                dataMap.putLong(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_TIMESTAMP, System.currentTimeMillis())
                dataMap.putString(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_EVENT_LABEL, label)
            }.asPutDataRequest().setUrgent()
            dataClient.putDataItem(req).await()
            return true
        } catch(e: Exception) { return false }
    }

    suspend fun sendEventEnd(eventId: String): Boolean {
        try {
            val req = PutDataMapRequest.create(com.mtpnda.biocue.core.domain.model.DataLayerSchema.PATH_EVENT_END).apply {
                dataMap.putString(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_EVENT_ID, eventId)
                dataMap.putLong(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_TIMESTAMP, System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            dataClient.putDataItem(req).await()
            return true
        } catch(e: Exception) { return false }
    }
}
