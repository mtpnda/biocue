with open("mobile/src/main/java/com/mtpnda/biocue/ui/screens/caregiver/CaregiverDashboardScreen.kt", "w") as f:
    f.write("""package com.mtpnda.biocue.ui.screens.caregiver

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.mtpnda.biocue.ui.theme.*
import com.mtpnda.biocue.ui.components.*
import com.mtpnda.biocue.JournalEntry
import com.mtpnda.biocue.ReviewEvent

@Composable
fun CaregiverDashboardScreen(
    userName: String,
    tier: Int,
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
        containerColor = CanvasB,
        topBar = {
            DashboardTopBar(isCaregiver = true, onOpenProfile = onOpenProfile)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Top Status Card
            item {
                StatusMeterCard(userName, tier, "Sitting still") // Could pass activity dynamically
            }
            
            // SOS Alert
            if (tier >= 2) {
                item {
                    SosAlertCard(userName)
                }
            }
            
            // Pattern Review
            if (reviews.isNotEmpty()) {
                item { PatternReviewCard(reviews, userName, onResolve = onResolveReview) }
            }
            
            // Journal
            item { JournalCard(journal, journalOffset, onChangeJournalDate) }
            
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Preview
@Composable
fun CaregiverDashboardScreenPreviewTier0() {
    BiocueTheme {
        CaregiverDashboardScreen(
            userName = "Alex",
            tier = 0,
            journal = listOf(
                JournalEntry("10:00 AM", "Living Room", "Reading", "NONE", "NONE", "#34D399")
            ),
            journalOffset = 0,
            reviews = listOf(),
            hasOngoing = false,
            onResolveReview = { _, _ -> },
            onChangeJournalDate = {},
            onOpenProfile = {}
        )
    }
}

@Preview
@Composable
fun CaregiverDashboardScreenPreviewTier2() {
    BiocueTheme {
        CaregiverDashboardScreen(
            userName = "Alex",
            tier = 2,
            journal = listOf(
                JournalEntry("2:00 PM", "Grocery Store", "Shopping", "OVERWHELMED", "Overwhelmed", "#EF4444")
            ),
            journalOffset = 0,
            reviews = listOf(
                ReviewEvent("1", "2:30 PM", "AUTO", "NONE", listOf("Noise" to "Loud"))
            ),
            hasOngoing = true,
            onResolveReview = { _, _ -> },
            onChangeJournalDate = {},
            onOpenProfile = {}
        )
    }
}
""")

