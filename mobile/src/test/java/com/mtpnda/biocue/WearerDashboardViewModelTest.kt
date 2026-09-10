package com.mtpnda.biocue

import android.app.Application
import com.mtpnda.biocue.core.data.local.DetectionDatabase
import com.mtpnda.biocue.core.data.local.EventDao
import com.mtpnda.biocue.core.data.local.SensorDataDao
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WearerDashboardViewModelTest {

    // 1. Dependencies clearly grouped
    private val application = mockk<Application>(relaxed = true)
    private val db = mockk<DetectionDatabase>(relaxed = true)
    private val eventDao = mockk<EventDao>(relaxed = true)
    private val sensorDataDao = mockk<SensorDataDao>(relaxed = true)
    
    private val testDispatcher = StandardTestDispatcher()
    
    // Subject under test
    private lateinit var viewModel: WearerDashboardViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        io.mockk.mockkStatic(com.google.firebase.firestore.FirebaseFirestore::class)
        every { com.google.firebase.firestore.FirebaseFirestore.getInstance() } returns mockk(relaxed = true)
        
        io.mockk.mockkStatic(com.google.firebase.auth.FirebaseAuth::class)
        every { com.google.firebase.auth.FirebaseAuth.getInstance() } returns mockk(relaxed = true)
        
        // Setup mock behavior
        every { db.eventDao() } returns eventDao
        every { db.sensorDataDao() } returns sensorDataDao
        
        // Initialize clean ViewModel for each test
        viewModel = WearerDashboardViewModel(application, db)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    /**
     * Table-Driven Test pattern in Kotlin.
     * 
     * Instead of duplicating assertions, we define a "Table" of inputs and expected outputs.
     * This is incredibly scalable and clean for Android/Kotlin development.
     */
    @Test
    fun `setMockIntensity updates state correctly`() = runTest {
        // Initial state
        assertEquals(com.mtpnda.biocue.MockIntensity.OFF, viewModel.mockIntensity.value)

        // Set to HIGH
        viewModel.setMockIntensity(application, com.mtpnda.biocue.MockIntensity.HIGH)
        assertEquals(com.mtpnda.biocue.MockIntensity.HIGH, viewModel.mockIntensity.value)

        // Set to MEDIUM
        viewModel.setMockIntensity(application, com.mtpnda.biocue.MockIntensity.MEDIUM)
        assertEquals(com.mtpnda.biocue.MockIntensity.MEDIUM, viewModel.mockIntensity.value)
        
        // Set to OFF
        viewModel.setMockIntensity(application, com.mtpnda.biocue.MockIntensity.OFF)
        assertEquals(com.mtpnda.biocue.MockIntensity.OFF, viewModel.mockIntensity.value)
    }
}