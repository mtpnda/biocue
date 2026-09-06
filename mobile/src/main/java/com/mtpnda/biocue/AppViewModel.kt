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
import java.util.UUID

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
                    _caregiverPairingStatus.value = "ERROR"
                }
            } else {
                _caregiverPairingStatus.value = "NOT_FOUND"
            }
        }
    }
    
    fun acknowledgeRevocation() {
        _pairingRevoked.value = false
        _caregiverPairingStatus.value = null
    }
}
