package com.mtpnda.biocue

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mtpnda.biocue.ui.theme.BiocueTheme
import kotlinx.coroutines.delay

val SurfaceColor = Color(0xFF1E293B)
val CalmColor = Color(0xFF34D399)
val WarnColor = Color(0xFFFBBF24)
val DangerColor = Color(0xFF818CF8)
val TextDim = Color(0xFF94A3B8)

class MainActivity : ComponentActivity() {
    private fun isEmulator(): Boolean {
        return (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.HARDWARE.contains("goldfish")
                || android.os.Build.HARDWARE.contains("ranchu")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || android.os.Build.PRODUCT.contains("sdk_google")
                || android.os.Build.PRODUCT.contains("google_sdk")
                || android.os.Build.PRODUCT.contains("sdk")
                || android.os.Build.PRODUCT.contains("sdk_x86")
                || android.os.Build.PRODUCT.contains("vbox86p")
                || android.os.Build.PRODUCT.contains("emulator")
                || android.os.Build.PRODUCT.contains("simulator")
    }

    private val viewModel: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        setContent {
            BiocueTheme {
                val watchState by viewModel.watchConnectionState.collectAsState()
                val userName by viewModel.userName.collectAsState()
                

                if (userName == null) {
                    com.mtpnda.biocue.connect.LoginScreen(onLoginSuccess = { name ->
                        viewModel.setUserName(name)
                    })
                } else if (watchState == com.mtpnda.biocue.connect.ConnectionState.CONNECTED || isEmulator()) {
                    DashboardScreen(viewModel, userName!!)
                } else {
                    com.mtpnda.biocue.connect.ConnectionGuideScreen(
                        manager = viewModel.watchConnectionManager,
                        onConnected = {}
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: MainViewModel, userName: String) {
    val isCaregiver by viewModel.isCaregiverMode.collectAsState()
    val tier by viewModel.tier.collectAsState()
    val sosLat by viewModel.sosLatitude.collectAsState()
    val sosLng by viewModel.sosLongitude.collectAsState()
    val stress by viewModel.stressLevel.collectAsState()
    val activity by viewModel.activityLabel.collectAsState()
    val journal by viewModel.journal.collectAsState()
    val reviews by viewModel.reviewEvents.collectAsState()
    val connStatus by viewModel.connectionStatus.collectAsState()
    
    

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("BioCue", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Button(
                    onClick = { viewModel.toggleMode() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                ) {
                    Text(if (isCaregiver) "Caregiver" else "Wearer", color = TextDim)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isCaregiver) {
                item { CaregiverStatusCard(tier, connStatus, userName) }
                item { CurrentStateCard(stress, activity, connStatus, userName) }
                if (tier >= 2) {
                    item { PushNotificationPreview(userName) }
                }
                item { PatternReviewCard(reviews, userName, onResolve = { id, label -> viewModel.resolveReviewEvent(id, label) }) }
                item { JournalCard(journal) }
            } else {
                item { CurrentStateCard(stress, activity, connStatus, userName) }
                item { QuickActionsCard() }
                item { JournalCard(journal) }
            }
        }
    }
}

@Composable
fun CaregiverStatusCard(tier: Int, connectionStatus: ConnectionStatus, userName: String) {
    if (connectionStatus != ConnectionStatus.CONNECTED) {
        val warningText = if (connectionStatus == ConnectionStatus.DISCONNECTED) "Watch is disconnected" else "Watch is off body"
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF332200)) // Warning brown
                .border(1.dp, WarnColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .padding(vertical = 32.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⚠️", fontSize = 48.sp, modifier = Modifier.padding(bottom = 12.dp))
            Text(
                warningText, 
                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WarnColor,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                "Unable to monitor ${userName}'s stress levels. Please ensure the watch is worn and connected.",
                fontSize = 13.sp, color = TextDim, textAlign = TextAlign.Center
            )
        }
        return
    }

    val isDanger = tier >= 2
    val bgColor = if (isDanger) Color(0xFF312E81) else Color(0xFF064E3B)
    val borderColor = if (isDanger) DangerColor.copy(alpha = 0.3f) else CalmColor.copy(alpha = 0.3f)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(vertical = 32.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(if (isDanger) "🌩️" else "✨", fontSize = 48.sp, modifier = Modifier.padding(bottom = 12.dp))
        Text(
            if (isDanger) "${userName} may need support" else "${userName} is doing fine", 
            fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            if (isDanger) "Stress has been elevated for a few minutes. Pacing detected at current location." 
            else "No elevated stress detected. Sitting still at home.",
            fontSize = 13.sp, color = TextDim, textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CurrentStateCard(stressLevel: Int, activity: String, connectionStatus: ConnectionStatus, userName: String) {
    val isAvailable = connectionStatus == ConnectionStatus.CONNECTED
    val meterColor = when {
        !isAvailable -> TextDim
        stressLevel < 40 -> CalmColor
        stressLevel < 65 -> WarnColor
        else -> DangerColor
    }
    val meterLabel = when {
        !isAvailable -> "—"
        stressLevel < 40 -> "Low"
        stressLevel < 65 -> "Moderate"
        else -> "High"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            SectionLabel("Current State")
            
            if (!isAvailable) {
                val warningText = if (connectionStatus == ConnectionStatus.DISCONNECTED) "Watch disconnected" else "Watch is off body"
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp).clip(RoundedCornerShape(8.dp)).background(WarnColor.copy(alpha = 0.12f)).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚠️", fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
                    Text(warningText, fontSize = 12.sp, color = WarnColor, fontWeight = FontWeight.Bold)
                }
            }

            // Always show the meter structure, but with placeholder values when unavailable
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Stress Level", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isAvailable) Color.White else TextDim)
                Text(meterLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = meterColor)
            }
            Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))) {
                val animatedWidth by animateFloatAsState(targetValue = if (isAvailable) stressLevel / 100f else 0f)
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(animatedWidth).clip(CircleShape).background(meterColor))
            }
            Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("ACTIVITY:", fontSize = 11.sp, color = TextDim, modifier = Modifier.padding(end = 10.dp))
                Text(
                    if (isAvailable) activity else "— No data",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAvailable) Color.White else TextDim
                )
            }
        }
    }
}

@Composable
fun QuickActionsCard() {
    var muted by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var msgSent by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            SectionLabel("Quick Actions")
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionBtn(text = if (muted) "✓ Phone Muted" else "🔇 Mute Phone", isOn = muted, modifier = Modifier.weight(1f)) { muted = !muted }
                ActionBtn(text = if (playing) "✓ Playing" else "🎵 Calm Audio", isOn = playing, modifier = Modifier.weight(1f)) { playing = !playing }
            }
            ActionBtn(
                text = if (msgSent) "✓ Message Sent" else "💬 Message Caregiver", 
                isOn = msgSent, 
                modifier = Modifier.fillMaxWidth()
            ) {
                msgSent = true
            }
            LaunchedEffect(msgSent) {
                if (msgSent) {
                    delay(3000)
                    msgSent = false
                }
            }
        }
    }
}

@Composable
fun ActionBtn(text: String, isOn: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isOn) CalmColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun JournalCard(journal: List<JournalEntry>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            SectionLabel("Today's Journal")
            if (journal.isEmpty()) {
                Text("No events today. All calm ✨", fontSize = 13.sp, color = TextDim, modifier = Modifier.fillMaxWidth().padding(20.dp), textAlign = TextAlign.Center)
            } else {
                journal.forEach { entry ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Box(modifier = Modifier.padding(top = 5.dp, end = 12.dp).size(8.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(entry.colorHex))))
                        Column {
                            Text(entry.time, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("${entry.place} · ${entry.activity}", fontSize = 12.sp, color = TextDim, modifier = Modifier.padding(top = 2.dp))
                            Row(modifier = Modifier.padding(top = 4.dp)) {
                                Text(entry.labelIcon, fontSize = 13.sp, modifier = Modifier.padding(end = 4.dp))
                                Text(entry.labelText, fontSize = 13.sp, color = Color(android.graphics.Color.parseColor(entry.colorHex)))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PushNotificationPreview(userName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DangerColor.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("PUSH NOTIFICATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DangerColor, modifier = Modifier.padding(bottom = 6.dp))
            Text("${userName}'s stress has been elevated for 5 minutes. They may need support.", fontSize = 13.sp, color = Color.White)
        }
    }
}

@Composable
fun PatternReviewCard(reviews: List<ReviewEvent>, userName: String, onResolve: (String, String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                SectionLabel("Pattern Review")
                if (reviews.isNotEmpty()) {
                    Box(modifier = Modifier.clip(CircleShape).background(DangerColor).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text(reviews.size.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (reviews.isEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✨", fontSize = 24.sp, modifier = Modifier.padding(bottom = 6.dp))
                    Text("All caught up", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CalmColor)
                    Text("BioCue is learning ${userName}'s patterns.", fontSize = 12.sp, color = TextDim, modifier = Modifier.padding(top = 4.dp))
                }
            } else {
                val rev = reviews.first()
                
                val isUnlabeled = rev.currentLabel == "NONE"
                
                if (isUnlabeled) {
                    Text("A stress spike was detected but ${userName} didn't label it on the watch.", fontSize = 13.sp, color = Color.White, modifier = Modifier.padding(bottom = 12.dp))
                } else {
                    val labelFormat = rev.currentLabel.lowercase().replaceFirstChar { it.uppercase() }
                    Text("${userName} reported feeling ${labelFormat}. Please add environmental context to help BioCue learn.", fontSize = 13.sp, color = Color.White, modifier = Modifier.padding(bottom = 12.dp))
                }
                
                Text(rev.time, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 12.dp))
                
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.2f)).padding(12.dp)) {
                    Column {
                        Text("CONTEXT CUES", fontSize = 10.sp, color = TextDim, modifier = Modifier.padding(bottom = 8.dp))
                        rev.cues.forEach { cue ->
                            Row(modifier = Modifier.padding(bottom = 4.dp)) {
                                Text(cue.first, fontSize = 13.sp, modifier = Modifier.padding(end = 8.dp))
                                Text(cue.second, fontSize = 13.sp, color = Color(0xFFCBD5E1))
                            }
                        }
                    }
                }
                
                if (isUnlabeled) {
                    Text("💡 Ask ${userName} first if you can — they may remember what happened.", fontSize = 12.sp, color = WarnColor, modifier = Modifier.padding(vertical = 14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabelBtn("🌩️", "Overwhelmed", Color(0xFFA5B4FC)) { onResolve(rev.eventId, "OVERWHELMED") }
                        LabelBtn("✨", "Excited", Color(0xFFFCD34D)) { onResolve(rev.eventId, "EXCITED") }
                        LabelBtn("🏃", "Active", Color(0xFFCBD5E1)) { onResolve(rev.eventId, "ACTIVE") }
                        LabelBtn("🤷", "Unsure / Wasn't Present", TextDim) { onResolve(rev.eventId, "NONE") }
                    }
                } else {
                    Text("💡 What was happening around ${userName}?", fontSize = 12.sp, color = CalmColor, modifier = Modifier.padding(vertical = 14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabelBtn("🔊", "Loud Noises", Color(0xFFFCA5A5)) { onResolve(rev.eventId, "Loud Noises") }
                        LabelBtn("👥", "Crowded Space", Color(0xFFFDBA74)) { onResolve(rev.eventId, "Crowded") }
                        LabelBtn("📋", "Task Demand", Color(0xFF6EE7B7)) { onResolve(rev.eventId, "Demand") }
                        LabelBtn("🔄", "Routine Change", Color(0xFF93C5FD)) { onResolve(rev.eventId, "Routine Change") }
                        LabelBtn("🤷", "Unknown", TextDim) { onResolve(rev.eventId, "Unknown Context") }
                    }
                }
            }
        }
    }
}

@Composable
fun LabelBtn(icon: String, text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.15f),
            contentColor = color
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 18.sp, modifier = Modifier.padding(end = 10.dp))
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextDim,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}



@Preview(showBackground = true, name = "Wearer Dashboard")

@Preview(showBackground = true, name = "Caregiver Dashboard")
@Composable
fun CaregiverDashboardPreview() {
    BiocueTheme {
        Surface(color = Color(0xFF0F172A)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CaregiverStatusCard(tier = 0, connectionStatus = ConnectionStatus.CONNECTED, userName = "User")
                CurrentStateCard(28, "[Activity]", ConnectionStatus.CONNECTED, userName = "User")
                PatternReviewCard(emptyList(), userName = "User", onResolve = { _, _ -> })
            }
        }
    }
}
