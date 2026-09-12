package com.mtpnda.biocue.ui.screens.wearer

import androidx.compose.foundation.background
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
import com.mtpnda.biocue.data.Pairing
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
    onOpenProfile: () -> Unit,
    pairingCode: String,
    timeRemainingSeconds: Int,
    connectedCaregivers: List<Pairing>,
    onGeneratePairingCode: () -> Unit,
    onRemoveCaregiver: (String) -> Unit
) {
    Scaffold(
        containerColor = CanvasA,
        topBar = {
            ConnectionBanner(
                connStatus = connStatus,
                onOpenProfile = onOpenProfile,
                backgroundColor = CanvasA
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "My Circle",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = LabelColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Hi $userName — you control who can see your day.",
                    fontSize = 17.sp,
                    color = TextDim
                )
            }

            // Connected Caregivers
            item {
                SectionHeader("Connected caregivers · ${connectedCaregivers.size} of 2")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (connectedCaregivers.isEmpty()) {
                            Text("No caregivers connected yet.", color = TextDim, fontSize = 15.sp)
                        } else {
                            connectedCaregivers.forEachIndexed { index, pairing ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Avatar icon
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(CalmBlue.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("👤", fontSize = 20.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pairing.caregiverName, color = LabelColor, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                        Text("Connected · reads stress events", color = TextDim, fontSize = 13.sp)
                                    }
                                    TextButton(onClick = { onRemoveCaregiver(pairing.id) }) {
                                        Text("Remove", color = DangerColor, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                if (index < connectedCaregivers.size - 1) {
                                    HorizontalDivider(color = LabelColor.copy(alpha = 0.05f))
                                }
                            }
                        }
                    }
                }
            }

            // Pairing Code Generation
            item {
                SectionHeader("Add a caregiver")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassCard)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Pairing code", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = LabelColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Single-use · expires in 10 minutes. Share it out loud or in person.", color = TextDim, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Render pairing code digits
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            val displayCode = if (pairingCode.isNotEmpty()) pairingCode.padEnd(6, '·') else "······"
                            displayCode.forEach { char ->
                                Text(
                                    text = char.toString(),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LabelColor,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = if (pairingCode.isNotEmpty()) {
                                "Expires in ${timeRemainingSeconds / 60}:${(timeRemainingSeconds % 60).toString().padStart(2, '0')}"
                            } else {
                                "No active code"
                            },
                            color = TextDim,
                            fontSize = 13.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { onGeneratePairingCode() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = CalmBlue.copy(alpha = 0.1f)),
                            enabled = connectedCaregivers.size < 2
                        ) {
                            Text(
                                text = if (connectedCaregivers.size >= 2) "Maximum caregivers reached" else "Generate pairing code", 
                                color = CalmBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Privacy Summary
            item {
                SectionHeader("What leaves this phone")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        PrivacyRow("📤", "Stress events", "Start, end, activity and your label — synced to your circle", CalmBlue.copy(alpha = 0.14f))
                        HorizontalDivider(color = LabelColor.copy(alpha = 0.05f))
                        PrivacyRow("🔒", "Heart data & movement", "Stays on this phone, always", Color.Gray.copy(alpha = 0.14f))
                        HorizontalDivider(color = LabelColor.copy(alpha = 0.05f))
                        PrivacyRow("📍", "Location", "Sent once, only when you trigger an SOS", StatusAmber.copy(alpha = 0.14f))
                    }
                }
            }

            // Developer Stubs (Preserved for testing)
            if (isStubAllowed) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0))) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Developer Stubs", color = Color.DarkGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            Text("Continuous Sensor Mock", color = Color.Gray, fontSize = 12.sp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onSetMockIntensity(MockIntensity.LOW) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (mockIntensity == MockIntensity.LOW) CalmBlue else Color.LightGray)) {
                                    Text("Low", fontSize = 12.sp)
                                }
                                Button(onClick = { onSetMockIntensity(MockIntensity.MEDIUM) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (mockIntensity == MockIntensity.MEDIUM) CalmBlue else Color.LightGray)) {
                                    Text("Medium", fontSize = 12.sp)
                                }
                                Button(onClick = { onSetMockIntensity(MockIntensity.HIGH) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (mockIntensity == MockIntensity.HIGH) CalmBlue else Color.LightGray)) {
                                    Text("High", fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Simulate Watch Tap (Manual Event)", color = Color.Gray, fontSize = 12.sp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onSimulateManualEvent("OVERWHELMED") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)) {
                                    Text("Overwhelm", fontSize = 12.sp)
                                }
                                Button(onClick = { onSimulateManualEvent("SENSORY") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)) {
                                    Text("Sensory", fontSize = 12.sp)
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

@Composable
fun PrivacyRow(emoji: String, title: String, subtitle: String, bgTint: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(bgTint, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = LabelColor, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(subtitle, color = TextDim, fontSize = 13.sp)
        }
    }
}
