package com.mtpnda.biocue.ui.screens.caregiver

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mtpnda.biocue.ui.theme.*
import com.mtpnda.biocue.ui.components.*
import com.mtpnda.biocue.JournalEntry
import com.mtpnda.biocue.ReviewEvent

@Composable
fun CaregiverDashboardScreen(
    userName: String,
    journal: List<JournalEntry>,
    journalOffset: Int,
    reviews: List<ReviewEvent>,
    hasOngoing: Boolean,
    onResolveReview: (String, String) -> Unit,
    onChangeJournalDate: (Int) -> Unit,
    onOpenProfile: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DashboardTopBar(isCaregiver = true, onOpenProfile = onOpenProfile)
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            if (hasOngoing) {
                item { PushNotificationPreview(userName) }
            }
            
            item { PatternReviewCard(reviews, userName, onResolve = onResolveReview) }
            item { JournalCard(journal, journalOffset, onChangeJournalDate) }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}
