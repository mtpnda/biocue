package com.mtpnda.biocue.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtpnda.biocue.R
import com.mtpnda.biocue.data.Pairing
import com.mtpnda.biocue.ui.theme.*

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
    onContinue: () -> Unit
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.pairing_wearer_title),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(GlassSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = stringResource(R.string.pairing_code_label), color = TextDim, fontSize = 14.sp)
                        Text(
                            text = pairingCode,
                            color = PrimaryBlue,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 8.sp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        Text(
                            text = stringResource(R.string.pairing_code_expires, timeRemainingSeconds),
                            color = TextDim,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onGenerateCode,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DangerColor)
                        ) {
                            Text(stringResource(R.string.pairing_generate), fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                Text(
                    text = stringResource(R.string.pairing_connected_caregivers),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
                )
                if (connectedCaregivers.isEmpty()) {
                    Text(stringResource(R.string.pairing_no_caregivers), color = TextDim, modifier = Modifier.padding(start = 4.dp))
                } else if (connectedCaregivers.size >= 2) {
                    Text(stringResource(R.string.pairing_max_reached), color = TextDim, modifier = Modifier.padding(start = 4.dp))
                }
            }

            items(connectedCaregivers) { caregiver ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(caregiver.caregiverName, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                        TextButton(onClick = { onRemove(caregiver.id) }) {
                            Text(stringResource(R.string.pairing_remove), color = DangerColor)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = stringResource(R.string.pairing_pending_requests),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
                )
                if (pendingRequests.isEmpty()) {
                    Text("No pending requests.", color = TextDim, modifier = Modifier.padding(start = 4.dp))
                }
            }

            items(pendingRequests) { request ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(request.caregiverName, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { onDeny(request.id) }) {
                                Text(stringResource(R.string.pairing_deny), color = DangerColor)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onApprove(request.id) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Text(stringResource(R.string.pairing_approve))
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(48.dp))
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    enabled = pendingRequests.isEmpty()
                ) {
                    Text(stringResource(R.string.pairing_continue), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun CaregiverPairingScreen(
    status: String?,
    wearerName: String?,
    onSubmitCode: (String) -> Unit,
    onUnlink: () -> Unit,
    onContinue: () -> Unit
) {
    var code by remember { mutableStateOf("") }

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
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(GlassSurface)
                    .border(1.dp, GlassBorder, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("🔗", fontSize = 32.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
            
            if (status == "CONNECTED") {
                Text(
                    text = "Connected to Wearer",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = wearerName ?: "Unknown Wearer",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(stringResource(R.string.pairing_continue), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onUnlink,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerColor)
                ) {
                    Text("Unlink from Wearer", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            } else {
                Text(
                    text = stringResource(R.string.pairing_caregiver_title),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it.uppercase() },
                    label = { Text(stringResource(R.string.pairing_enter_code), color = TextDim) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = GlassBorder,
                        unfocusedContainerColor = GlassSurface,
                        focusedContainerColor = GlassSurface
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onSubmitCode(code) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = code.length == 6,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(stringResource(R.string.pairing_connect), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (status != null) {
                    val statusText = when (status) {
                        "SEARCHING" -> stringResource(R.string.pairing_searching)
                        "PENDING" -> stringResource(R.string.pairing_waiting_approval, wearerName ?: "")
                        "NOT_FOUND" -> stringResource(R.string.pairing_code_not_found)
                        "MAX_REACHED" -> stringResource(R.string.pairing_max_reached)
                        "ERROR" -> "A network error occurred. Please try again."
                        else -> ""
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(GlassSurface)
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = statusText,
                            color = TextDim,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
