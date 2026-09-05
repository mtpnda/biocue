package com.mtpnda.biocue.wear.presentation

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.mtpnda.biocue.wear.SensorCollectionService
import com.mtpnda.biocue.wear.WearDataSender
import com.mtpnda.biocue.wear.presentation.theme.BiocueTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow

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


    private var isTracking by mutableStateOf(false)
    private var sensorService by mutableStateOf<SensorCollectionService?>(null)
    private val _isPhoneReachable = MutableStateFlow(true)
    private var permissionsGrantedState by mutableStateOf(false)
    private var locationEnabledState by mutableStateOf(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as SensorCollectionService.LocalBinder
            sensorService = localBinder.service
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            sensorService = null
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val missing = permissionManager.getRequiredPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            permissionsGrantedState = true
            startTrackingService()
        } else {
            val denied = results.filterValues { !it }.keys
            if (denied.isNotEmpty()) {
                // If they explicitly denied one, stop the chain and let them read the Toast.
                android.widget.Toast.makeText(this, "Denied: ${denied.first().substringAfterLast('.')}. Please allow in Settings.", android.widget.Toast.LENGTH_LONG).show()
                permissionsGrantedState = false
            } else {
                // If they granted it, but others are missing, auto-request the next one!
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ requestPermissions() }, 200)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionsGrantedState = hasAllPermissions()
        val locManager = getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        locationEnabledState = locManager.isLocationEnabled
        
        var launchState = WatchState.ANCHOR
        if (intent?.action == "com.mtpnda.biocue.ACTION_LOG_EMOTION" || 
            intent?.getStringExtra("launch_state") == "CAROUSEL") {
            launchState = WatchState.CAROUSEL
        }

        if (hasAllPermissions() && !isTracking) {
            startTrackingService()
        }

        setContent {
            val scope = rememberCoroutineScope()
            val sender = remember { WearDataSender(this@MainActivity) }
            
            LaunchedEffect(Unit) {
                sender.startMonitoring(scope)
            }

            val senderPhoneReachable by sender.isPhoneReachable.collectAsState()
            val servicePhoneReachable by (sensorService?.isPhoneReachable ?: _isPhoneReachable).collectAsState()
            val phoneReachable = senderPhoneReachable || servicePhoneReachable || isEmulator()

            val testModeEnabled by (sensorService?.isTestModeActive ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()
            val currentEventId = remember { mutableStateOf<String?>(null) }

            WearApp(
                hasPermissions = permissionsGrantedState,
                isLocationEnabled = locationEnabledState,
                isTracking = isTracking,
                isPhoneReachable = phoneReachable,
                isTestModeEnabled = testModeEnabled,
                onOpenLocationSettings = {
                    startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                },
                onRequestPermissions = { requestPermissions() },
                onToggleTracking = {
                    if (isTracking) {
                        stopTrackingService()
                    } else {
                        if (hasAllPermissions()) startTrackingService() else requestPermissions()
                    }
                },
                onToggleTestMode = {
                    sensorService?.setTestMode(!testModeEnabled)
                },
                onStartEvent = {
                    val newId = java.util.UUID.randomUUID().toString()
                    currentEventId.value = newId
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) { sender.sendEventStart(newId, isManual = true) }
                },
                onLabelEvent = { label ->
                    currentEventId.value?.let { eventId ->
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) { sender.sendEventLabel(eventId, label) }
                    }
                },
                onEndEvent = {
                    currentEventId.value?.let { eventId ->
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) { sender.sendEventEnd(eventId) }
                    }
                    currentEventId.value = null
                },
                launchState = launchState,
                onPermissionsResult = { granted -> permissionsGrantedState = granted }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val granted = hasAllPermissions()
        permissionsGrantedState = granted
        
        val locManager = getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        locationEnabledState = locManager.isLocationEnabled
        
        if (granted && locationEnabledState && !isTracking) {
            startTrackingService()
        }
    }

    private val permissionManager = com.mtpnda.biocue.wear.permissions.PermissionFactory.create(com.mtpnda.biocue.wear.BuildConfig.DEBUG)

    private fun hasAllPermissions(): Boolean {
        return permissionManager.hasAllPermissions(this)
    }

    private fun requestPermissions() {
        val permissions = permissionManager.getRequiredPermissions()
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isEmpty()) {
            permissionsGrantedState = true
            startTrackingService()
        } else {
            val nextPerm = missingPermissions.first()
            if (shouldShowRequestPermissionRationale(nextPerm)) {
                android.widget.Toast.makeText(this, "Please open Settings > Apps > BioCue and grant ${nextPerm.substringAfterLast('.')}", android.widget.Toast.LENGTH_LONG).show()
                // Do not launch if they permanently denied it, just tell them.
            } else {
                permissionLauncher.launch(arrayOf(nextPerm))
                android.widget.Toast.makeText(this, "Requesting ${nextPerm.substringAfterLast('.')}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startTrackingService() {
        val intent = Intent(this, SensorCollectionService::class.java)
        startForegroundService(intent)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        isTracking = true
    }

    private fun stopTrackingService() {
        try { unbindService(serviceConnection) } catch (_: Exception) {}
        val intent = Intent(this, SensorCollectionService::class.java)
        stopService(intent)
        sensorService = null
        isTracking = false
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unbindService(serviceConnection) } catch (_: Exception) {}
    }
}

enum class WatchState { SETUP_PERMISSIONS, SETUP_LOCATION, SETUP_PHONE, ANCHOR, GLOWING, CAROUSEL, ONGOING, SUCCESS }

data class Emotion(val icon: String, val label: String, val color: Color)
val emotions = listOf(
    Emotion("🌩️", "Overwhelmed", Color(0xFFA5B4FC)),
    Emotion("✨", "Excited", Color(0xFFFCD34D)),
    Emotion("🏃", "Active", Color(0xFFCBD5E1))
)

@Composable
fun WearApp(
    hasPermissions: Boolean,
    isLocationEnabled: Boolean,
    isTracking: Boolean,
    isPhoneReachable: Boolean,
    isTestModeEnabled: Boolean,
    onOpenLocationSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
    onToggleTracking: () -> Unit,
    onToggleTestMode: () -> Unit,
    onStartEvent: () -> Unit,
    onLabelEvent: (String) -> Unit,
    onEndEvent: () -> Unit,
    launchState: WatchState = WatchState.ANCHOR,
    onPermissionsResult: ((Boolean) -> Unit)? = null
) {
    BiocueTheme {
        var watchState by remember { 
            mutableStateOf(
                when {
                    !hasPermissions -> WatchState.SETUP_PERMISSIONS
                    !isLocationEnabled -> WatchState.SETUP_LOCATION
                    !isPhoneReachable -> WatchState.SETUP_PHONE
                    else -> launchState
                }
            )
        }
        var selectedEmotion by remember { mutableStateOf<Emotion?>(null) }
        
        // When permissions/location change, auto-progress
        LaunchedEffect(hasPermissions, isLocationEnabled) {
            if (hasPermissions && isLocationEnabled && (watchState == WatchState.SETUP_PERMISSIONS || watchState == WatchState.SETUP_LOCATION)) {
                if (!isPhoneReachable) {
                    watchState = WatchState.SETUP_PHONE
                } else {
                    watchState = launchState
                }
                if (hasPermissions) onPermissionsResult?.invoke(true)
            } else if (hasPermissions && !isLocationEnabled && watchState == WatchState.SETUP_PERMISSIONS) {
                watchState = WatchState.SETUP_LOCATION
            }
        }

        // When phone connection changes, force block or unblock main screen
        LaunchedEffect(isPhoneReachable) {
            if (!isPhoneReachable) {
                watchState = WatchState.SETUP_PHONE
            } else if (watchState == WatchState.SETUP_PHONE && hasPermissions) {
                watchState = launchState
            }
        }

        AppScaffold {
            when (watchState) {
                WatchState.SETUP_PERMISSIONS -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Welcome to BioCue", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text("Sensor access is needed to monitor stress.", fontSize = 12.sp, color = Color.LightGray, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 4.dp))
                        CompactButton(
                            onClick = onRequestPermissions, 
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                        ) {
                            Text("Grant Access")
                        }
                    }
                }
                WatchState.SETUP_LOCATION -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Location Disabled", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                        Text("Please turn on location in settings to track stress patterns.", fontSize = 12.sp, color = Color.LightGray, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 16.dp))
                        Button(
                            onClick = onOpenLocationSettings,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2E))
                        ) {
                            Text("Open Settings", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
                WatchState.SETUP_PHONE -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("📱", fontSize = 28.sp, modifier = Modifier.padding(bottom = 6.dp))
                        Text("Connect Phone", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text("Open BioCue on your phone to connect.", fontSize = 11.sp, color = Color.LightGray, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
                WatchState.ANCHOR, WatchState.GLOWING -> {
                    // Simple full screen column for Anchor
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Connection Status Dot Indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isPhoneReachable) Color(0xFF34D399) else Color(0xFFF87171))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isPhoneReachable) "Phone Connected" else "Phone Disconnected",
                                fontSize = 10.sp,
                                color = if (isPhoneReachable) Color(0xFF34D399) else Color(0xFFF87171),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        AnchorButton(
                            isGlowing = watchState == WatchState.GLOWING && isTracking,
                            onClick = {
                                if (!isTracking) {
                                    onToggleTracking()
                                } else {
                                    if (watchState == WatchState.ANCHOR) {
                                        watchState = WatchState.GLOWING
                                    } else {
                                        onStartEvent()
                                        watchState = WatchState.CAROUSEL
                                    }
                                }
                            }
                        )
                        if (!isTracking) {
                            Text(
                                text = "Sensors Off",
                                color = Color(0xFFFBBF24),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else if (watchState == WatchState.GLOWING) {
                            Text(
                                text = "Tap again if overwhelmed",
                                color = Color(0xFF818CF8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (isTestModeEnabled) "Test Mode: ON" else "Test Mode: OFF",
                                    color = if (isTestModeEnabled) Color(0xFF34D399) else Color.Gray,
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { onToggleTestMode() }
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }
                WatchState.CAROUSEL -> {
                    val listState = rememberTransformingLazyColumnState()
                    val transformationSpec = rememberTransformationSpec()
                    ScreenScaffold(scrollState = listState) { contentPadding ->
                        TransformingLazyColumn(
                            state = listState,
                            contentPadding = contentPadding,
                            flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(listState),
                            rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(listState)
                        ) {
                            item { 
                                ListHeader(
                                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                                    transformation = SurfaceTransformation(transformationSpec)
                                ) { 
                                    Text("How do you feel?", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) 
                                } 
                            }
                            items(emotions.size) { index ->
                                val emotion = emotions[index]
                                Button(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .transformedHeight(this, transformationSpec),
                                    transformation = SurfaceTransformation(transformationSpec),
                                    onClick = {
                                        selectedEmotion = emotion
                                        onLabelEvent(emotion.label.uppercase())
                                        watchState = WatchState.ONGOING
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2E))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(emotion.icon, fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                                        Text(emotion.label, color = emotion.color, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                WatchState.ONGOING -> {
                    var isConfirmingEnd by remember { mutableStateOf(false) }
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val infiniteTransition = rememberInfiniteTransition()
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.15f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(selectedEmotion?.color?.copy(alpha = if (isConfirmingEnd) 0.3f else 0.15f) ?: Color(0xFF1E293B))
                                .clickable {
                                    if (!isConfirmingEnd) {
                                        isConfirmingEnd = true
                                    } else {
                                        onEndEvent()
                                        watchState = WatchState.SUCCESS
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = selectedEmotion?.icon ?: "",
                                fontSize = 44.sp,
                                modifier = Modifier.scale(if (isConfirmingEnd) scale else 1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isConfirmingEnd) "Tap again to end feeling" else "Tap icon twice to end",
                            color = if (isConfirmingEnd) (selectedEmotion?.color ?: Color(0xFF818CF8)) else Color.LightGray,
                            fontWeight = if (isConfirmingEnd) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                WatchState.SUCCESS -> {
                    LaunchedEffect(Unit) {
                        delay(2000)
                        watchState = WatchState.ANCHOR
                    }
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("✅", fontSize = 48.sp)
                        Text("Ended", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

@Preview(device = "id:wearos_large_round", showSystemUi = true, backgroundColor = 0xff000000, showBackground = true)
@Composable
fun WearAppPreview() {
    WearApp(hasPermissions = true, isLocationEnabled = true, isTracking = true, isPhoneReachable = true, isTestModeEnabled = false, onOpenLocationSettings = {}, onRequestPermissions = {}, onToggleTracking = {}, onToggleTestMode = {}, onStartEvent = {}, onLabelEvent = {}, onEndEvent = {})
}

@Preview(device = "id:wearos_large_round", showSystemUi = true, backgroundColor = 0xff000000, showBackground = true)
@Composable
fun WearAppDisconnectedPreview() {
    WearApp(hasPermissions = true, isLocationEnabled = true, isTracking = true, isPhoneReachable = false, isTestModeEnabled = false, onOpenLocationSettings = {}, onRequestPermissions = {}, onToggleTracking = {}, onToggleTestMode = {}, onStartEvent = {}, onLabelEvent = {}, onEndEvent = {})
}

@Preview(device = "id:wearos_large_round", showSystemUi = true, backgroundColor = 0xff000000, showBackground = true)
@Composable
fun WearAppSensorsOffPreview() {
    WearApp(hasPermissions = true, isLocationEnabled = true, isTracking = false, isPhoneReachable = true, isTestModeEnabled = false, onOpenLocationSettings = {}, onRequestPermissions = {}, onToggleTracking = {}, onToggleTestMode = {}, onStartEvent = {}, onLabelEvent = {}, onEndEvent = {})
}

@Preview(device = "id:wearos_large_round", showSystemUi = true, backgroundColor = 0xff000000, showBackground = true)
@Composable
fun WearAppSetupPreview() {
    WearApp(hasPermissions = false, isLocationEnabled = true, isTracking = false, isPhoneReachable = true, isTestModeEnabled = false, onOpenLocationSettings = {}, onRequestPermissions = {}, onToggleTracking = {}, onToggleTestMode = {}, onStartEvent = {}, onLabelEvent = {}, onEndEvent = {})
}

@Composable
fun AnchorButton(isGlowing: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(if (isGlowing) Color(0xFF4F46E5).copy(alpha = 0.2f) else Color(0xFF1E293B))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isGlowing) {
            Box(
                modifier = Modifier
                    .size(60.dp * glowScale)
                    .clip(CircleShape)
                    .background(Color(0xFF818CF8).copy(alpha = 0.4f))
            )
        }
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(0xFF334155)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isGlowing) Color(0xFF818CF8) else Color(0xFF94A3B8))
            )
        }
    }
}