import Foundation
import Combine
import CoreBluetooth
import BioCueCore

// WEARER-ONLY. The iPhone end of BioCue Link v1: a CoreBluetooth *central* that connects to the
// watch's GATT server. Protocol + security model: docs/WATCH_PHONE_CONNECTION.md.

enum WatchLinkState: Equatable {
    case stopped
    case bluetoothOff
    case unauthorized
    case unsupported
    /// Bluetooth is on, no watch has been chosen yet.
    case idle
    /// Looking for a NEW watch (the user tapped "Connect watch").
    case scanning
    /// Reconnecting to the remembered watch.
    case connecting
    /// Connected; pairing / HELLO in progress.
    case handshaking
    case connected(watchName: String, battery: Int?)
    case refused(String)
}

protocol WatchLinking: AnyObject {
    var statePublisher: AnyPublisher<WatchLinkState, Never> { get }
    /// Decoded frames from the watch, in order. Critical ones must be passed to `acknowledge` once stored.
    var frames: AsyncStream<DecodedFrame> { get }
    /// Converts the watch's timestamps to phone time (measured at the last successful handshake).
    var clock: WatchClock { get }
    func start()
    /// Scan for a new watch (the watch must be showing "Pair iPhone").
    func beginPairing()
    func forgetWatch()
    /// Disconnect and stop scanning WITHOUT forgetting the watch (sign-out / leaving the wearer experience).
    func shutdown()
    func acknowledge(seq: Int)
    func requestSosLocation()
}

final class WatchLinkCentral: NSObject, ObservableObject, WatchLinking, CBCentralManagerDelegate, CBPeripheralDelegate {

    @Published private(set) var state: WatchLinkState = .stopped
    var statePublisher: AnyPublisher<WatchLinkState, Never> { $state.eraseToAnyPublisher() }

    let frames: AsyncStream<DecodedFrame>
    private(set) var clock: WatchClock = .identity
    private let framesContinuation: AsyncStream<DecodedFrame>.Continuation

    private static let restoreID = "com.mtpnda.biocue.central"
    /// Per ACCOUNT, never per phone: another account signing in on this phone must not inherit this person's watch.
    private let rememberedKey: String
    private static let pairingScanSeconds: TimeInterval = 60
    private static let candidateSettleSeconds: TimeInterval = 2

    private let role: LinkRole
    private let defaults: UserDefaults
    private let serviceUUID = CBUUID(string: LinkProtocol.serviceUUID)
    private let uplinkUUID = CBUUID(string: LinkProtocol.uplinkUUID)
    private let downlinkUUID = CBUUID(string: LinkProtocol.downlinkUUID)
    private let infoUUID = CBUUID(string: LinkProtocol.infoUUID)

    private var central: CBCentralManager?
    private var peripheral: CBPeripheral?
    private var uplink: CBCharacteristic?
    private var downlink: CBCharacteristic?
    private var info: CBCharacteristic?

    private let inbound = LinkInbound()
    private var outbound = LinkOutbound()
    private var writeQueue: [Data] = []
    private var writing = false
    private var established = false

    private var pairingMode = false
    private var candidates: [UUID: (peripheral: CBPeripheral, rssi: Int)] = [:]
    private var pickWork: DispatchWorkItem?
    private var scanTimeoutWork: DispatchWorkItem?

    /// Throws `RoleError` for a caregiver: the type cannot even be constructed for the wrong role.
    init(gate: RoleGate, accountId: String, defaults: UserDefaults = .standard) throws {
        try gate.require(\.linkWatch, action: "open the watch link")
        self.rememberedKey = "biocue.watch.peripheralID.\(accountId)"
        self.role = gate.role.linkRole
        self.defaults = defaults
        var continuation: AsyncStream<DecodedFrame>.Continuation?
        self.frames = AsyncStream<DecodedFrame> { continuation = $0 }
        self.framesContinuation = continuation!
        super.init()
    }

    deinit { framesContinuation.finish() }

    // MARK: - Public

    func start() {
        guard central == nil else { return }
        state = .idle
        // Main queue: every delegate callback below runs on the main thread, so no locking is needed.
        central = CBCentralManager(delegate: self, queue: .main,
                                   options: [CBCentralManagerOptionRestoreIdentifierKey: Self.restoreID])
    }

    func beginPairing() {
        if central == nil { start() }
        forgetWatch()
        pairingMode = true
        if central?.state == .poweredOn { startScan() }
    }

    func forgetWatch() {
        pickWork?.cancel(); scanTimeoutWork?.cancel()
        if let p = peripheral { central?.cancelPeripheralConnection(p) }
        peripheral = nil
        resetConnectionState()
        defaults.removeObject(forKey: rememberedKey)
        if state != .bluetoothOff && state != .unauthorized && state != .unsupported { state = .idle }
    }

    func shutdown() {
        pickWork?.cancel(); scanTimeoutWork?.cancel()
        central?.stopScan()
        if let p = peripheral { central?.cancelPeripheralConnection(p) }
        peripheral = nil
        pairingMode = false
        resetConnectionState()
        state = .stopped
    }

    func acknowledge(seq: Int) { enqueue(.ack(ackedSeq: seq)) }
    func requestSosLocation() { enqueue(.sosGpsRequest) }

    // MARK: - CBCentralManagerDelegate

    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        switch central.state {
        case .poweredOn:
            if peripheral == nil, let id = rememberedID, let p = central.retrievePeripherals(withIdentifiers: [id]).first {
                peripheral = p
            }
            if let p = peripheral { connect(p) }
            else if pairingMode { startScan() }
            else { state = .idle }
        case .poweredOff: state = .bluetoothOff
        case .unauthorized: state = .unauthorized
        case .unsupported: state = .unsupported
        default: break
        }
    }

    func centralManager(_ central: CBCentralManager, willRestoreState dict: [String: Any]) {
        // iOS relaunched the app in the background for this link: pick the connection back up.
        if let restored = (dict[CBCentralManagerRestoredStatePeripheralsKey] as? [CBPeripheral])?.first {
            peripheral = restored
            restored.delegate = self
        }
    }

    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral,
                        advertisementData: [String: Any], rssi RSSI: NSNumber) {
        guard pairingMode else { return }   // never adopt a watch we were not asked to find
        let rssi = RSSI.intValue
        if rssi == 127 { return }           // "unavailable"
        candidates[peripheral.identifier] = (peripheral, rssi)
        if pickWork == nil {
            let work = DispatchWorkItem { [weak self] in self?.pickNearestCandidate() }
            pickWork = work
            DispatchQueue.main.asyncAfter(deadline: .now() + Self.candidateSettleSeconds, execute: work)
        }
    }

    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        resetConnectionState()
        state = .handshaking
        peripheral.discoverServices([serviceUUID])
    }

    func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
        retryAfterDelay()
    }

    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        resetConnectionState()
        if case .refused = state { return }   // a refusal is final until the user acts
        // Remembered watch: ask iOS to reconnect whenever it is next in range (no timeout).
        if rememberedID != nil, self.peripheral != nil { connect(peripheral) } else { state = .idle }
    }

    // MARK: - CBPeripheralDelegate (handshake: services -> characteristics -> read info -> notify -> HELLO)

    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        if let error = error { return fail("Could not read the watch: \(error.localizedDescription)") }
        guard let service = peripheral.services?.first(where: { $0.uuid == serviceUUID }) else {
            return fail("This device is not a BioCue watch.")
        }
        peripheral.discoverCharacteristics([uplinkUUID, downlinkUUID, infoUUID], for: service)
    }

    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        if let error = error { return fail("Could not read the watch: \(error.localizedDescription)") }
        for c in service.characteristics ?? [] {
            if c.uuid == uplinkUUID { uplink = c }
            else if c.uuid == downlinkUUID { downlink = c }
            else if c.uuid == infoUUID { info = c }
        }
        guard uplink != nil, downlink != nil, let info = info else { return fail("The watch app is out of date.") }
        // The first access to a protected characteristic is what makes iOS show its pairing prompt
        // (the watch shows the same 6-digit number; the wearer confirms both).
        peripheral.readValue(for: info)
    }

    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        if characteristic.uuid == infoUUID {
            if error != nil { return fail("Pairing was cancelled or failed. Try Connect watch again.") }
            if let uplink = uplink { peripheral.setNotifyValue(true, for: uplink) }
            return
        }
        guard characteristic.uuid == uplinkUUID, error == nil, let value = characteristic.value else { return }
        handleUplink([UInt8](value))
    }

    func peripheral(_ peripheral: CBPeripheral, didUpdateNotificationStateFor characteristic: CBCharacteristic, error: Error?) {
        guard characteristic.uuid == uplinkUUID else { return }
        if error != nil { return fail("Pairing was cancelled or failed. Try Connect watch again.") }
        guard characteristic.isNotifying else { return }
        enqueue(.hello(maxVersion: LinkProtocol.version, role: role))
    }

    func peripheral(_ peripheral: CBPeripheral, didWriteValueFor characteristic: CBCharacteristic, error: Error?) {
        writing = false
        if error != nil { writeQueue.removeAll(); return }
        pumpWrites()
    }

    // MARK: - Inbound

    private func handleUplink(_ chunk: [UInt8]) {
        guard let frame = inbound.receive(chunk: chunk) else { return }
        switch frame.message {
        case .helloAck(_, let status, let name, let battery, let watchTime):
            if status == .ok {
                established = true
                clock = WatchClock.fromHello(watchTimeMs: watchTime, phoneNowMs: Int64(Date().timeIntervalSince1970 * 1000))
                if let p = peripheral { defaults.set(p.identifier.uuidString, forKey: rememberedKey) }
                state = .connected(watchName: name.isEmpty ? "Watch" : name, battery: battery == 255 ? nil : battery)
            } else {
                refuse(status)
            }
        case .pong, .unknown:
            break
        default:
            // Anything else is only meaningful after a successful HELLO.
            if established { framesContinuation.yield(frame) }
        }
    }

    private func refuse(_ status: HelloStatus) {
        let text: String
        switch status {
        case .roleNotAllowed: text = "This watch only connects to a wearer's account."
        case .versionUnsupported: text = "Update BioCue on your phone and watch, then try again."
        case .notInPairingWindow: text = "On the watch, tap Pair iPhone, then try again."
        case .ok: text = ""
        }
        defaults.removeObject(forKey: rememberedKey)
        if let p = peripheral { central?.cancelPeripheralConnection(p) }
        peripheral = nil
        state = .refused(text)
    }

    // MARK: - Outbound (one write in flight at a time)

    private func enqueue(_ message: LinkMessage) {
        guard let p = peripheral, downlink != nil else { return }
        let maxChunk = max(20, p.maximumWriteValueLength(for: .withResponse))
        guard let chunks = try? outbound.chunks(for: message, maxChunkBytes: maxChunk) else { return }
        writeQueue.append(contentsOf: chunks.map { Data($0) })
        pumpWrites()
    }

    private func pumpWrites() {
        guard !writing, let p = peripheral, let ch = downlink, !writeQueue.isEmpty else { return }
        writing = true
        p.writeValue(writeQueue.removeFirst(), for: ch, type: .withResponse)
    }

    // MARK: - Helpers

    private var rememberedID: UUID? { defaults.string(forKey: rememberedKey).flatMap(UUID.init(uuidString:)) }

    private func connect(_ p: CBPeripheral) {
        p.delegate = self
        peripheral = p
        state = .connecting
        if p.state == .connected, let c = central {
            // State restoration can hand us a peripheral that is ALREADY connected: iOS will not call
            // didConnect again, so restart the handshake ourselves.
            centralManager(c, didConnect: p)
        } else {
            central?.connect(p, options: nil)   // pending connect: completes whenever the watch is in range
        }
    }

    private func startScan() {
        candidates.removeAll(); pickWork?.cancel(); pickWork = nil
        state = .scanning
        central?.scanForPeripherals(withServices: [serviceUUID], options: nil)
        scanTimeoutWork?.cancel()
        let work = DispatchWorkItem { [weak self] in
            guard let self = self, self.pairingMode, self.peripheral == nil else { return }
            self.central?.stopScan()
            self.pairingMode = false
            self.state = .refused("No watch found. On the watch, tap Pair iPhone, then try again.")
        }
        scanTimeoutWork = work
        DispatchQueue.main.asyncAfter(deadline: .now() + Self.pairingScanSeconds, execute: work)
    }

    private func pickNearestCandidate() {
        pickWork = nil
        guard pairingMode, let best = candidates.values.max(by: { $0.rssi < $1.rssi }) else { return }
        central?.stopScan()
        scanTimeoutWork?.cancel()
        pairingMode = false
        candidates.removeAll()
        connect(best.peripheral)
    }

    private func retryAfterDelay() {
        guard let p = peripheral else { return }
        DispatchQueue.main.asyncAfter(deadline: .now() + 5) { [weak self] in
            guard let self = self, self.peripheral === p else { return }
            self.connect(p)
        }
    }

    private func fail(_ message: String) {
        if let p = peripheral { central?.cancelPeripheralConnection(p) }
        state = .refused(message)
    }

    private func resetConnectionState() {
        uplink = nil; downlink = nil; info = nil
        writeQueue.removeAll(); writing = false
        established = false
        inbound.reset()
    }
}
