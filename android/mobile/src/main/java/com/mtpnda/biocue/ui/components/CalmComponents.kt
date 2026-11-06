package com.mtpnda.biocue.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtpnda.biocue.ui.theme.AppMetrics
import com.mtpnda.biocue.ui.theme.AppRadius
import com.mtpnda.biocue.ui.theme.InkMuted
import com.mtpnda.biocue.ui.theme.InkPrimary
import com.mtpnda.biocue.ui.theme.InkSecondary
import com.mtpnda.biocue.ui.theme.LineBorder
import com.mtpnda.biocue.ui.theme.LineStrong
import com.mtpnda.biocue.ui.theme.NewsreaderFont
import com.mtpnda.biocue.ui.theme.OnPrimaryCharcoal
import com.mtpnda.biocue.ui.theme.PrimaryCharcoal
import com.mtpnda.biocue.ui.theme.AlertCoralChip
import com.mtpnda.biocue.ui.theme.AlertCoralInk
import com.mtpnda.biocue.ui.theme.ShadowTint
import com.mtpnda.biocue.ui.theme.StatePalette
import com.mtpnda.biocue.ui.theme.Surface2
import com.mtpnda.biocue.ui.theme.SurfaceCard

private val PillShape = RoundedCornerShape(AppRadius.Pill)

/** Low, soft, warm-tinted elevation (never grey). */
fun Modifier.softShadow(shape: Shape, elevation: Dp = 2.dp): Modifier =
    this.shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = ShadowTint,
        spotColor = ShadowTint
    )

/** Parchment card with a gentle warm border. Optionally tappable. */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    containerColor: Color = SurfaceCard,
    borderColor: Color = LineBorder,
    shape: Shape = RoundedCornerShape(AppRadius.Card),
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .softShadow(shape)
            .clip(shape)
            .background(containerColor)
            .border(1.dp, borderColor, shape)
            .then(
                if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick)
                else Modifier
            )
            .padding(contentPadding),
        content = content
    )
}

/** Primary action: warm charcoal pill with soft cream text. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    containerColor: Color = PrimaryCharcoal,
    contentColor: Color = OnPrimaryCharcoal
) {
    Button(
        // While loading we keep the enabled colours (so the spinner stays
        // visible) but swallow taps to prevent double submits.
        onClick = { if (!loading) onClick() },
        enabled = enabled,
        modifier = modifier
            .heightIn(min = AppMetrics.ButtonHeight)
            .then(if (enabled) Modifier.softShadow(PillShape, 3.dp) else Modifier),
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = LineBorder,
            disabledContentColor = InkMuted
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            disabledElevation = 0.dp
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = contentColor,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.5.sp),
            textAlign = TextAlign.Center
        )
    }
}

/** Secondary / destructive action: outlined pill. Defaults to a neutral look. */
@Composable
fun OutlinedPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = LineStrong,
    contentColor: Color = InkPrimary,
    containerColor: Color = SurfaceCard
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = AppMetrics.ButtonHeight),
        shape = PillShape,
        border = BorderStroke(1.5.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContentColor = InkMuted
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.5.sp),
            textAlign = TextAlign.Center
        )
    }
}

/** Destructive-but-gentle variant: soft coral outline, coral ink. */
@Composable
fun CoralOutlinedPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedPillButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        borderColor = AlertCoralChip,
        contentColor = AlertCoralInk
    )
}

/**
 * Circular icon button. The visual circle is [size] (40dp) but the tappable
 * area is always at least 48dp.
 */
@Composable
fun CircleIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    containerColor: Color = Surface2,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(maxOf(size, AppMetrics.MinTouchTarget))
            .clip(CircleShape)
            .semantics { this.contentDescription = contentDescription }
            .clickable(
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(containerColor)
                .border(1.dp, LineBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/** Soft pill badge with an optional status dot. */
@Composable
fun StatusPill(
    text: String,
    palette: StatePalette,
    modifier: Modifier = Modifier,
    textColor: Color = palette.ink,
    showDot: Boolean = true
) {
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(palette.soft)
            .border(1.dp, palette.chip.copy(alpha = 0.6f), PillShape)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showDot) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(palette.chip, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}

/** Small tinted circle holding an emoji. */
@Composable
fun EmojiAvatar(
    emoji: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    containerColor: Color = Surface2,
    borderColor: Color = LineBorder
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .border(1.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = (size.value * 0.45f).sp)
    }
}

/** Rounded text field in the parchment style. */
@Composable
fun CalmTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = InkPrimary),
        shape = RoundedCornerShape(AppRadius.Md),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = InkPrimary,
            unfocusedTextColor = InkPrimary,
            focusedContainerColor = SurfaceCard,
            unfocusedContainerColor = SurfaceCard,
            focusedBorderColor = PrimaryCharcoal,
            unfocusedBorderColor = LineBorder,
            cursorColor = PrimaryCharcoal,
            focusedLabelColor = InkSecondary,
            unfocusedLabelColor = InkSecondary
        )
    )
}

/**
 * Pairing code in large serif type on a soft stone plate. Screen readers get a
 * spaced-out reading ("1 2 3 4 5 6") instead of one big number.
 */
@Composable
fun PairingCode(
    code: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 40
) {
    val spoken = if (code.isNotEmpty()) {
        "Pairing code: " + code.toList().joinToString(" ")
    } else {
        "No active pairing code"
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.Md))
            .background(Surface2)
            .border(1.dp, LineBorder, RoundedCornerShape(AppRadius.Md))
            .padding(vertical = 18.dp)
            .clearAndSetSemantics { contentDescription = spoken },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (code.isNotEmpty()) code else "······",
            fontFamily = NewsreaderFont,
            fontSize = fontSize.sp,
            letterSpacing = 6.sp,
            color = if (code.isNotEmpty()) InkPrimary else InkMuted,
            textAlign = TextAlign.Center
        )
    }
}
