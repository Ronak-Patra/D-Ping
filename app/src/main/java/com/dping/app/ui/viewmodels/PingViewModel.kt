package com.dping.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dping.app.data.model.DiagnosticRecord
import com.dping.app.data.model.RecentTarget
import com.dping.app.data.repository.DiagnosticRepository
import com.dping.app.network.ping.PingEngine
import com.dping.app.network.ping.PingPacket
import com.dping.app.network.ping.PingPacketStatus
import com.dping.app.network.ping.PingStats
import com.dping.app.network.traceroute.HopInfo
import com.dping.app.network.traceroute.TracerouteEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PingUiState(
    val target: String = "8.8.8.8",
    val packetSize: String = "64",
    val intervalMs: String = "1000",
    val resolveHostname: Boolean = false,
    val keepRunningBg: Boolean = false,
    val newestFirst: Boolean = true,
    val isRunning: Boolean = false,
    val isTracerouteRunning: Boolean = false,
    val activeTab: String = "PING", // "PING" or "TRACEROUTE"
    val packets: List<PingPacket> = emptyList(),
    val hops: List<HopInfo> = emptyList(),
    val stats: PingStats = PingStats(),
    val recentTargets: List<RecentTarget> = emptyList(),
    val favourites: List<com.dping.app.data.model.FavouriteHost> = emptyList(),
    val statusMessage: String = ""
)

class PingViewModel(
    private val repository: DiagnosticRepository,
    private val pingEngine: PingEngine = PingEngine(),
    private val tracerouteEngine: TracerouteEngine = TracerouteEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PingUiState())
    val uiState: StateFlow<PingUiState> = _uiState.asStateFlow()

    private var pingJob: Job? = null
    private var tracerouteJob: Job? = null
    private var testStartTime = 0L

    init {
        viewModelScope.launch {
            repository.getRecentTargets().collect { recents ->
                _uiState.update { it.copy(recentTargets = recents) }
            }
        }
        viewModelScope.launch {
            repository.getFavourites().collect { favs ->
                _uiState.update { it.copy(favourites = favs) }
            }
        }
        viewModelScope.launch {
            repository.preferences.packetSizeFlow.collect { size ->
                _uiState.update { it.copy(packetSize = size.toString()) }
            }
        }
        viewModelScope.launch {
            repository.preferences.intervalMsFlow.collect { interval ->
                _uiState.update { it.copy(intervalMs = interval.toString()) }
            }
        }
        viewModelScope.launch {
            repository.preferences.resolveHostnameFlow.collect { resolve ->
                _uiState.update { it.copy(resolveHostname = resolve) }
            }
        }
        viewModelScope.launch {
            repository.preferences.newestFirstFlow.collect { newest ->
                _uiState.update { it.copy(newestFirst = newest) }
            }
        }
    }

    fun onTargetChange(newTarget: String) {
        _uiState.update { it.copy(target = newTarget) }
    }

    fun onPacketSizeChange(newSize: String) {
        _uiState.update { it.copy(packetSize = newSize.filter { c -> c.isDigit() }) }
    }

    fun onIntervalChange(newInterval: String) {
        _uiState.update { it.copy(intervalMs = newInterval.filter { c -> c.isDigit() }) }
    }

    fun onResolveHostnameToggle(enabled: Boolean) {
        _uiState.update { it.copy(resolveHostname = enabled) }
        viewModelScope.launch { repository.preferences.setResolveHostname(enabled) }
    }

    fun onKeepRunningBgToggle(enabled: Boolean) {
        _uiState.update { it.copy(keepRunningBg = enabled) }
        viewModelScope.launch { repository.preferences.setKeepRunningBg(enabled) }
    }

    fun resetSettings() {
        _uiState.update {
            it.copy(
                packetSize = "64",
                intervalMs = "1000",
                resolveHostname = false,
                keepRunningBg = false
            )
        }
        viewModelScope.launch { repository.preferences.resetDefaults() }
    }

    fun startPing(presetCount: Int = 0) {
        stopAllTests()

        val target = _uiState.value.target.trim()
        if (target.isBlank()) return

        val packetSizeInt = _uiState.value.packetSize.toIntOrNull() ?: 64
        val intervalMsLong = _uiState.value.intervalMs.toLongOrNull() ?: 1000L

        _uiState.update {
            it.copy(
                isRunning = true,
                activeTab = "PING",
                packets = emptyList(),
                stats = PingStats(target = target, isRunning = true),
                statusMessage = "Testing connection to $target..."
            )
        }

        testStartTime = System.currentTimeMillis()
        viewModelScope.launch { repository.recordTargetUsed(target) }

        val rttList = mutableListOf<Double>()
        var sent = 0
        var received = 0

        pingJob = viewModelScope.launch {
            pingEngine.startPing(
                target = target,
                packetSize = packetSizeInt,
                intervalMs = intervalMsLong,
                count = presetCount,
                resolveHostname = _uiState.value.resolveHostname
            ).collect { packet ->
                sent++
                if (packet.status == PingPacketStatus.SUCCESS) {
                    received++
                    rttList.add(packet.rttMs)
                }

                val lost = sent - received
                val lossPercent = if (sent > 0) (lost.toDouble() / sent) * 100.0 else 0.0
                val minRtt = if (rttList.isNotEmpty()) rttList.minOrNull() ?: 0.0 else 0.0
                val maxRtt = if (rttList.isNotEmpty()) rttList.maxOrNull() ?: 0.0 else 0.0
                val avgRtt = if (rttList.isNotEmpty()) rttList.average() else 0.0
                val mdevRtt = if (rttList.size >= 2) {
                    val mean = avgRtt
                    Math.sqrt(rttList.map { (it - mean) * (it - mean) }.average())
                } else 0.0

                _uiState.update { state ->
                    state.copy(
                        packets = state.packets + packet,
                        stats = PingStats(
                            sent = sent,
                            received = received,
                            lost = lost,
                            lossPercentage = lossPercent,
                            minRtt = minRtt,
                            avgRtt = avgRtt,
                            maxRtt = maxRtt,
                            mdevRtt = mdevRtt,
                            isRunning = true,
                            target = target
                        )
                    )
                }
            }

            // Finished test
            onPingFinished(target, sent, received, rttList)
        }
    }

    private fun onPingFinished(target: String, sent: Int, received: Int, rttList: List<Double>) {
        val duration = System.currentTimeMillis() - testStartTime
        val lost = sent - received
        val lossPercent = if (sent > 0) (lost.toDouble() / sent) * 100.0 else 0.0
        val avgRtt = if (rttList.isNotEmpty()) rttList.average() else 0.0

        _uiState.update {
            it.copy(
                isRunning = false,
                stats = it.stats.copy(isRunning = false),
                statusMessage = "Ping test completed. $received/$sent packets received."
            )
        }

        if (sent > 0) {
            val summary = String.format(
                java.util.Locale.US,
                "Sent: %d, Loss: %.1f%%, Avg: %.1fms",
                sent, lossPercent, avgRtt
            )
            viewModelScope.launch {
                repository.saveRecord(
                    DiagnosticRecord(
                        testType = "Ping",
                        target = target,
                        timestamp = testStartTime,
                        durationMs = duration,
                        isSuccess = received > 0,
                        summary = summary
                    )
                )
            }
        }
    }

    fun startTraceroute() {
        stopAllTests()

        val target = _uiState.value.target.trim()
        if (target.isBlank()) return

        _uiState.update {
            it.copy(
                isTracerouteRunning = true,
                activeTab = "TRACEROUTE",
                hops = emptyList(),
                statusMessage = "Tracing route to $target..."
            )
        }

        testStartTime = System.currentTimeMillis()
        viewModelScope.launch { repository.recordTargetUsed(target) }

        tracerouteJob = viewModelScope.launch {
            val hopList = mutableListOf<HopInfo>()
            var reachedDestination = false

            tracerouteEngine.startTraceroute(target).collect { hop ->
                hopList.add(hop)
                if (hop.isDestinationReached) {
                    reachedDestination = true
                }
                _uiState.update { state -> state.copy(hops = hopList.toList()) }
            }

            val status = if (reachedDestination) {
                "Traceroute complete: reached $target in ${hopList.size} hops."
            } else {
                "Traceroute complete: $target is unreachable (${hopList.size} hops probed)."
            }

            _uiState.update {
                it.copy(
                    isTracerouteRunning = false,
                    statusMessage = status
                )
            }

            val duration = System.currentTimeMillis() - testStartTime
            repository.saveRecord(
                DiagnosticRecord(
                    testType = "Traceroute",
                    target = target,
                    timestamp = testStartTime,
                    durationMs = duration,
                    isSuccess = reachedDestination,
                    summary = if (reachedDestination) "Reached in ${hopList.size} hops" else "Target unreachable (${hopList.size} hops)"
                )
            )
        }
    }

    fun addFavourite(name: String, host: String, description: String = "") {
        viewModelScope.launch {
            repository.addFavourite(
                com.dping.app.data.model.FavouriteHost(
                    name = name.ifBlank { host },
                    host = host.trim(),
                    description = description
                )
            )
        }
    }

    fun deleteFavourite(id: Long) {
        viewModelScope.launch {
            repository.deleteFavourite(id)
        }
    }

    fun stopAllTests() {
        pingJob?.cancel()
        tracerouteJob?.cancel()
        pingJob = null
        tracerouteJob = null
        _uiState.update {
            it.copy(
                isRunning = false,
                isTracerouteRunning = false,
                stats = it.stats.copy(isRunning = false)
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAllTests()
    }
}
