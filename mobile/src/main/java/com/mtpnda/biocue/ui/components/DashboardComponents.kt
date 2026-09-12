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
            containerColor = color.copy(alpha = 0.12f),
            contentColor = color
        ),
        shape = RoundedCornerShape(10.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 18.sp, modifier = Modifier.padding(end = 10.dp))
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LabelColor)
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = LabelColor,
        modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
    )
}

@Composable
fun DashboardTopBar(isCaregiver: Boolean, onOpenProfile: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .statusBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(if (isCaregiver) "BioCue · Caregiver" else "BioCue", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = LabelColor)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(GlassCard)
                    .clickable { onOpenProfile() },
                contentAlignment = Alignment.Center
            ) {
                Text("⚙️", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun ConnectionBanner(connStatus: ConnectionStatus, onOpenProfile: () -> Unit, backgroundColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .statusBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val (icon, color, text) = when (connStatus) {
                ConnectionStatus.CONNECTED -> Triple("🟢", StatusGreen, "Watch connected")
                ConnectionStatus.DISCONNECTED -> Triple("🔴", StatusRed, "Watch disconnected")
                ConnectionStatus.SEARCHING -> Triple("🟡", StatusAmber, "Searching for watch...")
            }
            Text(icon, fontSize = 10.sp, modifier = Modifier.padding(end = 6.dp))
            Text(text, color = color, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(GlassCard)
                .clickable { onOpenProfile() },
            contentAlignment = Alignment.Center
        ) {
            Text("⚙️", fontSize = 18.sp)
        }
    }
}

@Composable
fun StatusMeterCard(userName: String, tier: Int, activity: String) {
    val statusText = when (tier) {
        0 -> "$userName is calm ✨"
        1 -> "$userName's stress is rising 📈"
        else -> "$userName may need support 🚨"
    }
    val statusColor = when (tier) {
        0 -> Color(0xFF1C7A3D) // Dark Green
        1 -> Color(0xFF9A6A00) // Dark Amber
        else -> Color(0xFFC81E14) // Dark Red
    }
    val fillColor = when (tier) {
        0 -> StatusGreen
        1 -> StatusAmber
        else -> StatusRed
    }
    val fillFraction = when (tier) {
        0 -> 0.18f
        1 -> 0.55f
        else -> 0.92f
    }
    val chipIcon = if (tier > 0) "🔄" else "🪑"
    val chipText = if (tier > 0) "Pacing" else activity

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Meter Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .height(14.dp)
                    .background(Color.Black.copy(alpha = 0.06f), RoundedCornerShape(7.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fillFraction)
                        .height(14.dp)
                        .background(fillColor, RoundedCornerShape(7.dp))
                )
            }
            
            // Activity Chip & Update Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(chipIcon, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(chipText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LabelColor)
                }
                Text("Updated just now", fontSize = 13.sp, color = TextDim)
            }
        }
    }
}

@Composable
fun SosAlertCard(userName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = StatusRed.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("SOS / High Stress Alert", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusRed, modifier = Modifier.padding(bottom = 6.dp))
            Text("$userName triggered a manual SOS or their stress has been elevated for 5 minutes.", fontSize = 15.sp, color = LabelColor)
        }
    }
}

@Composable
fun JournalCard(journal: List<JournalEntry>, journalOffset: Int, onChangeDate: (Int) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dateLabel = when (journalOffset) {
                0 -> "Today"
                -1 -> "Yesterday"
                else -> "${-journalOffset} days ago"
            }
            SectionHeader("Journal · $dateLabel")
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.clip(CircleShape).background(GlassCard).clickable { onChangeDate(journalOffset - 1) }.padding(8.dp)) {
                    Text("←", fontSize = 14.sp, color = LabelColor)
                }
                if (journalOffset < 0) {
                    Box(modifier = Modifier.clip(CircleShape).background(GlassCard).clickable { onChangeDate(journalOffset + 1) }.padding(8.dp)) {
                        Text("→", fontSize = 14.sp, color = LabelColor)
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GlassCard),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                if (journal.isEmpty()) {
                    Text("No events on this day. All calm ✨", fontSize = 14.sp, color = TextDim, modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    journal.forEachIndexed { index, entry ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                            // Emoji badge
                            Box(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(36.dp)
                                    .background(Color(android.graphics.Color.parseColor(entry.colorHex)).copy(alpha = 0.13f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(entry.labelIcon, fontSize = 16.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.labelText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = LabelColor)
                                Text(
                                    text = "${entry.time} · ${entry.activity} · ${if(entry.labelText=="Excited") "Manual check-in" else "Auto-detected"}",
                                    fontSize = 13.sp,
                                    color = TextDim,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Text(entry.time, fontSize = 14.sp, color = TextDim, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
                        }
                        if (index < journal.size - 1) {
                            HorizontalDivider(color = LabelColor.copy(alpha = 0.05f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PatternReviewCard(reviews: List<ReviewEvent>, userName: String, onResolve: (String, String) -> Unit) {
    Column {
        SectionHeader("Pattern Review")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GlassCard),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                if (reviews.isEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✨", fontSize = 28.sp, modifier = Modifier.padding(bottom = 6.dp))
                        Text("All caught up", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C7A3D))
                        Text("BioCue is learning ${userName}'s patterns.", fontSize = 14.sp, color = TextDim, modifier = Modifier.padding(top = 4.dp))
                    }
                } else {
                    val rev = reviews.first()
                    val isUnlabeled = rev.currentLabel == "NONE"
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(StatusAmber, CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Review needed · ${rev.time}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LabelColor)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (isUnlabeled) {
                        Text("A stress spike was detected but ${userName} didn't label it on the watch.", fontSize = 15.sp, color = LabelColor, modifier = Modifier.padding(bottom = 12.dp))
                    } else {
                        val labelFormat = rev.currentLabel.lowercase().replaceFirstChar { it.uppercase() }
                        Text("${userName} reported feeling ${labelFormat}. Please add environmental context to help BioCue learn.", fontSize = 15.sp, color = LabelColor, modifier = Modifier.padding(bottom = 12.dp))
                    }
                    
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CanvasB).padding(12.dp)) {
                        Column {
                            Text("CONTEXT CUES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDim, modifier = Modifier.padding(bottom = 8.dp))
                            rev.cues.forEach { cue ->
                                Row(modifier = Modifier.padding(bottom = 4.dp)) {
                                    Text(cue.first, fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
                                    Text(cue.second, fontSize = 14.sp, color = LabelColor)
                                }
                            }
                        }
                    }
                    
                    if (isUnlabeled) {
                        Text("💡 Ask ${userName} first if you can — they may remember what happened.", fontSize = 13.sp, color = StatusAmber, modifier = Modifier.padding(vertical = 14.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val EventTypes = com.mtpnda.biocue.core.domain.model.EventTypes
                            LabelBtn(EventTypes.getIconForLabel(EventTypes.LABEL_OVERWHELMED), EventTypes.LABEL_OVERWHELMED, Color(android.graphics.Color.parseColor(EventTypes.getColorHexForLabel(EventTypes.LABEL_OVERWHELMED)))) { onResolve(rev.eventId, EventTypes.LABEL_OVERWHELMED) }
                            LabelBtn(EventTypes.getIconForLabel(EventTypes.LABEL_EXCITED), EventTypes.LABEL_EXCITED, Color(android.graphics.Color.parseColor(EventTypes.getColorHexForLabel(EventTypes.LABEL_EXCITED)))) { onResolve(rev.eventId, EventTypes.LABEL_EXCITED) }
                            LabelBtn(EventTypes.getIconForLabel(EventTypes.LABEL_ACTIVE), EventTypes.LABEL_ACTIVE, Color(android.graphics.Color.parseColor(EventTypes.getColorHexForLabel(EventTypes.LABEL_ACTIVE)))) { onResolve(rev.eventId, EventTypes.LABEL_ACTIVE) }
                            LabelBtn(EventTypes.getIconForLabel(EventTypes.LABEL_NONE), "Unsure / Wasn't Present", TextDim) { onResolve(rev.eventId, EventTypes.LABEL_NONE) }
                        }
                    } else {
                        Text("💡 What was happening around ${userName}?", fontSize = 13.sp, color = CalmBlue, modifier = Modifier.padding(vertical = 14.dp))
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
}
