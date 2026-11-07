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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import com.mtpnda.biocue.core.link.HelloStatus
import com.mtpnda.biocue.wear.link.BleLinkServer
import com.mtpnda.biocue.wear.link.LinkPermissions
import com.mtpnda.biocue.wear.link.PhoneTarget
import com.mtpnda.biocue.wear.link.PhoneTargetStore
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

    // ---- phone type (Android phone via Data Layer, or iPhone via BLE link) ----
    private val phoneTargetStore by lazy { PhoneTargetStore(this) }
    private var phoneTargetState by mutableStateOf(PhoneTarget.ANDROID)

    private val bleLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (phoneTargetState == PhoneTarget.IPHONE && LinkPermissions.hasAll(this)) beginIphonePairing()
        else android.widget.Toast.makeText(this, "Bluetooth access is needed to pair an iPhone.", android.widget.Toast.LENGTH_LONG).show()
    }

    private fun beginIphonePairing() {
        val link = BleLinkServer.get(this)
        link.start()
        link.openPairingWindow()
    }

    private fun startIphonePairing() {
        val missing = LinkPermissions.missing(this)
        if (missing.isEmpty()) beginIphonePairing() else bleLauncher.launch(missing.toTypedArray())
    }

    private fun selectPhoneTarget(target: PhoneTarget) {
        phoneTargetStore.set(target)
        phoneTargetState = target
        if (target == PhoneTarget.IPHONE) startIphonePairing() else BleLinkServer.get(this).stop()
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as SensorCollectionService.LocalBinder
            sensorService = localBinder.service
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            sensorService = null
        }
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (_: Exception) {}
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
                val deniedPerm = denied.first().substringAfterLast('.')
                android.widget.Toast.makeText(this, "$deniedPerm denied. Opening Settings...", android.widget.Toast.LENGTH_LONG).show()
                permissionsGrantedState = false
                openAppSettings()
            } else {
                // If they granted it, but others are missing, auto-request the next one!
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ requestPermissions() }, 200)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionsGrantedState = hasAllPermissions()
        phoneTargetState = phoneTargetStore.get()
        val locManager = getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        locationEnabledState = locManager.isLocationEnabled
        
        var launchState = WatchState.ANCHOR
        if (intent?.action == "com.mtpnda.biocue.ACTION_LOG_EMOTION" || 
            intent?.getStringExtra("launch_state") == "CAROUSEL") {
            launchState = WatchState.CAROUSEL
        } else if (intent?.getStringExtra("launch_state") == "SETUP_PHONE") {
            launchState = WatchState.SETUP_PHONE
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

            val bleLinkState by BleLinkServer.get(this@MainActivity).state.collectAsState()
            val senderPhoneReachable by sender.isPhoneReachable.collectAsState()
            val servicePhoneReachable by (sensorService?.isPhoneReachable ?: _isPhoneReachable).collectAsState()
            val phoneReachable = if (phoneTargetState == PhoneTarget.IPHONE) {
                bleLinkState is BleLinkServer.LinkState.Established
            } else {
                senderPhoneReachable || servicePhoneReachable || isEmulator()
            }

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
                onOpenAppSettings = { openAppSettings() },
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
                onPermissionsResult = { granted -> permissionsGrantedState = granted },
                phoneTarget = phoneTargetState.name,
                iphoneLinkStatus = describeLink(bleLinkState),
                onSelectPhoneTarget = { selectPhoneTarget(PhoneTarget.valueOf(it)) },
                onStartIphonePairing = { startIphonePairing() }
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
            val needsSettings = missingPermissions.any { shouldShowRequestPermissionRationale(it) }
            if (needsSettings) {
                android.widget.Toast.makeText(this, "Please enable permissions in Settings", android.widget.Toast.LENGTH_LONG).show()
                openAppSettings()
            } else {
                // Request all missing permissions together (ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION must be paired)
                permissionLauncher.launch(missingPermissions.toTypedArray())
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

/** Short, glanceable text for the iPhone link on a watch screen. */
private fun describeLink(s: BleLinkServer.LinkState): String = when (s) {
    BleLinkServer.LinkState.Stopped -> "Tap Pair iPhone to begin."
    BleLinkServer.LinkState.Unsupported -> "Bluetooth LE is not available on this watch."
    BleLinkServer.LinkState.NoPermission -> "Allow Bluetooth access to continue."
    BleLinkServer.LinkState.BluetoothOff -> "Turn Bluetooth on."
    is BleLinkServer.LinkState.Advertising ->
        if (s.pairingOpen) "On your iPhone: open BioCue, then Connect watch." else "Waiting for your iPhone…"
    BleLinkServer.LinkState.Handshaking -> "Connecting…"
    BleLinkServer.LinkState.Established -> "iPhone connected."
    is BleLinkServer.LinkState.Refused ->
        if (s.reason == HelloStatus.ROLE_NOT_ALLOWED) "That is a caregiver account. Use the wearer's iPhone."
        else "Connection refused. Try Pair iPhone again."
}

enum class WatchState { SETUP_PERMISSIONS, SETUP_LOCATION, SETUP_PHONE, ANCHOR, GLOWING, CAROUSEL, ONGOING, SUCCESS }

data class Emotion(val icon: String, val label: String, val color: Color)
val emotions = listOf(
    Emotion(com.mtpnda.biocue.core.domain.model.EventTypes.getIconForLabel(com.mtpnda.biocue.core.domain.model.EventTypes.LABEL_OVERWHELMED), com.mtpnda.biocue.core.domain.model.EventTypes.LABEL_OVERWHELMED, Color(android.graphics.Color.parseColor(com.mtpnda.biocue.core.domain.model.EventTypes.getColorHexForLabel(com.mtpnda.biocue.core.domain.model.EventTypes.LABEL_OVERWHELMED)))),
    Emotion(com.mtpnda.biocue.core.domain.model.EventTypes.getIconForLabel(com.mtpnda.biocue.core.domain.model.EventTypes.LABEL_EXCITED), com.mtpnda.biocue.core.domain.model.EventTypes.LABEL_EXCITED, Color(android.graphics.Color.parseColor(com.mtpnda.biocue.core.domain.model.EventTypes.getColorHexForLabel(com.mtpnda.biocue.core.domain.model.EventTypes.LABEL_EXCITED)))),
    Emotion(com.mtpnda.biocue.core.domain.model.EventTypes.getIconForLabel(com.mtpnda.biocue.core.domain.model.EventTypes.LABEL_ACTIVE), com.mtpnda.biocue.core.domain.model.EventTypes.LABEL_ACTIVE, Color(android.graphics.Color.parseColor(com.mtpnda.biocue.core.domain.model.EventTypes.getColorHexForLabel(com.mtpnda.biocue.core.domain.model.EventTypes.LABEL_ACTIVE))))
)

@Composable
fun WearApp(
    hasPermissions: Boolean,
    isLocationEnabled: Boolean,
    isTracking: Boolean,
    isPhoneReachable: Boolean,
    isTestModeEnabled: Boolean,
    onOpenLocationSettings: () -> Unit,
    onOpenAppSettings: () -> Unit = {},
    onRequestPermissions: () -> Unit,
    onToggleTracking: () -> Unit,
    onToggleTestMode: () -> Unit,
    onStartEvent: () -> Unit,
    onLabelEvent: (String) -> Unit,
    onEndEvent: () -> Unit,
    launchState: WatchState = WatchState.ANCHOR,
    onPermissionsResult: ((Boolean) -> Unit)? = null,
    phoneTarget: String = "ANDROID",
    iphoneLinkStatus: String = "",
    onSelectPhoneTarget: (String) -> Unit = {},
    onStartIphonePairing: () -> Unit = {}
) {
    BiocueTheme {
        var watchState by remember { 
            mutableStateOf(
                when {
                    !hasPermissions -> WatchState.SETUP_PERMISSIONS
                    !isLocationEnabled -> WatchState.SETUP_LOCATION
                    else -> launchState
                }
            )
        }
        var selectedEmotion by remember { mutableStateOf<Emotion?>(null) }
        
        // When permissions/location change, auto-progress
        LaunchedEffect(hasPermissions, isLocationEnabled) {
            if (hasPermissions && isLocationEnabled && (watchState == WatchState.SETUP_PERMISSIONS || watchState == WatchState.SETUP_LOCATION)) {
                watchState = launchState
                if (hasPermissions) onPermissionsResult?.invoke(true)
            } else if (hasPermissions && !isLocationEnabled && watchState == WatchState.SETUP_PERMISSIONS) {
                watchState = WatchState.SETUP_LOCATION
            }
        }

        AppScaffold {
            when (watchState) {
                WatchState.SETUP_PERMISSIONS -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Welcome to BioCue", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text(
                            "Sensor access needed to monitor stress.",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompactButton(
                                onClick = onRequestPermissions, 
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), contentColor = Color.White)
                            ) {
                                Text("Grant", fontSize = 11.sp, color = Color.White)
                            }
                            CompactButton(
                                onClick = onOpenAppSettings, 
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E3A), contentColor = Color.White)
                            ) {
                                Text("Settings", fontSize = 11.sp, color = Color.White)
                            }
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
                        if (phoneTarget == "IPHONE") {
                            Text(
                                iphoneLinkStatus.ifEmpty { "On your iPhone: open BioCue, then Connect watch." },
                                fontSize = 11.sp, color = Color.White, textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            CompactButton(
                                onClick = onStartIphonePairing,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), contentColor = Color.White)
                            ) { Text("Pair iPhone", color = Color.White, fontWeight = FontWeight.SemiBold) }
                            CompactButton(
                                onClick = { onSelectPhoneTarget("ANDROID") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E3A), contentColor = Color.White)
                            ) { Text("Use Android phone", fontSize = 11.sp, color = Color.White) }
                        } else {
                            Text("Open BioCue on your phone to connect.", fontSize = 11.sp, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 4.dp))
                            CompactButton(
                                onClick = { onSelectPhoneTarget("IPHONE") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), contentColor = Color.White)
                            ) { Text("I use an iPhone", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold) }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        CompactButton(
                            onClick = { watchState = WatchState.ANCHOR },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E3A), contentColor = Color.White)
                        ) { Text("Back to Watch", fontSize = 11.sp, color = Color.White) }
                    }
                }
                WatchState.ANCHOR, WatchState.GLOWING -> {
                    // Simple full screen column for Anchor
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Connection Status Dot Indicator - always visible on Anchor screen
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .clickable {
                                    watchState = WatchState.SETUP_PHONE
                                }
                        ) {
                            val dotColor = if (isPhoneReachable) Color(0xFF34D399) else Color(0xFFF59E0B)
                            val statusText = when {
                                phoneTarget == "IPHONE" -> if (isPhoneReachable) "iPhone Connected" else "iPhone Disconnected"
                                else -> if (isPhoneReachable) "Android Connected" else "Android Disconnected"
                            }
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = statusText,
                                fontSize = 10.sp,
                                color = dotColor,
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
                            Text(
                                text = "Breathe · tap if you need help",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
                WatchState.CAROUSEL -> {
                    val pagerState = rememberPagerState(pageCount = { emotions.size })
                    Box(modifier = Modifier.fillMaxSize()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val emotion = emotions[page]
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        selectedEmotion = emotion
                                        onLabelEvent(emotion.label.uppercase())
                                        watchState = WatchState.ONGOING
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = emotion.icon,
                                    fontSize = 48.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = emotion.label,
                                    color = emotion.color,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                        
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(emotions.size) { iteration ->
                                val color = if (pagerState.currentPage == iteration) Color.White else Color.DarkGray
                                Box(
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .size(6.dp)
                                )
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
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring
        Box(
            modifier = Modifier
                .size(80.dp * pulseScale)
                .clip(CircleShape)
                .background(Color(0xFF64748B).copy(alpha = 0.2f))
        )
        // Inner button
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(0xFF334155).copy(alpha = 0.8f))
        )
    }
}