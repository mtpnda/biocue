package com.mtpnda.biocue

import android.content.Context
import com.mtpnda.biocue.wear.WearDataSender
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class WearDataSenderTest {

    private lateinit var mockContext: Context
    private lateinit var mockDataClient: DataClient
    private lateinit var mockNodeClient: NodeClient

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockDataClient = mockk(relaxed = true)
        mockNodeClient = mockk(relaxed = true)
        
        mockkStatic(Wearable::class)
        every { Wearable.getDataClient(any<Context>()) } returns mockDataClient
        every { Wearable.getNodeClient(any<Context>()) } returns mockNodeClient

        val mockNode = mockk<Node>(relaxed = true)
        every { mockNodeClient.connectedNodes } returns Tasks.forResult(listOf(mockNode))
    }

    @Test
    fun sendSensorWindow_putsDataItem() = runBlocking {
        val sender = WearDataSender(mockContext)
        val mockDataItem = mockk<DataItem>()
        
        every { mockDataClient.putDataItem(any()) } returns Tasks.forResult(mockDataItem)

        sender.sendSensorWindow(
            hrMean = 85.0,
            rmssd = 40.0,
            accMag = 0.5,
            repetitiveness = 0.8,
            hrSlope = 2.0,
            isOffBody = false,
            isExercising = false
        )

        // Verify that putDataItem was called
        verify { mockDataClient.putDataItem(any()) }
    }

    @Test
    fun sendEventStart_putsDataItem() = runBlocking {
        val sender = WearDataSender(mockContext)
        val mockDataItem = mockk<DataItem>()

        every { mockDataClient.putDataItem(any()) } returns Tasks.forResult(mockDataItem)

        val result = sender.sendEventStart("event_001", isManual = true)

        verify { mockDataClient.putDataItem(any()) }
        org.junit.Assert.assertTrue(result)
    }

    @Test
    fun sendEventLabel_putsDataItem() = runBlocking {
        val sender = WearDataSender(mockContext)
        val mockDataItem = mockk<DataItem>()

        every { mockDataClient.putDataItem(any()) } returns Tasks.forResult(mockDataItem)

        val result = sender.sendEventLabel("event_001", "OVERWHELMED")

        verify { mockDataClient.putDataItem(any()) }
        org.junit.Assert.assertTrue(result)
    }

    @Test
    fun sendEventEnd_putsDataItem() = runBlocking {
        val sender = WearDataSender(mockContext)
        val mockDataItem = mockk<DataItem>()

        every { mockDataClient.putDataItem(any()) } returns Tasks.forResult(mockDataItem)

        val result = sender.sendEventEnd("event_001")

        verify { mockDataClient.putDataItem(any()) }
        org.junit.Assert.assertTrue(result)
    }
}
