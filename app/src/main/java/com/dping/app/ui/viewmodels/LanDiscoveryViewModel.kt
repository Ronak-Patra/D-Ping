package com.dping.app.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dping.app.network.discovery.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LanDiscoveryUiState(
    val networkInfo: LanNetworkInfo = LanNetworkInfo(),
    val devices: List<LanDevice> = emptyList(),
    val isScanning: Boolean = false,
    val scannedCount: Int = 0,
    val totalCount: Int = 254,
    val filter: LanDeviceFilter = LanDeviceFilter.ALL,
    val searchQuery: String = "",
    val statusMessage: String = "Ready to discover LAN devices",
    val isRangeMode: Boolean = false,
    val startIp: String = "192.168.1.1",
    val endIp: String = "192.168.1.50"
)

class LanDiscoveryViewModel(
    context: Context,
    private val scanner: LanDiscoveryScanner = LanDiscoveryScanner()
) : ViewModel() {

    private val networkInfoProvider = NetworkInfoProvider(context)
    private val _uiState = MutableStateFlow(LanDiscoveryUiState())
    val uiState: StateFlow<LanDiscoveryUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    init {
        initNetworkAndStart()
    }

    fun initNetworkAndStart() {
        viewModelScope.launch {
            val myIp = networkInfoProvider.getMyIpInfo()
            val gw = networkInfoProvider.getGatewayInfo()

            val localIp = if (myIp.wifiIp != "0.0.0.0") myIp.wifiIp else "192.168.1.100"
            val gatewayIp = if (gw.gatewayIp != "Unavailable") gw.gatewayIp else {
                val parts = localIp.split(".")
                if (parts.size == 4) "${parts[0]}.${parts[1]}.${parts[2]}.1" else "192.168.1.1"
            }

            val parts = localIp.split(".")
            val base = if (parts.size == 4) "${parts[0]}.${parts[1]}.${parts[2]}" else "192.168.1"

            _uiState.update {
                it.copy(
                    networkInfo = LanNetworkInfo(
                        ssid = myIp.connectionType,
                        localIp = localIp,
                        gatewayIp = gatewayIp
                    ),
                    startIp = "$base.1",
                    endIp = "$base.50"
                )
            }

            startScan()
        }
    }

    fun setScanMode(isRange: Boolean) {
        _uiState.update { it.copy(isRangeMode = isRange) }
    }

    fun onStartIpChange(ip: String) {
        _uiState.update { it.copy(startIp = ip) }
    }

    fun onEndIpChange(ip: String) {
        _uiState.update { it.copy(endIp = ip) }
    }

    fun onSearchChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onFilterChange(filter: LanDeviceFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun startScan() {
        stopScan()

        val netInfo = _uiState.value.networkInfo
        val parts = netInfo.localIp.split(".")
        val subnetBase = if (parts.size == 4) "${parts[0]}.${parts[1]}.${parts[2]}" else "192.168.1"
        val isRange = _uiState.value.isRangeMode
        val startIp = _uiState.value.startIp.trim()
        val endIp = _uiState.value.endIp.trim()

        _uiState.update {
            it.copy(
                isScanning = true,
                scannedCount = 0,
                devices = emptyList(),
                statusMessage = if (isRange) "Scanning IP range $startIp - $endIp..." else "Scanning subnet $subnetBase.0/24..."
            )
        }

        val deviceList = mutableListOf<LanDevice>()

        scanJob = viewModelScope.launch {
            val scanFlow = if (isRange) {
                scanner.scanRange(
                    startIpStr = startIp,
                    endIpStr = endIp,
                    localIp = netInfo.localIp,
                    gatewayIp = netInfo.gatewayIp
                )
            } else {
                scanner.scanLan(
                    subnetBase = subnetBase,
                    localIp = netInfo.localIp,
                    gatewayIp = netInfo.gatewayIp
                )
            }

            scanFlow.collect { progress ->
                progress.deviceFound?.let { device ->
                    val existingIdx = deviceList.indexOfFirst { it.ip == device.ip }
                    if (existingIdx != -1) {
                        deviceList[existingIdx] = device
                    } else {
                        deviceList.add(device)
                    }
                    // Sort: Gateways first, then current device, then by IP
                    deviceList.sortWith(
                        compareByDescending<LanDevice> { it.isGateway }
                            .thenByDescending { it.isCurrentDevice }
                            .thenBy { ipToLong(it.ip) }
                    )
                }

                _uiState.update { current ->
                    current.copy(
                        scannedCount = progress.scannedIps,
                        totalCount = progress.totalIps,
                        devices = deviceList.toList(),
                        isScanning = !progress.isFinished,
                        statusMessage = if (progress.isFinished) {
                            "Scan complete: ${deviceList.size} devices found"
                        } else {
                            "Scanned ${progress.scannedIps}/${progress.totalIps} hosts (${deviceList.size} devices found)"
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
                statusMessage = "Scan paused (${it.devices.size} devices found)"
            )
        }
    }

    private fun ipToLong(ip: String): Long {
        return try {
            val parts = ip.split(".").map { it.toLong() }
            (parts[0] shl 24) + (parts[1] shl 16) + (parts[2] shl 8) + parts[3]
        } catch (_: Exception) {
            0L
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }
}
