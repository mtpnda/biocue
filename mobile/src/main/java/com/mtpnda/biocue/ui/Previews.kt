package com.mtpnda.biocue.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mtpnda.biocue.ui.theme.BiocueTheme
import com.mtpnda.biocue.ConnectionStatus
import com.mtpnda.biocue.MockIntensity
import com.mtpnda.biocue.ReviewEvent
import com.mtpnda.biocue.ui.screens.wearer.WearerDashboardScreen
import com.mtpnda.biocue.ui.screens.caregiver.CaregiverDashboardScreen

@Preview(showBackground = true, name = "1. Wearer Platform (Complete)")
@Composable
fun WearerPlatformCompletePreview() {
    BiocueTheme {
        WearerDashboardScreen(
            userName = "Alex",
            isStubAllowed = false,
            mockIntensity = MockIntensity.OFF,
            mockActivity = null,
            tier = 0,
            sosLat = 0.0,
            sosLng = 0.0,
            stress = 28,
            activity = "Sitting Still",
            journal = listOf(),
            journalOffset = 0,
            reviews = listOf(),
            ongoingEventLabel = null,
            connStatus = ConnectionStatus.CONNECTED,
            isCalmAudioPlaying = false,
            onToggleCalmAudio = {},
            onSetMockIntensity = {},
            onSetMockActivity = {},
            onSimulateManualEvent = {},
            onEndOngoingEvent = {},
            onResolveReview = { _, _ -> },
            onChangeJournalDate = {},
            onOpenProfile = {}
        )
    }
}

@Preview(showBackground = true, name = "2. Wearer Platform + Debug")
@Composable
fun WearerPlatformDebugPreview() {
    BiocueTheme {
        WearerDashboardScreen(
            userName = "Alex",
            isStubAllowed = true,
            mockIntensity = MockIntensity.MEDIUM,
            mockActivity = "STIMMING",
            tier = 0,
            sosLat = 0.0,
            sosLng = 0.0,
            stress = 60,
            activity = "Pacing / Stimming",
            journal = listOf(),
            journalOffset = 0,
            reviews = listOf(),
            ongoingEventLabel = null,
            connStatus = ConnectionStatus.CONNECTED,
            isCalmAudioPlaying = true,
            onToggleCalmAudio = {},
            onSetMockIntensity = {},
            onSetMockActivity = {},
            onSimulateManualEvent = {},
            onEndOngoingEvent = {},
            onResolveReview = { _, _ -> },
            onChangeJournalDate = {},
            onOpenProfile = {}
        )
    }
}

@Preview(showBackground = true, name = "3. Caregiver Platform (Complete)")
@Composable
fun CaregiverPlatformCompletePreview() {
    BiocueTheme {
        CaregiverDashboardScreen(
            userName = "Alex",
            tier = 0,
            journal = listOf(),
            journalOffset = 0,
            reviews = listOf(
                ReviewEvent("1", "9:00 AM", "AUTO", "NONE", listOf("🔄" to "Detected Spontaneous Spike"))
            ),
            hasOngoing = false,
            onResolveReview = { _, _ -> },
            onChangeJournalDate = {},
            onOpenProfile = {}
        )
    }
}
