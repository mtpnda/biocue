package com.mtpnda.biocue

import android.app.Application
import com.mtpnda.biocue.connect.WatchConnectionManager
import com.mtpnda.biocue.core.data.local.DetectionDatabase
import com.mtpnda.biocue.core.data.local.EventDao
import com.mtpnda.biocue.core.data.local.SensorDataDao
import com.mtpnda.biocue.core.data.local.StressEventEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.content.Context

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MainViewModelTest {

    private val application = mockk<Application>(relaxed = true)
    private val db = mockk<DetectionDatabase>(relaxed = true)
    
    private val eventDao = mockk<EventDao>(relaxed = true)
    private val sensorDataDao = mockk<SensorDataDao>(relaxed = true)
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { db.eventDao() } returns eventDao
        every { db.sensorDataDao() } returns sensorDataDao
        
        // Mock connection state
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testToggleMockStream() = runTest {
        val viewModel = MainViewModel(application, db)
        
        // Initially false
        assertFalse(viewModel.isMockingStream.value)
        
        // Toggle on
        viewModel.toggleMockStream(application)
        assertTrue(viewModel.isMockingStream.value)
        
        // Toggle off
        viewModel.toggleMockStream(application)
        assertFalse(viewModel.isMockingStream.value)
    }
}