package com.mtpnda.biocue

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mtpnda.biocue.core.data.local.StressEventEntity
import com.mtpnda.biocue.data.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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
    private var observeJob: kotlinx.coroutines.Job? = null
    private val reviewSubmission = com.mtpnda.biocue.core.pairing.ReviewSubmission(
        com.mtpnda.biocue.data.FirestorePairingStore(), com.mtpnda.biocue.core.pairing.Clock { System.currentTimeMillis() })

    /** Spec 07: on revocation, drop everything cached in memory about the wearer. */
    fun wipe() {
        observeJob?.cancel(); observeJob = null
        currentWearerUid = null
        _reviewEvents.value = emptyList()
        _journal.value = emptyList()
        _hasOngoingEvent.value = false
    }

    fun startObservation(wearerUid: String) {
        wipe()   // never show a previous wearer's data while the new one loads
        currentWearerUid = wearerUid
        val caregiverUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val pairingId = "${wearerUid}_$caregiverUid"
        observeJob = viewModelScope.launch {
            combine(
                firestoreRepo.observeWearerEvents(wearerUid, 0, Long.MAX_VALUE),
                firestoreRepo.observeReviews(pairingId),
                _journalOffset
            ) { events: List<Map<String, Any>>, reviews: Map<String, com.mtpnda.biocue.core.pairing.Review>, offset: Int ->
                Triple(events, reviews, offset)
            }.catch { android.util.Log.e("CaregiverVM", "observation ended", it) }.collect { (events, reviews, offset) ->
                val entityList: List<StressEventEntity> = events.map { map ->
                    StressEventEntity(
                        id = map["id"] as? String ?: "",
                        userId = wearerUid,
                        startTime = (map["startTime"] as? Number)?.toLong() ?: 0L,
                        endTime = (map["endTime"] as? Number)?.toLong(),
                        triggerType = map["triggerType"] as? String ?: "MANUAL",
                        label = map["label"] as? String ?: "NONE",
                        isResolved = (map["isResolved"] as? Boolean) ?: false
                    )
                }.map { entity ->
                    // Spec 07: the caregiver's own review is shown on events that are still open (first resolution wins).
                    com.mtpnda.biocue.core.pairing.ReviewApplier.effective(entity, reviews[entity.id])
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
            val caregiverUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val currentEvent = _reviewEvents.value.find { it.eventId == eventId } ?: return@launch

            // Optimistic UI, identical staging rules to before (they live in ResolveLogic).
            val (finalLabel, resolved) = com.mtpnda.biocue.core.domain.ResolveLogic.processResolution(currentEvent.currentLabel, newLabelOrContext)
            if (resolved) {
                _reviewEvents.value = _reviewEvents.value.filter { it.eventId != eventId }
            } else {
                _reviewEvents.value = _reviewEvents.value.map { if (it.eventId == eventId) it.copy(currentLabel = finalLabel) else it }
            }

            try {
                // Spec 07: the review goes to pairings/{id}/reviews/{eventId} — caregivers never write to the wearer's event.
                val pairing = com.mtpnda.biocue.core.pairing.Pairing("${wearerUid}_$caregiverUid", wearerUid, caregiverUid, "", "",
                    com.mtpnda.biocue.core.pairing.PairingStatus.APPROVED, 0L)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    reviewSubmission.submit(pairing, eventId, currentEvent.currentLabel, newLabelOrContext)
                }
            } catch (e: Exception) {
                android.util.Log.e("CaregiverVM", "could not submit review", e)
            }
        }
    }

    fun changeJournalDate(delta: Int) {
        val newOffset = _journalOffset.value + delta
        if (newOffset <= 0) _journalOffset.value = newOffset
    }
}
