# 08. Watch ↔ Phone Connection: Architecture & Protocol

> **Added by the spec merge.** Docs 01 and 03 define the Android path only (Wearable Data Layer, `/sos_gps_request` → `/sos_location`). This document **extends** them with the iPhone path (BioCue Link v1 over Bluetooth LE) and keeps the same logical messages, so the phone-side pipeline is unchanged. Nothing in 01/03 is replaced.
> Constraint inherited from 01 / 04: the watch UI never shows raw vitals; the link screens added to the watch (phone-type choice, "Pair iPhone") show **status text only**.

Status: **v1, implemented, not yet validated on real hardware** (see §10).
Normative for: `core-detection/.../core/link/*` (Kotlin), `wear/.../wear/link/*` (watch), `ios/Packages/BioCueCore/.../Link/*` and `ios/BioCue/Sources/Wearer/WatchLinkCentral.swift` (iPhone).

---

## 1. Why there are two transports

| | Android phone | iPhone |
|---|---|---|
| Transport | **Wearable Data Layer** (Google Play services) | **BioCue Link v1** over Bluetooth LE |
| Watch role | Data Layer node | BLE GATT **server** (peripheral) |
| Phone role | Data Layer node | BLE **central** (CoreBluetooth) |
| Store-and-forward | Provided by Play services (DataItems sync when reconnected) | **We implement it** (watch outbox + ACK) |
| Phone-side code | `WatchDataReceiverService` | `WatchLinkCentral` + `WearerService` |

**The Data Layer cannot reach iOS.** Google's documentation states that it is available only between Wear OS watches and paired Android devices, and that it does not work when a Wear OS watch is paired with an iPhone; for that case Google points developers at cloud APIs. So an iPhone needs a transport of our own.

**Why Bluetooth LE rather than the cloud-relay Google suggests.** The app promises that heart data and movement never leave the wearer's phone. A cloud relay would put raw vitals on a server, on every window, to reach a phone sitting next to the watch. It would also need watch internet access, add latency and cost, and stop working offline. BLE keeps the data on the two devices. The cost is that we own the protocol, security and delivery guarantees — which is what the rest of this document specifies.

**The transports are interchangeable at the message level.** Both carry the same five logical messages (`DataLayerSchema` ⇄ `LinkMessage`). The phone pipeline (`DetectionPipeline`) does not know or care which transport delivered a window.

### One phone at a time
The watch talks to exactly one *kind* of phone, chosen on the watch (`PhoneTarget = ANDROID | IPHONE`, persisted in `SharedPreferences`). If both an Android phone and an iPhone received the same stream, both would record the same events. Selecting one disables the other path. The default is `ANDROID`, so existing installs are unchanged.

---

## 2. Component map

```
 WATCH (Wear OS)                                              IPHONE (wearer's)
 ┌──────────────────────────────┐                        ┌───────────────────────────────┐
 │ SensorCollectionService      │                        │ WearerService (@MainActor)    │
 │   └─ WearDataSender ─────────┼─ PhoneTarget=ANDROID ─►│  (Android: WatchDataReceiver  │
 │        │  (router)           │   Data Layer           │   Service — separate device)  │
 │        └─ PhoneTarget=IPHONE │                        │   ▲ frames (AsyncStream)      │
 │             ▼                │                        │   │ ack(seq) after persisting │
 │  BleLinkServer               │   BLE GATT             │ WatchLinkCentral (CoreBluet.) │
 │   ├ GATT server + advertiser │◄══ BioCue Link v1 ════►│   ├ scan/connect/restore      │
 │   ├ chunk + flow control     │   encrypted + MITM     │   ├ handshake, HELLO(role)    │
 │   └ WatchLinkSession  ◄──────┼── pure state machine   │   └ LinkInbound / LinkOutbound│
 │        └ LinkOutbox (64)     │   (unit-tested)        │       (BioCueCore, pure Swift)│
 └──────────────────────────────┘                        └───────────────────────────────┘
        core-detection/link (Kotlin, pure)  ⇄  byte-identical  ⇄  BioCueCore/Link (Swift, pure)
```

Everything that decides *what the bytes mean* is pure code with no platform dependencies (`LinkCodec`, `LinkFraming`, `LinkOutbox`, `WatchLinkSession`, and the Swift twins). Everything that touches Bluetooth (`BleLinkServer`, `WatchLinkCentral`) is a thin shell around it.

---

## 3. GATT layout

Service UUID `7a1c0001-8f3b-4c5e-9a52-b10c0e000001` (custom 128-bit).

| Characteristic | UUID suffix | Properties | Carries |
|---|---|---|---|
| **Uplink** | `…0002…` | NOTIFY | watch → phone chunked frames |
| **Downlink** | `…0003…` | WRITE (with response) | phone → watch chunked frames |
| **Info** | `…0004…` | READ | 2 bytes: `[maxVersion u8][flags u8]`, flags bit0 = pairing window open |

Every characteristic and the Uplink's CCCD descriptor require an **encrypted, MITM-protected** link (`PERMISSION_*_ENCRYPTED_MITM`). The advertisement contains only the service UUID: no device name, no manufacturer data.

---

## 4. Wire format

All multi-byte integers are **big-endian**. `str` = `[len u8][UTF-8 bytes]` (max 255 bytes).

### 4.1 Frame (one application message)

```
[ver u8 = 1][type u8][seq u16][payloadLen u16][payload …]
```

A decoder MUST reject: frames shorter than 6 bytes; `ver != 1`; `payloadLen` that disagrees with the actual size; payloads shorter than the type requires; an unknown `HELLO` role or `HELLO_ACK` status. An unknown **type** with a well-formed frame decodes as `Unknown(type)` and is ignored (forward compatibility). Maximum frame size 1024 bytes.

### 4.2 Message types

| Type | Name | Dir | Payload | Critical |
|---|---|---|---|---|
| `0x01` | SENSOR_WINDOW | ↑ | `i64 ts, f64 hrMean, f64 rmssd, f64 accMag, f64 repetitiveness, f64 hrSlope, u8 flags` (bit0 offBody, bit1 exercising) — 49 B | no |
| `0x02` | EVENT_START | ↑ | `i64 ts, u8 trigger (0 manual / 1 auto), str eventId` | **yes** |
| `0x03` | EVENT_LABEL | ↑ | `i64 ts, str eventId, str label` | **yes** |
| `0x04` | EVENT_END | ↑ | `i64 ts, str eventId` | **yes** |
| `0x05` | SOS_LOCATION | ↑ | `i64 ts, f64 lat, f64 lng` — 24 B | **yes** |
| `0x06` | HELLO_ACK | ↑ | `u8 version, u8 status, str watchName, u8 battery (255 = unknown), i64 watchTimeMs` | no |
| `0x07` | PONG | ↑ | empty | no |
| `0x81` | HELLO | ↓ | `u8 maxVersion, u8 role (0 wearer, 1 caregiver)` | — |
| `0x82` | ACK | ↓ | `u16 ackedSeq` | — |
| `0x83` | PING | ↓ | empty | — |
| `0x84` | SOS_GPS_REQUEST | ↓ | empty | — |

`HELLO_ACK.status`: `0 OK`, `1 ROLE_NOT_ALLOWED`, `2 VERSION_UNSUPPORTED`, `3 NOT_IN_PAIRING_WINDOW`.

### 4.2b One clock

Events and windows are stamped by the **watch's** clock, but "is this event still ongoing?" (15-minute rule) compares them with the **phone's** "now". A watch clock off by more than a few minutes silently broke that rule (a 20-minute-slow watch made a 14-minute-old event look 34 minutes old, so a duplicate event opened). `HELLO_ACK.watchTimeMs` carries the watch's wall clock at the moment it answered; the phone computes `offset = phoneNow − watchTimeMs` once per connection and converts **every** watch timestamp with `WatchClock.toPhoneTime` (Kotlin and Swift twins, same numbers in both test suites). The one-way Bluetooth delay (tens of ms) is ignored deliberately.

*Android Data Layer path:* there is no handshake to measure an offset, and delayed DataItem delivery makes "stamp on receipt" wrong for events. Instead `ClockSkewEstimator` (core, 4 tests) estimates the skew from the **prompt telemetry windows**: for each, `receipt − watchTimestamp = skew + deliveryDelay`, and a delay is never negative, so the **smallest recent value** is the best estimate (the idea NTP uses); only the last 9 samples count so a corrected/drifting clock is followed. The receiver feeds it on every window and converts window, event-start and event-end timestamps. *Wired in `WatchDataReceiverService`; written, not built or run.* Until the first window arrives the offset is 0 (no correction).

### 4.3 Chunking (fitting a frame into BLE packets)

A notification/write carries at most **ATT MTU − 3** bytes — **20 bytes at the default MTU of 23**. A 55-byte sensor-window frame therefore needs three chunks at the default MTU. Each chunk is `[flags u8][data …]`; flags bit0 = FIRST, bit1 = LAST (a frame that fits has both = `0x03`).

The receiver keeps one reassembler per direction per connection. A FIRST chunk always resets it, so a lost tail cannot poison the next frame. Chunks that arrive with no FIRST, empty chunks, and frames that exceed 1024 bytes are dropped and counted, never crash. One corrupt frame never takes the link down.

The watch sizes chunks from the **negotiated MTU** (`onMtuChanged`, never below 20). The phone sizes writes from `maximumWriteValueLength(for: .withResponse)`.

---

## 5. Delivery semantics

| Class | Messages | Guarantee |
|---|---|---|
| Telemetry | SENSOR_WINDOW | **Latest-wins, best-effort.** Not queued while the phone is away; not acknowledged. |
| Critical | EVENT_START/LABEL/END, SOS_LOCATION | **At-least-once.** Retained in the watch **outbox** until the phone sends `ACK(seq)`; replayed after the next successful handshake with their **original bytes and seq**. |

* **Outbox:** capacity **64** frames; when full the *oldest* is dropped (and counted). It is **saved to a small file on the watch** (atomic write, CRC-checked) and reloaded at start, so a watch reboot does not lose un-ACKed events; sequence numbers continue after a restore (never reused). A corrupt or unreadable file starts empty instead of crashing, and a failed save never loses the in-memory event.
* **The phone ACKs only after the event is durably stored locally** and must not wait for any cloud write (Firestore queues writes offline, but awaiting one blocks until the server confirms).
* **Receivers must be idempotent**, because replays are normal. The iOS rules (`EventIngest`): a duplicate start for a known id is ignored; a label for an already-resolved event is ignored (a stale replay must not reopen a finished review); a second end never moves the end time. Duplicates are still ACKed so the watch can forget them.
* Sequence numbers are 16-bit and wrap; the outbox is ≤ 64 so there is no ambiguity.

---

## 6. Connection lifecycle

```
phone                                   watch
  │  scan (new watch) / connect (remembered)
  │ ───────────────── connect ────────────►  onConnectionStateChange
  │                                          · refuse if unbonded AND pairing window closed (before any pairing prompt)
  │                                          · refuse a 2nd phone while one is connected
  │  discover service + characteristics
  │  READ Info ───────────────────────────►  first protected access → OS pairing (numeric comparison)
  │  enable NOTIFY on Uplink ─────────────►  CCCD write (also protected)
  │  WRITE Downlink: HELLO(1, WEARER) ────►  WatchLinkSession.onHello
  │ ◄──────────── HELLO_ACK(OK, name, batt)  state = ESTABLISHED; pairing window closes (single use)
  │ ◄──────────── replay of un-ACKed critical frames, in original order
  │ ◄──────────── SENSOR_WINDOW … (streaming)
  │  ACK(seq) ──────────────────────────►    outbox.ack(seq)
```

* **HELLO timeout:** the watch drops a connection that has not sent HELLO within **10 s**.
* **Reconnect:** the iPhone asks iOS for a *pending* connect to the remembered watch, which completes whenever the watch is next in range — no polling, no timeout. The watch restarts advertising on disconnect.
* **iOS background:** `UIBackgroundModes = bluetooth-central` plus CoreBluetooth **state restoration** (`com.mtpnda.biocue.central`) lets iOS deliver notifications to a suspended app and relaunch it after termination. A **user force-quit** stops this until the app is opened again (an iOS rule, not ours).
* **Restored-while-connected:** iOS does not call `didConnect` for an already-connected restored peripheral, so the handshake is restarted explicitly.

---

## 7. Security model

1. **OS-level pairing, not custom crypto.** All attributes require LE Secure Connections with MITM protection (numeric comparison): the watch and the iPhone both display a 6-digit number and the wearer confirms on both. Until that succeeds, a phone can neither read nor write anything.
2. **Pairing window.** A phone that is not already bonded is disconnected on connect *before any pairing prompt* unless the wearer opened the window on the watch ("Pair iPhone", **120 s**, single use — it closes when the first handshake succeeds).
3. **The iPhone only ever auto-connects to the one remembered watch.** Scanning for a *new* watch happens only when the user taps "Connect watch"; if several watches answer, the strongest signal wins after a 2 s settle, and the OS confirmation is still required.
4. **Role enforcement at the protocol level.** The phone's first message declares its role. A **CAREGIVER** gets `HELLO_ACK(ROLE_NOT_ALLOWED)` and is disconnected; it never receives telemetry, events or pongs (tested). On iOS the `WatchLinkCentral` type cannot even be *constructed* for a caregiver (`RoleGate` throws). This is defence in depth: a caregiver's phone has no reason to hold a link to raw vitals.
5. **Privacy of the advertisement.** No name, no manufacturer data — and the service UUID **only while the 120 s pairing window is open** (`AdvertisePolicy`). Once bonded the watch advertises without it: the iPhone reconnects to the remembered watch by identity, so a passer-by cannot tell that a BioCue watch is nearby. *Unverified on hardware: that iOS really reconnects without the UUID, and that an empty advertisement payload is accepted.*
6. **Data at rest (iPhone).** Events and windows are stored in Application Support, excluded from backup, with file protection `completeUntilFirstUserAuthentication`. Windows are tied to the owning account and are not shown to, or kept for, a different account that signs in later. Retention: 7 days / 5,000 windows.

**Not provided:** application-layer encryption or signing on top of BLE security; replay protection beyond OS link encryption; forward secrecy beyond what LE Secure Connections gives.

---

## 8. Failure modes

| Situation | Behaviour |
|---|---|
| Bluetooth off / permission denied / no BLE | Watch: `BluetoothOff` / `NoPermission` / `Unsupported` shown on the watch. iPhone: `.bluetoothOff` / `.unauthorized` / `.unsupported` with an *Open Settings* action. Bluetooth turning back on restarts the link automatically. |
| Phone out of range | Telemetry dropped; critical frames wait in the outbox (≤ 64, oldest evicted); replayed on reconnect. |
| User cancels the pairing prompt | iPhone shows "Pairing was cancelled or failed"; nothing is remembered. |
| Caregiver account tries to link | `ROLE_NOT_ALLOWED`; watch shows "That is a caregiver account"; iPhone never constructs the link. |
| Version mismatch | `VERSION_UNSUPPORTED`; both sides tell the user to update. |
| Corrupt/garbled bytes | Counted and dropped; link stays up. |
| Notification queue busy | Watch retries every 50 ms; after 20 consecutive failures it drops the queue rather than spin. |
| Watch reboot | Un-ACKed critical frames are reloaded from the outbox file. |
| Both phones configured | Prevented: one `PhoneTarget` at a time. |

---

## 9. Verification

| What | How | Result |
|---|---|---|
| Wire format | Independent **Python reference** (`tools/gen_link_vectors.py`) generates 17 message vectors + chunk vectors for both languages | Kotlin: pass. Swift: written, **not run** |
| Kotlin protocol + session logic | `./gradlew :core-detection:test` — also compiled/run with a stand-alone Kotlin 2.0.20 + JUnit during development | **60 pure-JVM tests pass** (link, session, pipeline, roles + existing clusterer/scoring tests); mutation-checked (deliberately breaking codec/framing is caught) |
| Role enforcement on the link | `WatchLinkSessionTest` (caregiver refused, never receives data) | pass |
| Detection parity Android ⇄ iOS | `tools/gen_parity_fixtures.sh` runs the **real Android Kotlin** (scoring, clustering, resolve logic, icons, pipeline) on ~740 cases and emits Swift fixtures | fixtures generated; Swift side **not run** |
| Watch BLE code | Type-checked against a real `android.jar` (API 34) | compiles; mutation-checked; **never executed on a device** |
| Swift | Syntax-parsed only (no Swift toolchain was available) | **not compiled** |

---

## 10. Open risks — validate before relying on this

1. **Hardware (most important).** Nothing here has run on a Galaxy Watch 4 Classic and an iPhone. Confirm: (a) the watch can advertise and act as a GATT server alongside its Galaxy Wearable connection; (b) the watch's Bluetooth stack negotiates **numeric comparison** (if it only offers "Just Works", the MITM-protected attributes will refuse access and the link will never establish — fall back by relaxing to `PERMISSION_*_ENCRYPTED` *and* accepting a weaker pairing, a deliberate security decision); (c) the advertisement is visible to iOS; (d) the MTU negotiated.
2. **Watch setup.** Galaxy Watches are set up with an Android phone. Google and Samsung do not support iPhone pairing for them; a third-party tool (Merge) claims to bridge it, which was not verified. BioCue's link does not depend on OS-level pairing, but the watch must first be set up *somehow*. An iPhone-only household may not be able to do that.
3. **Outbox file wear / real-reboot behaviour** is untested (the storage logic is unit-tested on a JVM).
4. **iOS force-quit** stops background delivery until the app is reopened.
5. **Battery.** Continuous advertising (low-power mode) while the phone is away, plus streaming windows. Not measured.
6. **A cloud relay remains a fallback** if (1) fails. It would send derived data only if the privacy promise is to be kept, and would still need the watch to have internet.

---

## 11. Changing the protocol

1. Edit **`tools/gen_link_vectors.py`** first (it is the executable spec).
2. `python3 tools/gen_link_vectors.py` regenerates `LinkGoldenVectors.kt` and `LinkGoldenVectors.swift`.
3. Make `LinkCodec.kt` and `LinkCodec.swift` pass them. Bump `LinkProtocol.VERSION` for incompatible changes.
4. Detection logic changes: update the Kotlin, then `tools/gen_parity_fixtures.sh` (needs `kotlinc`) and make the Swift port pass.
