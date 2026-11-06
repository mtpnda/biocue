package com.mtpnda.biocue.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mtpnda.biocue.R
import com.mtpnda.biocue.ui.components.EmojiAvatar
import com.mtpnda.biocue.ui.components.PillButton
import com.mtpnda.biocue.ui.theme.BgColor
import com.mtpnda.biocue.ui.theme.InkSecondary
import com.mtpnda.biocue.ui.theme.RisingAmberChip
import com.mtpnda.biocue.ui.theme.RisingAmberSoft

@Composable
fun AccessRevokedScreen(
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .systemBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        EmojiAvatar(
            emoji = "🔒",
            size = 88.dp,
            containerColor = RisingAmberSoft,
            borderColor = RisingAmberChip
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.revoked_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(id = R.string.revoked_body),
            style = MaterialTheme.typography.bodyLarge,
            color = InkSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        PillButton(
            text = stringResource(id = R.string.revoked_ok),
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
