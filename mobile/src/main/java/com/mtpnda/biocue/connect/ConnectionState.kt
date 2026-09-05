package com.mtpnda.biocue.connect

enum class ConnectionState {
    CHECKING,        // running detection
    NOT_PAIRED,      // phone sees no watch node at all
    APP_MISSING,     // watch paired, but our wear app absent / not advertising
    NO_TRANSPORT,    // app present, but ping got no pong (rare: GMS glitch)
    CONNECTED,       // everything works
}
