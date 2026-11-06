package com.mtpnda.biocue

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

class AlertManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val tier1Channel = NotificationChannel(
                "tier1_alerts",
                "Early Warning Alerts",
                NotificationManager.IMPORTANCE_LOW
            )
            val tier2Channel = NotificationChannel(
                "tier2_alerts",
                "Escalation Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(tier1Channel)
            notificationManager.createNotificationChannel(tier2Channel)
        }
    }

    fun triggerTier1Alert(score: Double) {
        val notification = NotificationCompat.Builder(context, "tier1_alerts")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Elevated Stress Signals")
            .setContentText("Stress Level: ${((score / 3.0) * 100).toInt().coerceIn(0, 100)}%. Wearer may need a break.")
            .build()
        notificationManager.notify(1, notification)
    }

    fun triggerTier2Alert(score: Double) {
        val notification = NotificationCompat.Builder(context, "tier2_alerts")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Sustained Distress Pattern")
            .setContentText("Stress Level: ${((score / 3.0) * 100).toInt().coerceIn(0, 100)}%. Caregiver attention required immediately.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager.notify(2, notification)
    }
}
