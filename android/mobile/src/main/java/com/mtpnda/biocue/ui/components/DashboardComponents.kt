package com.mtpnda.biocue.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import com.mtpnda.biocue.ConnectionStatus
import com.mtpnda.biocue.JournalEntry
import com.mtpnda.biocue.ReviewEvent
import com.mtpnda.biocue.ui.theme.AlertCoralChip
import com.mtpnda.biocue.ui.theme.AlertCoralInk
import com.mtpnda.biocue.ui.theme.AlertCoralSoft
import com.mtpnda.biocue.ui.theme.AppRadius
import com.mtpnda.biocue.ui.theme.CalmGreenInk
import com.mtpnda.biocue.ui.theme.PrimaryCharcoal
import com.mtpnda.biocue.ui.theme.SurfaceCard
import com.mtpnda.biocue.ui.theme.InfoBlueInk
import com.mtpnda.biocue.ui.theme.InkMuted
import com.mtpnda.biocue.ui.theme.InkPrimary
import com.mtpnda.biocue.ui.theme.InkSecondary
import com.mtpnda.biocue.ui.theme.LineBorder
import com.mtpnda.biocue.ui.theme.NeutralGrayChip
import com.mtpnda.biocue.ui.theme.NeutralPalette
import com.mtpnda.biocue.ui.theme.CalmPalette
import com.mtpnda.biocue.ui.theme.RisingAmberChip
import com.mtpnda.biocue.ui.theme.RisingAmberInk
import com.mtpnda.biocue.ui.theme.RisingPalette
import com.mtpnda.biocue.ui.theme.Surface2
import com.mtpnda.biocue.ui.theme.paletteForTier

private val PillShape = RoundedCornerShape(AppRadius.Pill)

/** Decode a "#RRGGBB" string without ever crashing the UI on bad data. */
private fun parseHexColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(NeutralGrayChip)

/**
 * Resolution button used in Pattern Review. [color] is the event-type colour;
 * it is washed out into a soft pastel pill with readable dark ink.
 */
@Composable
fun LabelBtn(icon: String, text: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(PillShape)
            .background(color.copy(alpha = 0.22f))
            .border(1.dp, color.copy(alpha = 0.55f), PillShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 18.sp, modifier = Modifier.padding(end = 12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = InkPrimary
        )
    }
}

/** Uppercase, tracked section label (e.g. "JOURNAL · TODAY"). */
@Composable
fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = InkSecondary,
        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp, top = 8.dp)
    )
}

@Composable
private fun SettingsButton(onOpenProfile: () -> Unit) {
    CircleIconButton(
        onClick = onOpenProfile,
        contentDescription = "Settings and profile"
    ) {
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = null,
            tint = InkPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun DashboardTopBar(isCaregiver: Boolean, onOpenProfile: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("BioCue", style = MaterialTheme.typography.titleLarge)
            if (isCaregiver) {
                Spacer(Modifier.width(12.dp))
                StatusPill(
                    text = "Caregiver",
                    palette = NeutralPalette,
                    showDot = false
                )
            }
        }
        SettingsButton(onOpenProfile)
    }
}

@Composable
fun ConnectionBanner(connStatus: ConnectionStatus, onOpenProfile: () -> Unit, backgroundColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (connStatus) {
            ConnectionStatus.CONNECTED -> StatusPill("Watch connected", CalmPalette)
            ConnectionStatus.SEARCHING -> StatusPill(
                text = "Searching for watch…",
                palette = RisingPalette,
                textColor = InkPrimary
            )
            ConnectionStatus.DISCONNECTED -> StatusPill("Watch disconnected", NeutralPalette)
        }
        SettingsButton(onOpenProfile)
    }
}

@Composable
fun StatusMeterCard(userName: String, tier: Int, activity: String) {
    val palette = paletteForTier(tier)
    val statusText = when (tier) {
        0 -> "$userName is calm ✨"
        1 -> "$userName's stress is rising"
        else -> "$userName may need support"
    }
    val targetFraction = when (tier) {
        0 -> 0.18f
        1 -> 0.55f
        else -> 0.92f
    }
    // A slow ease-out so a change of state reads as a gentle shift, not a jolt.
    val fillFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 400, easing = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)),
        label = "stressMeterFill"
    )
    val chipIcon = if (tier > 0) "🔄" else "🪑"
    val chipText = if (tier > 0) "Pacing" else activity

    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.titleLarge,
            color = palette.ink
        )

        // Meter track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(12.dp)
                .clip(PillShape)
                .background(Surface2)
                .border(1.dp, LineBorder, PillShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fillFraction)
                    .fillMaxHeight()
                    .clip(PillShape)
                    .background(palette.chip)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .background(Surface2)
                    .border(1.dp, LineBorder, PillShape)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(chipIcon, fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Text(chipText, style = MaterialTheme.typography.labelMedium, color = InkPrimary)
            }
            Text(
                "Updated just now",
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary
            )
        }
    }
}

@Composable
fun SosAlertCard(userName: String) {
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = AlertCoralSoft,
        borderColor = AlertCoralChip
    ) {
        Text(
            text = "High stress alert",
            style = MaterialTheme.typography.titleLarge,
            color = AlertCoralInk,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = "$userName triggered a manual SOS or their stress has been elevated for 5 minutes. " +
                "Please check in with them when you can.",
            style = MaterialTheme.typography.bodyLarge,
            color = InkPrimary
        )
    }
}

/**
 * Parse and format raw event labels cleanly for display.
 * E.g. "OVERWHELMED - Crowded Space" -> Pair("Overwhelmed", "Crowded Space")
 * "EXCITED" -> Pair("Excited", null)
 */
private fun formatJournalLabel(raw: String): Pair<String, String?> {
    val trimmed = raw.trim()
    if (trimmed.isEmpty() || trimmed == "NONE") return Pair("Unlabeled Moment", null)
    if (!trimmed.contains(" - ")) {
        val title = when (trimmed.uppercase(Locale.US)) {
            "OVERWHELMED" -> "Overwhelmed"
            "EXCITED" -> "Excited"
            "ACTIVE" -> "Active"
            else -> trimmed.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        }
        return Pair(title, null)
    }
    val parts = trimmed.split(" - ", limit = 2)
    val emotionRaw = parts[0].trim()
    val contextRaw = parts.getOrNull(1)?.trim()
    val emotion = when (emotionRaw.uppercase(Locale.US)) {
        "OVERWHELMED" -> "Overwhelmed"
        "EXCITED" -> "Excited"
        "ACTIVE" -> "Active"
        else -> emotionRaw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
    }
    return Pair(emotion, contextRaw)
}

@Composable
fun JournalCard(journal: List<JournalEntry>, journalOffset: Int, onChangeDate: (Int) -> Unit) {
    // Defensively decompose any legacy or combined entries separated by " + "
    val effectiveEntries = remember(journal) {
        journal.flatMap { entry ->
            val parts = entry.labelText.split(" + ").map { it.trim() }.filter { it.isNotEmpty() && it != "NONE" }
            if (parts.size <= 1) {
                listOf(entry)
            } else {
                parts.map { part ->
                    entry.copy(
                        labelText = part,
                        labelIcon = com.mtpnda.biocue.core.domain.model.EventTypes.getIconForLabel(part),
                        colorHex = com.mtpnda.biocue.core.domain.model.EventTypes.getColorHexForLabel(part)
                    )
                }
            }
        }
    }

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

            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(
                    onClick = { onChangeDate(journalOffset - 1) },
                    contentDescription = "Previous day"
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = PrimaryCharcoal,
                        modifier = Modifier.size(22.dp)
                    )
                }
                CircleIconButton(
                    onClick = { onChangeDate(journalOffset + 1) },
                    enabled = journalOffset < 0,
                    contentDescription = "Next day"
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = if (journalOffset < 0) PrimaryCharcoal else InkSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        SoftCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (effectiveEntries.isEmpty()) {
                Text(
                    text = "No events on this day. All calm ✨",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                    modifier = Modifier.padding(vertical = 14.dp)
                )
            } else {
                effectiveEntries.forEachIndexed { index, entry ->
                    val tint = parseHexColor(entry.colorHex)
                    val (title, contextCue) = formatJournalLabel(entry.labelText)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        EmojiAvatar(
                            emoji = entry.labelIcon,
                            containerColor = tint.copy(alpha = 0.22f),
                            borderColor = tint.copy(alpha = 0.50f),
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = InkPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = entry.time,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkMuted
                                )
                            }

                            if (!contextCue.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Surface2)
                                        .border(1.dp, LineBorder, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = contextCue,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkPrimary
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = entry.activity,
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary
                            )
                        }
                    }
                    if (index < effectiveEntries.size - 1) {
                        HorizontalDivider(color = LineBorder)
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
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            if (reviews.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("✨", fontSize = 28.sp, modifier = Modifier.padding(bottom = 6.dp))
                    Text(
                        "All caught up",
                        style = MaterialTheme.typography.titleLarge,
                        color = CalmGreenInk
                    )
                    Text(
                        "BioCue is learning ${userName}'s patterns.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                val rev = reviews.first()
                val isUnlabeled = rev.currentLabel == "NONE"
                val EventTypes = com.mtpnda.biocue.core.domain.model.EventTypes

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(RisingAmberChip, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Review needed · ${rev.time}",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkPrimary
                    )
                }

                Spacer(Modifier.height(12.dp))

                if (isUnlabeled) {
                    Text(
                        "A stress spike was detected but $userName didn't label it on the watch.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkPrimary
                    )
                } else {
                    val labelFormat = rev.currentLabel.lowercase().replaceFirstChar { it.uppercase() }
                    Text(
                        "$userName reported feeling $labelFormat. Please add environmental context to help BioCue learn.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkPrimary
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Context cues as soft chips
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppRadius.Md))
                        .background(Surface2)
                        .border(1.dp, LineBorder, RoundedCornerShape(AppRadius.Md))
                        .padding(14.dp)
                ) {
                    Text(
                        "CONTEXT CUES",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSecondary,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rev.cues.forEach { cue ->
                            Row(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(SurfaceCard)
                                    .border(1.dp, LineBorder, PillShape)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cue.first, fontSize = 14.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    cue.second,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkPrimary
                                )
                            }
                        }
                    }
                }

                if (isUnlabeled) {
                    Text(
                        "💡 Ask $userName first if you can — they may remember what happened.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RisingAmberInk,
                        modifier = Modifier.padding(vertical = 14.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabelBtn(EventTypes.getIconForLabel(EventTypes.LABEL_OVERWHELMED), EventTypes.LABEL_OVERWHELMED, parseHexColor(EventTypes.getColorHexForLabel(EventTypes.LABEL_OVERWHELMED))) { onResolve(rev.eventId, EventTypes.LABEL_OVERWHELMED) }
                        LabelBtn(EventTypes.getIconForLabel(EventTypes.LABEL_EXCITED), EventTypes.LABEL_EXCITED, parseHexColor(EventTypes.getColorHexForLabel(EventTypes.LABEL_EXCITED))) { onResolve(rev.eventId, EventTypes.LABEL_EXCITED) }
                        LabelBtn(EventTypes.getIconForLabel(EventTypes.LABEL_ACTIVE), EventTypes.LABEL_ACTIVE, parseHexColor(EventTypes.getColorHexForLabel(EventTypes.LABEL_ACTIVE))) { onResolve(rev.eventId, EventTypes.LABEL_ACTIVE) }
                        LabelBtn(EventTypes.getIconForLabel(EventTypes.LABEL_NONE), "Unsure / Wasn't Present", NeutralGrayChip) { onResolve(rev.eventId, EventTypes.LABEL_NONE) }
                    }
                } else {
                    Text(
                        "💡 What was happening around $userName?",
                        style = MaterialTheme.typography.bodySmall,
                        color = InfoBlueInk,
                        modifier = Modifier.padding(vertical = 14.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabelBtn(EventTypes.getIconForLabel(EventTypes.CONTEXT_LOUD_NOISES), EventTypes.CONTEXT_LOUD_NOISES, parseHexColor(EventTypes.getColorHexForLabel(EventTypes.CONTEXT_LOUD_NOISES))) { onResolve(rev.eventId, EventTypes.CONTEXT_LOUD_NOISES) }
                        LabelBtn(EventTypes.getIconForLabel(EventTypes.CONTEXT_CROWDED), EventTypes.CONTEXT_CROWDED, parseHexColor(EventTypes.getColorHexForLabel(EventTypes.CONTEXT_CROWDED))) { onResolve(rev.eventId, EventTypes.CONTEXT_CROWDED) }
                        LabelBtn(EventTypes.getIconForLabel(EventTypes.CONTEXT_DEMAND), EventTypes.CONTEXT_DEMAND, parseHexColor(EventTypes.getColorHexForLabel(EventTypes.CONTEXT_DEMAND))) { onResolve(rev.eventId, EventTypes.CONTEXT_DEMAND) }
                        LabelBtn(EventTypes.getIconForLabel(EventTypes.CONTEXT_ROUTINE), EventTypes.CONTEXT_ROUTINE, parseHexColor(EventTypes.getColorHexForLabel(EventTypes.CONTEXT_ROUTINE))) { onResolve(rev.eventId, EventTypes.CONTEXT_ROUTINE) }
                        LabelBtn(EventTypes.getIconForLabel(EventTypes.CONTEXT_UNKNOWN), EventTypes.CONTEXT_UNKNOWN, NeutralGrayChip) { onResolve(rev.eventId, EventTypes.CONTEXT_UNKNOWN) }
                    }
                }

                // Free-Text Labeling & Context Input (Spec 19)
                Spacer(Modifier.height(16.dp))
                Text(
                    "OR WRITE IN YOUR OWN WORDS",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                var customText by remember(rev.eventId) { mutableStateOf("") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                if (isUnlabeled) "What were you feeling or experiencing?" else "Add notes, triggers, or what helped...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(AppRadius.Sm),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.width(8.dp))
                    PillButton(
                        text = "Save",
                        onClick = {
                            val trimmed = customText.trim()
                            if (trimmed.isNotEmpty()) {
                                onResolve(rev.eventId, trimmed)
                                customText = ""
                            }
                        },
                        enabled = customText.trim().isNotEmpty()
                    )
                }
            }
        }
    }
}
