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
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.mtpnda.biocue.ui.theme.BiocueTheme
import com.mtpnda.biocue.ui.screens.profile.ProfileScreen

class MainActivity : ComponentActivity() {

    fun isEmulator(): Boolean {
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

    private val db by lazy {
        androidx.room.Room.databaseBuilder(
            applicationContext,
            com.mtpnda.biocue.core.data.local.DetectionDatabase::class.java, "sensor-database"
        ).build()
    }
    
    private val appViewModel: AppViewModel by viewModels()

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
                val auth = FirebaseAuth.getInstance()
                var isUserLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
                val userRole by appViewModel.userRole.collectAsState()
                val pairedWearer by appViewModel.pairedWearer.collectAsState()
                val pairingRevoked by appViewModel.pairingRevoked.collectAsState()
                var showProfileScreen by remember { mutableStateOf(false) }

                LaunchedEffect(isUserLoggedIn) {
                    if (isUserLoggedIn) {
                        appViewModel.loadUserProfile()
                    }
                }

                if (!isUserLoggedIn) {
                    // Not logged in -> show login
                    com.mtpnda.biocue.connect.LoginScreen(onLoginSuccess = { 
                        isUserLoggedIn = true 
                    })
                } else if (pairingRevoked) {
                    // Caregiver was removed by wearer
                    com.mtpnda.biocue.connect.AccessRevokedScreen(
                        onDismiss = { appViewModel.acknowledgeRevocation() }
                    )
                } else if (userRole == null) {
                    // Logged in but no role selected yet
                    com.mtpnda.biocue.connect.RoleSelectionScreen(
                        onRoleSelected = { role -> appViewModel.saveUserRole(role) }
                    )
                } else if (showProfileScreen) {
                    val roleStr = if (userRole == com.mtpnda.biocue.data.UserRole.CAREGIVER) 
                        androidx.compose.ui.res.stringResource(R.string.mode_caregiver) else 
                        androidx.compose.ui.res.stringResource(R.string.mode_wearer)
                    ProfileScreen(
                        userName = auth.currentUser?.displayName ?: "User",
                        role = roleStr,
                        onBack = { showProfileScreen = false },
                        onLogout = { 
                            auth.signOut()
                            isUserLoggedIn = false
                            showProfileScreen = false
                        }
                    )
                } else if (userRole == com.mtpnda.biocue.data.UserRole.WEARER) {
                    // WEARER FLOW
                    val wearerViewModel: WearerDashboardViewModel = viewModel(
                        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                return WearerDashboardViewModel(application, db) as T
                            }
                        }
                    )
                    WearerDashboardRoute(wearerViewModel, auth.currentUser?.displayName ?: "Wearer", isEmulator(), onOpenProfile = { showProfileScreen = true })
                } else {
                    // CAREGIVER FLOW
                    if (pairedWearer != null) {
                        val caregiverViewModel: CaregiverDashboardViewModel = viewModel()
                        LaunchedEffect(pairedWearer!!.wearerUid) {
                            caregiverViewModel.startObservation(pairedWearer!!.wearerUid)
                        }
                        CaregiverDashboardRoute(caregiverViewModel, pairedWearer!!.wearerName, onOpenProfile = { showProfileScreen = true })
                    } else {
                        com.mtpnda.biocue.connect.CaregiverPairingScreen(
                            status = appViewModel.caregiverPairingStatus.collectAsState().value,
                            wearerName = pairedWearer?.wearerName,
                            onSubmitCode = { code -> appViewModel.submitPairingCode(code) },
                            onContinue = {}
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WearerDashboardRoute(viewModel: WearerDashboardViewModel, userName: String, isStubAllowed: Boolean = false, onOpenProfile: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mockIntensity by viewModel.mockIntensity.collectAsState()
    val tier by viewModel.tier.collectAsState()
    val sosLat by viewModel.sosLatitude.collectAsState()
    val sosLng by viewModel.sosLongitude.collectAsState()
    val stress by viewModel.stressLevel.collectAsState()
    val activity by viewModel.activityLabel.collectAsState()
    val journal by viewModel.journal.collectAsState()
    val journalOffset by viewModel.journalOffset.collectAsState()
    val reviews by viewModel.reviewEvents.collectAsState()
    val hasOngoing by viewModel.hasOngoingEvent.collectAsState()
    val connStatus by viewModel.connectionStatus.collectAsState()
    val mockActivity by viewModel.mockActivity.collectAsState()
    val isCalmAudioPlaying by viewModel.isCalmAudioPlaying.collectAsState()

    com.mtpnda.biocue.ui.screens.wearer.WearerDashboardScreen(
        userName = userName,
        isStubAllowed = isStubAllowed,
        mockIntensity = mockIntensity,
        mockActivity = mockActivity,
        tier = tier,
        sosLat = sosLat ?: 0.0,
        sosLng = sosLng ?: 0.0,
        stress = stress,
        activity = activity,
        journal = journal,
        journalOffset = journalOffset,
        reviews = reviews,
        hasOngoing = hasOngoing,
        connStatus = connStatus,
        isCalmAudioPlaying = isCalmAudioPlaying,
        onToggleCalmAudio = { viewModel.toggleCalmAudio(context) },
        onSetMockIntensity = { viewModel.setMockIntensity(context, it) },
        onSetMockActivity = { viewModel.setMockActivity(it) },
        onSimulateManualEvent = { viewModel.simulateWatchManualEvent(context, it) },
        onResolveReview = { id, label -> viewModel.resolveReviewEvent(id, label) },
        onChangeJournalDate = { delta -> viewModel.changeJournalDate(delta) },
        onOpenProfile = onOpenProfile
    )
}

@Composable
fun CaregiverDashboardRoute(viewModel: CaregiverDashboardViewModel, userName: String, onOpenProfile: () -> Unit) {
    val journal by viewModel.journal.collectAsState()
    val journalOffset by viewModel.journalOffset.collectAsState()
    val reviews by viewModel.reviewEvents.collectAsState()
    val hasOngoing by viewModel.hasOngoingEvent.collectAsState()

    com.mtpnda.biocue.ui.screens.caregiver.CaregiverDashboardScreen(
        userName = userName,
        journal = journal,
        journalOffset = journalOffset,
        reviews = reviews,
        hasOngoing = hasOngoing,
        onResolveReview = { _, _ -> },
        onChangeJournalDate = { viewModel.changeJournalDate(it) },
        onOpenProfile = onOpenProfile
    )
}
