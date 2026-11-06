package com.mtpnda.biocue.mockemitter

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.mtpnda.biocue.core.domain.model.DataLayerSchema
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MockEmitterEngineTest {

    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        context = spyk(ApplicationProvider.getApplicationContext<Context>())
    }

    @Test
    fun `scenarios generate accurate physiological sensor windows`() {
        val calm = ScenarioType.CALM.toSensorWindow(jitter = false)
        assertEquals(70.0, calm.hrMean, 0.01)
        assertEquals(55.0, calm.rmssd, 0.01)
        assertFalse(calm.isExercising)
        assertFalse(calm.isOffBody)

        val overwhelmed = ScenarioType.OVERWHELMED.toSensorWindow(jitter = false)
        assertEquals(128.0, overwhelmed.hrMean, 0.01)
        assertEquals(16.0, overwhelmed.rmssd, 0.01)
        assertEquals(0.65, overwhelmed.repetitiveness, 0.01)
        assertFalse(overwhelmed.isExercising)

        val exercise = ScenarioType.EXERCISING.toSensorWindow(jitter = false)
        assertTrue(exercise.isExercising)
        assertEquals(140.0, exercise.hrMean, 0.01)

        val offBody = ScenarioType.OFF_BODY.toSensorWindow(jitter = false)
        assertTrue(offBody.isOffBody)
        assertEquals(0.0, offBody.hrMean, 0.01)
    }

    @Test
    fun `emitSensorWindow sends intent broadcast with full schema`() = runTest(testDispatcher) {
        val engine = MockEmitterEngine(context, testScope)
        val window = ScenarioType.CALM.toSensorWindow(jitter = false)

        val slot = slot<Intent>()
        every { context.sendBroadcast(capture(slot)) } just Runs

        engine.emitSensorWindow(window)

        verify { context.sendBroadcast(any()) }
        val capturedIntent = slot.captured
        assertEquals("com.mtpnda.biocue.MOCK_SENSOR_WINDOW", capturedIntent.action)
        assertEquals("com.mtpnda.biocue", capturedIntent.`package`)
        assertEquals(70.0, capturedIntent.getDoubleExtra("hrMean", 0.0), 0.01)
        assertEquals(55.0, capturedIntent.getDoubleExtra("rmssd", 0.0), 0.01)
        assertEquals(0.05, capturedIntent.getDoubleExtra("accMag", 0.0), 0.01)
        assertFalse(capturedIntent.getBooleanExtra("isOffBody", true))
        assertFalse(capturedIntent.getBooleanExtra("isExercising", true))
    }

    @Test
    fun `emitEventStart sends manual event broadcast`() = runTest(testDispatcher) {
        val engine = MockEmitterEngine(context, testScope)
        val slot = slot<Intent>()
        every { context.sendBroadcast(capture(slot)) } just Runs

        val eventId = engine.emitEventStart(isManual = true)

        assertNotNull(eventId)
        verify { context.sendBroadcast(any()) }
        val captured = slot.captured
        assertEquals("com.mtpnda.biocue.MOCK_MANUAL_EVENT", captured.action)
        assertEquals("MANUAL", captured.getStringExtra("trigger"))
    }

    @Test
    fun `setWatchTestMode broadcasts SET_TEST_MODE to wear app`() {
        val engine = MockEmitterEngine(context, testScope)
        val slot = slot<Intent>()
        every { context.sendBroadcast(capture(slot)) } just Runs

        engine.setWatchTestMode(true)

        verify { context.sendBroadcast(any()) }
        val captured = slot.captured
        assertEquals("com.mtpnda.biocue.wear.SET_TEST_MODE", captured.action)
        assertEquals("com.mtpnda.biocue", captured.`package`)
        assertTrue(captured.getBooleanExtra("enabled", false))
    }
}
