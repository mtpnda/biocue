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
import com.mtpnda.biocue.core.link.LinkMessage
import com.mtpnda.biocue.wear.link.BleLinkServer
import com.mtpnda.biocue.wear.link.PhoneTarget
import com.mtpnda.biocue.wear.link.PhoneTargetStore

class WearDataSender(private val context: Context) {
    // Which transport carries data: the Wearable Data Layer (Android phone, the original path)
    // or BioCue Link over BLE (iPhone). Read on every call so a switch takes effect immediately.
    // Falls back to ANDROID on any problem, so behaviour is unchanged unless iPhone is chosen.
    private val targetStore = PhoneTargetStore(context)
    private fun usesIphone(): Boolean = targetStore.get() == PhoneTarget.IPHONE
    private fun ble(): BleLinkServer = BleLinkServer.get(context)

    private val dataClient = com.google.android.gms.wearable.Wearable.getDataClient(context)
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
        if (usesIphone()) {
            val established = ble().isEstablished.value
            _isPhoneReachable.value = established
            return established
        }
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

    
    suspend fun sendSosLocation(lat: Double, lng: Double): Boolean {
        if (usesIphone()) return ble().send(LinkMessage.SosLocation(System.currentTimeMillis(), lat, lng))
        if (EmulatorWatchClient.isEmulator()) {
            val json = org.json.JSONObject().apply {
                put("type", "SOS_LOCATION")
                put("latitude", lat)
                put("longitude", lng)
            }
            EmulatorWatchClient.send(json)
        }
        return try {
            checkReachability()
            val putDataReq = PutDataMapRequest.create("/sos_location").apply {
                dataMap.putLong("timestamp", System.currentTimeMillis())
                dataMap.putDouble("latitude", lat)
                dataMap.putDouble("longitude", lng)
            }.asPutDataRequest().setUrgent()
            
            dataClient.putDataItem(putDataReq).await()
            Log.d("WearDataSender", "Sent SOS Location: $lat, $lng")
            true
        } catch (e: Exception) {
            Log.e("WearDataSender", "Failed to send SOS Location", e)
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
        if (usesIphone()) {
            return ble().send(
                LinkMessage.SensorWindow(System.currentTimeMillis(), hrMean, rmssd, accMag, repetitiveness, hrSlope, isOffBody, isExercising)
            )
        }
        if (EmulatorWatchClient.isEmulator()) {
            val json = org.json.JSONObject().apply {
                put("type", "SENSOR_WINDOW")
                put("timestamp", System.currentTimeMillis())
                put("hrMean", hrMean)
                put("rmssd", rmssd)
                put("accMag", accMag)
                put("repetitiveness", repetitiveness)
                put("hrSlope", hrSlope)
                put("isOffBody", isOffBody)
                put("isExercising", isExercising)
            }
            EmulatorWatchClient.send(json)
        }
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
        if (usesIphone()) return ble().send(LinkMessage.EventStart(System.currentTimeMillis(), eventId, isManual))
        if (EmulatorWatchClient.isEmulator()) {
            val json = org.json.JSONObject().apply {
                put("type", "EVENT_START")
                put("eventId", eventId)
                put("timestamp", System.currentTimeMillis())
                put("trigger", if (isManual) "MANUAL" else "AUTO")
            }
            EmulatorWatchClient.send(json)
        }
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
        if (usesIphone()) return ble().send(LinkMessage.EventLabel(System.currentTimeMillis(), eventId, label))
        if (EmulatorWatchClient.isEmulator()) {
            val json = org.json.JSONObject().apply {
                put("type", "EVENT_LABEL")
                put("eventId", eventId)
                put("label", label)
                put("timestamp", System.currentTimeMillis())
            }
            EmulatorWatchClient.send(json)
        }
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
        if (usesIphone()) return ble().send(LinkMessage.EventEnd(System.currentTimeMillis(), eventId))
        if (EmulatorWatchClient.isEmulator()) {
            val json = org.json.JSONObject().apply {
                put("type", "EVENT_END")
                put("eventId", eventId)
                put("timestamp", System.currentTimeMillis())
            }
            EmulatorWatchClient.send(json)
        }
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
