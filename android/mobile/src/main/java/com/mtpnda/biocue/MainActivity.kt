package com.mtpnda.biocue

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.mtpnda.biocue.ui.theme.BiocueTheme
import com.mtpnda.biocue.ui.screens.profile.ProfileScreen

class MainActivity : AppCompatActivity() {

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
        // Spec 07: a removed caregiver's on-disk Firestore cache can only be cleared before Firestore is first used,
        // so revocation sets a flag and the clean-up happens here, at the start of the next launch.
        val purgePrefs = getSharedPreferences("biocue_pairing", MODE_PRIVATE)
        if (purgePrefs.getBoolean("purge_firestore_cache", false)) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().clearPersistence().addOnCompleteListener {
                if (it.isSuccessful) purgePrefs.edit().putBoolean("purge_firestore_cache", false).apply()
            }
        }
        enableEdgeToEdge()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        runCatching { startService(android.content.Intent(this, WatchDataReceiverService::class.java)) }
        
        setContent {
            BiocueTheme {
                val auth = FirebaseAuth.getInstance()
                var isUserLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
                val userRole by appViewModel.userRole.collectAsState()
                val pairedWearer by appViewModel.pairedWearer.collectAsState()
                val pairingRevoked by appViewModel.pairingRevoked.collectAsState()
                val userName by appViewModel.userName.collectAsState()
                val profileLoaded by appViewModel.profileLoaded.collectAsState()
                var showProfileScreen by remember { mutableStateOf(false) }
                var showManageConnections by remember { mutableStateOf(false) }

                LaunchedEffect(isUserLoggedIn) {
                    if (isUserLoggedIn) {
                        appViewModel.loadUserProfile()
                    }
                }

                if (!isUserLoggedIn) {
                    // Not logged in -> show login
                    com.mtpnda.biocue.connect.LoginScreen(onLoginSuccess = { 
                        isUserLoggedIn = true
                        appViewModel.loadUserProfile()
                    })
                } else if (pairingRevoked) {
                    // Caregiver was removed by wearer
                    // Spec 07: wipe everything cached about the wearer the moment access is revoked.
                    val caregiverVmToWipe: CaregiverDashboardViewModel = viewModel()
                    LaunchedEffect(Unit) { caregiverVmToWipe.wipe() }
                    com.mtpnda.biocue.connect.AccessRevokedScreen(
                        onDismiss = { appViewModel.acknowledgeRevocation() }
                    )
                } else if (!profileLoaded) {
                    // Waiting for Firestore to say whether this account already has a role.
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) { androidx.compose.material3.CircularProgressIndicator() }
                } else if (userRole == null) {
                    // Logged in but no role selected yet
                    com.mtpnda.biocue.connect.RoleSelectionScreen(
                        onRoleSelected = { role -> appViewModel.saveUserRole(role) }
                    )
                } else if (showManageConnections) {
                    if (userRole == com.mtpnda.biocue.data.UserRole.WEARER) {
                        com.mtpnda.biocue.connect.PairingScreen(
                            pairingCode = appViewModel.pairingCode.collectAsState().value,
                            timeRemainingSeconds = appViewModel.timeRemainingSeconds.collectAsState().value,
                            connectedCaregivers = appViewModel.connectedCaregivers.collectAsState().value,
                            pendingRequests = appViewModel.pendingRequests.collectAsState().value,
                            onGenerateCode = { appViewModel.generatePairingCode() },
                            onApprove = { id -> appViewModel.approveCaregiver(id) },
                            onDeny = { id -> appViewModel.denyCaregiver(id) },
                            onRemove = { id -> appViewModel.removeCaregiver(id) },
                            onBack = { showManageConnections = false },
                            onContinue = { showManageConnections = false }
                        )
                    } else {
                        com.mtpnda.biocue.connect.CaregiverPairingScreen(
                            status = appViewModel.caregiverPairingStatus.collectAsState().value,
                            wearerName = pairedWearer?.wearerName,
                            onSubmitCode = { code -> appViewModel.submitPairingCode(code) },
                            onUnlink = { 
                                pairedWearer?.id?.let { id -> appViewModel.removeCaregiver(id) }
                                appViewModel.acknowledgeRevocation()
                            },
                            onBack = { showManageConnections = false },
                            onContinue = { showManageConnections = false }
                        )
                    }
                } else if (showProfileScreen) {
                    val roleStr = if (userRole == com.mtpnda.biocue.data.UserRole.CAREGIVER) 
                        androidx.compose.ui.res.stringResource(R.string.mode_caregiver) else 
                        androidx.compose.ui.res.stringResource(R.string.mode_wearer)
                    ProfileScreen(
                        userName = userName,
                        role = roleStr,
                        onBack = { showProfileScreen = false },
                        onManageConnections = {
                            showProfileScreen = false
                            showManageConnections = true
                            if (userRole == com.mtpnda.biocue.data.UserRole.WEARER && appViewModel.pairingCode.value.isEmpty()) {
                                appViewModel.generatePairingCode()
                            }
                        },
                        onLogout = { 
                            auth.signOut()
                            appViewModel.resetForSignOut()
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
                    WearerDashboardRoute(wearerViewModel, appViewModel, userName, isEmulator(), onOpenProfile = { showProfileScreen = true })
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
                            onUnlink = {},
                            onContinue = {}
                        )
                    }
                }
            }
        }
    }
}
