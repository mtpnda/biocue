package com.mtpnda.biocue.connect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ConnectionGuideScreen(
    manager: WatchConnectionManager,
    onConnected: () -> Unit = {},
) {
    val state by manager.state.collectAsState()
    val watchName by manager.watchName.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (state) {
            ConnectionState.CHECKING -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Looking for your watch…")
            }

            ConnectionState.NOT_PAIRED -> Guide(
                emoji = "⌚",
                title = "Watch not connected",
                body = "This phone can't see a smartwatch right now.\n\n" +
                    "1. Make sure the watch is on and Bluetooth is enabled\n" +
                    "2. Open Galaxy Wearable and connect your watch\n" +
                    "3. Come back — this screen updates automatically",
                primaryLabel = "Open Galaxy Wearable",
                onPrimary = { manager.openPairingApp() },
                onRetry = { manager.recheckNow() },
            )

            ConnectionState.APP_MISSING -> Guide(
                emoji = "📲",
                title = "Install BioCue on ${watchName ?: "your watch"}",
                body = "Your watch is connected, but the BioCue watch app " +
                    "isn't installed (or isn't running) on it yet.",
                primaryLabel = "Install on watch",
                onPrimary = { manager.promptInstallOnWatch() },
                onRetry = { manager.recheckNow() },
            )

            ConnectionState.NO_TRANSPORT -> Guide(
                emoji = "📡",
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
                Text("✅", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(8.dp))
                Text("Connected to ${watchName ?: "watch"}",
                    style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onConnected) { Text("Continue") }
            }
        }
    }
}

@Composable
private fun Guide(
    emoji: String,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onRetry: (() -> Unit)?,
) {
    Text(emoji, style = MaterialTheme.typography.displayMedium)
    Spacer(Modifier.height(8.dp))
    Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    Spacer(Modifier.height(12.dp))
    Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(24.dp))
    Button(onClick = onPrimary) { Text(primaryLabel) }
    if (onRetry != null) {
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRetry) { Text("Check again") }
    }
}
