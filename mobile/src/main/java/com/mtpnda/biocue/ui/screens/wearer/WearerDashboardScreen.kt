package com.mtpnda.biocue.ui.screens.wearer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtpnda.biocue.ui.theme.*
import com.mtpnda.biocue.ui.components.*
import com.mtpnda.biocue.JournalEntry
import com.mtpnda.biocue.ReviewEvent
import com.mtpnda.biocue.ConnectionStatus
import com.mtpnda.biocue.MockIntensity
import com.mtpnda.biocue.ui.theme.BiocueTheme

@Composable
fun WearerDashboardScreen(
    userName: String,
    isStubAllowed: Boolean,
    mockIntensity: MockIntensity,
    mockActivity: String?,
    tier: Int,
    sosLat: Double,
    sosLng: Double,
    stress: Int,
    activity: String,
    journal: List<JournalEntry>,
    journalOffset: Int,
    reviews: List<ReviewEvent>,
    ongoingEventLabel: String?,
    connStatus: ConnectionStatus,
    isCalmAudioPlaying: Boolean,
    onToggleCalmAudio: () -> Unit,
    onSetMockIntensity: (MockIntensity) -> Unit,
    onSetMockActivity: (String?) -> Unit,
    onSimulateManualEvent: (String) -> Unit,
    onEndOngoingEvent: () -> Unit,
    onResolveReview: (String, String) -> Unit,
    onChangeJournalDate: (Int) -> Unit,
    onOpenProfile: () -> Unit
) {
    var isGeneratingPairingCode by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DashboardTopBar(isCaregiver = false, onOpenProfile = onOpenProfile)
        },
        containerColor = Color(0xFF000000)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item { CurrentStateCard(stress, activity, connStatus, userName) }

            if (tier >= 2) {
                item { PushNotificationPreview(userName) }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        SectionLabel("Quick Actions")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                LabelBtn(
                                    icon = if (isCalmAudioPlaying) "✓" else "🎵",
                                    text = if (isCalmAudioPlaying) "Playing" else "Calm Audio",
                                    color = if (isCalmAudioPlaying) CalmColor else Color.White,
                                    onClick = onToggleCalmAudio
                                )
                            }
                        }
                    }
                }
            }

            item { PatternReviewCard(reviews, userName, onResolve = onResolveReview) }
            item { JournalCard(journal, journalOffset, onChangeJournalDate) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        SectionLabel("Your Caregivers")
                        Spacer(modifier = Modifier.height(12.dp))
                        if (isGeneratingPairingCode) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "842 195",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 4.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Expires in 09:59",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            Button(
                                onClick = { isGeneratingPairingCode = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A))
                            ) {
                                Text("Generate pairing code", color = Color.White)
                            }
                        }
                    }
                }
            }

            if (isStubAllowed) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF27272A))) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Developer Stubs", color = Color.Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            Text("Continuous Sensor Mock", color = Color.Gray, fontSize = 12.sp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onSetMockIntensity(MockIntensity.LOW) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (mockIntensity == MockIntensity.LOW) Color.Yellow else Color.DarkGray)) {
                                    Text("Low", fontSize = 12.sp, color = if (mockIntensity == MockIntensity.LOW) Color.Black else Color.White)
                                }
                                Button(onClick = { onSetMockIntensity(MockIntensity.MEDIUM) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (mockIntensity == MockIntensity.MEDIUM) Color.Yellow else Color.DarkGray)) {
                                    Text("Medium", fontSize = 12.sp, color = if (mockIntensity == MockIntensity.MEDIUM) Color.Black else Color.White)
                                }
                                Button(onClick = { onSetMockIntensity(MockIntensity.HIGH) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (mockIntensity == MockIntensity.HIGH) Color.Yellow else Color.DarkGray)) {
                                    Text("High", fontSize = 12.sp, color = if (mockIntensity == MockIntensity.HIGH) Color.Black else Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Simulate Activity Override", color = Color.Gray, fontSize = 12.sp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onSetMockActivity(null) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (mockActivity == null) Color.White else Color.DarkGray)) {
                                    Text("Auto", fontSize = 12.sp, color = if (mockActivity == null) Color.Black else Color.White)
                                }
                                Button(onClick = { onSetMockActivity("STIMMING") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (mockActivity == "STIMMING") Color.White else Color.DarkGray)) {
                                    Text("Stimming", fontSize = 12.sp, color = if (mockActivity == "STIMMING") Color.Black else Color.White)
                                }
                                Button(onClick = { onSetMockActivity("EXERCISE") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (mockActivity == "EXERCISE") Color.White else Color.DarkGray)) {
                                    Text("Exercise", fontSize = 12.sp, color = if (mockActivity == "EXERCISE") Color.Black else Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Simulate Watch Tap (Manual Event)", color = Color.Gray, fontSize = 12.sp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onSimulateManualEvent("OVERWHELMED") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                                    Text("Overwhelm", fontSize = 12.sp, color = Color.White)
                                }
                                Button(onClick = { onSimulateManualEvent("SENSORY") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                                    Text("Sensory", fontSize = 12.sp, color = Color.White)
                                }
                                Button(onClick = { onSimulateManualEvent("SOCIAL") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                                    Text("Social", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WearerDashboardScreenPreview() {
    BiocueTheme {
        WearerDashboardScreen(
            userName = "Alex",
            isStubAllowed = true,
            mockIntensity = MockIntensity.LOW,
            mockActivity = null,
            tier = 2,
            sosLat = 0.0,
            sosLng = 0.0,
            stress = 40,
            activity = "STATIONARY",
            journal = emptyList(),
            journalOffset = 0,
            reviews = emptyList(),
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
