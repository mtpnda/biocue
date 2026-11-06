package com.mtpnda.biocue.connect

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

object EmulatorWatchBridge {
    private const val TAG = "EmulatorWatchBridge"
    const val PORT = 8765
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null

    fun start(context: Context, scope: CoroutineScope, onMessage: (JSONObject) -> Unit) {
        if (serverJob != null) return
        serverJob = scope.launch(Dispatchers.IO) {
            try {
                val ss = ServerSocket(PORT)
                serverSocket = ss
                Log.i(TAG, "EmulatorWatchBridge listening on port $PORT")
                while (true) {
                    val socket = ss.accept()
                    launch(Dispatchers.IO) {
                        handleClient(socket, onMessage)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "EmulatorWatchBridge server stopped: ${e.message}")
            }
        }
    }

    private fun handleClient(socket: Socket, onMessage: (JSONObject) -> Unit) {
        try {
            socket.use { s ->
                val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                val writer = PrintWriter(s.getOutputStream(), true)
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line?.trim() ?: continue
                    if (trimmed.isEmpty()) continue
                    if (trimmed == "PING") {
                        writer.println("PONG")
                        continue
                    }
                    val json = JSONObject(trimmed)
                    onMessage(json)
                    writer.println("OK")
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Client socket closed: ${e.message}")
        }
    }

    fun stop() {
        runCatching { serverSocket?.close() }
        serverJob?.cancel()
        serverJob = null
        serverSocket = null
    }
}
