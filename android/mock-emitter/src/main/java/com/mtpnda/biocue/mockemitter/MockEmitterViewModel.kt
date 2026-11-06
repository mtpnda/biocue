package com.mtpnda.biocue.mockemitter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mtpnda.biocue.core.domain.model.SensorWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MockEmitterViewModel(application: Application) : AndroidViewModel(application) {

    val engine = MockEmitterEngine(application, viewModelScope)

    private val _selectedScenario = MutableStateFlow(ScenarioType.CALM)
    val selectedScenario: StateFlow<ScenarioType> = _selectedScenario.asStateFlow()

    // Editable parameters for Custom or modified presets
    val customHr = MutableStateFlow(72f)
    val customHrv = MutableStateFlow(50f)
    val customMotion = MutableStateFlow(0.08f)
    val customRepetitiveness = MutableStateFlow(0.05f)
    val customSlope = MutableStateFlow(0.0f)
    val customIsOffBody = MutableStateFlow(false)
    val customIsExercising = MutableStateFlow(false)

    private val _activeEventId = MutableStateFlow<String?>(null)
    val activeEventId: StateFlow<String?> = _activeEventId.asStateFlow()

    private val _watchTestMode = MutableStateFlow(false)
    val watchTestMode: StateFlow<Boolean> = _watchTestMode.asStateFlow()

    fun selectScenario(scenario: ScenarioType) {
        _selectedScenario.value = scenario
        if (scenario != ScenarioType.CUSTOM) {
            customHr.value = scenario.hrMean.toFloat()
            customHrv.value = scenario.rmssd.toFloat()
            customMotion.value = scenario.accMag.toFloat()
            customRepetitiveness.value = scenario.repetitiveness.toFloat()
            customSlope.value = scenario.hrSlope.toFloat()
            customIsOffBody.value = scenario.isOffBody
            customIsExercising.value = scenario.isExercising
        }
    }

    fun getCurrentWindow(jitter: Boolean = true): SensorWindow {
        val scenario = _selectedScenario.value
        return if (scenario == ScenarioType.CUSTOM) {
            val hrJitter = if (jitter && !customIsOffBody.value) (Math.random() * 2.0 - 1.0) else 0.0
            val rmssJitter = if (jitter && !customIsOffBody.value) (Math.random() * 1.5 - 0.75) else 0.0
            SensorWindow(
                timestamp = System.currentTimeMillis(),
                hrMean = (customHr.value.toDouble() + hrJitter).coerceAtLeast(0.0),
                rmssd = (customHrv.value.toDouble() + rmssJitter).coerceAtLeast(0.0),
                accMag = customMotion.value.toDouble(),
                repetitiveness = customRepetitiveness.value.toDouble(),
                hrSlope = customSlope.value.toDouble(),
                isOffBody = customIsOffBody.value,
                isExercising = customIsExercising.value
            )
        } else {
            scenario.toSensorWindow(jitter)
        }
    }

    fun emitSingleShot() {
        viewModelScope.launch {
            engine.emitSensorWindow(getCurrentWindow(jitter = false))
        }
    }

    fun toggleStreaming() {
        if (engine.isStreaming.value) {
            engine.stopStreaming()
        } else {
            engine.startStreaming { getCurrentWindow(jitter = true) }
        }
    }

    fun setStreamInterval(seconds: Int) {
        engine.setInterval(seconds * 1000L)
    }

    fun triggerEventStart(isManual: Boolean = true) {
        viewModelScope.launch {
            val id = engine.emitEventStart(isManual = isManual)
            _activeEventId.value = id
        }
    }

    fun triggerEventLabel(label: String) {
        val id = _activeEventId.value ?: return
        viewModelScope.launch {
            engine.emitEventLabel(id, label)
        }
    }

    fun triggerEventEnd() {
        val id = _activeEventId.value ?: return
        viewModelScope.launch {
            engine.emitEventEnd(id)
            _activeEventId.value = null
        }
    }

    fun toggleWatchTestMode() {
        val next = !_watchTestMode.value
        _watchTestMode.value = next
        engine.setWatchTestMode(next)
    }
}
