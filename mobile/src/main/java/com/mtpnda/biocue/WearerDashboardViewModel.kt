package com.mtpnda.biocue

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mtpnda.biocue.core.data.local.SensorWindowEntity
import com.mtpnda.biocue.core.domain.model.Baseline
import com.mtpnda.biocue.core.ml.ScoringEngine
import com.mtpnda.biocue.core.domain.model.SensorWindow
import com.mtpnda.biocue.data.FirestoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WearerDashboardViewModel(
    application: Application,
    private val db: com.mtpnda.biocue.core.data.local.DetectionDatabase
) : AndroidViewModel(application) {
    private val scoringEngine = ScoringEngine()
    private val firestoreRepo = FirestoreRepository

    private val baseline = Baseline(hourOfDay = 12, hrMean = 75.0, hrStd = 5.0, hrvMean = 40.0, hrvStd = 8.0)

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.CONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _stressLevel = MutableStateFlow(0)
    val stressLevel: StateFlow<Int> = _stressLevel.asStateFlow()

    private val _tier = MutableStateFlow(0)
    val tier: StateFlow<Int> = _tier.asStateFlow()

    private val _activityLabel = MutableStateFlow("🪑 Sitting Still")
    val activityLabel: StateFlow<String> = _activityLabel.asStateFlow()

    private val _mockActivity = MutableStateFlow<String?>(null)
    val mockActivity: StateFlow<String?> = _mockActivity.asStateFlow()

    private val _mockIntensity = MutableStateFlow(MockIntensity.OFF)
    val mockIntensity: StateFlow<MockIntensity> = _mockIntensity.asStateFlow()

    private val _sosLatitude = MutableStateFlow<Double?>(null)
    val sosLatitude: StateFlow<Double?> = _sosLatitude.asStateFlow()

    private val _sosLongitude = MutableStateFlow<Double?>(null)
    val sosLongitude: StateFlow<Double?> = _sosLongitude.asStateFlow()

    private val _reviewEvents = MutableStateFlow<List<ReviewEvent>>(emptyList())
    val reviewEvents: StateFlow<List<ReviewEvent>> = _reviewEvents.asStateFlow()

    private val _journal = MutableStateFlow<List<JournalEntry>>(emptyList())
    val journal: StateFlow<List<JournalEntry>> = _journal.asStateFlow()

    private val _ongoingEventLabel = MutableStateFlow<String?>(null)
    val ongoingEventLabel: StateFlow<String?> = _ongoingEventLabel.asStateFlow()

    private val _journalOffset = MutableStateFlow(0)
    val journalOffset: StateFlow<Int> = _journalOffset.asStateFlow()

    private val _isCalmAudioPlaying = MutableStateFlow(false)
    val isCalmAudioPlaying = _isCalmAudioPlaying.asStateFlow()
    private var mediaPlayer: MediaPlayer? = null

    private val currentUserId: String
        get() = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

    init {
        startLocalPolling()
    }

    private fun startLocalPolling() {
        val uid = currentUserId
        if (uid == "anonymous") return

        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val latestData = db.sensorDataDao().getRecentWindows(1).firstOrNull()
                    if (latestData != null) {
                        processRealData(latestData)
                        val ageMs = System.currentTimeMillis() - latestData.timestamp
                        _connectionStatus.value = if (ageMs > 30000) ConnectionStatus.DISCONNECTED else ConnectionStatus.CONNECTED
                    } else {
                        _connectionStatus.value = ConnectionStatus.SEARCHING
                    }

                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, _journalOffset.value)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val start = cal.timeInMillis
                    
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    val end = cal.timeInMillis

                    val unresolved = db.eventDao().getUnresolvedEvents(uid)
                    val ongoing = unresolved.find { it.endTime == null }
                    _ongoingEventLabel.value = ongoing?.label

                    _reviewEvents.value = unresolved.map { event ->
                        val timeStr = SimpleDateFormat("h:mm a", Locale.US).format(Date(event.startTime))
                        ReviewEvent(
                            eventId = event.id,
                            time = timeStr,
                            triggerType = event.triggerType,
                            currentLabel = event.label,
                            cues = listOf(
                                "🔄" to if (event.triggerType == "AUTO") "Detected Spontaneous Spike" else "Manually Triggered",
                                "📍" to if (_sosLatitude.value != null) "123 Main St, San Francisco, CA" else "Unknown Location"
                            )
                        )
                    }

                    val resolvedEvents = db.eventDao().getEventsBetween(uid, start, end).filter { it.isResolved }
                    val clusteredEvents = com.mtpnda.biocue.core.domain.EventClusterer.cluster(resolvedEvents)

                    _journal.value = clusteredEvents.map { cluster ->
                        val timeStr = SimpleDateFormat("h:mm a", Locale.US).format(Date(cluster.startTime))
                        val uniqueLabels = cluster.events.map { it.label }.filter { it != "NONE" }.distinct()
                        val labelText = if (uniqueLabels.isEmpty()) "Unknown" else uniqueLabels.joinToString(" + ")
                        val hasAuto = cluster.events.any { it.triggerType == "AUTO" }
                        val hasManual = cluster.events.any { it.triggerType == "MANUAL" }
                        val activityStr = when {
                            hasAuto && hasManual -> "Auto-detected + Manual Input"
                            hasAuto -> "Auto-detected"
                            else -> "Manual"
                        }
                        val icon = com.mtpnda.biocue.core.domain.model.EventTypes.getIconForLabel(labelText)
                        val color = com.mtpnda.biocue.core.domain.model.EventTypes.getColorHexForLabel(labelText)
                        JournalEntry(
                            time = timeStr,
                            place = "Logged Location",
                            activity = activityStr,
                            labelIcon = icon,
                            labelText = labelText,
                            colorHex = color
                        )
                    }

                    // Sync up to Firebase
                    // Note: Removed continuous status sync to save cloud cost/battery.
                    // Only Event-driven data (Journal/Meltdowns) is synced now.
                } catch (e: Exception) {}
                delay(2000)
            }
        }
    }

    private fun processRealData(entity: SensorWindowEntity) {
        val window = SensorWindow(
            timestamp = entity.timestamp, hrMean = entity.hrMean, rmssd = entity.rmssd,
            accMag = entity.accMag, repetitiveness = entity.repetitiveness,
            hrSlope = entity.hrSlope, isOffBody = entity.isOffBody, isExercising = entity.isExercising
        )
        val score = scoringEngine.computeRuleBasedScore(window, baseline, false)
        _stressLevel.value = ((score / 3.0) * 100).toInt().coerceIn(0, 100)
        
        val tier = when {
            score > 2.5 -> 2
            score > 1.5 -> 1
            else -> 0
        }
        _tier.value = tier
        if (tier == 2) requestSosLocation()

        _activityLabel.value = when {
            entity.isExercising -> "🏃 Exercising"
            entity.repetitiveness > 0.5 -> "🔄 Pacing / Stimming"
            entity.accMag > 0.2 -> "🚶 Active"
            else -> "🪑 Sitting Still"
        }
    }

    fun setMockActivity(activity: String?) { _mockActivity.value = activity }

    fun setMockIntensity(context: Context, intensity: MockIntensity) {
        _mockIntensity.value = intensity
        if (intensity != MockIntensity.OFF) {
            viewModelScope.launch(Dispatchers.IO) {
                while (_mockIntensity.value != MockIntensity.OFF) {
                    val currentMock = _mockIntensity.value
                    val overrideActivity = _mockActivity.value

                    val hr = when (currentMock) {
                        MockIntensity.LOW -> 72.0
                        MockIntensity.MEDIUM -> 88.0
                        MockIntensity.HIGH -> 110.0
                        else -> 70.0
                    }
                    val hrv = when (currentMock) {
                        MockIntensity.LOW -> 45.0
                        MockIntensity.MEDIUM -> 22.0
                        MockIntensity.HIGH -> 10.0
                        else -> 50.0
                    }

                    val entity = com.mtpnda.biocue.core.data.local.SensorWindowEntity(
                        timestamp = System.currentTimeMillis(),
                        hrMean = hr,
                        rmssd = hrv,
                        accMag = if (overrideActivity == "SITTING") 0.1 else if (overrideActivity == "STIMMING") 0.4 else if (overrideActivity == "EXERCISING") 0.8 else 0.1,
                        repetitiveness = if (overrideActivity == "STIMMING") 0.8 else 0.1,
                        hrSlope = 0.0,
                        isOffBody = false,
                        isExercising = overrideActivity == "EXERCISING"
                    )

                    db.sensorDataDao().insert(entity)
                    
                    val trigger = intentTriggerMap[currentMock]
                    if (trigger != null) {
                        val intent = Intent("com.mtpnda.biocue.MOCK_EVENT_TRIGGER").apply { putExtra("triggerType", trigger) }
                        context.sendBroadcast(intent)
                    }

                    delay(2000)
                }
            }
        }
    }
    
    private val intentTriggerMap = mapOf(
        MockIntensity.LOW to "CALM",
        MockIntensity.MEDIUM to "ELEVATED",
        MockIntensity.HIGH to "OVERWHELMED"
    )

    fun updateSosLocation(lat: Double, lng: Double) {
        _sosLatitude.value = lat
        _sosLongitude.value = lng
    }

    private fun requestSosLocation() {
        val application = getApplication<Application>()
        val messageClient = com.google.android.gms.wearable.Wearable.getMessageClient(application)
        com.google.android.gms.wearable.Wearable.getNodeClient(application).connectedNodes.addOnSuccessListener { nodes ->
            nodes.firstOrNull()?.let { node -> messageClient.sendMessage(node.id, "/sos_gps_request", ByteArray(0)) }
        }
    }

    fun simulateWatchManualEvent(context: Context, labelStr: String) {
        updateSosLocation(37.7749, -122.4194)
        val uid = currentUserId
        if (uid == "anonymous") return

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val eventId = java.util.UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            
            val newEvent = com.mtpnda.biocue.core.data.local.StressEventEntity(
                id = eventId,
                userId = uid,
                startTime = now,
                endTime = null,
                triggerType = "MANUAL",
                label = labelStr,
                isResolved = false
            )
            
            // 1. Save locally
            db.eventDao().insertEvent(newEvent)
            
            // 2. Sync to firestore
            try {
                com.mtpnda.biocue.data.FirestoreRepository.syncEvent(
                    wearerUid = uid,
                    eventId = eventId,
                    startTime = now,
                    endTime = null,
                    triggerType = "MANUAL",
                    label = labelStr,
                    isResolved = false
                )
            } catch (e: Exception) {}
            
            // Show toast on Main thread
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Simulated $labelStr sent to Caregiver", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun endOngoingEvent() {
        val uid = currentUserId
        if (uid == "anonymous") return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val ongoingEvent = db.eventDao().getLatestEvent(uid)
            if (ongoingEvent != null && ongoingEvent.endTime == null) {
                val now = System.currentTimeMillis()
                val updated = ongoingEvent.copy(endTime = now)
                db.eventDao().updateEvent(updated)
                try {
                    com.mtpnda.biocue.data.FirestoreRepository.syncEvent(
                        wearerUid = uid,
                        eventId = updated.id,
                        startTime = updated.startTime,
                        endTime = now,
                        triggerType = updated.triggerType,
                        label = updated.label,
                        isResolved = updated.isResolved
                    )
                } catch (e: Exception) {}
            }
        }
    }

    fun resolveReviewEvent(eventId: String, newLabelOrContext: String) {
        viewModelScope.launch {
            val event = db.eventDao().getEventById(eventId)
            if (event != null) {
                val (finalLabel, resolved) = com.mtpnda.biocue.shared.ResolveLogic.processResolution(event.label, newLabelOrContext)
                val updated = event.copy(label = finalLabel, isResolved = resolved)
                db.eventDao().updateEvent(updated)
                try {
                    firestoreRepo.syncEvent(
                        wearerUid = currentUserId,
                        eventId = updated.id,
                        startTime = updated.startTime,
                        endTime = updated.endTime,
                        triggerType = updated.triggerType,
                        label = updated.label,
                        isResolved = updated.isResolved
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun changeJournalDate(delta: Int) {
        val newOffset = _journalOffset.value + delta
        if (newOffset <= 0) _journalOffset.value = newOffset
    }

    fun toggleCalmAudio(context: Context) {
        if (_isCalmAudioPlaying.value) {
            mediaPlayer?.pause()
            _isCalmAudioPlaying.value = false
        } else {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(context, R.raw.calm_audio)
                mediaPlayer?.isLooping = true
            }
            mediaPlayer?.start()
            _isCalmAudioPlaying.value = true
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
