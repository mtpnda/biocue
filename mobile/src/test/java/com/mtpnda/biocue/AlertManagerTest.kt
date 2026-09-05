package com.mtpnda.biocue

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AlertManagerTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var alertManager: AlertManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        alertManager = AlertManager(context)
    }

    @Test
    fun triggerTier1Alert_createsNotification() {
        alertManager.triggerTier1Alert(1.8)

        val shadowNm = shadowOf(notificationManager)
        val notification = shadowNm.getNotification(1)
        assertEquals("Elevated Stress Signals", notification.extras.getCharSequence("android.title"))
    }

    @Test
    fun triggerTier2Alert_createsHighPriorityNotification() {
        alertManager.triggerTier2Alert(2.8)

        val shadowNm = shadowOf(notificationManager)
        val notification = shadowNm.getNotification(2)
        assertEquals("Sustained Distress Pattern", notification.extras.getCharSequence("android.title"))
    }
}
