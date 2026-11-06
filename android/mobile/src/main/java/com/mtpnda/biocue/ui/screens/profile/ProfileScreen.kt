package com.mtpnda.biocue.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtpnda.biocue.R
import com.mtpnda.biocue.ui.components.CircleIconButton
import com.mtpnda.biocue.ui.components.CoralOutlinedPillButton
import com.mtpnda.biocue.ui.components.EmojiAvatar
import com.mtpnda.biocue.ui.components.SectionHeader
import com.mtpnda.biocue.ui.components.SoftCard
import com.mtpnda.biocue.ui.components.StatusPill
import com.mtpnda.biocue.ui.components.softShadow
import com.mtpnda.biocue.ui.theme.BgColor
import com.mtpnda.biocue.ui.theme.InkPrimary
import com.mtpnda.biocue.ui.theme.InkSecondary
import com.mtpnda.biocue.ui.theme.LineBorder
import com.mtpnda.biocue.ui.theme.NeutralPalette
import com.mtpnda.biocue.ui.theme.NewsreaderFont
import com.mtpnda.biocue.ui.theme.OnPrimaryCharcoal
import com.mtpnda.biocue.ui.theme.PrimaryCharcoal

@Composable
fun ProfileScreen(
    userName: String,
    role: String,
    onBack: () -> Unit,
    onManageConnections: () -> Unit,
    onLogout: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconButton(onClick = onBack, contentDescription = "Back") {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = InkPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.profile_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Your account",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Identity
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .softShadow(CircleShape, 3.dp)
                        .clip(CircleShape)
                        .background(PrimaryCharcoal),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.trim().take(1).uppercase().ifEmpty { "?" },
                        fontFamily = NewsreaderFont,
                        fontSize = 38.sp,
                        color = OnPrimaryCharcoal
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = userName, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                StatusPill(
                    text = stringResource(R.string.profile_role, role),
                    palette = NeutralPalette,
                    showDot = false
                )
            }

            // Settings
            Column {
                SectionHeader("Settings")
                SoftCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    val currentLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags()
                    val isId = currentLocales.contains("id") || currentLocales.contains("in")

                    Box(modifier = Modifier.fillMaxWidth()) {
                        SettingsRow(
                            emoji = "🌐",
                            title = "Language",
                            subtitle = if (isId) "Bahasa Indonesia" else "English",
                            onClick = { expanded = true }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = InkSecondary
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("English", style = MaterialTheme.typography.bodyLarge) },
                                onClick = {
                                    val localeList = androidx.core.os.LocaleListCompat.forLanguageTags("en")
                                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(localeList)
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Bahasa Indonesia", style = MaterialTheme.typography.bodyLarge) },
                                onClick = {
                                    val localeList = androidx.core.os.LocaleListCompat.forLanguageTags("id")
                                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(localeList)
                                    expanded = false
                                }
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 74.dp),
                        color = LineBorder
                    )

                    SettingsRow(
                        emoji = "👥",
                        title = "Caregivers & Circle",
                        subtitle = "Manage connected caregivers & pairing code",
                        onClick = onManageConnections
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = InkSecondary
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 74.dp),
                        color = LineBorder
                    )

                    var showDisclaimerDialog by remember { mutableStateOf(false) }

                    SettingsRow(
                        emoji = "🩺",
                        title = "Health & Medical Disclaimer",
                        subtitle = "BioCue is for wellness & habit awareness",
                        onClick = { showDisclaimerDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = InkSecondary
                        )
                    }

                    if (showDisclaimerDialog) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showDisclaimerDialog = false },
                            title = {
                                Text("Important Medical Disclaimer", style = MaterialTheme.typography.titleMedium)
                            },
                            text = {
                                Text(
                                    "BioCue is NOT a certified medical device and is not intended to diagnose, treat, cure, mitigate, or prevent any disease, illness, or medical condition.\n\n" +
                                    "The app provides general wellness, relaxation, habit cueing, and stress awareness insights only.\n\n" +
                                    "Always seek the advice of a physician or qualified healthcare provider with any medical questions. Never disregard professional medical advice because of information provided by BioCue.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            confirmButton = {
                                androidx.compose.material3.TextButton(onClick = { showDisclaimerDialog = false }) {
                                    Text("Understood", color = PrimaryCharcoal)
                                }
                            }
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 74.dp),
                        color = LineBorder
                    )

                    SettingsRow(
                        emoji = "🌍",
                        title = "Official Website",
                        subtitle = "biocue.matapandastudio.deno.net",
                        onClick = {
                            uriHandler.openUri("https://biocue.matapandastudio.deno.net")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = InkSecondary
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 74.dp),
                        color = LineBorder
                    )

                    SettingsRow(
                        emoji = "🛡️",
                        title = "Privacy Policy & Terms",
                        subtitle = "Data protection & terms of service",
                        onClick = {
                            uriHandler.openUri("https://biocue.matapandastudio.deno.net/privacy")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = InkSecondary
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 74.dp),
                        color = LineBorder
                    )

                    SettingsRow(
                        emoji = "💬",
                        title = "Help & Support",
                        subtitle = "Guides, FAQ & caregiver resources",
                        onClick = {
                            uriHandler.openUri("https://biocue.matapandastudio.deno.net/support")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = InkSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Account — pinned to the bottom so it is always reachable
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            SectionHeader("Account")
            CoralOutlinedPillButton(
                text = stringResource(R.string.profile_logout),
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SettingsRow(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EmojiAvatar(emoji = emoji, size = 44.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary
            )
        }
        trailing()
    }
}
