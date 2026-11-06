// CAREGIVER-ONLY. Must not reference wearer or watch code (enforced by RoleBoundaryTest).
package com.mtpnda.biocue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun CaregiverDashboardRoute(viewModel: CaregiverDashboardViewModel, userName: String, onOpenProfile: () -> Unit) {
    val journal by viewModel.journal.collectAsState()
    val journalOffset by viewModel.journalOffset.collectAsState()
    val reviews by viewModel.reviewEvents.collectAsState()
    val hasOngoing by viewModel.hasOngoingEvent.collectAsState()
    val tier = if (hasOngoing) 2 else 0

    com.mtpnda.biocue.ui.screens.caregiver.CaregiverDashboardScreen(
        userName = userName,
        tier = tier,
        journal = journal,
        journalOffset = journalOffset,
        reviews = reviews,
        hasOngoing = hasOngoing,
        onResolveReview = { id, label -> viewModel.resolveReviewEvent(id, label) },
        onChangeJournalDate = { viewModel.changeJournalDate(it) },
        onOpenProfile = onOpenProfile
    )
}
