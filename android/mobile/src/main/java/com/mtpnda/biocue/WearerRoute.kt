// WEARER-ONLY. Must not reference caregiver code (enforced by RoleBoundaryTest).
package com.mtpnda.biocue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun WearerDashboardRoute(viewModel: WearerDashboardViewModel, appViewModel: AppViewModel, userName: String, isStubAllowed: Boolean = false, onOpenProfile: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val approvedCaregivers = appViewModel.connectedCaregivers.collectAsState().value
    androidx.compose.runtime.LaunchedEffect(approvedCaregivers) { viewModel.startReviewImport(approvedCaregivers) }
    val mockIntensity by viewModel.mockIntensity.collectAsState()
    val tier by viewModel.tier.collectAsState()
    val sosLat by viewModel.sosLatitude.collectAsState()
    val sosLng by viewModel.sosLongitude.collectAsState()
    val stress by viewModel.stressLevel.collectAsState()
    val activity by viewModel.activityLabel.collectAsState()
    val journal by viewModel.journal.collectAsState()
    val journalOffset by viewModel.journalOffset.collectAsState()
    val reviews by viewModel.reviewEvents.collectAsState()
    val ongoingEventLabel by viewModel.ongoingEventLabel.collectAsState()
    val connStatus by viewModel.connectionStatus.collectAsState()
    val watchState by viewModel.watchState.collectAsState()
    val watchDisplayName by viewModel.watchDisplayName.collectAsState()
    val latestHeartRate by viewModel.latestHeartRate.collectAsState()
    val latestHrv by viewModel.latestHrv.collectAsState()
    val lastReadingTimestamp by viewModel.lastReadingTimestamp.collectAsState()
    val mockActivity by viewModel.mockActivity.collectAsState()
    val isCalmAudioPlaying by viewModel.isCalmAudioPlaying.collectAsState()

    com.mtpnda.biocue.ui.screens.wearer.WearerDashboardScreen(
        userName = userName,
        isStubAllowed = isStubAllowed,
        mockIntensity = mockIntensity,
        mockActivity = mockActivity,
        tier = tier,
        sosLat = sosLat ?: 0.0,
        sosLng = sosLng ?: 0.0,
        stress = stress,
        activity = activity,
        journal = journal,
        journalOffset = journalOffset,
        reviews = reviews,
        ongoingEventLabel = ongoingEventLabel,
        connStatus = connStatus,
        watchState = watchState,
        watchDisplayName = watchDisplayName,
        latestHeartRate = latestHeartRate,
        latestHrv = latestHrv,
        lastReadingTimestamp = lastReadingTimestamp,
        isCalmAudioPlaying = isCalmAudioPlaying,
        onToggleCalmAudio = { viewModel.toggleCalmAudio(context) },
        onRecheckWatch = { viewModel.recheckWatchConnection() },
        onOpenWatchManager = { viewModel.openWatchManager() },
        onPromptInstallOnWatch = { viewModel.promptInstallOnWatch() },
        onQuickCheckIn = { viewModel.recordQuickCheckIn(it) },
        onTriggerSos = { viewModel.triggerManualSos(context) },
        onSetMockIntensity = { viewModel.setMockIntensity(context, it) },
        onSetMockActivity = { viewModel.setMockActivity(it) },
        onSimulateManualEvent = { viewModel.simulateWatchManualEvent(context, it) },
        onEndOngoingEvent = { viewModel.endOngoingEvent() },
        onResolveReview = { id, label -> viewModel.resolveReviewEvent(id, label) },
        onChangeJournalDate = { delta -> viewModel.changeJournalDate(delta) },
        onOpenProfile = onOpenProfile,
        pairingCode = appViewModel.pairingCode.collectAsState().value,
        timeRemainingSeconds = appViewModel.timeRemainingSeconds.collectAsState().value,
        connectedCaregivers = appViewModel.connectedCaregivers.collectAsState().value,
        onGeneratePairingCode = { appViewModel.generatePairingCode() },
        onRemoveCaregiver = { appViewModel.removeCaregiver(it) }
    )
}
