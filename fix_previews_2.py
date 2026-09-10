import re

caregiver_file = "mobile/src/main/java/com/mtpnda/biocue/ui/screens/caregiver/CaregiverDashboardScreen.kt"
wearer_file = "mobile/src/main/java/com/mtpnda/biocue/ui/screens/wearer/WearerDashboardScreen.kt"

# Caregiver
with open(caregiver_file, 'r') as f:
    cg_content = f.read()

cg_content = cg_content.replace("BioCueTheme", "BiocueTheme")
cg_content = cg_content.replace("onResolveReview = {_,_,_->}, onChangeJournalDate = {}, onOpenProfile = {}, modifier = Modifier", "onResolveReview = {_,_->}, onChangeJournalDate = {}, onOpenProfile = {}")

with open(caregiver_file, 'w') as f:
    f.write(cg_content)

# Wearer
with open(wearer_file, 'r') as f:
    w_content = f.read()

w_content = w_content.replace("BioCueTheme", "BiocueTheme")
w_content = w_content.replace("mockIntensity = 0.5f", "mockIntensity = com.mtpnda.biocue.MockIntensity.LOW")
w_content = w_content.replace("stress = 20.0f", "stress = 20")
w_content = w_content.replace("onResolveReview = {_,_,_->},", "onResolveReview = {_,_->},")
w_content = w_content.replace("modifier = Modifier", "")
w_content = w_content.replace("com.mtpnda.biocue.ui.screens.wearer.ConnectionStatus.DISCONNECTED", "com.mtpnda.biocue.ConnectionStatus.DISCONNECTED")

with open(wearer_file, 'w') as f:
    f.write(w_content)

print("Fixed previews again.")
