package com.mtpnda.biocue.ui.screens.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtpnda.biocue.R
import com.mtpnda.biocue.ui.theme.*
import com.mtpnda.biocue.ui.components.SectionLabel

@Composable
fun ProfileScreen(
    userName: String,
    role: String,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    androidx.compose.ui.res.stringResource(R.string.profile_title),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        androidx.compose.ui.res.stringResource(R.string.profile_name, userName),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        androidx.compose.ui.res.stringResource(R.string.profile_role, role),
                        color = CalmColor,
                        fontSize = 14.sp
                    )
                }
            }

            SectionLabel("Language / Bahasa")
            
            var expanded by remember { mutableStateOf(false) }
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

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = DangerColor),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text(
                    androidx.compose.ui.res.stringResource(R.string.profile_logout),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
