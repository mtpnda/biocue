package com.mtpnda.biocue.core.link

/**
 * BioCue Link v1 — constants shared by every implementation of the watch<->phone
 * BLE link (Wear OS GATT server in :wear, CoreBluetooth central in the iOS app).
 *
 * Why this exists: Google's Wearable Data Layer API only connects Wear OS watches
 * to *Android* phones. It does not work with iPhones, so an iPhone link needs a
 * transport of our own. The logical messages mirror [com.mtpnda.biocue.core.domain.model.DataLayerSchema]
 * 1:1 so the phone-side pipeline does not care which transport delivered them.
 *
 * Full spec: docs/WATCH_PHONE_CONNECTION.md (the byte layouts below are normative).
 */
object LinkProtocol {
    /** Highest frame/handshake version this build speaks. */
    const val VERSION: Int = 1

    // ---- GATT layout (128-bit UUIDs, lowercase) -------------------------------
    const val SERVICE_UUID = "7a1c0001-8f3b-4c5e-9a52-b10c0e000001"
    /** watch -> phone, NOTIFY. Carries chunked frames. */
    const val UPLINK_UUID = "7a1c0002-8f3b-4c5e-9a52-b10c0e000001"
    /** phone -> watch, WRITE (with response). Carries chunked frames. */
    const val DOWNLINK_UUID = "7a1c0003-8f3b-4c5e-9a52-b10c0e000001"
    /** READ. 2 bytes: [maxVersion u8][flags u8]; flag bit0 = pairing window open. */
    const val INFO_UUID = "7a1c0004-8f3b-4c5e-9a52-b10c0e000001"
    /** Standard Client Characteristic Configuration Descriptor. */
    const val CCCD_UUID = "00002902-0000-1000-8000-00805f9b34fb"

    // ---- Framing --------------------------------------------------------------
    /** [ver u8][type u8][seq u16][payloadLen u16] */
    const val FRAME_HEADER_BYTES = 6
    /** Upper bound on one reassembled frame. Anything larger is dropped. */
    const val MAX_FRAME_BYTES = 1024
    /** Strings on the wire are [len u8][utf8]. */
    const val MAX_STRING_BYTES = 255

    /** Chunk header flags (1 byte at the start of every BLE write/notification). */
    const val CHUNK_FIRST = 0x01
    const val CHUNK_LAST = 0x02

    // ---- Message type codes ---------------------------------------------------
    // 0x01..0x7F = watch -> phone (uplink); 0x81..0xFF = phone -> watch (downlink)
    const val T_SENSOR_WINDOW = 0x01
    const val T_EVENT_START = 0x02
    const val T_EVENT_LABEL = 0x03
    const val T_EVENT_END = 0x04
    const val T_SOS_LOCATION = 0x05
    const val T_HELLO_ACK = 0x06
    const val T_PONG = 0x07

    const val T_HELLO = 0x81
    const val T_ACK = 0x82
    const val T_PING = 0x83
    const val T_SOS_GPS_REQUEST = 0x84

    // ---- Delivery ------------------------------------------------------------
    /** Max unacknowledged critical frames the watch keeps (oldest dropped first). */
    const val CRITICAL_OUTBOX_CAP = 64
    /** Watch drops the link if no HELLO arrives within this long after connect. */
    const val HELLO_TIMEOUT_MS = 10_000L
    /** How long "pairing mode" accepts a first-time (unbonded) phone. */
    const val PAIRING_WINDOW_MS = 120_000L
}
