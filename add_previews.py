import os

caregiver_file = "mobile/src/main/java/com/mtpnda/biocue/ui/screens/caregiver/CaregiverDashboardScreen.kt"
wearer_file = "mobile/src/main/java/com/mtpnda/biocue/ui/screens/wearer/WearerDashboardScreen.kt"

preview_import = "\nimport androidx.compose.ui.tooling.preview.Preview\n"

# Caregiver
with open(caregiver_file, 'r') as f:
    content = f.read()

if "@Preview" not in content:
    if "import androidx.compose.ui.tooling.preview.Preview" not in content:
        content = content.replace("import androidx.compose.runtime.", "import androidx.compose.ui.tooling.preview.Preview\nimport androidx.compose.runtime.")
    
    preview_code = """

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Caregiver Dashboard Tier 0")
@Composable
fun CaregiverDashboardScreenPreview_Tier0() {
    BioCueTheme {
        CaregiverDashboardScreen(tier = 0, activity = "🪑 Sitting still", modifier = Modifier)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Caregiver Dashboard Tier 2")
@Composable
fun CaregiverDashboardScreenPreview_Tier2() {
    BioCueTheme {
        CaregiverDashboardScreen(tier = 2, activity = "🔄 Pacing", modifier = Modifier)
    }
}
"""
    with open(caregiver_file, 'w') as f:
        f.write(content + preview_code)


# Wearer
with open(wearer_file, 'r') as f:
    content = f.read()

if "@Preview" not in content:
    if "import androidx.compose.ui.tooling.preview.Preview" not in content:
        content = content.replace("import androidx.compose.runtime.", "import androidx.compose.ui.tooling.preview.Preview\nimport androidx.compose.runtime.")
    
    preview_code = """

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Wearer Dashboard")
@Composable
fun WearerDashboardScreenPreview() {
    BioCueTheme {
        WearerDashboardScreen(modifier = Modifier)
    }
}
"""
    with open(wearer_file, 'w') as f:
        f.write(content + preview_code)

print("Added previews.")
