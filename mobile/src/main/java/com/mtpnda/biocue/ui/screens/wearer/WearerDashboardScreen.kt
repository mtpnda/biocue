package com.mtpnda.biocue.ui.screens.wearer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtpnda.biocue.ui.theme.*
import com.mtpnda.biocue.ui.components.*
import com.mtpnda.biocue.JournalEntry
import com.mtpnda.biocue.ReviewEvent
import com.mtpnda.biocue.ConnectionStatus
import com.mtpnda.biocue.MockIntensity

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
    hasOngoing: Boolean,
    connStatus: ConnectionStatus,
    isCalmAudioPlaying: Boolean,
    onToggleCalmAudio: () -> Unit,
    onSetMockIntensity: (MockIntensity) -> Unit,
    onSetMockActivity: (String?) -> Unit,
    onSimulateManualEvent: (String) -> Unit,
    onResolveReview: (String, String) -> Unit,
    onChangeJournalDate: (Int) -> Unit,
    onOpenProfile: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DashboardTopBar(isCaregiver = false, onOpenProfile = onOpenProfile)
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
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
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}
