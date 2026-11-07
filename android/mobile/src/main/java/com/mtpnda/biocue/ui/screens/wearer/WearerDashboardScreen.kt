package com.mtpnda.biocue.ui.screens.wearer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtpnda.biocue.ConnectionStatus
import com.mtpnda.biocue.JournalEntry
import com.mtpnda.biocue.MockIntensity
import com.mtpnda.biocue.ReviewEvent
import com.mtpnda.biocue.connect.ConnectionState
import com.mtpnda.biocue.data.Pairing
import com.mtpnda.biocue.ui.components.DashboardTopBar
import com.mtpnda.biocue.ui.components.EmojiAvatar
import com.mtpnda.biocue.ui.components.JournalCard
import com.mtpnda.biocue.ui.components.OutlinedPillButton
import com.mtpnda.biocue.ui.components.PillButton
import com.mtpnda.biocue.ui.components.SectionHeader
import com.mtpnda.biocue.ui.components.SoftCard
import com.mtpnda.biocue.ui.theme.AlertCoralChip
import com.mtpnda.biocue.ui.theme.AlertCoralInk
import com.mtpnda.biocue.ui.theme.AlertCoralSoft
import com.mtpnda.biocue.ui.theme.AppRadius
import com.mtpnda.biocue.ui.theme.BgColor
import com.mtpnda.biocue.ui.theme.CalmPalette
import com.mtpnda.biocue.ui.theme.InfoBlueSoft
import com.mtpnda.biocue.ui.theme.InkPrimary
import com.mtpnda.biocue.ui.theme.InkSecondary
import com.mtpnda.biocue.ui.theme.LineBorder
import com.mtpnda.biocue.ui.theme.NeutralGraySoft
import com.mtpnda.biocue.ui.theme.NeutralPalette
import com.mtpnda.biocue.ui.theme.OnPrimaryCharcoal
import com.mtpnda.biocue.ui.theme.PrimaryCharcoal
import com.mtpnda.biocue.ui.theme.RisingAmberSoft
import com.mtpnda.biocue.ui.theme.RisingPalette
import com.mtpnda.biocue.ui.theme.Surface2
import com.mtpnda.biocue.ui.theme.SurfaceCard
import com.mtpnda.biocue.ui.theme.paletteForTier
import kotlinx.coroutines.delay

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
    watchState: ConnectionState = ConnectionState.CHECKING,
    watchDisplayName: String? = null,
    latestHeartRate: Int? = null,
    latestHrv: Int? = null,
    lastReadingTimestamp: Long? = null,
    isCalmAudioPlaying: Boolean,
    onToggleCalmAudio: () -> Unit,
    onRecheckWatch: () -> Unit = {},
    onOpenWatchManager: () -> Unit = {},
    onPromptInstallOnWatch: () -> Unit = {},
    onQuickCheckIn: (String) -> Unit = {},
    onTriggerSos: () -> Unit = {},
    onSetMockIntensity: (MockIntensity) -> Unit = {},
    onSetMockActivity: (String?) -> Unit = {},
    onSimulateManualEvent: (String) -> Unit = {},
    onEndOngoingEvent: () -> Unit = {},
    onResolveReview: (String, String) -> Unit = { _, _ -> },
    onChangeJournalDate: (Int) -> Unit = {},
    onOpenProfile: () -> Unit = {},
    pairingCode: String = "",
    timeRemainingSeconds: Int = 0,
    connectedCaregivers: List<Pairing> = emptyList(),
    onGeneratePairingCode: () -> Unit = {},
    onRemoveCaregiver: (String) -> Unit = {}
) {
    var showDevStubs by remember { mutableStateOf(false) }
    var showPrivacyDetails by remember { mutableStateOf(false) }
    var showSosConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BgColor,
        topBar = {
            DashboardTopBar(
                isCaregiver = false,
                onOpenProfile = onOpenProfile
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Personal Greeting & Calming Subtitle
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Hi, $userName",
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Take today at your own pace · BioCue is quietly listening in the background.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkSecondary
                )
            }

            // 2. Interactive "My Watch" Status Box
            item {
                InteractiveWatchCard(
                    watchState = watchState,
                    watchDisplayName = watchDisplayName,
                    connStatus = connStatus,
                    onRecheckWatch = onRecheckWatch,
                    onOpenWatchManager = onOpenWatchManager,
                    onPromptInstallOnWatch = onPromptInstallOnWatch
                )
            }

            // 3. Live Sensory State & Vitals Telemetry Card
            item {
                WearerLiveSensoryCard(
                    userName = userName,
                    tier = tier,
                    activity = activity,
                    latestHeartRate = latestHeartRate,
                    latestHrv = latestHrv,
                    lastReadingTimestamp = lastReadingTimestamp
                )
            }

            // 4. Quick Emotional / Sensory Check-in (1-Tap Logging)
            item {
                QuickCheckInCard(onCheckIn = onQuickCheckIn)
            }

            // 5. Sensory Spike Active Banner (If elevated or ongoing meltdown)
            if (tier >= 2 || ongoingEventLabel != null) {
                item {
                    SoftCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = AlertCoralSoft,
                        borderColor = AlertCoralChip
                    ) {
                        Text(
                            text = "Sensory spike noted",
                            style = MaterialTheme.typography.titleMedium,
                            color = AlertCoralInk
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Take a slow breath. Your circle has been quietly updated in the background.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkPrimary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedPillButton(
                            text = "I'm feeling grounded now",
                            onClick = onEndOngoingEvent,
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = AlertCoralChip,
                            contentColor = AlertCoralInk
                        )
                    }
                }
            }

            // 6. Neurodivergent Sensory Grounding Toolkit
            item {
                SectionHeader("GROUNDING TOOLS")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Box Breathing Visual Pacer
                    BreathingPacerCard()

                    // 1-Tap Soothing Soundscape
                    SoothingAudioCard(
                        isPlaying = isCalmAudioPlaying,
                        onToggle = onToggleCalmAudio
                    )

                    // SOS Emergency Alert Button
                    OutlinedPillButton(
                        text = "🚨 Signal My Circle (Need Help)",
                        onClick = { showSosConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = AlertCoralChip,
                        contentColor = AlertCoralInk
                    )
                }
            }

            // 7. Recent Moments & Reflections (Journal)
            item {
                JournalCard(
                    journal = journal,
                    journalOffset = journalOffset,
                    onChangeDate = onChangeJournalDate
                )
            }

            // 8. Discreet Privacy & Data Guarantee (Collapsible Drawer)
            item {
                SoftCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Surface2
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPrivacyDetails = !showPrivacyDetails },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔒", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                            Text(
                                text = "PRIVACY & DATA GUARANTEE",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSecondary
                            )
                        }
                        Text(
                            text = if (showPrivacyDetails) "Hide" else "Review",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSecondary
                        )
                    }

                    AnimatedVisibility(
                        visible = showPrivacyDetails,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            PrivacyRow(
                                emoji = "📤",
                                title = "Stress events",
                                subtitle = "Start, end, and your label — shared only with approved circle",
                                bgTint = InfoBlueSoft
                            )
                            HorizontalDivider(color = LineBorder)
                            PrivacyRow(
                                emoji = "🔒",
                                title = "Heart data & movement",
                                subtitle = "Stays strictly on this phone and watch, always",
                                bgTint = NeutralGraySoft
                            )
                            HorizontalDivider(color = LineBorder)
                            PrivacyRow(
                                emoji = "📍",
                                title = "Emergency location",
                                subtitle = "Sent once, only if an emergency SOS is triggered",
                                bgTint = RisingAmberSoft
                            )
                        }
                    }
                }
            }

            // 9. Developer Controls (Only in debug/emulator builds)
            if (isStubAllowed) {
                item {
                    SoftCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Surface2
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDevStubs = !showDevStubs },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DEVELOPER CONTROLS",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSecondary
                            )
                            Text(
                                text = if (showDevStubs) "Collapse" else "Expand",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSecondary
                            )
                        }

                        AnimatedVisibility(
                            visible = showDevStubs,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                Text(
                                    "Continuous Sensor Mock",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSecondary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StubChip("Low", mockIntensity == MockIntensity.LOW, { onSetMockIntensity(MockIntensity.LOW) }, Modifier.weight(1f))
                                    StubChip("Medium", mockIntensity == MockIntensity.MEDIUM, { onSetMockIntensity(MockIntensity.MEDIUM) }, Modifier.weight(1f))
                                    StubChip("High", mockIntensity == MockIntensity.HIGH, { onSetMockIntensity(MockIntensity.HIGH) }, Modifier.weight(1f))
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    "Simulate Watch Tap (Manual Event)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSecondary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StubChip("Overwhelm", false, { onSimulateManualEvent("OVERWHELMED") }, Modifier.weight(1f))
                                    StubChip("Sensory", false, { onSimulateManualEvent("SENSORY") }, Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showSosConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSosConfirmDialog = false },
            title = { Text("Signal Your Circle?") },
            text = { Text("This will notify your approved caregivers and share your current location so they know you need space or help.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSosConfirmDialog = false
                        onTriggerSos()
                    }
                ) {
                    Text("Signal Now", color = AlertCoralInk, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSosConfirmDialog = false }) {
                    Text("Cancel", color = InkSecondary)
                }
            }
        )
    }
}

@Composable
private fun InteractiveWatchCard(
    watchState: ConnectionState,
    watchDisplayName: String?,
    connStatus: ConnectionStatus,
    onRecheckWatch: () -> Unit,
    onOpenWatchManager: () -> Unit,
    onPromptInstallOnWatch: () -> Unit
) {
    SectionHeader("MY WATCH")
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(AppRadius.Pill))
                    .background(
                        when (watchState) {
                            ConnectionState.CONNECTED -> CalmPalette.soft
                            ConnectionState.APP_MISSING, ConnectionState.NO_TRANSPORT -> RisingPalette.soft
                            else -> NeutralPalette.soft
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "⌚", fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = watchDisplayName ?: when (watchState) {
                        ConnectionState.CONNECTED -> "Smartwatch"
                        ConnectionState.APP_MISSING -> "Watch Detected"
                        ConnectionState.NOT_PAIRED -> "No Watch Paired"
                        ConnectionState.NO_TRANSPORT -> "Watch Unreachable"
                        ConnectionState.CHECKING -> "Searching for watch…"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = InkPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (watchState) {
                        ConnectionState.CONNECTED -> "🟢 Connected · Sensor telemetry active"
                        ConnectionState.APP_MISSING -> "🟡 Paired, but BioCue watch app missing"
                        ConnectionState.NOT_PAIRED -> "⚪ Pair via Galaxy Wearable / Bluetooth"
                        ConnectionState.NO_TRANSPORT -> "🟠 Bluetooth connection hiccup"
                        ConnectionState.CHECKING -> "🟡 Searching for watch…"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when (watchState) {
                        ConnectionState.CONNECTED -> CalmPalette.ink
                        ConnectionState.APP_MISSING, ConnectionState.NO_TRANSPORT -> RisingPalette.ink
                        else -> InkSecondary
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (watchState) {
                ConnectionState.NOT_PAIRED -> {
                    OutlinedPillButton(
                        text = "Open Galaxy Wearable",
                        onClick = onOpenWatchManager,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedPillButton(
                        text = "Re-check",
                        onClick = onRecheckWatch,
                        modifier = Modifier.weight(0.7f)
                    )
                }
                ConnectionState.APP_MISSING -> {
                    PillButton(
                        text = "Install on Watch",
                        onClick = onPromptInstallOnWatch,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedPillButton(
                        text = "Re-check",
                        onClick = onRecheckWatch,
                        modifier = Modifier.weight(0.7f)
                    )
                }
                ConnectionState.NO_TRANSPORT -> {
                    OutlinedPillButton(
                        text = "Open Wearable",
                        onClick = onOpenWatchManager,
                        modifier = Modifier.weight(1f)
                    )
                    PillButton(
                        text = "Retry Link",
                        onClick = onRecheckWatch,
                        modifier = Modifier.weight(0.8f)
                    )
                }
                ConnectionState.CONNECTED -> {
                    OutlinedPillButton(
                        text = "Check Connection",
                        onClick = onRecheckWatch,
                        modifier = Modifier.weight(1f)
                    )
                }
                ConnectionState.CHECKING -> {
                    OutlinedPillButton(
                        text = "Refresh",
                        onClick = onRecheckWatch,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun WearerLiveSensoryCard(
    userName: String,
    tier: Int,
    activity: String,
    latestHeartRate: Int?,
    latestHrv: Int?,
    lastReadingTimestamp: Long?
) {
    val palette = paletteForTier(tier)
    val statusTitle = when (tier) {
        0 -> "You are calm ✨"
        1 -> "Sensory levels rising 📈"
        else -> "Take a quiet moment 🌿"
    }
    val statusSubtitle = when (tier) {
        0 -> "Bio-signals are in a comfortable, steady baseline."
        1 -> "Gentle sensory elevation detected. Grounding helps."
        else -> "Sensory spike active. Your circle has been quietly updated."
    }
    val targetFraction = when (tier) {
        0 -> 0.18f
        1 -> 0.55f
        else -> 0.92f
    }
    val fillFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 400, easing = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)),
        label = "wearerMeterFill"
    )

    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = statusTitle,
                style = MaterialTheme.typography.titleLarge,
                color = palette.ink
            )
            if (lastReadingTimestamp != null) {
                Text(
                    text = "Live Sync",
                    style = MaterialTheme.typography.labelSmall,
                    color = CalmPalette.ink
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = statusSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(AppRadius.Pill))
                .background(palette.soft)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fillFraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(AppRadius.Pill))
                    .background(palette.chip)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VitalsChip(
                icon = "❤️",
                value = if (latestHeartRate != null) "$latestHeartRate BPM" else "-- BPM",
                label = "Heart Rate",
                modifier = Modifier.weight(1f)
            )
            VitalsChip(
                icon = "🌿",
                value = if (latestHrv != null) "$latestHrv ms" else "Steady",
                label = "HRV Rhythm",
                modifier = Modifier.weight(1f)
            )
            VitalsChip(
                icon = "🚶",
                value = activity.substringAfter(" ").ifEmpty { "Resting" },
                label = "Movement",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun VitalsChip(icon: String, value: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Surface2)
            .border(1.dp, LineBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = InkSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun QuickCheckInCard(onCheckIn: (String) -> Unit) {
    var lastCheckedLabel by remember { mutableStateOf<String?>(null) }

    Column {
        SectionHeader("HOW ARE YOU FEELING RIGHT NOW?")
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CheckInChip("🌿 Calm", Modifier.weight(1f)) {
                    lastCheckedLabel = "Calm"
                    onCheckIn("CALM")
                }
                CheckInChip("✨ Happy", Modifier.weight(1f)) {
                    lastCheckedLabel = "Happy"
                    onCheckIn("EXCITED")
                }
                CheckInChip("⚡ Sensory", Modifier.weight(1f)) {
                    lastCheckedLabel = "Sensory Spike"
                    onCheckIn("SENSORY")
                }
                CheckInChip("🌩️ Overwhelm", Modifier.weight(1f)) {
                    lastCheckedLabel = "Overwhelmed"
                    onCheckIn("OVERWHELMED")
                }
            }

            AnimatedVisibility(
                visible = lastCheckedLabel != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = "✓ Logged '$lastCheckedLabel' to your reflections.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CalmPalette.ink,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun CheckInChip(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(AppRadius.Pill)
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(Surface2)
            .border(1.dp, LineBorder, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = InkPrimary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BreathingPacerCard() {
    var isBreathingActive by remember { mutableStateOf(false) }
    var phaseTitle by remember { mutableStateOf("Breathe in slowly") }
    var phaseSubtitle by remember { mutableStateOf("Feel your lungs expand · 4s") }
    var targetScale by remember { mutableFloatStateOf(0.80f) }

    LaunchedEffect(isBreathingActive) {
        if (!isBreathingActive) return@LaunchedEffect
        while (isBreathingActive) {
            // 1. Inhale 4s
            phaseTitle = "Breathe in slowly"
            phaseSubtitle = "Feel your lungs expand · 4s"
            targetScale = 1.15f
            delay(4000)
            if (!isBreathingActive) break

            // 2. Hold 4s
            phaseTitle = "Hold gently"
            phaseSubtitle = "Soften your jaw & shoulders · 4s"
            delay(4000)
            if (!isBreathingActive) break

            // 3. Exhale 4s
            phaseTitle = "Slow exhale"
            phaseSubtitle = "Gently release all tension · 4s"
            targetScale = 0.75f
            delay(4000)
            if (!isBreathingActive) break

            // 4. Rest 4s
            phaseTitle = "Rest peacefully"
            phaseSubtitle = "Stay present in your body · 4s"
            delay(4000)
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isBreathingActive) targetScale else 0.80f,
        animationSpec = tween(durationMillis = 3800, easing = FastOutSlowInEasing),
        label = "pacerScale"
    )

    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🫧", fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
                Column {
                    Text("4-4-4-4 Calm Breathing Pacer", style = MaterialTheme.typography.titleMedium)
                    Text("Guided visual breath to ground your nervous system", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                }
            }
        }

        if (isBreathingActive) {
            Spacer(modifier = Modifier.height(18.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppRadius.Md))
                    .background(Surface2)
                    .border(1.dp, LineBorder, RoundedCornerShape(AppRadius.Md))
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Expanding / Contracting Serene Breathing Orb
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer subtle breathing halo
                    Box(
                        modifier = Modifier
                            .size((130 * animatedScale).dp)
                            .clip(CircleShape)
                            .background(CalmPalette.soft.copy(alpha = 0.40f))
                    )
                    // Inner calming core
                    Box(
                        modifier = Modifier
                            .size((95 * animatedScale).dp)
                            .clip(CircleShape)
                            .background(CalmPalette.soft)
                            .border(2.dp, CalmPalette.chip, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🫧",
                            fontSize = (26 * animatedScale).sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Guidance Text & Pacing Cues — Stable, legibly placed outside the orb without overflow
                Text(
                    text = phaseTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = InkPrimary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = phaseSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        OutlinedPillButton(
            text = if (isBreathingActive) "Stop Breathing Pacer" else "Start Breathing Pacer",
            onClick = { isBreathingActive = !isBreathingActive },
            modifier = Modifier.fillMaxWidth(),
            borderColor = if (isBreathingActive) CalmPalette.chip else LineBorder,
            contentColor = if (isBreathingActive) CalmPalette.ink else InkPrimary
        )
    }
}

@Composable
private fun SoothingAudioCard(isPlaying: Boolean, onToggle: () -> Unit) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isPlaying) "🔊" else "🌿",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column {
                    Text(
                        text = if (isPlaying) "Soothing audio playing…" else "Soothing Soundscape",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isPlaying) CalmPalette.ink else InkPrimary
                    )
                    Text(
                        text = "Calming ambient rain & frequencies",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }
            }
            PillButton(
                text = if (isPlaying) "Pause" else "Play",
                onClick = onToggle
            )
        }
    }
}

@Composable
private fun StubChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(AppRadius.Pill)
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(if (selected) PrimaryCharcoal else SurfaceCard)
            .border(1.dp, if (selected) PrimaryCharcoal else LineBorder, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) OnPrimaryCharcoal else InkPrimary
        )
    }
}

@Composable
private fun PrivacyRow(emoji: String, title: String, subtitle: String, bgTint: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EmojiAvatar(emoji = emoji, size = 40.dp, containerColor = bgTint)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary
            )
        }
    }
}
