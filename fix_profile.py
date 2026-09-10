import re

with open("mobile/src/main/java/com/mtpnda/biocue/ui/screens/profile/ProfileScreen.kt", "r") as f:
    content = f.read()

# Add missing imports for state
if "import androidx.compose.runtime.getValue" not in content:
    content = content.replace("import androidx.compose.foundation.layout.*", "import androidx.compose.foundation.layout.*\nimport androidx.compose.runtime.getValue\nimport androidx.compose.runtime.setValue\nimport androidx.compose.runtime.mutableStateOf\nimport androidx.compose.runtime.remember")

# Find the button section and replace it with the Dropdown
btn_section = r"            Button\([\s\S]*?            \}\n"

new_section = """            var expanded by remember { mutableStateOf(false) }
            val currentLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags()
            val isId = currentLocales.contains("id") || currentLocales.contains("in")

            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🌐", modifier = Modifier.padding(end = 12.dp), fontSize = 18.sp)
                            Text(
                                if (isId) "Bahasa Indonesia" else "English",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                        Text("▼", color = Color.White, fontSize = 12.sp)
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color(0xFF1E293B))
                ) {
                    DropdownMenuItem(
                        text = { Text("English", color = Color.White) },
                        onClick = {
                            val localeList = androidx.core.os.LocaleListCompat.forLanguageTags("en")
                            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(localeList)
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Bahasa Indonesia", color = Color.White) },
                        onClick = {
                            val localeList = androidx.core.os.LocaleListCompat.forLanguageTags("id")
                            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(localeList)
                            expanded = false
                        }
                    )
                }
            }
"""

content = re.sub(btn_section, new_section, content)

with open("mobile/src/main/java/com/mtpnda/biocue/ui/screens/profile/ProfileScreen.kt", "w") as f:
    f.write(content)

