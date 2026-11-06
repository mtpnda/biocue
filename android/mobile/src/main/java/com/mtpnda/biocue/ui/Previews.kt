package com.mtpnda.biocue.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mtpnda.biocue.ConnectionStatus
import com.mtpnda.biocue.JournalEntry
import com.mtpnda.biocue.MockIntensity
import com.mtpnda.biocue.ReviewEvent
import com.mtpnda.biocue.connect.AccessRevokedScreen
import com.mtpnda.biocue.connect.CaregiverPairingScreen
import com.mtpnda.biocue.connect.PairingScreen
import com.mtpnda.biocue.connect.RoleSelectionScreen
import com.mtpnda.biocue.data.Pairing
import com.mtpnda.biocue.data.PairingStatus
import com.mtpnda.biocue.ui.screens.caregiver.CaregiverDashboardScreen
import com.mtpnda.biocue.ui.screens.profile.ProfileScreen
import com.mtpnda.biocue.ui.screens.wearer.WearerDashboardScreen
import com.mtpnda.biocue.ui.theme.BiocueTheme

// ---- Shared sample data ----------------------------------------------------

private val calmJournal = listOf(
    JournalEntry("10:00 AM", "Living Room", "Reading", "✨", "Excited", "#fcd34d")
)

private val elevatedJournal = listOf(
    JournalEntry("2:00 PM", "Grocery Store", "Shopping", "🌩️", "Overwhelmed", "#a5b4fc"),
    JournalEntry("9:30 AM", "Kitchen", "Cooking", "✨", "Excited", "#fcd34d"),
)

private val sampleCaregivers = listOf(
    Pairing(id = "1", caregiverName = "Maya", status = PairingStatus.APPROVED),
    Pairing(id = "2", caregiverName = "Jordan", status = PairingStatus.APPROVED),
)

@Composable
private fun WearerPreviewContent(
    tier: Int,
    connStatus: ConnectionStatus,
    stubs: Boolean = false,
    journal: List<JournalEntry> = emptyList(),
    caregivers: List<Pairing> = emptyList(),
    pairingCode: String = "123456",
) {
    BiocueTheme {
        WearerDashboardScreen(
            userName = "Alex",
            isStubAllowed = stubs,
            mockIntensity = if (stubs) MockIntensity.MEDIUM else MockIntensity.OFF,
            mockActivity = null,
            tier = tier,
            sosLat = 0.0,
            sosLng = 0.0,
            stress = if (tier == 0) 28 else 85,
            activity = "Sitting Still",
            journal = journal,
            journalOffset = 0,
            reviews = listOf(),
            ongoingEventLabel = null,
            connStatus = connStatus,
            isCalmAudioPlaying = false,
            onToggleCalmAudio = {},
            onSetMockIntensity = {},
            onSetMockActivity = {},
            onSimulateManualEvent = {},
            onEndOngoingEvent = {},
            onResolveReview = { _, _ -> },
            onChangeJournalDate = {},
            onOpenProfile = {},
            pairingCode = pairingCode,
            timeRemainingSeconds = 540,
            connectedCaregivers = caregivers,
            onGeneratePairingCode = {},
            onRemoveCaregiver = {}
        )
    }
}

// ---- Wearer ----------------------------------------------------------------

@Preview(showBackground = true, widthDp = 411, heightDp = 1250, name = "1. Wearer · Tier 0 · calm, empty circle")
@Composable
fun WearerTier0Preview() = WearerPreviewContent(
    tier = 0,
    connStatus = ConnectionStatus.CONNECTED,
    journal = calmJournal,
)

@Preview(showBackground = true, widthDp = 411, heightDp = 1500, name = "2. Wearer · Tier 2 · watch searching, full circle")
@Composable
fun WearerTier2Preview() = WearerPreviewContent(
    tier = 2,
    connStatus = ConnectionStatus.SEARCHING,
    journal = elevatedJournal,
    caregivers = sampleCaregivers,
)

@Preview(showBackground = true, widthDp = 411, heightDp = 1500, name = "3. Wearer · disconnected + dev stubs")
@Composable
fun WearerDebugPreview() = WearerPreviewContent(
    tier = 0,
    connStatus = ConnectionStatus.DISCONNECTED,
    stubs = true,
    caregivers = sampleCaregivers.take(1),
    pairingCode = "",
)

// ---- Caregiver -------------------------------------------------------------

@Preview(showBackground = true, widthDp = 411, heightDp = 1100, name = "4. Caregiver · Tier 0 · with review")
@Composable
fun CaregiverTier0Preview() {
    BiocueTheme {
        CaregiverDashboardScreen(
            userName = "Alex",
            tier = 0,
            journal = calmJournal,
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

@Preview(showBackground = true, widthDp = 411, heightDp = 1500, name = "5. Caregiver · Tier 2 · SOS + labelled review")
@Composable
fun CaregiverTier2Preview() {
    BiocueTheme {
        CaregiverDashboardScreen(
            userName = "Alex",
            tier = 2,
            journal = elevatedJournal,
            journalOffset = -1,
            reviews = listOf(
                ReviewEvent(
                    "2", "2:30 PM", "MANUAL", "OVERWHELMED",
                    listOf("🔊" to "Loud noises nearby", "👥" to "Crowded space")
                )
            ),
            hasOngoing = true,
            onResolveReview = { _, _ -> },
            onChangeJournalDate = {},
            onOpenProfile = {}
        )
    }
}

// ---- Account & connection screens -----------------------------------------

@Preview(showBackground = true, widthDp = 411, heightDp = 800, name = "6. Role selection")
@Composable
fun RoleSelectionPreview() {
    BiocueTheme { RoleSelectionScreen(onRoleSelected = {}) }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 800, name = "7. Profile")
@Composable
fun ProfilePreview() {
    BiocueTheme {
        ProfileScreen(
            userName = "Alex",
            role = "Wearer",
            onBack = {},
            onManageConnections = {},
            onLogout = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 1000, name = "8. Pairing · wearer")
@Composable
fun WearerPairingPreview() {
    BiocueTheme {
        PairingScreen(
            pairingCode = "482915",
            timeRemainingSeconds = 540,
            connectedCaregivers = sampleCaregivers.take(1),
            pendingRequests = listOf(
                Pairing(id = "3", caregiverName = "Sam", status = PairingStatus.PENDING)
            ),
            onGenerateCode = {},
            onApprove = {},
            onDeny = {},
            onRemove = {},
            onContinue = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 800, name = "9. Pairing · caregiver (code not found)")
@Composable
fun CaregiverPairingPreview() {
    BiocueTheme {
        CaregiverPairingScreen(
            status = "NOT_FOUND",
            wearerName = null,
            onSubmitCode = {},
            onUnlink = {},
            onContinue = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 700, name = "10. Access revoked")
@Composable
fun AccessRevokedPreview() {
    BiocueTheme { AccessRevokedScreen(onDismiss = {}) }
}
