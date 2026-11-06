package com.mtpnda.biocue.wear.link

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelUuid
import android.os.SystemClock
import android.util.Log
import com.mtpnda.biocue.core.link.DecodedFrame
import com.mtpnda.biocue.core.link.HelloStatus
import com.mtpnda.biocue.core.link.LinkCodec
import com.mtpnda.biocue.core.link.LinkFraming
import com.mtpnda.biocue.core.link.LinkMessage
import com.mtpnda.biocue.core.link.LinkOutbox
import com.mtpnda.biocue.core.link.LinkProtocol as P
import com.mtpnda.biocue.core.link.LinkProtocolException
import com.mtpnda.biocue.core.link.LinkReassembler
import com.mtpnda.biocue.core.link.WatchLinkSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * The watch end of BioCue Link v1: a BLE GATT *server* (peripheral) that an iPhone connects to.
 *
 * Why BLE and not the Wearable Data Layer: the Data Layer only connects a Wear OS watch to an
 * ANDROID phone. See docs/WATCH_PHONE_CONNECTION.md for the full reasoning and the protocol.
 *
 * Security model: every characteristic requires an encrypted + MITM-protected (LE Secure
 * Connections numeric comparison) link, so a phone cannot read or write anything until the wearer
 * has confirmed the pairing code on the watch. A phone that is not yet bonded is dropped on connect
 * unless the wearer has opened the pairing window. The app-layer HELLO additionally restricts the
 * link to a WEARER's phone ([WatchLinkSession]).
 *
 * All state changes happen on one private thread, so callbacks (binder threads) and callers
 * (coroutines/UI) never race. Nothing here touches Bluetooth until [start] is called, so Android-phone
 * users pay nothing for this class.
 */
@SuppressLint("MissingPermission") // every Bluetooth call is behind LinkPermissions.hasAll()
class BleLinkServer private constructor(private val appContext: Context) {

    sealed class LinkState {
        object Stopped : LinkState()
        object Unsupported : LinkState()
        object NoPermission : LinkState()
        object BluetoothOff : LinkState()
        data class Advertising(val pairingOpen: Boolean) : LinkState()
        /** A phone is connected but has not completed HELLO yet. */
        object Handshaking : LinkState()
        object Established : LinkState()
        data class Refused(val reason: HelloStatus) : LinkState()
    }

    private val _state = MutableStateFlow<LinkState>(LinkState.Stopped)
    val state: StateFlow<LinkState> = _state.asStateFlow()

    private val _established = MutableStateFlow(false)
    /** True while an authenticated WEARER phone is connected and past HELLO. */
    val isEstablished: StateFlow<Boolean> = _established.asStateFlow()

    /** Invoked (on the BLE thread) when the phone asks for a GPS fix for an SOS. */
    @Volatile var onSosGpsRequest: (() -> Unit)? = null

    // ---- state (touched only on [handler]'s thread) ---------------------------------------
    private val thread by lazy { HandlerThread("biocue-ble").also { it.start() } }
    private val handler by lazy { Handler(thread.looper) }

    private var wantRunning = false
    private var gattServer: BluetoothGattServer? = null
    private var uplinkChar: BluetoothGattCharacteristic? = null
    private var connected: BluetoothDevice? = null
    private var subscribed = false
    private var mtu = DEFAULT_MTU
    private var pairingUntil = 0L
    private var advertising = false
    private var pendingDisconnect = false
    private var txBusy = false
    private var txFailures = 0
    private val txQueue = ArrayDeque<ByteArray>()
    private var inbound = LinkReassembler()

    private val session = WatchLinkSession(
        watchName = Build.MODEL.orEmpty().take(32),
        batteryPercent = { batteryPercent() },
        remoteIsBonded = { connected?.bondState == BluetoothDevice.BOND_BONDED },
        pairingWindowOpen = { pairingOpen() },
        // Un-ACKed events are saved to a small file so a watch reboot does not lose them.
        outbox = LinkOutbox(storage = com.mtpnda.biocue.core.link.FileOutboxStorage(java.io.File(appContext.filesDir, "link_outbox.bin"))),
    )

    // ---- public API ---------------------------------------------------------------------

    fun start() = post {
        wantRunning = true
        startInternal()
        registerBluetoothReceiver()
    }

    fun stop() = post {
        wantRunning = false
        unregisterBluetoothReceiver()
        teardown(LinkState.Stopped)
    }

    /** Lets a first-time phone connect and pair. Closes itself after [durationMs] or on first success. */
    fun openPairingWindow(durationMs: Long = P.PAIRING_WINDOW_MS) = post {
        pairingUntil = SystemClock.elapsedRealtime() + durationMs
        handler.removeCallbacks(closePairingRunnable)
        handler.postDelayed(closePairingRunnable, durationMs)
        if (advertising) { stopAdvertising(); startAdvertising() }
        publishState()
    }

    /**
     * Hand a message to the link. Critical messages are retained until the phone ACKs them, so a
     * phone that is away does not lose events. Telemetry is dropped when nobody is listening.
     * @return true if the message was accepted for delivery (sent now, or retained for later)
     */
    fun send(message: LinkMessage): Boolean {
        post {
            try {
                handleOutput(session.submit(message))
            } catch (e: LinkProtocolException) {
                Log.w(TAG, "dropping unencodable message: ${e.message}")
            }
        }
        return message.isCritical || _established.value
    }

    // ---- lifecycle ----------------------------------------------------------------------

    private fun startInternal() {
        if (gattServer != null) return
        if (!appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            _state.value = LinkState.Unsupported; return
        }
        if (!LinkPermissions.hasAll(appContext)) { _state.value = LinkState.NoPermission; return }
        val manager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = manager?.adapter
        if (adapter == null) { _state.value = LinkState.Unsupported; return }
        if (!adapter.isEnabled) { _state.value = LinkState.BluetoothOff; return }

        val server = manager.openGattServer(appContext, serverCallback)
        if (server == null) { _state.value = LinkState.Unsupported; return }
        gattServer = server
        server.addService(buildService()) // advertising starts in onServiceAdded
    }

    private fun teardown(finalState: LinkState) {
        handler.removeCallbacks(helloTimeoutRunnable)
        stopAdvertising()
        runCatching { gattServer?.close() }
        gattServer = null
        uplinkChar = null
        connected = null
        subscribed = false
        txQueue.clear(); txBusy = false; txFailures = 0; pendingDisconnect = false
        session.onDisconnected()
        _established.value = false
        _state.value = finalState
    }

    private fun buildService(): BluetoothGattService {
        val service = BluetoothGattService(UUID.fromString(P.SERVICE_UUID), BluetoothGattService.SERVICE_TYPE_PRIMARY)

        val uplink = BluetoothGattCharacteristic(
            UUID.fromString(P.UPLINK_UUID), BluetoothGattCharacteristic.PROPERTY_NOTIFY, 0
        )
        uplink.addDescriptor(
            BluetoothGattDescriptor(
                UUID.fromString(P.CCCD_UUID),
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM or BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM
            )
        )
        val downlink = BluetoothGattCharacteristic(
            UUID.fromString(P.DOWNLINK_UUID),
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM
        )
        val info = BluetoothGattCharacteristic(
            UUID.fromString(P.INFO_UUID),
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM
        )
        service.addCharacteristic(uplink)
        service.addCharacteristic(downlink)
        service.addCharacteristic(info)
        uplinkChar = uplink
        return service
    }

    // ---- advertising --------------------------------------------------------------------

    private fun startAdvertising() {
        val manager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val advertiser = manager?.adapter?.bluetoothLeAdvertiser
        if (advertiser == null) { _state.value = LinkState.Unsupported; return }
        val settings = AdvertiseSettings.Builder()
            // Faster discovery while the user is actively pairing; frugal otherwise.
            .setAdvertiseMode(if (pairingOpen()) AdvertiseSettings.ADVERTISE_MODE_BALANCED else AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .setTimeout(0)
            .build()
        // No device name, no manufacturer data — and the service id ONLY while the wearer is pairing. Once bonded,
        // the iPhone reconnects to the remembered watch by identity, so a passer-by cannot tell a BioCue watch is nearby.
        val data = AdvertiseData.Builder().setIncludeDeviceName(false).apply {
            if (com.mtpnda.biocue.core.link.AdvertisePolicy.includeServiceUuid(pairingOpen())) {
                addServiceUuid(ParcelUuid(UUID.fromString(P.SERVICE_UUID)))
            }
        }.build()
        advertiser.startAdvertising(settings, data, advertiseCallback)
    }

    private fun stopAdvertising() {
        if (!advertising) return
        val manager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        runCatching { manager?.adapter?.bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback) }
        advertising = false
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            post { advertising = true; publishState() }
        }
        override fun onStartFailure(errorCode: Int) {
            Log.w(TAG, "advertising failed: $errorCode")
            post { advertising = false; _state.value = LinkState.Unsupported }
        }
    }

    // ---- GATT server callbacks (binder threads -> hop to [handler]) -----------------------

    private val serverCallback = object : BluetoothGattServerCallback() {

        override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
            post { if (status == BluetoothGatt.GATT_SUCCESS && wantRunning) startAdvertising() }
        }

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            post {
                if (newState == BluetoothProfile.STATE_CONNECTED) onDeviceConnected(device)
                else if (newState == BluetoothProfile.STATE_DISCONNECTED) onDeviceDisconnected(device)
            }
        }

        override fun onMtuChanged(device: BluetoothDevice, newMtu: Int) {
            post { if (device == connected) mtu = newMtu }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic
        ) {
            post {
                if (characteristic.uuid == UUID.fromString(P.INFO_UUID)) {
                    val flags = if (pairingOpen()) 1 else 0
                    val value = byteArrayOf(P.VERSION.toByte(), flags.toByte())
                    val slice = if (offset <= value.size) value.copyOfRange(offset, value.size) else ByteArray(0)
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, slice)
                } else {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_READ_NOT_PERMITTED, offset, null)
                }
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice, requestId: Int, descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?
        ) {
            post {
                var status = BluetoothGatt.GATT_SUCCESS
                if (descriptor.uuid == UUID.fromString(P.CCCD_UUID) && device == connected) {
                    subscribed = value != null && value.isNotEmpty() &&
                        (value[0].toInt() and 0x01) != 0
                } else {
                    status = BluetoothGatt.GATT_WRITE_NOT_PERMITTED
                }
                if (responseNeeded) gattServer?.sendResponse(device, requestId, status, offset, null)
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice, requestId: Int, characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?
        ) {
            post {
                val ok = characteristic.uuid == UUID.fromString(P.DOWNLINK_UUID) && device == connected && value != null
                if (responseNeeded) {
                    gattServer?.sendResponse(
                        device, requestId,
                        if (ok) BluetoothGatt.GATT_SUCCESS else BluetoothGatt.GATT_WRITE_NOT_PERMITTED, offset, null
                    )
                }
                if (ok) onDownlinkChunk(value!!)
            }
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            post {
                txBusy = false
                if (status != BluetoothGatt.GATT_SUCCESS) txFailures++ else txFailures = 0
                pump()
            }
        }
    }

    // ---- connection handling (handler thread) -----------------------------------------------

    private fun onDeviceConnected(device: BluetoothDevice) {
        // One phone at a time.
        if (connected != null && connected != device) {
            gattServer?.cancelConnection(device); return
        }
        // A stranger outside the pairing window must not even reach the pairing prompt.
        if (device.bondState != BluetoothDevice.BOND_BONDED && !pairingOpen()) {
            Log.i(TAG, "refusing unbonded connection outside pairing window")
            gattServer?.cancelConnection(device); return
        }
        connected = device
        subscribed = false
        mtu = DEFAULT_MTU
        inbound = LinkReassembler()
        txQueue.clear(); txBusy = false; txFailures = 0; pendingDisconnect = false
        session.onConnected()
        stopAdvertising() // a peripheral serving one phone does not need to keep shouting
        handler.removeCallbacks(helloTimeoutRunnable)
        handler.postDelayed(helloTimeoutRunnable, P.HELLO_TIMEOUT_MS)
        _state.value = LinkState.Handshaking
    }

    private fun onDeviceDisconnected(device: BluetoothDevice) {
        if (device != connected) return
        handler.removeCallbacks(helloTimeoutRunnable)
        connected = null
        subscribed = false
        txQueue.clear(); txBusy = false; pendingDisconnect = false
        session.onDisconnected()
        _established.value = false
        if (wantRunning && gattServer != null) { startAdvertising(); publishState() }
    }

    private fun onDownlinkChunk(chunk: ByteArray) {
        val frame = inbound.accept(chunk) ?: return
        val decoded: DecodedFrame = try {
            LinkCodec.decode(frame)
        } catch (e: LinkProtocolException) {
            Log.w(TAG, "bad frame from phone: ${e.message}"); return
        }
        handleOutput(session.onDownlink(decoded))
    }

    private fun handleOutput(out: WatchLinkSession.Output) {
        val wasEstablished = _established.value
        if (out.frames.isNotEmpty()) enqueueFrames(out.frames)
        if (out.disconnect) pendingDisconnect = true
        if (out.sosGpsRequested) onSosGpsRequest?.invoke()

        when (session.state) {
            WatchLinkSession.State.ESTABLISHED -> {
                handler.removeCallbacks(helloTimeoutRunnable)
                _established.value = true
                if (!wasEstablished) closePairingWindow() // the window is single-use
                _state.value = LinkState.Established
            }
            WatchLinkSession.State.REJECTED ->
                _state.value = session.lastRejection?.let { LinkState.Refused(it) } ?: LinkState.Handshaking
            else -> Unit
        }
        if (pendingDisconnect && txQueue.isEmpty() && !txBusy) pump() // nothing to drain: disconnect now
    }

    // ---- sending (handler thread) ---------------------------------------------------------

    private fun enqueueFrames(frames: List<ByteArray>) {
        val chunkSize = (mtu - 3).coerceAtLeast(DEFAULT_MTU - 3)
        for (f in frames) for (c in LinkFraming.split(f, chunkSize)) txQueue.addLast(c)
        pump()
    }

    private fun pump() {
        val device = connected ?: return
        if (txBusy) return
        val chunk = txQueue.removeFirstOrNull()
        if (chunk == null) {
            if (pendingDisconnect) { pendingDisconnect = false; gattServer?.cancelConnection(device) }
            return
        }
        if (!subscribed) { txQueue.addFirst(chunk); return } // phone has not enabled notifications yet
        if (txFailures > MAX_TX_FAILURES) {
            Log.w(TAG, "notify keeps failing, dropping queue"); txQueue.clear(); txFailures = 0; return
        }
        val ch = uplinkChar ?: return
        if (notify(device, ch, chunk)) {
            txBusy = true
        } else {
            txFailures++
            txQueue.addFirst(chunk)
            handler.postDelayed({ pump() }, RETRY_DELAY_MS)
        }
    }

    private fun notify(device: BluetoothDevice, ch: BluetoothGattCharacteristic, value: ByteArray): Boolean {
        val server = gattServer ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            server.notifyCharacteristicChanged(device, ch, false, value) == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            ch.value = value
            @Suppress("DEPRECATION")
            server.notifyCharacteristicChanged(device, ch, false)
        }
    }

    // ---- misc ---------------------------------------------------------------------------

    private fun pairingOpen() = SystemClock.elapsedRealtime() < pairingUntil

    private val closePairingRunnable = Runnable { closePairingWindow() }

    private fun closePairingWindow() {
        pairingUntil = 0L
        handler.removeCallbacks(closePairingRunnable)
        if (advertising) { stopAdvertising(); startAdvertising() }
        publishState()
    }

    private val helloTimeoutRunnable = Runnable {
        val out = session.onHelloTimeout()
        if (out.disconnect) {
            connected?.let { gattServer?.cancelConnection(it) }
        }
    }

    private fun publishState() {
        val current = _state.value
        // Do not clobber an active connection's state with an advertising update.
        if (connected != null || current is LinkState.Refused) return
        if (gattServer != null && wantRunning) _state.value = LinkState.Advertising(pairingOpen())
    }

    private fun batteryPercent(): Int {
        val bm = appContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val pct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        return if (pct in 0..100) pct else 255
    }

    private fun post(block: () -> Unit) { handler.post(block) }

    // Bluetooth turned on/off while we are supposed to be running.
    private var btReceiver: BroadcastReceiver? = null

    private fun registerBluetoothReceiver() {
        if (btReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val s = intent?.getIntExtra(android.bluetooth.BluetoothAdapter.EXTRA_STATE, -1)
                post {
                    if (s == android.bluetooth.BluetoothAdapter.STATE_OFF && wantRunning) teardown(LinkState.BluetoothOff)
                    else if (s == android.bluetooth.BluetoothAdapter.STATE_ON && wantRunning) startInternal()
                }
            }
        }
        btReceiver = receiver
        appContext.registerReceiver(receiver, IntentFilter(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    private fun unregisterBluetoothReceiver() {
        btReceiver?.let { runCatching { appContext.unregisterReceiver(it) } }
        btReceiver = null
    }

    companion object {
        private const val TAG = "BleLinkServer"
        private const val DEFAULT_MTU = 23
        private const val MAX_TX_FAILURES = 20
        private const val RETRY_DELAY_MS = 50L

        @Volatile private var instance: BleLinkServer? = null

        fun get(context: Context): BleLinkServer =
            instance ?: synchronized(this) {
                instance ?: BleLinkServer(context.applicationContext).also { instance = it }
            }
    }
}
