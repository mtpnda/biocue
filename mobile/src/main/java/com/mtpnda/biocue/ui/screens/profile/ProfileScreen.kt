package com.mtpnda.biocue.ui.screens.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtpnda.biocue.R
import com.mtpnda.biocue.ui.theme.*

@Composable
fun ProfileScreen(
    userName: String,
    role: String,
    onBack: () -> Unit,
    onManageConnections: () -> Unit,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgSlate)
    ) {
        // Ambient Background
        Box(
            modifier = Modifier
                .offset(x = (-100).dp, y = (-200).dp)
                .size(500.dp)
                .background(Color(0xFF0EA5E9).copy(alpha = 0.15f), shape = RoundedCornerShape(250.dp))
                .blur(100.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .size(500.dp)
                .background(Color(0xFF34D399).copy(alpha = 0.15f), shape = RoundedCornerShape(250.dp))
                .blur(100.dp)
        )
        
        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .statusBarsPadding(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GlassSurface)
                            .clickable(onClick = onBack)
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        androidx.compose.ui.res.stringResource(R.string.profile_title),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // User Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(GlassSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(Color(0xFF1E293B))
                                    .border(1.dp, GlassBorder, RoundedCornerShape(32.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    userName.take(2).uppercase(),
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Column(modifier = Modifier.padding(start = 16.dp)) {
                                Text(
                                    androidx.compose.ui.res.stringResource(R.string.profile_name, userName),
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PrimaryBlue.copy(alpha = 0.1f))
                                        .border(1.dp, PrimaryBlue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        androidx.compose.ui.res.stringResource(R.string.profile_role, role),
                                        color = PrimaryBlue,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Text("SETTINGS", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(GlassSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    val currentLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags()
                    val isId = currentLocales.contains("id") || currentLocales.contains("in")

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = true }
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceColor)
                                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🌐", fontSize = 18.sp)
                                }
                                Column(modifier = Modifier.padding(start = 16.dp)) {
                                    Text("Language", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    Text(if (isId) "Bahasa Indonesia" else "English", color = TextDim, fontSize = 13.sp)
                                }
                            }
                            Text("▼", color = TextDim, fontSize = 12.sp)
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(SurfaceColor)
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

                    HorizontalDivider(modifier = Modifier.padding(start = 76.dp), color = GlassBorder)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onManageConnections)
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceColor)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🔗", fontSize = 18.sp)
                            }
                            Column(modifier = Modifier.padding(start = 16.dp)) {
                                Text("Manage Connections", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Text("Pair with Caregivers", color = TextDim, fontSize = 13.sp)
                            }
                        }
                        Text("▶", color = TextDim, fontSize = 12.sp)
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text("ACCOUNT", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(GlassSurface)
                        .clickable(onClick = onLogout)
                        .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DangerColor.copy(alpha = 0.1f))
                                .border(1.dp, DangerColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🚪", fontSize = 18.sp)
                        }
                        Text(
                            androidx.compose.ui.res.stringResource(R.string.profile_logout),
                            color = DangerColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
