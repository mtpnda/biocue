package com.mtpnda.biocue

import android.util.Log
import com.mtpnda.biocue.core.domain.model.SensorWindow
import com.mtpnda.biocue.core.domain.model.Baseline
import com.mtpnda.biocue.core.ml.ScoringEngine
import com.mtpnda.biocue.core.domain.DetectionPipeline
import com.mtpnda.biocue.core.domain.LatestEvent
import com.mtpnda.biocue.core.domain.PipelineAction
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WatchDataReceiverService : WearableListenerService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val scoringEngine = ScoringEngine()

    companion object {
        /**
         * ONE CLOCK: the watch stamps messages with ITS clock but the pipeline compares them with the phone's "now".
         * There is no handshake on this path, so the skew is estimated from prompt telemetry windows (see ClockSkewEstimator).
         */
        val clockSkew = com.mtpnda.biocue.core.link.ClockSkewEstimator()

        /** The watch's one SOS GPS fix. In memory ONLY — never stored (docs/spec/12_PUSH_AND_GPS.md §4). */
        val sosLocation = kotlinx.coroutines.flow.MutableStateFlow<Pair<Double, Double>?>(null)
        private const val SOS_MAX_AGE_MS = 2 * 60_000L
    }
    private lateinit var alertManager: AlertManager
    
            val db by lazy {
                androidx.room.Room.databaseBuilder(
                    applicationContext,
                    com.mtpnda.biocue.core.data.local.DetectionDatabase::class.java, "sensor-database"
                ).fallbackToDestructiveMigration().build()
            }

    private val currentUserId: String
        get() = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
    
    // Hardcoded baseline for demonstration
    private val mockReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == "com.mtpnda.biocue.MOCK_SENSOR_WINDOW") {
                val getNum = { key: String, def: Double ->
                    if (intent.hasExtra(key)) {
                        intent.getDoubleExtra(key, intent.getFloatExtra(key, def.toFloat()).toDouble())
                    } else def
                }
                val window = SensorWindow(
                    timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis()),
                    hrMean = getNum("hrMean", 75.0),
                    rmssd = getNum("rmssd", 40.0),
                    accMag = getNum("accMag", 0.1),
                    repetitiveness = getNum("repetitiveness", 0.0),
                    hrSlope = getNum("hrSlope", 0.0),
                    isOffBody = intent.getBooleanExtra("isOffBody", false),
                    isExercising = intent.getBooleanExtra("isExercising", false)
                )
                processWindow(window)
            } else if (intent?.action == "com.mtpnda.biocue.MOCK_MANUAL_EVENT") {
                val trigger = intent.getStringExtra("trigger") ?: "MANUAL"
                kotlinx.coroutines.runBlocking {
                    val newEvent = com.mtpnda.biocue.core.data.local.StressEventEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        userId = currentUserId,
                        startTime = System.currentTimeMillis(),
                        endTime = null,
                        triggerType = trigger,
                        label = "NONE",
                        isResolved = false
                    )
                    db.eventDao().insertEvent(newEvent)
                    syncEventToCloud(newEvent)
                }
            } else if (intent?.action == "com.mtpnda.biocue.MOCK_EVENT_LABEL") {
                val label = intent.getStringExtra("label") ?: "OVERWHELMED"
                val eventId = intent.getStringExtra("eventId")
                kotlinx.coroutines.runBlocking {
                    val target = if (eventId != null) {
                        db.eventDao().getEventById(eventId)
                    } else {
                        db.eventDao().getUnresolvedEvents(currentUserId).firstOrNull { it.endTime == null }
                    }
                    if (target != null) {
                        val updated = target.copy(label = label, isResolved = false)
                        db.eventDao().updateEvent(updated)
                        syncEventToCloud(updated)
                    }
                }
            } else if (intent?.action == "com.mtpnda.biocue.MOCK_EVENT_END") {
                val eventId = intent.getStringExtra("eventId")
                kotlinx.coroutines.runBlocking {
                    val target = if (eventId != null) {
                        db.eventDao().getEventById(eventId)
                    } else {
                        db.eventDao().getUnresolvedEvents(currentUserId).firstOrNull { it.endTime == null }
                    }
                    if (target != null) {
                        val updated = target.copy(endTime = System.currentTimeMillis())
                        db.eventDao().updateEvent(updated)
                        syncEventToCloud(updated)
                    }
                }
            } else if (intent?.action == "com.mtpnda.biocue.MOCK_SOS_LOCATION") {
                val lat = intent.getDoubleExtra("lat", -6.2088)
                val lng = intent.getDoubleExtra("lng", 106.8456)
                sosLocation.value = Pair(lat, lng)
            }
        }
    }

    private val dummyBaseline = Baseline(
        hourOfDay = 12, hrMean = 75.0, hrStd = 5.0, hrvMean = 40.0, hrvStd = 8.0
    )

    override fun onCreate() {
        super.onCreate()
        alertManager = AlertManager(this)
        val filter = android.content.IntentFilter().apply {
            addAction("com.mtpnda.biocue.MOCK_SENSOR_WINDOW")
            addAction("com.mtpnda.biocue.MOCK_MANUAL_EVENT")
            addAction("com.mtpnda.biocue.MOCK_EVENT_LABEL")
            addAction("com.mtpnda.biocue.MOCK_EVENT_END")
            addAction("com.mtpnda.biocue.MOCK_SOS_LOCATION")
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val flags = if (BuildConfig.DEBUG) {
                android.content.Context.RECEIVER_EXPORTED
            } else {
                android.content.Context.RECEIVER_NOT_EXPORTED
            }
            registerReceiver(mockReceiver, filter, flags)
        } else {
            registerReceiver(mockReceiver, filter)
        }

        if (BuildConfig.DEBUG) {
            com.mtpnda.biocue.connect.EmulatorWatchBridge.start(this, scope) { json ->
                handleBridgeMessage(json)
            }
        }
    }

    fun handleBridgeMessage(json: org.json.JSONObject) {
        when (json.optString("type")) {
            "SENSOR_WINDOW" -> {
                val window = SensorWindow(
                    timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                    hrMean = json.optDouble("hrMean", 75.0),
                    rmssd = json.optDouble("rmssd", 40.0),
                    accMag = json.optDouble("accMag", 0.1),
                    repetitiveness = json.optDouble("repetitiveness", 0.0),
                    hrSlope = json.optDouble("hrSlope", 0.0),
                    isOffBody = json.optBoolean("isOffBody", false),
                    isExercising = json.optBoolean("isExercising", false)
                )
                processWindow(window)
            }
            "EVENT_START" -> {
                val eventId = json.optString("eventId", java.util.UUID.randomUUID().toString())
                val trigger = json.optString("trigger", "MANUAL")
                val ts = json.optLong("timestamp", System.currentTimeMillis())
                kotlinx.coroutines.runBlocking {
                    val newEvent = com.mtpnda.biocue.core.data.local.StressEventEntity(
                        id = eventId,
                        userId = currentUserId,
                        startTime = ts,
                        endTime = null,
                        triggerType = trigger,
                        label = "NONE",
                        isResolved = false
                    )
                    db.eventDao().insertEvent(newEvent)
                    syncEventToCloud(newEvent)
                }
            }
            "EVENT_LABEL" -> {
                val eventId = json.optString("eventId")
                val label = json.optString("label", "OVERWHELMED")
                kotlinx.coroutines.runBlocking {
                    val target = if (eventId.isNotEmpty()) {
                        db.eventDao().getEventById(eventId)
                    } else {
                        db.eventDao().getUnresolvedEvents(currentUserId).firstOrNull { it.endTime == null }
                    }
                    if (target != null) {
                        val updated = target.copy(label = label, isResolved = false)
                        db.eventDao().updateEvent(updated)
                        syncEventToCloud(updated)
                    }
                }
            }
            "EVENT_END" -> {
                val eventId = json.optString("eventId")
                val ts = json.optLong("timestamp", System.currentTimeMillis())
                kotlinx.coroutines.runBlocking {
                    val target = if (eventId.isNotEmpty()) {
                        db.eventDao().getEventById(eventId)
                    } else {
                        db.eventDao().getUnresolvedEvents(currentUserId).firstOrNull { it.endTime == null }
                    }
                    if (target != null) {
                        val updated = target.copy(endTime = ts)
                        db.eventDao().updateEvent(updated)
                        syncEventToCloud(updated)
                    }
                }
            }
            "SOS_LOCATION" -> {
                val lat = json.optDouble("latitude", -6.2088)
                val lng = json.optDouble("longitude", 106.8456)
                sosLocation.value = Pair(lat, lng)
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        // Wearer-only: raw vitals must never be processed under a caregiver account.
        if (!com.mtpnda.biocue.role.RoleStore.mayReceiveWatchData(com.mtpnda.biocue.role.RoleStore.get(applicationContext))) {
            Log.w("WatchDataReceiver", "Ignoring watch data: this account is a caregiver")
            return
        }
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path
                Log.d("WatchDataReceiver", "onDataChanged received path: $path from URI ${event.dataItem.uri}")
                val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                val map = dataMapItem.dataMap
                
                when (path) {
                    "/sos_location" -> {
                        // Spec 03/12: the watch answers a GPS request with ONE fix. Ignore a stale item redelivered later.
                        val ts = clockSkew.toPhoneTime(map.getLong(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_TIMESTAMP))
                        if (System.currentTimeMillis() - ts <= SOS_MAX_AGE_MS) {
                            sosLocation.value = Pair(map.getDouble("latitude"), map.getDouble("longitude"))
                        }
                    }
                    com.mtpnda.biocue.core.domain.model.DataLayerSchema.PATH_SENSOR_WINDOW -> {
                        val window = SensorWindow(
                            timestamp = run {
                                val watchTs = map.getLong(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_TIMESTAMP)
                                clockSkew.observe(watchTs, System.currentTimeMillis())   // windows arrive promptly: they measure the skew
                                clockSkew.toPhoneTime(watchTs)
                            },
                            hrMean = map.getDouble(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_HR_MEAN),
                            rmssd = map.getDouble(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_RMSSD),
                            accMag = map.getDouble(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_ACC_MAG),
                            repetitiveness = map.getDouble(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_REPETITIVENESS),
                            hrSlope = map.getDouble(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_HR_SLOPE),
                            isOffBody = map.getBoolean(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_IS_OFF_BODY),
                            isExercising = map.getBoolean(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_IS_EXERCISING)
                        )
                        processWindow(window)
                    }
                    com.mtpnda.biocue.core.domain.model.DataLayerSchema.PATH_EVENT_START -> {
                        val id = map.getString(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_EVENT_ID) ?: return
                        val ts = clockSkew.toPhoneTime(map.getLong(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_TIMESTAMP))
                        val trigger = map.getString(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_EVENT_TRIGGER_TYPE) ?: "MANUAL"
                        kotlinx.coroutines.runBlocking {
                            val newEvent = com.mtpnda.biocue.core.data.local.StressEventEntity(
                                id = id, userId = currentUserId, startTime = ts, endTime = null, triggerType = trigger, label = "NONE", isResolved = false
                            )
                            db.eventDao().insertEvent(newEvent)
                            syncEventToCloud(newEvent)
                        }
                    }
                    com.mtpnda.biocue.core.domain.model.DataLayerSchema.PATH_EVENT_LABEL -> {
                        val id = map.getString(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_EVENT_ID) ?: return
                        val label = map.getString(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_EVENT_LABEL) ?: "NONE"
                        kotlinx.coroutines.runBlocking {
                            val eventToUpdate = db.eventDao().getEventById(id)
                            if (eventToUpdate != null) {
                                val updated = eventToUpdate.copy(label = label, isResolved = false)
                                db.eventDao().updateEvent(updated)
                                syncEventToCloud(updated)
                            }
                        }
                    }
                    com.mtpnda.biocue.core.domain.model.DataLayerSchema.PATH_EVENT_END -> {
                        val id = map.getString(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_EVENT_ID) ?: return
                        val ts = clockSkew.toPhoneTime(map.getLong(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_TIMESTAMP))
                        kotlinx.coroutines.runBlocking {
                            val eventToUpdate = db.eventDao().getEventById(id)
                            if (eventToUpdate != null) {
                                val updated = eventToUpdate.copy(endTime = ts)
                                db.eventDao().updateEvent(updated)
                                syncEventToCloud(updated)
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun syncEventToCloud(event: com.mtpnda.biocue.core.data.local.StressEventEntity) {
        val uid = currentUserId
        if (uid == "anonymous") return
        try {
            com.mtpnda.biocue.data.FirestoreRepository.syncEvent(
                wearerUid = uid,
                eventId = event.id,
                startTime = event.startTime,
                endTime = event.endTime,
                triggerType = event.triggerType,
                label = event.label,
                isResolved = event.isResolved
            )
        } catch (e: Exception) {
            // Firestore handles offline caching automatically
        }
    }

    private fun processWindow(window: SensorWindow) {
        kotlinx.coroutines.runBlocking {
            // The decision itself (tiering, "is an event already ongoing?", orphan handling) lives in
            // DetectionPipeline so it is unit-tested and shared with iOS. This method only performs I/O.
            val latestEvent = db.eventDao().getLatestEvent(currentUserId)
            val decision = DetectionPipeline.decide(
                window = window,
                baseline = dummyBaseline,
                latestEvent = latestEvent?.let { LatestEvent(it.id, it.startTime, it.endTime) },
                nowMs = System.currentTimeMillis(),
                poorSleep = false,
                scoring = scoringEngine
            )
            Log.d("WatchDataReceiver", "Computed Fusion Score: ${decision.score}")

            // Save to database
            db.sensorDataDao().insert(
                com.mtpnda.biocue.core.data.local.SensorWindowEntity(
                    timestamp = window.timestamp,
                    hrMean = window.hrMean,
                    rmssd = window.rmssd,
                    accMag = window.accMag,
                    repetitiveness = window.repetitiveness,
                    hrSlope = window.hrSlope,
                    isOffBody = window.isOffBody,
                    isExercising = window.isExercising
                )
            )

            when (decision.tier) {
                2 -> alertManager.triggerTier2Alert(decision.score)
                1 -> alertManager.triggerTier1Alert(decision.score)
            }

            for (action in decision.actions) {
                when (action) {
                    is PipelineAction.CreateAutoEvent -> {
                        val newEvent = com.mtpnda.biocue.core.data.local.StressEventEntity(
                            id = java.util.UUID.randomUUID().toString(),
                            userId = currentUserId,
                            startTime = action.startTime,
                            endTime = null,
                            triggerType = "AUTO",
                            label = "NONE",
                            isResolved = false
                        )
                        db.eventDao().insertEvent(newEvent)
                        syncEventToCloud(newEvent)
                    }
                    is PipelineAction.CloseEvent -> {
                        // Tier 0: the person is calm again, close the ongoing event.
                        val toClose = latestEvent
                        if (toClose != null && toClose.id == action.eventId) {
                            val updated = toClose.copy(endTime = action.endTime)
                            db.eventDao().updateEvent(updated)
                            syncEventToCloud(updated)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        com.mtpnda.biocue.connect.EmulatorWatchBridge.stop()
        runCatching { unregisterReceiver(mockReceiver) }
    }
}
