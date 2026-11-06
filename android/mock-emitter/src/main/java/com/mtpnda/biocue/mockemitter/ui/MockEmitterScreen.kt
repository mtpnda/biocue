package com.mtpnda.biocue.mockemitter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtpnda.biocue.core.domain.model.EventTypes
import com.mtpnda.biocue.mockemitter.EmissionRecord
import com.mtpnda.biocue.mockemitter.MockEmitterViewModel
import com.mtpnda.biocue.mockemitter.ScenarioType
import com.mtpnda.biocue.mockemitter.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockEmitterScreen(viewModel: MockEmitterViewModel) {
    val selectedScenario by viewModel.selectedScenario.collectAsState()
    val isStreaming by viewModel.engine.isStreaming.collectAsState()
    val streamIntervalMs by viewModel.engine.streamIntervalMs.collectAsState()
    val emissionHistory by viewModel.engine.emissionHistory.collectAsState()
    val totalEmissions by viewModel.engine.totalEmissions.collectAsState()
    val activeEventId by viewModel.activeEventId.collectAsState()
    val watchTestMode by viewModel.watchTestMode.collectAsState()

    val customHr by viewModel.customHr.collectAsState()
    val customHrv by viewModel.customHrv.collectAsState()
    val customMotion by viewModel.customMotion.collectAsState()
    val customRepetitiveness by viewModel.customRepetitiveness.collectAsState()
    val customSlope by viewModel.customSlope.collectAsState()
    val customIsOffBody by viewModel.customIsOffBody.collectAsState()
    val customIsExercising by viewModel.customIsExercising.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "BioCue Sensor Emitter",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF3730A3))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("DEV ONLY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA5B4FC))
                        }
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "Sent: $totalEmissions",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = SuccessGreen
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Streaming Controller
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Transmission Engine", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                                Text(
                                    if (isStreaming) "Continuous stream active" else "Engine paused",
                                    fontSize = 12.sp,
                                    color = if (isStreaming) SuccessGreen else TextSecondary
                                )
                            }
                            Button(
                                onClick = { viewModel.toggleStreaming() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isStreaming) DangerRose else PrimaryIndigo
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (isStreaming) "⏹ Stop" else "▶ Stream", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Interval:", fontSize = 12.sp, color = TextSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(1, 2, 3, 5).forEach { seconds ->
                                    val isSelected = streamIntervalMs == seconds * 1000L
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) PrimaryIndigo else DarkSurfaceVariant)
                                            .clickable { viewModel.setStreamInterval(seconds) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("${seconds}s", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                            FilledTonalButton(
                                onClick = { viewModel.emitSingleShot() },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("⚡ Emit 1x", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Preset Scenarios
            item {
                Text("Preset Scenarios", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScenarioType.values().forEach { scenario ->
                        val isSelected = selectedScenario == scenario
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectScenario(scenario) }
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) PrimaryIndigo else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) DarkSurfaceVariant else DarkSurface
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(scenario.icon, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        scenario.title,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                    Text(
                                        scenario.description,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        lineHeight = 14.sp
                                    )
                                }
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryIndigo)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Fine-Tuning Sliders
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sensor Parameter Sliders", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                        Text("Dragging sliders automatically switches scenario to Custom", fontSize = 11.sp, color = TextSecondary)

                        Spacer(modifier = Modifier.height(14.dp))

                        // Heart Rate
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Heart Rate (HR)", fontSize = 13.sp, color = TextPrimary)
                            Text("${customHr.toInt()} BPM", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DangerRose)
                        }
                        Slider(
                            value = customHr,
                            onValueChange = {
                                viewModel.selectScenario(ScenarioType.CUSTOM)
                                viewModel.customHr.value = it
                            },
                            valueRange = 40f..180f,
                            colors = SliderDefaults.colors(thumbColor = DangerRose, activeTrackColor = DangerRose)
                        )

                        // HRV (RMSSD)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Heart Rate Variability (RMSSD)", fontSize = 13.sp, color = TextPrimary)
                            Text("${customHrv.toInt()} ms", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SecondaryTeal)
                        }
                        Slider(
                            value = customHrv,
                            onValueChange = {
                                viewModel.selectScenario(ScenarioType.CUSTOM)
                                viewModel.customHrv.value = it
                            },
                            valueRange = 5f..100f,
                            colors = SliderDefaults.colors(thumbColor = SecondaryTeal, activeTrackColor = SecondaryTeal)
                        )

                        // Accelerometer Magnitude
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Movement / Motion (AccMag)", fontSize = 13.sp, color = TextPrimary)
                            Text(String.format("%.2f g", customMotion), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WarningAmber)
                        }
                        Slider(
                            value = customMotion,
                            onValueChange = {
                                viewModel.selectScenario(ScenarioType.CUSTOM)
                                viewModel.customMotion.value = it
                            },
                            valueRange = 0.0f..3.0f,
                            colors = SliderDefaults.colors(thumbColor = WarningAmber, activeTrackColor = WarningAmber)
                        )

                        // Repetitiveness
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Repetitive Stimming Power", fontSize = 13.sp, color = TextPrimary)
                            Text(String.format("%.2f", customRepetitiveness), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                        }
                        Slider(
                            value = customRepetitiveness,
                            onValueChange = {
                                viewModel.selectScenario(ScenarioType.CUSTOM)
                                viewModel.customRepetitiveness.value = it
                            },
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = PrimaryIndigo, activeTrackColor = PrimaryIndigo)
                        )

                        // Toggles: Off-body & Exercising
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = customIsExercising,
                                    onCheckedChange = {
                                        viewModel.selectScenario(ScenarioType.CUSTOM)
                                        viewModel.customIsExercising.value = it
                                    }
                                )
                                Text("Exercising", fontSize = 13.sp, color = TextPrimary)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = customIsOffBody,
                                    onCheckedChange = {
                                        viewModel.selectScenario(ScenarioType.CUSTOM)
                                        viewModel.customIsOffBody.value = it
                                    }
                                )
                                Text("Off Wrist", fontSize = 13.sp, color = TextPrimary)
                            }
                        }
                    }
                }
            }

            // Stress Event Controls
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Stress Event Simulation", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                        Text("Emits lifecycle events (Start -> Label -> End) across Wear & Phone", fontSize = 11.sp, color = TextSecondary)

                        Spacer(modifier = Modifier.height(12.dp))

                        if (activeEventId == null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.triggerEventStart(isManual = true) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                                ) {
                                    Text("Start Manual Event", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.triggerEventStart(isManual = false) },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Start Auto Event", fontSize = 12.sp)
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Active Event: ${activeEventId?.take(8)}…",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningAmber
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(
                                        EventTypes.LABEL_OVERWHELMED,
                                        EventTypes.LABEL_EXCITED,
                                        EventTypes.LABEL_ACTIVE
                                    ).forEach { label ->
                                        FilledTonalButton(
                                            onClick = { viewModel.triggerEventLabel(label) },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(label, fontSize = 11.sp)
                                        }
                                    }
                                }
                                Button(
                                    onClick = { viewModel.triggerEventEnd() },
                                    colors = ButtonDefaults.buttonColors(containerColor = DangerRose),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Resolve / End Event", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Watch Test Mode Integration
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Watch Synthetic Mode", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                            Text("Commands watch app to run internal test loop", fontSize = 11.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = watchTestMode,
                            onCheckedChange = { viewModel.toggleWatchTestMode() }
                        )
                    }
                }
            }

            // Live Emission History
            item {
                Text("Live Transmission Log", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            }

            if (emissionHistory.isEmpty()) {
                item {
                    Text(
                        "No data emitted yet. Tap 'Stream' or 'Emit 1x' to start sending telemetry.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(emissionHistory, key = { it.id }) { record ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(record.time, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(record.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryIndigo)
                                }
                                Text(
                                    "HRV: ${record.rmssd.toInt()}ms | Mot: ${String.format("%.2f", record.motion)}g",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DarkSurface)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(record.channels, fontSize = 10.sp, color = SuccessGreen, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
