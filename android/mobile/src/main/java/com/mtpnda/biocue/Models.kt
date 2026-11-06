package com.mtpnda.biocue

enum class ConnectionStatus {
    SEARCHING, CONNECTED, DISCONNECTED
}

enum class MockIntensity {
    OFF, LOW, MEDIUM, HIGH
}

data class JournalEntry(
    val time: String,
    val place: String,
    val activity: String,
    val labelIcon: String,
    val labelText: String,
    val colorHex: String
)

data class ReviewEvent(
    val eventId: String,
    val time: String,
    val triggerType: String,
    val currentLabel: String,
    val cues: List<Pair<String, String>>
)
