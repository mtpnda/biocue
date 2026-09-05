package com.mtpnda.biocue

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.mtpnda.biocue.connect.WatchConnectionManager
import com.mtpnda.biocue.connect.ConnectionState
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.mtpnda.biocue.core.data.local.DetectionDatabase
import com.mtpnda.biocue.core.data.local.SensorWindowEntity
import com.mtpnda.biocue.core.domain.model.Baseline
import com.mtpnda.biocue.core.domain.model.SensorWindow
import com.mtpnda.biocue.core.ml.ScoringEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class JournalEntry(
    val time: String,
    val place: String,
    val activity: String,
    val labelIcon: String,
    val labelText: String,
    val colorHex: String
)

data class ReviewEvent(
    val eventId: String,
    val time: String,
    val triggerType: String,
    val currentLabel: String,
    val cues: List<Pair<String, String>> // icon to text
)

enum class ConnectionStatus {
    CONNECTED, DISCONNECTED, OFF_BODY
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName.asStateFlow()

    fun setUserName(name: String) {
        _userName.value = name
    }

    private val db by lazy {
        Room.databaseBuilder(
            application,
            DetectionDatabase::class.java, "sensor-database"
        ).build()
    }

    private val scoringEngine = ScoringEngine()
    private val baseline = Baseline(hourOfDay = 12, hrMean = 75.0, hrStd = 5.0, hrvMean = 40.0, hrvStd = 8.0)

    val watchConnectionManager = WatchConnectionManager(application, viewModelScope)
    val watchConnectionState: StateFlow<ConnectionState> = watchConnectionManager.state
    val watchName: StateFlow<String?> = watchConnectionManager.watchName

    private val _recentWindows = MutableStateFlow<List<SensorWindowEntity>>(emptyList())
    val recentWindows: StateFlow<List<SensorWindowEntity>> = _recentWindows

    private val _isCaregiverMode = MutableStateFlow(false)
    val isCaregiverMode = _isCaregiverMode.asStateFlow()

    private val _stressLevel = MutableStateFlow(0)
    val stressLevel = _stressLevel.asStateFlow()

    private val _activityLabel = MutableStateFlow("Waiting for data...")
    val activityLabel = _activityLabel.asStateFlow()

    
    private val _sosLatitude = MutableStateFlow<Double?>(null)
    val sosLatitude: kotlinx.coroutines.flow.StateFlow<Double?> = _sosLatitude.asStateFlow()

    private val _sosLongitude = MutableStateFlow<Double?>(null)
    val sosLongitude: kotlinx.coroutines.flow.StateFlow<Double?> = _sosLongitude.asStateFlow()
    
    fun updateSosLocation(lat: Double, lng: Double) {
        _sosLatitude.value = lat
        _sosLongitude.value = lng
    }

    private val _tier = MutableStateFlow(0)
    val tier = _tier.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus = _connectionStatus.asStateFlow()

    private val _journal = MutableStateFlow<List<JournalEntry>>(emptyList())
    val journal = _journal.asStateFlow()

    private val _reviewEvents = MutableStateFlow<List<ReviewEvent>>(emptyList())
    val reviewEvents = _reviewEvents.asStateFlow()

    init {
        watchConnectionManager.startMonitoring()
        startPollingData()
    }

    private fun startPollingData() {
        viewModelScope.launch {
            while (true) {
                try {
                    // Update connection state based on watch node reachability and recent sensor data
                    val currentWatchState = watchConnectionState.value
                    val windows = db.sensorDataDao().getRecentWindows(1)
                    
                    if (currentWatchState == ConnectionState.CONNECTED) {
                        if (windows.isNotEmpty()) {
                            val latest = windows.first()
                            val timeSinceLastUpdate = System.currentTimeMillis() - latest.timestamp
                            if (latest.isOffBody && timeSinceLastUpdate < 120_000) {
                                _connectionStatus.value = ConnectionStatus.OFF_BODY
                            } else {
                                _connectionStatus.value = ConnectionStatus.CONNECTED
                                if (timeSinceLastUpdate < 120_000) {
                                    processRealData(latest)
                                }
                            }
                        } else {
                            _connectionStatus.value = ConnectionStatus.CONNECTED
                        }
                    } else {
                        _connectionStatus.value = ConnectionStatus.DISCONNECTED
                    }
                    
                    // Update Journal and Pattern Review from DB
                    val unresolved = db.eventDao().getUnresolvedEvents()
                    val reviewList = unresolved.map { event ->
                        val timeStr = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US).format(java.util.Date(event.startTime))
                        ReviewEvent(
                            eventId = event.id,
                            time = timeStr,
                            triggerType = event.triggerType,
                            currentLabel = event.label,
                            cues = listOf(
                                "🔄" to if (event.triggerType == "AUTO") "Detected Spontaneous Spike" else "Manually Triggered",
                                "📍" to "Unknown Location"
                            )
                        )
                    }
                    _reviewEvents.value = reviewList

                    // Start of today for journal
                    val cal = java.util.Calendar.getInstance()
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    
                    val todayEvents = db.eventDao().getEventsSince(cal.timeInMillis)
                    val journalList = todayEvents.filter { it.isResolved }.map { event ->
                        val timeStr = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US).format(java.util.Date(event.startTime))
                        val (icon, color) = when (event.label) {
                            "OVERWHELMED" -> "🌩️" to "#a5b4fc"
                            "EXCITED" -> "✨" to "#fcd34d"
                            "ACTIVE" -> "🏃" to "#cbd5e1"
                            else -> "🤷" to "#94a3b8"
                        }
                        JournalEntry(
                            time = timeStr,
                            place = "Logged Location",
                            activity = if (event.triggerType == "AUTO") "Auto-detected" else "Manual",
                            labelIcon = icon,
                            labelText = event.label,
                            colorHex = color
                        )
                    }
                    _journal.value = journalList

                } catch (e: Exception) {
                    // Ignore in preview
                }
                delay(2000)
            }
        }
    }

    private fun processRealData(entity: SensorWindowEntity) {
        val window = SensorWindow(
            timestamp = entity.timestamp,
            hrMean = entity.hrMean,
            rmssd = entity.rmssd,
            accMag = entity.accMag,
            repetitiveness = entity.repetitiveness,
            hrSlope = entity.hrSlope,
            isOffBody = entity.isOffBody,
            isExercising = entity.isExercising
        )

        val score = scoringEngine.computeRuleBasedScore(window, baseline, false)
        
        val mappedStress = ((score / 3.0) * 100).toInt().coerceIn(0, 100)
        _stressLevel.value = mappedStress

        
        val tier = when {
            score > 2.5 -> 2
            score > 1.5 -> 1
            else -> 0
        }
        _tier.value = tier
        
        if (tier == 2) {
            requestSosLocation()
        }


        _activityLabel.value = when {
            entity.isExercising -> "🏃 Exercising"
            entity.repetitiveness > 0.5 -> "🔄 Pacing / Stimming"
            entity.accMag > 0.2 -> "🚶 Active"
            else -> "🪑 Sitting Still"
        }
    }

    fun toggleMode() {
        _isCaregiverMode.value = !_isCaregiverMode.value
    }
    
    fun resolveReviewEvent(eventId: String, newLabelOrContext: String) {
        viewModelScope.launch {
            val event = db.eventDao().getEventById(eventId)
            if (event != null) {
                val finalLabel = if (event.label == "NONE") {
                    newLabelOrContext
                } else {
                    "${event.label} - $newLabelOrContext"
                }
                db.eventDao().updateEvent(event.copy(label = finalLabel, isResolved = true))
            }
        }
    }

    private fun requestSosLocation() {
        android.util.Log.d("MainViewModel", "Tier 2 Detected! Requesting SOS GPS from Watch...")
        val application = getApplication<android.app.Application>()
        val messageClient = com.google.android.gms.wearable.Wearable.getMessageClient(application)
        val nodeClient = com.google.android.gms.wearable.Wearable.getNodeClient(application)
        
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            nodes.firstOrNull()?.also { node ->
                messageClient.sendMessage(node.id, "/sos_gps_request", ByteArray(0))
            }
        }
    }
}

