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
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.catch
import com.mtpnda.biocue.core.pairing.AccessEffect
import com.mtpnda.biocue.core.pairing.AccessState
import com.mtpnda.biocue.core.pairing.ApprovalResult
import com.mtpnda.biocue.core.pairing.CaregiverAccess
import com.mtpnda.biocue.core.pairing.Clock
import com.mtpnda.biocue.core.pairing.CodeResult
import com.mtpnda.biocue.core.pairing.PairingLifecycle
import com.mtpnda.biocue.core.pairing.SubmitResult
import com.mtpnda.biocue.data.FirestorePairingStore
import com.mtpnda.biocue.data.toDomain
import com.mtpnda.biocue.role.RoleStore
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

    // False until Firestore has answered once for the signed-in user (with a profile OR with "none").
    // Until then the UI must not offer role selection: a tap there could overwrite an existing role.
    private val _profileLoaded = MutableStateFlow(false)
    val profileLoaded: StateFlow<Boolean> = _profileLoaded.asStateFlow()

    private var profileJob: Job? = null

    // Pairing rules (spec 07) live in a unit-tested domain object; this class only talks to it and updates UI state.
    private val pairing = PairingLifecycle(FirestorePairingStore(), Clock { System.currentTimeMillis() })
    private val prefs by lazy { getApplication<Application>().getSharedPreferences("biocue_pairing", android.content.Context.MODE_PRIVATE) }
    // Per ACCOUNT, never per install: created fresh for each signed-in caregiver and dropped on sign-out.
    private var caregiverAccess = CaregiverAccess()
    private fun ackKey(uid: String) = "ack_removal_at_$uid"
    private var pairingJob: Job? = null

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
    
    private var timerJob: Job? = null

    fun loadUserProfile() {
        val uid = currentUserId
        if (uid == "anonymous") {
            _profileLoaded.value = true
            return
        }
        
        // Populate cached role immediately to avoid any screen flickers
        if (_userRole.value == null) {
            _userRole.value = RoleStore.get(getApplication())
        }

        // Restart cleanly: a second call (init + LaunchedEffect + login) must not stack listeners.
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            try {
                firestoreRepo.getUserProfile(uid).collect { profile ->
                    if (profile != null) {
                        _userRole.value = profile.role
                        _userName.value = profile.displayName
                        val now = System.currentTimeMillis()
                        if (!profile.pairingCode.isNullOrEmpty() && (profile.pairingCodeExpiresAt ?: 0L) > now) {
                            _pairingCode.value = profile.pairingCode
                            _timeRemainingSeconds.value = (((profile.pairingCodeExpiresAt ?: now) - now) / 1000).toInt()
                            startCountdownTimer()
                        }
                        RoleStore.save(getApplication(), profile.role)
                        pairingJob?.cancel()
                        pairingJob = if (profile.role == UserRole.CAREGIVER) {
                            observeCaregiverPairings(uid)
                        } else {
                            observeWearerPairings(uid)
                        }
                    }
                    _profileLoaded.value = true
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Profile listener ended", e)
                _profileLoaded.value = true
            }
        }
    }

    private fun startCountdownTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timeRemainingSeconds.value > 0 && _pairingCode.value.isNotEmpty()) {
                delay(1000)
                _timeRemainingSeconds.value -= 1
            }
            if (_timeRemainingSeconds.value <= 0) {
                _pairingCode.value = ""
            }
        }
    }

    /**
     * Sign-out: drop everything that belongs to the previous account. Without this, the next
     * account to sign in on this device would briefly inherit the previous user's role and screens.
     */
    fun resetForSignOut() {
        timerJob?.cancel(); timerJob = null
        profileJob?.cancel(); profileJob = null
        pairingJob?.cancel(); pairingJob = null
        caregiverAccess = CaregiverAccess()
        _userRole.value = null
        _profileLoaded.value = false
        _pairedWearer.value = null
        _pairingRevoked.value = false
        _caregiverPairingStatus.value = null
        _userName.value = "User"
        _pairingCode.value = ""
        _timeRemainingSeconds.value = 0
        _connectedCaregivers.value = emptyList()
        _pendingRequests.value = emptyList()
        RoleStore.clear(getApplication())
    }

    fun saveUserRole(role: UserRole) {
        val uid = currentUserId
        if (uid == "anonymous") return
        viewModelScope.launch {
            try {
                firestoreRepo.createUserProfile(uid, role, currentDisplayName)
                _userRole.value = role
                RoleStore.save(getApplication(), role)
                loadUserProfile()
            } catch (e: Exception) {
                // e.g. the server refused because this account already has a role (roles are permanent) or offline.
                Log.e("AppViewModel", "Could not save role", e)
                _userRole.value = role
                RoleStore.save(getApplication(), role)
                loadUserProfile()
            }
        }
    }
    
    // CAREGIVER LOGIC
    private fun observeCaregiverPairings(caregiverUid: String): Job {
        caregiverAccess = CaregiverAccess(prefs.getLong(ackKey(caregiverUid), 0L))
        return viewModelScope.launch {
            // ALL statuses (the old query filtered APPROVED, so a removal was never seen).
            firestoreRepo.getCaregiverPairings(caregiverUid)
                .catch { Log.e("AppViewModel", "pairing listener ended", it) }
                .collect { list ->
                    val update = caregiverAccess.onPairingsObserved(list.map { it.toDomain() })
                    when (val state = update.state) {
                        is AccessState.Connected -> {
                            _pairedWearer.value = list.firstOrNull { it.id == state.pairing.id }
                            _pairingRevoked.value = false
                            _caregiverPairingStatus.value = "CONNECTED"
                        }
                        is AccessState.Pending -> if (_caregiverPairingStatus.value != "SEARCHING") _caregiverPairingStatus.value = "PENDING"
                        AccessState.Revoked -> {
                            _pairedWearer.value = null
                            _pairingRevoked.value = true
                            _caregiverPairingStatus.value = "REVOKED"
                        }
                        AccessState.None -> Unit
                    }
                    if (AccessEffect.WIPE_LOCAL_CACHE in update.effects) {
                        // Spec 07: wipe cached wearer data. In-memory state is cleared by the caregiver screen; Firestore's
                        // on-disk cache can only be cleared before Firestore starts, so flag it for the next cold start.
                        prefs.edit().putBoolean("purge_firestore_cache", true).apply()
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
                val result = withContext(Dispatchers.IO) { pairing.submitCode(caregiverUid, currentDisplayName, code) }
                _caregiverPairingStatus.value = when (result) {
                    SubmitResult.NotFound -> "NOT_FOUND"
                    SubmitResult.Requested, SubmitResult.AlreadyPending -> "PENDING"
                    SubmitResult.AlreadyConnected -> "CONNECTED"
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error submitting pairing code", e)
                _caregiverPairingStatus.value = "ERROR"
            }
        }
    }

    fun acknowledgeRevocation() {
        prefs.edit().putLong(ackKey(currentUserId), caregiverAccess.acknowledgeRevocation()).apply()
        _pairingRevoked.value = false
        _caregiverPairingStatus.value = null
    }

    // WEARER LOGIC
    private fun observeWearerPairings(wearerUid: String): Job {
        return viewModelScope.launch {
            firestoreRepo.getCaregiversForWearer(wearerUid).collect { pairings ->
                _connectedCaregivers.value = pairings.filter { it.status == com.mtpnda.biocue.data.PairingStatus.APPROVED }
                _pendingRequests.value = pairings.filter { it.status == com.mtpnda.biocue.data.PairingStatus.PENDING }
                // Spec 07: the code is single-use — consume it as soon as the first request arrives.
                if (_pendingRequests.value.isNotEmpty() && _pairingCode.value.isNotEmpty()) {
                    try {
                        withContext(Dispatchers.IO) { pairing.onPairingsChanged(wearerUid) }
                        _pairingCode.value = ""
                    } catch (e: Exception) { Log.e("AppViewModel", "could not consume pairing code", e) }
                }
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
                    _timeRemainingSeconds.value = 600
                    startCountdownTimer()
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error generating pairing code", e)
            }
        }
    }

    fun approveCaregiver(pairingId: String) {
        val uid = currentUserId
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { pairing.approve(uid, pairingId) }
                if (result == ApprovalResult.MaxCaregiversReached) Log.i("AppViewModel", "approval refused: maximum caregivers reached")
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error approving caregiver", e)
            }
        }
    }

    /** Spec 07: Deny deletes the pairing document. */
    fun denyCaregiver(pairingId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { pairing.deny(pairingId) }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error denying caregiver", e)
            }
        }
    }

    /** Spec 07: Remove sets REMOVED and removedAt. */
    fun removeCaregiver(pairingId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { pairing.remove(pairingId) }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error removing caregiver", e)
            }
        }
    }
}
