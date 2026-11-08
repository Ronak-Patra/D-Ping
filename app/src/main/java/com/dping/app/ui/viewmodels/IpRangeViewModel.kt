package com.dping.app.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dping.app.network.discovery.NetworkInfoProvider
import com.dping.app.network.iprange.IpOccupancyStatus
import com.dping.app.network.iprange.IpRangeScanner
import com.dping.app.network.iprange.IpScanItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class IpFilterOption {
    ALL, OCCUPIED, FREE
}

data class IpRangeUiState(
    val startIp: String = "192.168.1.1",
    val endIp: String = "192.168.1.254",
    val cidr: String = "192.168.1.0/24",
    val isRangeMode: Boolean = true,
    val isScanning: Boolean = false,
    val scannedCount: Int = 0,
    val totalCount: Int = 0,
    val occupiedCount: Int = 0,
    val freeCount: Int = 0,
    val items: List<IpScanItem> = emptyList(),
    val filter: IpFilterOption = IpFilterOption.ALL,
    val statusMessage: String = "Ready to scan IP range"
)

class IpRangeViewModel(
    context: Context,
    private val scanner: IpRangeScanner = IpRangeScanner()
) : ViewModel() {

    private val networkInfoProvider = NetworkInfoProvider(context)
    private val _uiState = MutableStateFlow(IpRangeUiState())
    val uiState: StateFlow<IpRangeUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    init {
        detectSubnetRange()
    }

    fun detectSubnetRange() {
        viewModelScope.launch {
            val myIp = networkInfoProvider.getMyIpInfo()
            if (myIp.wifiIp != "0.0.0.0" && myIp.wifiIp.isNotBlank()) {
                val parts = myIp.wifiIp.split(".")
                if (parts.size == 4) {
                    val subnetBase = "${parts[0]}.${parts[1]}.${parts[2]}"
                    _uiState.update {
                        it.copy(
                            startIp = "$subnetBase.1",
                            endIp = "$subnetBase.254",
                            cidr = "$subnetBase.0/24"
                        )
                    }
                }
            }
        }
    }

    fun setScanMode(isRange: Boolean) {
        _uiState.update { it.copy(isRangeMode = isRange) }
    }

    fun onStartIpChange(newIp: String) {
        _uiState.update { it.copy(startIp = newIp) }
    }

    fun onEndIpChange(newIp: String) {
        _uiState.update { it.copy(endIp = newIp) }
    }

    fun onBaseIpChange(newIp: String) {
        _uiState.update { state ->
            val prefix = state.cidr.split("/").getOrNull(1)?.toIntOrNull() ?: 24
            val calc = com.dping.app.utils.SubnetCalculator.calculate(newIp, prefix)
            val octets = newIp.split(".")
            val netBase = if (octets.size == 4) "${octets[0]}.${octets[1]}.${octets[2]}" else "192.168.1"
            state.copy(
                cidr = "$newIp/$prefix",
                startIp = calc?.firstUsableHost ?: "$netBase.1",
                endIp = calc?.lastUsableHost ?: "$netBase.254"
            )
        }
    }

    fun onPrefixChange(prefix: Int) {
        _uiState.update { state ->
            val parts = state.cidr.trim().split("/")
            val baseIp = if (parts.isNotEmpty()) parts[0] else "192.168.1.0"
            val octets = baseIp.split(".")
            val netBase = if (octets.size == 4) "${octets[0]}.${octets[1]}.${octets[2]}" else "192.168.1"
            val calc = com.dping.app.utils.SubnetCalculator.calculate(baseIp, prefix)
            state.copy(
                cidr = "$baseIp/$prefix",
                startIp = calc?.firstUsableHost ?: "$netBase.1",
                endIp = calc?.lastUsableHost ?: "$netBase.254"
            )
        }
    }

    fun onCidrChange(newCidr: String) {
        _uiState.update { state ->
            val parts = newCidr.trim().split("/")
            if (parts.size == 2) {
                val octets = parts[0].split(".")
                if (octets.size == 4) {
                    val prefix = "${octets[0]}.${octets[1]}.${octets[2]}"
                    state.copy(
                        cidr = newCidr,
                        startIp = "$prefix.1",
                        endIp = "$prefix.254"
                    )
                } else {
                    state.copy(cidr = newCidr)
                }
            } else {
                state.copy(cidr = newCidr)
            }
        }
    }

    fun onFilterChange(newFilter: IpFilterOption) {
        _uiState.update { it.copy(filter = newFilter) }
    }

    fun startScan() {
        stopScan()

        val start = _uiState.value.startIp.trim()
        val end = _uiState.value.endIp.trim()

        _uiState.update {
            it.copy(
                isScanning = true,
                scannedCount = 0,
                occupiedCount = 0,
                freeCount = 0,
                items = emptyList(),
                statusMessage = "Scanning $start to $end..."
            )
        }

        val scanList = mutableListOf<IpScanItem>()

        scanJob = viewModelScope.launch {
            scanner.scanRange(start, end).collect { progress ->
                progress.latestItem?.let { newItem ->
                    scanList.add(newItem)
                }

                _uiState.update { state ->
                    state.copy(
                        scannedCount = progress.scannedIps,
                        totalCount = progress.totalIps,
                        occupiedCount = progress.occupiedCount,
                        freeCount = progress.freeCount,
                        items = scanList.toList(),
                        isScanning = !progress.isFinished,
                        statusMessage = if (progress.isFinished) {
                            "Scan completed! ${progress.occupiedCount} occupied, ${progress.freeCount} free."
                        } else {
                            "Scanning... ${progress.scannedIps}/${progress.totalIps} hosts inspected"
                        }
                    )
                }
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _uiState.update {
            it.copy(
                isScanning = false,
                statusMessage = "Scan stopped by user"
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }
}
