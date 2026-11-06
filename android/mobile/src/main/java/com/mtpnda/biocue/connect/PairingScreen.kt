package com.mtpnda.biocue.connect

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtpnda.biocue.R
import com.mtpnda.biocue.data.Pairing
import com.mtpnda.biocue.ui.components.CalmTextField
import com.mtpnda.biocue.ui.components.CircleIconButton
import com.mtpnda.biocue.ui.components.CoralOutlinedPillButton
import com.mtpnda.biocue.ui.components.EmojiAvatar
import com.mtpnda.biocue.ui.components.OutlinedPillButton
import com.mtpnda.biocue.ui.components.PairingCode
import com.mtpnda.biocue.ui.components.PillButton
import com.mtpnda.biocue.ui.components.SectionHeader
import com.mtpnda.biocue.ui.components.SoftCard
import com.mtpnda.biocue.ui.theme.AlertCoralChip
import com.mtpnda.biocue.ui.theme.AlertCoralInk
import com.mtpnda.biocue.ui.theme.AlertCoralSoft
import com.mtpnda.biocue.ui.theme.BgColor
import com.mtpnda.biocue.ui.theme.CalmPalette
import com.mtpnda.biocue.ui.theme.InfoBlueChip
import com.mtpnda.biocue.ui.theme.InfoBlueSoft
import com.mtpnda.biocue.ui.theme.InkPrimary
import com.mtpnda.biocue.ui.theme.InkSecondary
import com.mtpnda.biocue.ui.theme.LineBorder
import com.mtpnda.biocue.ui.theme.RisingAmberChip
import com.mtpnda.biocue.ui.theme.RisingAmberSoft
import com.mtpnda.biocue.ui.theme.SurfaceCard

@Composable
fun PairingScreen(
    pairingCode: String,
    timeRemainingSeconds: Int,
    connectedCaregivers: List<Pairing>,
    pendingRequests: List<Pairing>,
    onGenerateCode: () -> Unit,
    onApprove: (String) -> Unit,
    onDeny: (String) -> Unit,
    onRemove: (String) -> Unit,
    onBack: () -> Unit = {},
    onContinue: () -> Unit = {}
) {
    val context = LocalContext.current
    var caregiverToRemove by remember { mutableStateOf<Pairing?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar
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
                    text = "Caregivers & Circle",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Pairing & access control",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Generate / Share Pairing Code Card
            item {
                SectionHeader("ADD A CAREGIVER")
                SoftCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Single-Use Pairing Code",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Share this 6-digit code with your caregiver. They enter it in their BioCue app to request observer access.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        PairingCode(code = pairingCode, fontSize = 42)
                        Spacer(modifier = Modifier.height(10.dp))

                        val minutes = timeRemainingSeconds / 60
                        val seconds = timeRemainingSeconds % 60
                        Text(
                            text = if (pairingCode.isNotEmpty()) {
                                "Valid for ${minutes}m ${String.format("%02d", seconds)}s · Single use"
                            } else {
                                "No active code. Tap below to generate one."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (timeRemainingSeconds > 60) InkSecondary else AlertCoralInk
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Actions: Copy / Share / Generate
                        if (pairingCode.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedPillButton(
                                    text = "📋 Copy Code",
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("BioCue Code", pairingCode))
                                        Toast.makeText(context, "Copied code $pairingCode to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedPillButton(
                                    text = "↗️ Share",
                                    onClick = {
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "BioCue Pairing Code")
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Connect with me on BioCue! My 6-digit pairing code is: $pairingCode (expires in ${minutes + 1} minutes)."
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Pairing Code"))
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        PillButton(
                            text = if (pairingCode.isNotEmpty()) "Generate Fresh Code" else "Generate Pairing Code",
                            onClick = onGenerateCode,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 2. Pending Requests (Needs Wearer Approval)
            item {
                SectionHeader("WAITING FOR YOUR APPROVAL (${pendingRequests.size})")
                if (pendingRequests.isEmpty()) {
                    Text(
                        text = "No pending caregiver requests.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                }
            }

            items(pendingRequests) { request ->
                SoftCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = InfoBlueChip,
                    containerColor = InfoBlueSoft
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EmojiAvatar(
                            "🤝",
                            size = 40.dp,
                            containerColor = InfoBlueSoft,
                            borderColor = InfoBlueChip
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Allow ${request.caregiverName} to connect?",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "They will only see stress events — never your raw vitals or live location history.",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { onDeny(request.id) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = "Deny",
                                style = MaterialTheme.typography.labelLarge,
                                color = AlertCoralInk
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        PillButton(
                            text = "Approve",
                            onClick = { onApprove(request.id) }
                        )
                    }
                }
            }

            // 3. Connected Caregivers List
            item {
                SectionHeader("CONNECTED CAREGIVERS (${connectedCaregivers.size} of 2)")
                if (connectedCaregivers.isEmpty()) {
                    Text(
                        text = "No caregivers connected yet. Generate a code above to invite someone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                } else if (connectedCaregivers.size >= 2) {
                    Text(
                        text = "Maximum circle size reached (2 of 2 caregivers).",
                        style = MaterialTheme.typography.bodySmall,
                        color = CalmPalette.ink,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                }
            }

            items(connectedCaregivers) { caregiver ->
                SoftCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EmojiAvatar("👤", size = 40.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = caregiver.caregiverName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Connected · Reads stress events only",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSecondary
                                )
                            }
                        }
                        TextButton(
                            onClick = { caregiverToRemove = caregiver },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = "Remove",
                                style = MaterialTheme.typography.labelLarge,
                                color = AlertCoralInk
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                PillButton(
                    text = "Done",
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (caregiverToRemove != null) {
        val target = caregiverToRemove!!
        AlertDialog(
            onDismissRequest = { caregiverToRemove = null },
            title = { Text("Remove Caregiver?") },
            text = { Text("Are you sure you want to remove ${target.caregiverName}? Their access will be revoked immediately and all wearer data will be wiped from their device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemove(target.id)
                        caregiverToRemove = null
                    }
                ) {
                    Text("Revoke Access", color = AlertCoralInk, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { caregiverToRemove = null }) {
                    Text("Cancel", color = InkSecondary)
                }
            }
        )
    }
}

@Composable
fun CaregiverPairingScreen(
    status: String?,
    wearerName: String?,
    onSubmitCode: (String) -> Unit,
    onUnlink: () -> Unit,
    onBack: () -> Unit = {},
    onContinue: () -> Unit = {}
) {
    var code by remember { mutableStateOf("") }
    var showUnlinkDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar
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
                    text = "Caregiver Connection",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Observer pairing",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            EmojiAvatar(emoji = "🔗", size = 72.dp)
            Spacer(modifier = Modifier.height(20.dp))

            if (status == "CONNECTED") {
                Text(
                    text = "Connected to Wearer",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))

                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = wearerName ?: "Connected Wearer",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "You are approved to observe stress events and provide pattern feedback.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                PillButton(
                    text = "Go to Dashboard",
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                CoralOutlinedPillButton(
                    text = "Unlink from Wearer",
                    onClick = { showUnlinkDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "Connect to Wearer",
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enter the 6-digit pairing code shown on the wearer's phone to link accounts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                CalmTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it.uppercase().filter { ch -> ch.isLetterOrDigit() } },
                    label = "6-Digit Pairing Code"
                )

                Spacer(modifier = Modifier.height(20.dp))

                PillButton(
                    text = if (status == "SEARCHING") "Connecting…" else "Send Request",
                    onClick = { onSubmitCode(code) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = code.length == 6 && status != "SEARCHING"
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (status != null) {
                    val statusText = when (status) {
                        "SEARCHING" -> "Looking up pairing code in Firestore…"
                        "PENDING" -> "✓ Request sent! Waiting for wearer to tap Approve on their phone."
                        "NOT_FOUND" -> "Code not found or expired. Ask the wearer to generate a fresh code."
                        "MAX_REACHED" -> "Wearer already has the maximum of 2 caregivers connected."
                        "ERROR" -> "A network error occurred. Please verify internet and try again."
                        else -> ""
                    }
                    val (bg, border) = when (status) {
                        "SEARCHING", "PENDING" -> RisingAmberSoft to RisingAmberChip
                        "NOT_FOUND", "MAX_REACHED", "ERROR" -> AlertCoralSoft to AlertCoralChip
                        else -> SurfaceCard to LineBorder
                    }

                    if (statusText.isNotEmpty()) {
                        SoftCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { liveRegion = LiveRegionMode.Polite },
                            containerColor = bg,
                            borderColor = border
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    if (showUnlinkDialog) {
        AlertDialog(
            onDismissRequest = { showUnlinkDialog = false },
            title = { Text("Unlink from Wearer?") },
            text = { Text("Are you sure you want to stop observing ${wearerName ?: "this wearer"}? Your observer access will be removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnlinkDialog = false
                        onUnlink()
                    }
                ) {
                    Text("Unlink", color = AlertCoralInk, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlinkDialog = false }) {
                    Text("Cancel", color = InkSecondary)
                }
            }
        )
    }
}
