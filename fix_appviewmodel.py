import re

with open("mobile/src/main/java/com/mtpnda/biocue/AppViewModel.kt", "r") as f:
    content = f.read()

# Add _userName and userName stateflow
new_states = """    private val _caregiverPairingStatus = MutableStateFlow<String?>(null)
    val caregiverPairingStatus: StateFlow<String?> = _caregiverPairingStatus.asStateFlow()

    private val _userName = MutableStateFlow<String>("User")
    val userName: StateFlow<String> = _userName.asStateFlow()
"""
content = content.replace("    private val _caregiverPairingStatus = MutableStateFlow<String?>(null)\n    val caregiverPairingStatus: StateFlow<String?> = _caregiverPairingStatus.asStateFlow()\n", new_states)

# Update loadUserProfile
load_profile = """            firestoreRepo.getUserProfile(uid).collect { profile ->
                if (profile != null) {
                    _userRole.value = profile.role
                    _userName.value = profile.displayName
                    
                    if (profile.role == UserRole.CAREGIVER) {"""
content = re.sub(r"            firestoreRepo.getUserProfile\(uid\).collect \{ profile ->\n                if \(profile != null\) \{\n                    _userRole.value = profile.role\n                    \n                    if \(profile.role == UserRole.CAREGIVER\) \{", load_profile, content)

with open("mobile/src/main/java/com/mtpnda/biocue/AppViewModel.kt", "w") as f:
    f.write(content)
