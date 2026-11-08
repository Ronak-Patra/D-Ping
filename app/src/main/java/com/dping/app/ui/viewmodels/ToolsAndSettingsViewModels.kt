package com.dping.app.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dping.app.data.preferences.AppPreferences
import com.dping.app.network.discovery.GatewayInfo
import com.dping.app.network.discovery.InterfaceDetails
import com.dping.app.network.discovery.NetworkInfoProvider
import com.dping.app.network.dns.DnsQueryResult
import com.dping.app.network.dns.DnsService
import com.dping.app.network.http.HttpCheckResult
import com.dping.app.network.http.HttpChecker
import com.dping.app.network.http.TlsCertInfo
import com.dping.app.network.http.TlsInspector
import com.dping.app.network.tcp.TcpCheckResult
import com.dping.app.network.tcp.TcpPortChecker
import com.dping.app.ui.theme.AppThemeMode
import com.dping.app.utils.SubnetCalculator
import com.dping.app.utils.SubnetResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ToolsUiState(
    // DNS
    val dnsTarget: String = "google.com",
    val dnsRecordType: String = "A",
    val dnsResult: DnsQueryResult? = null,
    val isDnsLoading: Boolean = false,

    // TCP Port
    val tcpHost: String = "google.com",
    val tcpPort: String = "443",
    val tcpResult: TcpCheckResult? = null,
    val isTcpLoading: Boolean = false,

    // HTTP
    val httpUrl: String = "https://google.com",
    val httpResult: HttpCheckResult? = null,
    val isHttpLoading: Boolean = false,

    // TLS
    val tlsHost: String = "google.com",
    val tlsPort: String = "443",
    val tlsResult: TlsCertInfo? = null,
    val isTlsLoading: Boolean = false,

    // Subnet Calculator
    val subnetIp: String = "192.168.1.100",
    val subnetPrefix: Int = 24,
    val subnetResult: SubnetResult? = SubnetCalculator.calculate("192.168.1.100", 24),

    // Gateway
    val gatewayInfo: GatewayInfo? = null,
    val isGatewayLoading: Boolean = false,

    // Interfaces
    val interfaces: List<InterfaceDetails> = emptyList()
)

class ToolsViewModel(
    context: Context,
    private val dnsService: DnsService = DnsService(),
    private val tcpChecker: TcpPortChecker = TcpPortChecker(),
    private val httpChecker: HttpChecker = HttpChecker(),
    private val tlsInspector: TlsInspector = TlsInspector(),
    private val networkInfoProvider: NetworkInfoProvider = NetworkInfoProvider(context)
) : ViewModel() {

    private val _uiState = MutableStateFlow(ToolsUiState())
    val uiState: StateFlow<ToolsUiState> = _uiState.asStateFlow()

    fun onDnsTargetChange(newTarget: String) { _uiState.update { it.copy(dnsTarget = newTarget) } }
    fun onDnsTypeChange(newType: String) { _uiState.update { it.copy(dnsRecordType = newType) } }

    fun runDnsQuery(isReverse: Boolean = false) {
        val target = _uiState.value.dnsTarget.trim()
        if (target.isBlank()) return

        _uiState.update { it.copy(isDnsLoading = true) }
        viewModelScope.launch {
            val res = if (isReverse) {
                dnsService.reverseDns(target)
            } else {
                dnsService.queryDns(target, _uiState.value.dnsRecordType)
            }
            _uiState.update { it.copy(dnsResult = res, isDnsLoading = false) }
        }
    }

    fun onTcpHostChange(newHost: String) { _uiState.update { it.copy(tcpHost = newHost) } }
    fun onTcpPortChange(newPort: String) { _uiState.update { it.copy(tcpPort = newPort.filter { c -> c.isDigit() }) } }

    fun runTcpCheck() {
        val host = _uiState.value.tcpHost.trim()
        val port = _uiState.value.tcpPort.toIntOrNull() ?: 80
        if (host.isBlank()) return

        _uiState.update { it.copy(isTcpLoading = true) }
        viewModelScope.launch {
            val res = tcpChecker.checkPort(host, port)
            _uiState.update { it.copy(tcpResult = res, isTcpLoading = false) }
        }
    }

    fun onHttpUrlChange(newUrl: String) { _uiState.update { it.copy(httpUrl = newUrl) } }

    fun runHttpCheck() {
        val url = _uiState.value.httpUrl.trim()
        if (url.isBlank()) return

        _uiState.update { it.copy(isHttpLoading = true) }
        viewModelScope.launch {
            val res = httpChecker.checkUrl(url)
            _uiState.update { it.copy(httpResult = res, isHttpLoading = false) }
        }
    }

    fun onTlsHostChange(newHost: String) { _uiState.update { it.copy(tlsHost = newHost) } }
    fun onTlsPortChange(newPort: String) { _uiState.update { it.copy(tlsPort = newPort.filter { c -> c.isDigit() }) } }

    fun runTlsCheck() {
        val host = _uiState.value.tlsHost.trim()
        val port = _uiState.value.tlsPort.toIntOrNull() ?: 443
        if (host.isBlank()) return

        _uiState.update { it.copy(isTlsLoading = true) }
        viewModelScope.launch {
            val res = tlsInspector.inspectCertificate(host, port)
            _uiState.update { it.copy(tlsResult = res, isTlsLoading = false) }
        }
    }

    fun onSubnetIpChange(newIp: String) {
        _uiState.update {
            val calc = SubnetCalculator.calculate(newIp, it.subnetPrefix)
            it.copy(subnetIp = newIp, subnetResult = calc)
        }
    }

    fun onSubnetPrefixChange(newPrefix: Int) {
        _uiState.update {
            val calc = SubnetCalculator.calculate(it.subnetIp, newPrefix)
            it.copy(subnetPrefix = newPrefix, subnetResult = calc)
        }
    }

    fun refreshGateway() {
        _uiState.update { it.copy(isGatewayLoading = true) }
        viewModelScope.launch {
            val gw = networkInfoProvider.getGatewayInfo()
            _uiState.update { it.copy(gatewayInfo = gw, isGatewayLoading = false) }
        }
    }

    fun refreshInterfaces() {
        viewModelScope.launch {
            val ifaces = networkInfoProvider.getAllInterfaces()
            _uiState.update { it.copy(interfaces = ifaces) }
        }
    }
}

data class SettingsUiState(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val newestFirst: Boolean = true,
    val selectedLanguage: String = "en",
    val packetSize: Int = 64,
    val intervalMs: Long = 1000L,
    val resolveHostname: Boolean = false,
    val keepRunningBg: Boolean = false
)

class SettingsViewModel(
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.themeModeFlow.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            preferences.newestFirstFlow.collect { newest ->
                _uiState.update { it.copy(newestFirst = newest) }
            }
        }
        viewModelScope.launch {
            preferences.languageFlow.collect { lang ->
                _uiState.update { it.copy(selectedLanguage = lang) }
            }
        }
        viewModelScope.launch {
            preferences.packetSizeFlow.collect { size ->
                _uiState.update { it.copy(packetSize = size) }
            }
        }
        viewModelScope.launch {
            preferences.intervalMsFlow.collect { interval ->
                _uiState.update { it.copy(intervalMs = interval) }
            }
        }
        viewModelScope.launch {
            preferences.resolveHostnameFlow.collect { resolve ->
                _uiState.update { it.copy(resolveHostname = resolve) }
            }
        }
        viewModelScope.launch {
            preferences.keepRunningBgFlow.collect { keep ->
                _uiState.update { it.copy(keepRunningBg = keep) }
            }
        }
    }

    fun setTheme(mode: AppThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setNewestFirst(newestFirst: Boolean) {
        viewModelScope.launch { preferences.setNewestFirst(newestFirst) }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch { preferences.setLanguage(lang) }
    }

    fun setPacketSize(size: Int) {
        viewModelScope.launch { preferences.setPacketSize(size) }
    }

    fun setIntervalMs(interval: Long) {
        viewModelScope.launch { preferences.setIntervalMs(interval) }
    }

    fun setResolveHostname(resolve: Boolean) {
        viewModelScope.launch { preferences.setResolveHostname(resolve) }
    }

    fun setKeepRunningBg(keep: Boolean) {
        viewModelScope.launch { preferences.setKeepRunningBg(keep) }
    }

    fun resetDefaults() {
        viewModelScope.launch { preferences.resetDefaults() }
    }
}
