package com.dping.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dping.app.data.model.DiagnosticRecord
import com.dping.app.data.repository.DiagnosticRepository
import com.dping.app.network.speedtest.SpeedTestEngine
import com.dping.app.network.speedtest.SpeedTestPhase
import com.dping.app.network.speedtest.SpeedTestProgress
import com.dping.app.network.speedtest.SpeedTestProvider
import com.dping.app.network.speedtest.SpeedTestProviders
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SpeedTestViewModel(
    private val repository: DiagnosticRepository,
    private val engine: SpeedTestEngine = SpeedTestEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SpeedTestProgress(
            selectedProvider = SpeedTestProviders.DEFAULT,
            serverName = SpeedTestProviders.DEFAULT.name,
            serverLocation = SpeedTestProviders.DEFAULT.location
        )
    )
    val uiState: StateFlow<SpeedTestProgress> = _uiState.asStateFlow()

    private var testJob: Job? = null

    fun selectProvider(provider: SpeedTestProvider) {
        if (_uiState.value.phase == SpeedTestPhase.DOWNLOAD || _uiState.value.phase == SpeedTestPhase.UPLOAD) {
            return
        }
        _uiState.update {
            it.copy(
                selectedProvider = provider,
                serverName = provider.name,
                serverLocation = provider.location
            )
        }
    }

    fun startTest() {
        stopTest()

        val provider = _uiState.value.selectedProvider
        val history = mutableListOf<Float>()
        var peakSpeed = 0.0

        testJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            var finalProgress = SpeedTestProgress()

            engine.startSpeedTest(provider).collect { progress ->
                if (progress.currentSpeedMbps > 0.0) {
                    history.add(progress.currentSpeedMbps.toFloat())
                    if (history.size > 50) history.removeAt(0)
                    if (progress.currentSpeedMbps > peakSpeed) {
                        peakSpeed = progress.currentSpeedMbps
                    }
                }

                finalProgress = progress
                _uiState.update {
                    progress.copy(
                        speedHistory = history.toList(),
                        peakSpeedMbps = peakSpeed,
                        selectedProvider = provider,
                        serverName = progress.serverName,
                        serverLocation = progress.serverLocation,
                        connectionType = "Wi-Fi (5GHz) - Active"
                    )
                }
            }

            if (finalProgress.phase == SpeedTestPhase.COMPLETE) {
                val summary = String.format(
                    java.util.Locale.US,
                    "Down: %.1f Mbps | Up: %.1f Mbps | Ping: %.1f ms",
                    finalProgress.downloadSpeedMbps,
                    finalProgress.uploadSpeedMbps,
                    finalProgress.pingMs
                )
                repository.saveRecord(
                    DiagnosticRecord(
                        testType = "Speed Test",
                        target = "${finalProgress.serverName} (${finalProgress.serverLocation})",
                        timestamp = startTime,
                        durationMs = System.currentTimeMillis() - startTime,
                        isSuccess = true,
                        summary = summary
                    )
                )
            }
        }
    }

    fun stopTest() {
        testJob?.cancel()
        testJob = null
        _uiState.update {
            it.copy(
                phase = SpeedTestPhase.IDLE,
                currentSpeedMbps = 0.0,
                statusMessage = "Test stopped"
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTest()
    }
}
