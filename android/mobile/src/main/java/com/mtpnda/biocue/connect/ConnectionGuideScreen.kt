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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mtpnda.biocue.ui.components.EmojiAvatar
import com.mtpnda.biocue.ui.components.OutlinedPillButton
import com.mtpnda.biocue.ui.components.PillButton
import com.mtpnda.biocue.ui.components.SoftCard
import com.mtpnda.biocue.ui.theme.BgColor
import com.mtpnda.biocue.ui.theme.CalmPalette
import com.mtpnda.biocue.ui.theme.InfoPalette
import com.mtpnda.biocue.ui.theme.InkSecondary
import com.mtpnda.biocue.ui.theme.LineBorder
import com.mtpnda.biocue.ui.theme.PrimaryCharcoal
import com.mtpnda.biocue.ui.theme.RisingPalette
import com.mtpnda.biocue.ui.theme.StatePalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ConnectionGuideScreen(
    manager: WatchConnectionManager,
    onConnected: () -> Unit = {},
) {
    val state by manager.state.collectAsState()
    val watchName by manager.watchName.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (state) {
            ConnectionState.CHECKING -> {
                CircularProgressIndicator(color = PrimaryCharcoal, trackColor = LineBorder)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Looking for your watch…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkSecondary
                )
            }

            ConnectionState.NOT_PAIRED -> Guide(
                emoji = "⌚",
                palette = RisingPalette,
                title = "Watch not connected",
                body = "This phone can't see a smartwatch right now.\n\n" +
                    "1. Make sure the watch is on and Bluetooth is enabled\n" +
                    "2. Open Galaxy Wearable and connect your watch\n" +
                    "3. Come back — this screen updates automatically",
                primaryLabel = "Open Galaxy Wearable",
                onPrimary = { manager.openPairingApp() },
                onRetry = { manager.recheckNow() },
            )

            ConnectionState.APP_MISSING -> {
                var isInstalling by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                Guide(
                    emoji = "📲",
                    palette = InfoPalette,
                    title = "Install BioCue on ${watchName ?: "your watch"}",
                    body = "Your watch is connected, but the BioCue watch app " +
                        "isn't installed (or isn't running) on it yet.",
                    primaryLabel = "Install on watch",
                    isPrimaryLoading = isInstalling,
                    onPrimary = {
                        scope.launch {
                            isInstalling = true
                            manager.promptInstallOnWatch().join()
                            delay(2000) // Keep the loading state briefly so the user sees it worked
                            isInstalling = false
                        }
                    },
                    onRetry = { manager.recheckNow() },
                )
            }

            ConnectionState.NO_TRANSPORT -> Guide(
                emoji = "📡",
                palette = RisingPalette,
                title = "Connection hiccup",
                body = "${watchName ?: "Your watch"} is paired and has BioCue, " +
                    "but messages aren't getting through.\n\n" +
                    "• Open the BioCue app on the watch once\n" +
                    "• Toggle Bluetooth off/on\n" +
                    "• Restart both devices if it persists",
                primaryLabel = "Try again",
                onPrimary = { manager.recheckNow() },
                onRetry = null,
            )

            ConnectionState.CONNECTED -> {
                EmojiAvatar(
                    emoji = "✅",
                    size = 88.dp,
                    containerColor = CalmPalette.soft,
                    borderColor = CalmPalette.chip
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "Connected to ${watchName ?: "watch"}",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                PillButton(
                    text = "Continue",
                    onClick = onConnected,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun Guide(
    emoji: String,
    palette: StatePalette,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onRetry: (() -> Unit)?,
    isPrimaryLoading: Boolean = false,
) {
    EmojiAvatar(
        emoji = emoji,
        size = 88.dp,
        containerColor = palette.soft,
        borderColor = palette.chip
    )
    Spacer(Modifier.height(20.dp))
    Text(
        title,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(16.dp))
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Text(body, style = MaterialTheme.typography.bodyLarge)
    }
    Spacer(Modifier.height(24.dp))
    PillButton(
        text = if (isPrimaryLoading) "Sending to watch..." else primaryLabel,
        onClick = onPrimary,
        loading = isPrimaryLoading,
        modifier = Modifier.fillMaxWidth()
    )
    if (onRetry != null) {
        Spacer(Modifier.height(12.dp))
        OutlinedPillButton(
            text = "Check again",
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
