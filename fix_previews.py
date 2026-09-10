import re

caregiver_file = "mobile/src/main/java/com/mtpnda/biocue/ui/screens/caregiver/CaregiverDashboardScreen.kt"
wearer_file = "mobile/src/main/java/com/mtpnda/biocue/ui/screens/wearer/WearerDashboardScreen.kt"

theme_import = "import com.mtpnda.biocue.ui.theme.BioCueTheme\n"

# Fix Caregiver
with open(caregiver_file, 'r') as f:
    cg_content = f.read()

if "import com.mtpnda.biocue.ui.theme.BioCueTheme" not in cg_content:
    cg_content = cg_content.replace("import androidx.compose.ui.tooling.preview.Preview", "import androidx.compose.ui.tooling.preview.Preview\n" + theme_import)

cg_content = re.sub(
    r'CaregiverDashboardScreen\(tier = 0, activity = "🪑 Sitting still", modifier = Modifier\)',
    'CaregiverDashboardScreen(userName = "Alex", tier = 0, activity = "🪑 Sitting still", hasOngoing = false, journal = emptyList(), journalOffset = 0, reviews = emptyList(), onResolveReview = {_,_,_->}, onChangeJournalDate = {}, onOpenProfile = {}, modifier = Modifier)',
    cg_content
)
cg_content = re.sub(
    r'CaregiverDashboardScreen\(tier = 2, activity = "🔄 Pacing", modifier = Modifier\)',
    'CaregiverDashboardScreen(userName = "Alex", tier = 2, activity = "🔄 Pacing", hasOngoing = true, journal = emptyList(), journalOffset = 0, reviews = emptyList(), onResolveReview = {_,_,_->}, onChangeJournalDate = {}, onOpenProfile = {}, modifier = Modifier)',
    cg_content
)
with open(caregiver_file, 'w') as f:
    f.write(cg_content)

# Fix Wearer
with open(wearer_file, 'r') as f:
    w_content = f.read()

if "import com.mtpnda.biocue.ui.theme.BioCueTheme" not in w_content:
    w_content = w_content.replace("import androidx.compose.ui.tooling.preview.Preview", "import androidx.compose.ui.tooling.preview.Preview\n" + theme_import)

mock_wearer = """WearerDashboardScreen(
            userName = "Alex",
            isStubAllowed = true,
            mockIntensity = 0.5f,
            mockActivity = "Pacing",
            tier = 0,
            sosLat = 0.0,
            sosLng = 0.0,
            stress = 20.0f,
            activity = "Walking",
            journal = emptyList(),
            journalOffset = 0,
            reviews = emptyList(),
            ongoingEventLabel = null,
            connStatus = com.mtpnda.biocue.ui.screens.wearer.ConnectionStatus.DISCONNECTED,
            isCalmAudioPlaying = false,
            onToggleCalmAudio = {},
            onSetMockIntensity = {},
            onSetMockActivity = {},
            onSimulateManualEvent = {},
            onEndOngoingEvent = {},
            onResolveReview = {_,_,_->},
            onChangeJournalDate = {},
            onOpenProfile = {},
            modifier = Modifier
        )"""

w_content = w_content.replace('WearerDashboardScreen(modifier = Modifier)', mock_wearer)
with open(wearer_file, 'w') as f:
    f.write(w_content)

print("Fixed previews.")
