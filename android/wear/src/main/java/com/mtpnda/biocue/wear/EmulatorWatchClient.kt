package com.mtpnda.biocue.wear

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

object EmulatorWatchClient {
    private const val TAG = "EmulatorWatchClient"
    private const val HOST = "10.0.2.2"
    private const val PORT = 8765
    private const val TIMEOUT_MS = 600

    fun isEmulator(): Boolean {
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

    suspend fun send(json: JSONObject): Boolean = withContext(Dispatchers.IO) {
        if (!isEmulator()) return@withContext false
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(HOST, PORT), TIMEOUT_MS)
            socket.soTimeout = TIMEOUT_MS
            val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
            writer.println(json.toString())
            writer.flush()
            socket.close()
            Log.d(TAG, "Synced payload to emulator companion: ${json.optString("type")}")
            true
        } catch (e: Exception) {
            Log.d(TAG, "Emulator socket not reachable (${e.message})")
            false
        }
    }
}
