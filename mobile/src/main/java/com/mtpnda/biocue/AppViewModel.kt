package com.mtpnda.biocue
import com.mtpnda.biocue.data.UserProfile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mtpnda.biocue.data.FirestoreRepository
import com.mtpnda.biocue.data.Pairing
import com.mtpnda.biocue.data.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.random.Random
import android.util.Log

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val firestoreRepo = FirestoreRepository
    
    private val _userRole = MutableStateFlow<UserRole?>(null)
    val userRole: StateFlow<UserRole?> = _userRole.asStateFlow()
    
    private val _pairedWearer = MutableStateFlow<Pairing?>(null)
    val pairedWearer: StateFlow<Pairing?> = _pairedWearer.asStateFlow()
    
    private val _pairingRevoked = MutableStateFlow(false)
    val pairingRevoked: StateFlow<Boolean> = _pairingRevoked.asStateFlow()
    
    private val _caregiverPairingStatus = MutableStateFlow<String?>(null)
    val caregiverPairingStatus: StateFlow<String?> = _caregiverPairingStatus.asStateFlow()

    private val _userName = MutableStateFlow<String>("User")
    val userName: StateFlow<String> = _userName.asStateFlow()
    
    // WEARER PAIRING STATES
    private val _pairingCode = MutableStateFlow("")
    val pairingCode: StateFlow<String> = _pairingCode.asStateFlow()

    private val _timeRemainingSeconds = MutableStateFlow(0)
    val timeRemainingSeconds: StateFlow<Int> = _timeRemainingSeconds.asStateFlow()

    private val _connectedCaregivers = MutableStateFlow<List<Pairing>>(emptyList())
    val connectedCaregivers: StateFlow<List<Pairing>> = _connectedCaregivers.asStateFlow()

    private val _pendingRequests = MutableStateFlow<List<Pairing>>(emptyList())
    val pendingRequests: StateFlow<List<Pairing>> = _pendingRequests.asStateFlow()

    private val currentUserId: String
        get() = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        
    private val currentDisplayName: String
        get() = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.displayName ?: "User"
        
    init {
        loadUserProfile()
    }
    
    fun loadUserProfile() {
        val uid = currentUserId
        if (uid == "anonymous") return
        
        viewModelScope.launch {
            firestoreRepo.getUserProfile(uid).collect { profile ->
                if (profile != null) {
                    _userRole.value = profile.role
                    _userName.value = profile.displayName
                    if (profile.role == UserRole.CAREGIVER) {
                        observeCaregiverPairings(uid)
                    } else if (profile.role == UserRole.WEARER) {
                        observeWearerPairings(uid)
                    }
                }
            }
        }
    }

    fun saveUserRole(role: UserRole) {
        val uid = currentUserId
        if (uid == "anonymous") return
        viewModelScope.launch {
            firestoreRepo.createUserProfile(uid, role, currentDisplayName)
            _userRole.value = role
            loadUserProfile()
        }
    }
    
    // CAREGIVER LOGIC
    private fun observeCaregiverPairings(caregiverUid: String) {
        viewModelScope.launch {
            firestoreRepo.getPairedWearer(caregiverUid).collect { pairing ->
                if (pairing != null) {
                    if (pairing.status == com.mtpnda.biocue.data.PairingStatus.APPROVED) {
                        _pairedWearer.value = pairing
                        _pairingRevoked.value = false
                        _caregiverPairingStatus.value = "CONNECTED"
                    } else if (pairing.status == com.mtpnda.biocue.data.PairingStatus.REMOVED) {
                        _pairedWearer.value = null
                        _pairingRevoked.value = true
                        _caregiverPairingStatus.value = "REVOKED"
                    } else if (pairing.status == com.mtpnda.biocue.data.PairingStatus.PENDING) {
                        _caregiverPairingStatus.value = "PENDING"
                    }
                }
            }
        }
    }
    
    fun submitPairingCode(code: String) {
        val caregiverUid = currentUserId
        if (caregiverUid == "anonymous") return
        
        viewModelScope.launch {
            _caregiverPairingStatus.value = "SEARCHING"
            try {
                val wearerProfile = firestoreRepo.findWearerByCode(code)
                if (wearerProfile != null) {
                    val success = firestoreRepo.requestPairing(
                        wearerUid = wearerProfile.uid,
                        caregiverUid = caregiverUid,
                        caregiverName = currentDisplayName,
                        wearerName = wearerProfile.displayName
                    )
                    if (success) {
                        _caregiverPairingStatus.value = "PENDING"
                    } else {
                        _caregiverPairingStatus.value = "MAX_REACHED"
                    }
                } else {
                    _caregiverPairingStatus.value = "NOT_FOUND"
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error submitting pairing code", e)
                _caregiverPairingStatus.value = "ERROR"
            }
        }
    }
    
    fun acknowledgeRevocation() {
        _pairingRevoked.value = false
        _caregiverPairingStatus.value = null
    }

    // WEARER LOGIC
    private fun observeWearerPairings(wearerUid: String) {
        viewModelScope.launch {
            firestoreRepo.getCaregiversForWearer(wearerUid).collect { pairings ->
                _connectedCaregivers.value = pairings.filter { it.status == com.mtpnda.biocue.data.PairingStatus.APPROVED }
                _pendingRequests.value = pairings.filter { it.status == com.mtpnda.biocue.data.PairingStatus.PENDING }
            }
        }
    }

    fun generatePairingCode() {
        val uid = currentUserId
        if (uid == "anonymous") return
        
        val initialCode = String.format("%06d", Random.nextInt(1000000))
        viewModelScope.launch {
            try {
                val uniqueCode = firestoreRepo.updatePairingCode(uid, initialCode)
                if (uniqueCode != null) {
                    _pairingCode.value = uniqueCode
                    _timeRemainingSeconds.value = 300
                    
                    // simple timer
                    while (_timeRemainingSeconds.value > 0 && _pairingCode.value == uniqueCode) {
                        delay(1000)
                        _timeRemainingSeconds.value -= 1
                    }
                    if (_pairingCode.value == uniqueCode) {
                        _pairingCode.value = ""
                        firestoreRepo.updatePairingCode(uid, null)
                    }
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error generating pairing code", e)
            }
        }
    }

    fun approveCaregiver(pairingId: String) {
        viewModelScope.launch {
            try {
                firestoreRepo.updatePairingStatus(pairingId, com.mtpnda.biocue.data.PairingStatus.APPROVED)
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error approving caregiver", e)
            }
        }
    }

    fun denyCaregiver(pairingId: String) {
        viewModelScope.launch {
            try {
                firestoreRepo.updatePairingStatus(pairingId, com.mtpnda.biocue.data.PairingStatus.REMOVED)
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error denying caregiver", e)
            }
        }
    }

    fun removeCaregiver(pairingId: String) {
        viewModelScope.launch {
            try {
                firestoreRepo.updatePairingStatus(pairingId, com.mtpnda.biocue.data.PairingStatus.REMOVED)
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error removing caregiver", e)
            }
        }
    }
}
