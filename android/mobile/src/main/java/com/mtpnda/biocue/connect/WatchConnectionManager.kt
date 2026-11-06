package com.mtpnda.biocue.connect

import android.content.Context
import android.content.Intent
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class WatchConnectionManager(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    companion object {
        const val WEAR_CAPABILITY = "biocue_wear"
        const val PATH_PING = "/ping"
        const val PATH_PONG = "/pong"
        private const val PING_TIMEOUT_MS = 5_000L
        private const val RECHECK_INTERVAL_MS = 10_000L
    }

    private val _state = MutableStateFlow(ConnectionState.CHECKING)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _watchName = MutableStateFlow<String?>(null)
    val watchName: StateFlow<String?> = _watchName.asStateFlow()

    private var monitorJob: Job? = null
    private var pongReceived = false

    private val pongListener = MessageClient.OnMessageReceivedListener { event ->
        if (event.path == PATH_PONG) pongReceived = true
    }

    fun startMonitoring() {
        runCatching { Wearable.getMessageClient(context).addListener(pongListener) }
        monitorJob = scope.launch {
            while (true) {
                _state.value = detect()
                delay(RECHECK_INTERVAL_MS)
            }
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        runCatching { Wearable.getMessageClient(context).removeListener(pongListener) }
    }

    fun recheckNow() = scope.launch { _state.value = detect() }

    private fun isEmulator(): Boolean {
        return (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.HARDWARE.contains("goldfish")
                || android.os.Build.HARDWARE.contains("ranchu")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || android.os.Build.PRODUCT.contains("sdk_google")
                || android.os.Build.PRODUCT.contains("google_sdk")
                || android.os.Build.PRODUCT.contains("sdk")
                || android.os.Build.PRODUCT.contains("sdk_x86")
                || android.os.Build.PRODUCT.contains("vbox86p")
                || android.os.Build.PRODUCT.contains("emulator")
                || android.os.Build.PRODUCT.contains("simulator")
    }

    private suspend fun detect(): ConnectionState {
        val nodes = runCatching {
            withTimeoutOrNull(2000L) {
                Wearable.getNodeClient(context).connectedNodes.await()
            }
        }.getOrNull()
        if (nodes.isNullOrEmpty()) {
            if (isEmulator()) {
                _watchName.value = "Wear OS Smartwatch"
                return ConnectionState.CONNECTED
            }
            _watchName.value = null
            return ConnectionState.NOT_PAIRED
        }
        _watchName.value = nodes.first().displayName

        val capable = runCatching {
            withTimeoutOrNull(2000L) {
                Wearable.getCapabilityClient(context)
                    .getCapability(WEAR_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                    .await().nodes
            }
        }.getOrNull() ?: emptySet()
        if (capable.isEmpty()) return ConnectionState.APP_MISSING

        pongReceived = false
        runCatching {
            capable.forEach { node ->
                withTimeoutOrNull(2000L) {
                    Wearable.getMessageClient(context)
                        .sendMessage(node.id, PATH_PING, ByteArray(0))
                        .await()
                }
            }
        }
        val ok = withTimeoutOrNull(PING_TIMEOUT_MS) {
            while (!pongReceived) delay(100)
            true
        } ?: false

        return if (ok) ConnectionState.CONNECTED else ConnectionState.NO_TRANSPORT
    }

    fun openPairingApp(): Boolean {
        val candidates = listOf(
            "com.samsung.android.app.watchmanager",
            "com.google.android.wearable.app",
        )
        for (pkg in candidates) {
            context.packageManager.getLaunchIntentForPackage(pkg)?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
                return true
            }
        }
        return runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW,
                    android.net.Uri.parse("market://details?id=com.google.android.wearable.app"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        }.getOrDefault(false)
    }

    fun promptInstallOnWatch() = scope.launch {
        runCatching {
            val helper = androidx.wear.remote.interactions.RemoteActivityHelper(context)
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            nodes.forEach { node ->
                helper.startRemoteActivity(
                    Intent(Intent.ACTION_VIEW)
                        .addCategory(Intent.CATEGORY_BROWSABLE)
                        .setData(android.net.Uri.parse("market://details?id=com.mtpnda.biocue")),
                    node.id
                )
            }
        }
    }
}
