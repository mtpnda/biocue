package com.mtpnda.biocue

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mtpnda.biocue.core.data.local.StressEventEntity
import com.mtpnda.biocue.data.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CaregiverDashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val firestoreRepo = FirestoreRepository

    private val _reviewEvents = MutableStateFlow<List<ReviewEvent>>(emptyList())
    val reviewEvents: StateFlow<List<ReviewEvent>> = _reviewEvents.asStateFlow()

    private val _journal = MutableStateFlow<List<JournalEntry>>(emptyList())
    val journal: StateFlow<List<JournalEntry>> = _journal.asStateFlow()

    private val _hasOngoingEvent = MutableStateFlow(false)
    val hasOngoingEvent: StateFlow<Boolean> = _hasOngoingEvent.asStateFlow()

    private val _journalOffset = MutableStateFlow(0)
    val journalOffset: StateFlow<Int> = _journalOffset.asStateFlow()
    
    private val _isCalmAudioPlaying = MutableStateFlow(false)
    val isCalmAudioPlaying = _isCalmAudioPlaying.asStateFlow()

    private var currentWearerUid: String? = null

    fun startObservation(wearerUid: String) {
        currentWearerUid = wearerUid
        viewModelScope.launch {
            combine(
                firestoreRepo.observeWearerEvents(wearerUid, 0, Long.MAX_VALUE),
                _journalOffset
            ) { events, offset ->
                Pair(events, offset)
            }.collect { (events, offset) ->
                val entityList = events.map { map ->
                    StressEventEntity(
                        id = map["id"] as? String ?: "",
                        userId = wearerUid,
                        startTime = (map["startTime"] as? Number)?.toLong() ?: 0L,
                        endTime = (map["endTime"] as? Number)?.toLong(),
                        triggerType = map["triggerType"] as? String ?: "MANUAL",
                        label = map["label"] as? String ?: "NONE",
                        isResolved = (map["isResolved"] as? Boolean) ?: false
                    )
                }.sortedByDescending { it.startTime }
                
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, offset)
                val end = cal.timeInMillis

                val unresolved = entityList.filter { !it.isResolved }
                _reviewEvents.value = unresolved.map { event ->
                    val timeStr = SimpleDateFormat("h:mm a", Locale.US).format(Date(event.startTime))
                    ReviewEvent(
                        eventId = event.id,
                        time = timeStr,
                        triggerType = event.triggerType,
                        currentLabel = event.label,
                        cues = listOf(
                            "🔄" to if (event.triggerType == "AUTO") "Detected Spontaneous Spike" else "Manually Triggered",
                            "☁️" to "Synced from Wearer"
                        )
                    )
                }

                _hasOngoingEvent.value = unresolved.any { it.endTime == null }

                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                val dayEvents = entityList.filter { it.isResolved && it.startTime >= start && it.startTime <= end }.sortedByDescending { it.startTime }
                _journal.value = dayEvents.map { event ->
                    val timeStr = SimpleDateFormat("h:mm a", Locale.US).format(Date(event.startTime))
                    val activityStr = if (event.triggerType == "MANUAL") "Manual Event" else "Detected Event"
                    val labelText = event.label
                    
                    val icon = com.mtpnda.biocue.core.domain.model.EventTypes.getIconForLabel(labelText)
                    val color = com.mtpnda.biocue.core.domain.model.EventTypes.getColorHexForLabel(labelText)
                    JournalEntry(
                        time = timeStr, place = "Synced Location", activity = activityStr, labelIcon = icon, labelText = labelText, colorHex = color
                    )
                }
            }
        }
    }

    fun resolveReviewEvent(eventId: String, newLabelOrContext: String) {
        viewModelScope.launch {
            val wearerUid = currentWearerUid ?: return@launch
            val currentEvent = _reviewEvents.value.find { it.eventId == eventId } ?: return@launch
            
            val (finalLabel, resolved) = com.mtpnda.biocue.shared.ResolveLogic.processResolution(currentEvent.currentLabel, newLabelOrContext)
            
            if (resolved) {
                _reviewEvents.value = _reviewEvents.value.filter { it.eventId != eventId }
            } else {
                _reviewEvents.value = _reviewEvents.value.map { if (it.eventId == eventId) it.copy(currentLabel = finalLabel) else it }
            }
            
            try {
                firestoreRepo.updateEventLabelAndStatus(wearerUid, eventId, finalLabel, resolved)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun changeJournalDate(delta: Int) {
        val newOffset = _journalOffset.value + delta
        if (newOffset <= 0) _journalOffset.value = newOffset
    }
}
