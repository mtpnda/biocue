package com.mtpnda.biocue.wear

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.mtpnda.biocue.wear.location.LocationTracker
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService

class PingResponderService : WearableListenerService() {
    private val scope = CoroutineScope(Dispatchers.IO)
    companion object {
        private const val TAG = "PingResponderService"
        private const val PATH_PING = "/ping"
        private const val PATH_PONG = "/pong"
        private const val PATH_SOS_GPS_REQUEST = "/sos_gps_request"
    }

    override fun onMessageReceived(event: MessageEvent) {
        Log.d(TAG, "onMessageReceived: path=${event.path} from node=${event.sourceNodeId}")
        if (event.path == PATH_SOS_GPS_REQUEST) {
            // Spec 03: exactly ONE high-accuracy fix, then reply on /sos_location. (Previously this request was ignored.)
            scope.launch {
                val fix = LocationTracker(this@PingResponderService).getCurrentLocation()
                if (fix != null) WearDataSender(this@PingResponderService).sendSosLocation(fix.latitude, fix.longitude)
                else Log.w(TAG, "SOS GPS request: no fix available")
            }
        }
        if (event.path == PATH_PING) {
            Wearable.getMessageClient(this)
                .sendMessage(event.sourceNodeId, PATH_PONG, ByteArray(0))
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully sent /pong reply to ${event.sourceNodeId}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to send /pong reply to ${event.sourceNodeId}", e)
                }
        }
    }
}
