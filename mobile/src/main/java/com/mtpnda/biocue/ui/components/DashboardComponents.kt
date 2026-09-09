package com.mtpnda.biocue.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtpnda.biocue.R
import com.mtpnda.biocue.ui.theme.*
import com.mtpnda.biocue.JournalEntry
import com.mtpnda.biocue.ReviewEvent
import com.mtpnda.biocue.ConnectionStatus

@Composable
fun LabelBtn(icon: String, text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.15f),
            contentColor = color
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 18.sp, modifier = Modifier.padding(end = 10.dp))
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextDim,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
fun DashboardTopBar(isCaregiver: Boolean, onOpenProfile: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .statusBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("BioCue", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                if (isCaregiver) androidx.compose.ui.res.stringResource(R.string.mode_caregiver)
                else androidx.compose.ui.res.stringResource(R.string.mode_wearer),
                color = CalmColor, fontSize = 12.sp, fontWeight = FontWeight.Medium
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = onOpenProfile,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("👤", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun CurrentStateCard(stressLevel: Int, activity: String, connectionStatus: ConnectionStatus, userName: String) {
    if (connectionStatus != ConnectionStatus.CONNECTED) {
        val warningText = if (connectionStatus == ConnectionStatus.DISCONNECTED) "Watch is disconnected" else "Watch is off body"
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⚠️", fontSize = 28.sp, modifier = Modifier.padding(bottom = 8.dp))
            Text(warningText, color = WarnColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Unable to monitor ${userName}'s stress levels. Please ensure the watch is worn and connected.", 
                color = TextDim, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        }
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            SectionLabel("Current State")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("Stress Level", color = TextDim, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(stressLevel.toString(), fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("/100", fontSize = 14.sp, color = TextDim, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                    }
                }
                val (statusText, statusColor) = when {
                    stressLevel > 60 -> "High" to DangerColor
                    stressLevel > 35 -> "Moderate" to WarnColor
                    else -> "Low" to CalmColor
                }
                Text(statusText, color = statusColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            }
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color(0xFF334155))) {
                val fraction = (stressLevel / 100f).coerceIn(0f, 1f)
                val barColor = when {
                    stressLevel > 60 -> DangerColor
                    stressLevel > 35 -> WarnColor
                    else -> CalmColor
                }
                Box(modifier = Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(CircleShape).background(barColor))
            }
            Row(modifier = Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("ACTIVITY:", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Text(activity, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
fun JournalCard(journal: List<JournalEntry>, journalOffset: Int, onChangeOffset: (Int) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder, RoundedCornerShape(32.dp)),
        colors = CardDefaults.cardColors(containerColor = GlassSurface),
        shape = RoundedCornerShape(32.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Journal")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.IconButton(onClick = { onChangeOffset(journalOffset + 1) }, modifier = Modifier.size(24.dp)) {
                        Text("◀", color = TextDim, fontSize = 12.sp)
                    }
                    Text(
                        when (journalOffset) {
                            0 -> "Today"
                            1 -> "Yesterday"
                            else -> "${journalOffset} days ago"
                        },
                        fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    androidx.compose.material3.IconButton(onClick = { onChangeOffset(journalOffset - 1) }, enabled = journalOffset > 0, modifier = Modifier.size(24.dp)) {
                        Text("▶", color = if (journalOffset > 0) TextDim else Color.Transparent, fontSize = 12.sp)
                    }
                }
            }

            if (journal.isEmpty()) {
                Text("No events on this day. All calm ✨", fontSize = 13.sp, color = TextDim, modifier = Modifier.padding(vertical = 12.dp))
            } else {
                journal.forEach { entry ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Box(modifier = Modifier.padding(top = 5.dp, end = 12.dp).size(8.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(entry.colorHex))))
                        Column {
                            Text(entry.time, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("${entry.place} · ${entry.activity}", fontSize = 12.sp, color = TextDim, modifier = Modifier.padding(top = 2.dp))
                            Row(modifier = Modifier.padding(top = 4.dp)) {
                                Text(entry.labelIcon, fontSize = 13.sp, modifier = Modifier.padding(end = 4.dp))
                                Text(entry.labelText, fontSize = 13.sp, color = Color(android.graphics.Color.parseColor(entry.colorHex)))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PushNotificationPreview(userName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DangerColor.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("PUSH NOTIFICATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DangerColor, modifier = Modifier.padding(bottom = 6.dp))
            Text("${userName}'s stress has been elevated for 5 minutes. They may need support.", fontSize = 13.sp, color = Color.White)
        }
    }
}

@Composable
fun PatternReviewCard(reviews: List<ReviewEvent>, userName: String, onResolve: (String, String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder, RoundedCornerShape(32.dp)),
        colors = CardDefaults.cardColors(containerColor = GlassSurface),
        shape = RoundedCornerShape(32.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                SectionLabel("Pattern Review")
                if (reviews.isNotEmpty()) {
                    Box(modifier = Modifier.clip(CircleShape).background(DangerColor).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text(reviews.size.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (reviews.isEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✨", fontSize = 24.sp, modifier = Modifier.padding(bottom = 6.dp))
                    Text("All caught up", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CalmColor)
                    Text("BioCue is learning ${userName}'s patterns.", fontSize = 12.sp, color = TextDim, modifier = Modifier.padding(top = 4.dp))
                }
            } else {
                val rev = reviews.first()
                val isUnlabeled = rev.currentLabel == "NONE"
                
                if (isUnlabeled) {
                    Text("A stress spike was detected but ${userName} didn't label it on the watch.", fontSize = 13.sp, color = Color.White, modifier = Modifier.padding(bottom = 12.dp))
                } else {
                    val labelFormat = rev.currentLabel.lowercase().replaceFirstChar { it.uppercase() }
                    Text("${userName} reported feeling ${labelFormat}. Please add environmental context to help BioCue learn.", fontSize = 13.sp, color = Color.White, modifier = Modifier.padding(bottom = 12.dp))
                }
                
                Text(rev.time, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 12.dp))
                
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.2f)).padding(12.dp)) {
                    Column {
                        Text("CONTEXT CUES", fontSize = 10.sp, color = TextDim, modifier = Modifier.padding(bottom = 8.dp))
                        rev.cues.forEach { cue ->
                            Row(modifier = Modifier.padding(bottom = 4.dp)) {
                                Text(cue.first, fontSize = 13.sp, modifier = Modifier.padding(end = 8.dp))
                                Text(cue.second, fontSize = 13.sp, color = Color(0xFFCBD5E1))
                            }
                        }
                    }
                }
                
                if (isUnlabeled) {
                    Text("💡 Ask ${userName} first if you can — they may remember what happened.", fontSize = 12.sp, color = WarnColor, modifier = Modifier.padding(vertical = 14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val EventTypes = com.mtpnda.biocue.core.domain.model.EventTypes
                        LabelBtn(EventTypes.getIconForLabel(EventTypes.LABEL_OVERWHELMED), EventTypes.LABEL_OVERWHELMED, Color(android.graphics.Color.parseColor(EventTypes.getColorHexForLabel(EventTypes.LABEL_OVERWHELMED)))) { onResolve(rev.eventId, EventTypes.LABEL_OVERWHELMED) }
                        LabelBtn(EventTypes.getIconForLabel(EventTypes.LABEL_EXCITED), EventTypes.LABEL_EXCITED, Color(android.graphics.Color.parseColor(EventTypes.getColorHexForLabel(EventTypes.LABEL_EXCITED)))) { onResolve(rev.eventId, EventTypes.LABEL_EXCITED) }
                        LabelBtn(EventTypes.getIconForLabel(EventTypes.LABEL_ACTIVE), EventTypes.LABEL_ACTIVE, Color(android.graphics.Color.parseColor(EventTypes.getColorHexForLabel(EventTypes.LABEL_ACTIVE)))) { onResolve(rev.eventId, EventTypes.LABEL_ACTIVE) }
                        LabelBtn(EventTypes.getIconForLabel(EventTypes.LABEL_NONE), "Unsure / Wasn't Present", TextDim) { onResolve(rev.eventId, EventTypes.LABEL_NONE) }
                    }
                } else {
                    Text("💡 What was happening around ${userName}?", fontSize = 12.sp, color = CalmColor, modifier = Modifier.padding(vertical = 14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val EventTypes = com.mtpnda.biocue.core.domain.model.EventTypes
                        LabelBtn(EventTypes.getIconForLabel(EventTypes.CONTEXT_LOUD_NOISES), EventTypes.CONTEXT_LOUD_NOISES, Color(android.graphics.Color.parseColor(EventTypes.getColorHexForLabel(EventTypes.CONTEXT_LOUD_NOISES)))) { onResolve(rev.eventId, EventTypes.CONTEXT_LOUD_NOISES) }
                        LabelBtn(EventTypes.getIconForLabel(EventTypes.CONTEXT_CROWDED), EventTypes.CONTEXT_CROWDED, Color(android.graphics.Color.parseColor(EventTypes.getColorHexForLabel(EventTypes.CONTEXT_CROWDED)))) { onResolve(rev.eventId, EventTypes.CONTEXT_CROWDED) }
                        LabelBtn(EventTypes.getIconForLabel(EventTypes.CONTEXT_DEMAND), EventTypes.CONTEXT_DEMAND, Color(android.graphics.Color.parseColor(EventTypes.getColorHexForLabel(EventTypes.CONTEXT_DEMAND)))) { onResolve(rev.eventId, EventTypes.CONTEXT_DEMAND) }
                        LabelBtn(EventTypes.getIconForLabel(EventTypes.CONTEXT_ROUTINE), EventTypes.CONTEXT_ROUTINE, Color(android.graphics.Color.parseColor(EventTypes.getColorHexForLabel(EventTypes.CONTEXT_ROUTINE)))) { onResolve(rev.eventId, EventTypes.CONTEXT_ROUTINE) }
                        LabelBtn(EventTypes.getIconForLabel(EventTypes.CONTEXT_UNKNOWN), EventTypes.CONTEXT_UNKNOWN, TextDim) { onResolve(rev.eventId, EventTypes.CONTEXT_UNKNOWN) }
                    }
                }
            }
        }
    }
}
