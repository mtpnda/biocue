package com.mtpnda.biocue.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mtpnda.biocue.R
import com.mtpnda.biocue.data.UserRole
import com.mtpnda.biocue.ui.components.EmojiAvatar
import com.mtpnda.biocue.ui.components.SoftCard
import com.mtpnda.biocue.ui.theme.BgColor
import com.mtpnda.biocue.ui.theme.CalmGreenChip
import com.mtpnda.biocue.ui.theme.CalmGreenSoft
import com.mtpnda.biocue.ui.theme.InfoBlueChip
import com.mtpnda.biocue.ui.theme.InfoBlueSoft
import com.mtpnda.biocue.ui.theme.InkSecondary

@Composable
fun RoleSelectionScreen(
    onRoleSelected: (UserRole) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.role_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 28.dp)
        )

        RoleCard(
            emoji = "🩺",
            title = stringResource(id = R.string.role_wearer_label),
            description = stringResource(id = R.string.role_wearer_desc),
            avatarColor = CalmGreenSoft,
            avatarBorder = CalmGreenChip,
            onClick = { onRoleSelected(UserRole.WEARER) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        RoleCard(
            emoji = "🤝",
            title = stringResource(id = R.string.role_caregiver_label),
            description = stringResource(id = R.string.role_caregiver_desc),
            avatarColor = InfoBlueSoft,
            avatarBorder = InfoBlueChip,
            onClick = { onRoleSelected(UserRole.CAREGIVER) }
        )
    }
}

@Composable
private fun RoleCard(
    emoji: String,
    title: String,
    description: String,
    avatarColor: Color,
    avatarBorder: Color,
    onClick: () -> Unit
) {
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(24.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EmojiAvatar(
                emoji = emoji,
                size = 72.dp,
                containerColor = avatarColor,
                borderColor = avatarBorder
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
