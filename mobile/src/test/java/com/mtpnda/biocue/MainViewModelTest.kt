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
class MainViewModelTest {

    // 1. Dependencies clearly grouped
    private val application = mockk<Application>(relaxed = true)
    private val db = mockk<DetectionDatabase>(relaxed = true)
    private val eventDao = mockk<EventDao>(relaxed = true)
    private val sensorDataDao = mockk<SensorDataDao>(relaxed = true)
    
    private val testDispatcher = StandardTestDispatcher()
    
    // Subject under test
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Setup mock behavior
        every { db.eventDao() } returns eventDao
        every { db.sensorDataDao() } returns sensorDataDao
        
        // Initialize clean ViewModel for each test
        viewModel = MainViewModel(application, db)
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
    fun `toggleMockStream follows sequential state transitions`() = runTest {
        // Define the test data structure (The "Table" Schema)
        data class TestCase(
            val stepDescription: String,
            val performToggle: Boolean,
            val expectedState: Boolean
        )
        
        // Define the test cases (The "Table" Data)
        val testTable = listOf(
            TestCase("Initial state should be false", performToggle = false, expectedState = false),
            TestCase("First toggle turns stream ON", performToggle = true, expectedState = true),
            TestCase("Second toggle turns stream OFF", performToggle = true, expectedState = false),
            TestCase("Third toggle turns stream ON again", performToggle = true, expectedState = true)
        )

        // Iterate through the table and execute assertions
        testTable.forEach { case ->
            if (case.performToggle) {
                viewModel.setMockIntensity(application, com.mtpnda.biocue.MockIntensity.HIGH)
            }
            
            assertEquals(
                case.stepDescription, 
                case.expectedState, 
                viewModel.mockIntensity.value != com.mtpnda.biocue.MockIntensity.OFF
            )
        }
    }
}