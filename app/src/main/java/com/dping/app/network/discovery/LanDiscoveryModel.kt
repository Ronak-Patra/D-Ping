package com.dping.app.network.discovery

enum class LanDeviceType {
    ROUTER,
    PHONE,
    COMPUTER,
    PRINTER,
    SMART_TV_IOT,
    NAS_STORAGE,
    GENERIC
}

data class LanDevice(
    val ip: String,
    val hostname: String = "",
    val macAddress: String? = null,
    val vendor: String? = null,
    val latencyMs: Double = 0.0,
    val openServices: List<String> = emptyList(),
    val deviceType: LanDeviceType = LanDeviceType.GENERIC,
    val isGateway: Boolean = false,
    val isCurrentDevice: Boolean = false,
    val isOnline: Boolean = true
)

data class LanNetworkInfo(
    val ssid: String = "Wi-Fi Network",
    val localIp: String = "0.0.0.0",
    val gatewayIp: String = "0.0.0.0",
    val subnetMask: String = "255.255.255.0",
    val interfaceName: String = "wlan0"
)

enum class LanDeviceFilter {
    ALL,
    ONLINE,
    OFFLINE,
    GATEWAYS,
    COMPUTERS,
    PHONES,
    PRINTERS,
    IOT
}
