package com.mtpnda.biocue

import android.util.Log
import com.mtpnda.biocue.core.domain.model.SensorWindow
import com.mtpnda.biocue.core.domain.model.Baseline
import com.mtpnda.biocue.core.ml.ScoringEngine
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
    private lateinit var alertManager: AlertManager
    
            val db by lazy {
                androidx.room.Room.databaseBuilder(
                    applicationContext,
                    com.mtpnda.biocue.core.data.local.DetectionDatabase::class.java, "sensor-database"
                ).build()
            }
    
    // Hardcoded baseline for demonstration
    private val dummyBaseline = Baseline(
        hourOfDay = 12, hrMean = 75.0, hrStd = 5.0, hrvMean = 40.0, hrvStd = 8.0
    )

    override fun onCreate() {
        super.onCreate()
        alertManager = AlertManager(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path
                Log.d("WatchDataReceiver", "onDataChanged received path: $path from URI ${event.dataItem.uri}")
                val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                val map = dataMapItem.dataMap
                
                when (path) {
                    com.mtpnda.biocue.core.domain.model.DataLayerSchema.PATH_SENSOR_WINDOW -> {
                        val window = SensorWindow(
                            timestamp = map.getLong(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_TIMESTAMP),
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
                        val ts = map.getLong(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_TIMESTAMP)
                        val trigger = map.getString(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_EVENT_TRIGGER_TYPE) ?: "MANUAL"
                        kotlinx.coroutines.runBlocking {
                            db.eventDao().insertEvent(
                                com.mtpnda.biocue.core.data.local.StressEventEntity(
                                    id = id, startTime = ts, endTime = null, triggerType = trigger, label = "NONE", isResolved = false
                                )
                            )
                        }
                    }
                    com.mtpnda.biocue.core.domain.model.DataLayerSchema.PATH_EVENT_LABEL -> {
                        val id = map.getString(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_EVENT_ID) ?: return
                        val label = map.getString(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_EVENT_LABEL) ?: "NONE"
                        kotlinx.coroutines.runBlocking {
                            val eventToUpdate = db.eventDao().getEventById(id)
                            if (eventToUpdate != null) {
                                // Wearer labeled it, but we still want Caregiver to provide context, so leave isResolved = false
                                db.eventDao().updateEvent(eventToUpdate.copy(label = label, isResolved = false))
                            }
                        }
                    }
                    com.mtpnda.biocue.core.domain.model.DataLayerSchema.PATH_EVENT_END -> {
                        val id = map.getString(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_EVENT_ID) ?: return
                        val ts = map.getLong(com.mtpnda.biocue.core.domain.model.DataLayerSchema.KEY_TIMESTAMP)
                        kotlinx.coroutines.runBlocking {
                            val eventToUpdate = db.eventDao().getEventById(id)
                            if (eventToUpdate != null) {
                                db.eventDao().updateEvent(eventToUpdate.copy(endTime = ts))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun processWindow(window: SensorWindow) {
        kotlinx.coroutines.runBlocking {
            val score = scoringEngine.computeRuleBasedScore(window, dummyBaseline, poorSleepFlag = false)
            Log.d("WatchDataReceiver", "Computed Fusion Score: $score")
            
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

            if (score > 2.5) {
                alertManager.triggerTier2Alert(score)
                
                // AUTO EVENT GENERATION
                val latestEvent = db.eventDao().getLatestEvent()
                // If there's no ongoing event (either null, or the last one ended, OR the last one started >15 mins ago and is orphaned)
                val isOngoing = latestEvent != null && latestEvent.endTime == null && (System.currentTimeMillis() - latestEvent.startTime < 15 * 60 * 1000)
                
                if (!isOngoing) {
                    val newId = java.util.UUID.randomUUID().toString()
                    db.eventDao().insertEvent(
                        com.mtpnda.biocue.core.data.local.StressEventEntity(
                            id = newId,
                            startTime = window.timestamp,
                            endTime = null,
                            triggerType = "AUTO",
                            label = "NONE",
                            isResolved = false
                        )
                    )
                }
            } else if (score > 1.5) {
                alertManager.triggerTier1Alert(score)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
