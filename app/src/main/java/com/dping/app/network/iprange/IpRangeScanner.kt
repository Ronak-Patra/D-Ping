package com.dping.app.network.iprange

import com.dping.app.network.discovery.LanDeviceType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

enum class IpOccupancyStatus {
    OCCUPIED, FREE
}

data class IpScanItem(
    val ip: String,
    val status: IpOccupancyStatus,
    val hostname: String = "",
    val macAddress: String? = null,
    val vendor: String? = null,
    val latencyMs: Double = 0.0,
    val openPort: Int? = null,
    val deviceType: LanDeviceType = LanDeviceType.GENERIC
)

data class IpRangeScanProgress(
    val totalIps: Int = 0,
    val scannedIps: Int = 0,
    val occupiedCount: Int = 0,
    val freeCount: Int = 0,
    val latestItem: IpScanItem? = null,
    val isFinished: Boolean = false
)

class IpRangeScanner {

    private val commonPorts = listOf(80, 443, 53, 445, 22, 8080)

    fun scanRange(startIpStr: String, endIpStr: String, timeoutMs: Int = 400): Flow<IpRangeScanProgress> = flow {
        val startLong = ipToLong(startIpStr)
        val endLong = ipToLong(endIpStr)

        if (startLong == null || endLong == null || startLong > endLong) {
            emit(IpRangeScanProgress(isFinished = true))
            return@flow
        }

        val total = (endLong - startLong + 1).toInt().coerceAtMost(1024)
        var scanned = 0
        var occupied = 0
        var free = 0

        // Emit initial progress
        emit(
            IpRangeScanProgress(
                totalIps = total,
                scannedIps = 0,
                occupiedCount = 0,
                freeCount = 0,
                isFinished = false
            )
        )

        // Bounded concurrency
        val dispatcher = Dispatchers.IO.limitedParallelism(32)

        coroutineScope {
            val channel = kotlinx.coroutines.channels.Channel<IpScanItem>(capacity = 64)

            val scanJobs = (0 until total).map { offset ->
                val currentIp = longToIp(startLong + offset)
                launch(dispatcher) {
                    val result = probeIp(currentIp, timeoutMs)
                    channel.send(result)
                }
            }

            // Close channel when all probes complete
            launch {
                scanJobs.forEach { it.join() }
                channel.close()
            }

            for (item in channel) {
                scanned++
                if (item.status == IpOccupancyStatus.OCCUPIED) {
                    occupied++
                } else {
                    free++
                }

                emit(
                    IpRangeScanProgress(
                        totalIps = total,
                        scannedIps = scanned,
                        occupiedCount = occupied,
                        freeCount = free,
                        latestItem = item,
                        isFinished = (scanned == total)
                    )
                )
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun probeIp(ip: String, timeoutMs: Int): IpScanItem {
        val startTime = System.nanoTime()

        // Probe 1: Socket connect on common ports
        for (port in commonPorts) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), timeoutMs / 2)
                    val rtt = (System.nanoTime() - startTime) / 1_000_000.0
                    val mac = ArpTableHelper.getMacForIp(ip)
                    val vendor = ArpTableHelper.getVendorForMac(mac)
                    val hostname = resolveHostname(ip, port, vendor)
                    val devType = classifyDevice(ip, hostname, vendor, port)
                    return IpScanItem(
                        ip = ip,
                        status = IpOccupancyStatus.OCCUPIED,
                        hostname = hostname,
                        macAddress = mac,
                        vendor = vendor,
                        latencyMs = rtt,
                        openPort = port,
                        deviceType = devType
                    )
                }
            } catch (e: Exception) {
                // Check if connection was actively refused (which proves host is alive!)
                val msg = e.message ?: ""
                if (msg.contains("refused", ignoreCase = true) || msg.contains("ECONNREFUSED", ignoreCase = true)) {
                    val rtt = (System.nanoTime() - startTime) / 1_000_000.0
                    val mac = ArpTableHelper.getMacForIp(ip)
                    val vendor = ArpTableHelper.getVendorForMac(mac)
                    val hostname = resolveHostname(ip, port, vendor)
                    val devType = classifyDevice(ip, hostname, vendor, port)
                    return IpScanItem(
                        ip = ip,
                        status = IpOccupancyStatus.OCCUPIED,
                        hostname = hostname,
                        macAddress = mac,
                        vendor = vendor,
                        latencyMs = rtt,
                        openPort = port,
                        deviceType = devType
                    )
                }
            }
        }

        // Probe 2: ICMP isReachable
        try {
            val addr = InetAddress.getByName(ip)
            if (addr.isReachable(timeoutMs)) {
                val rtt = (System.nanoTime() - startTime) / 1_000_000.0
                val mac = ArpTableHelper.getMacForIp(ip)
                val vendor = ArpTableHelper.getVendorForMac(mac)
                val hostname = resolveHostname(ip, null, vendor)
                val devType = classifyDevice(ip, hostname, vendor, null)
                return IpScanItem(
                    ip = ip,
                    status = IpOccupancyStatus.OCCUPIED,
                    hostname = hostname,
                    macAddress = mac,
                    vendor = vendor,
                    latencyMs = rtt,
                    deviceType = devType
                )
            }
        } catch (_: Exception) {}

        return IpScanItem(
            ip = ip,
            status = IpOccupancyStatus.FREE,
            hostname = "Unassigned (Available)",
            latencyMs = 0.0,
            deviceType = LanDeviceType.GENERIC
        )
    }

    private fun classifyDevice(
        ip: String,
        hostname: String,
        vendor: String?,
        openPort: Int?
    ): LanDeviceType {
        val isGateway = ip.endsWith(".1") || ip.endsWith(".254")
        if (isGateway) return LanDeviceType.ROUTER

        val combined = "${vendor.orEmpty()} $hostname".lowercase()

        if (openPort == 9100 || openPort == 631 || combined.contains("printer") || combined.contains("canon") || combined.contains("epson") || combined.contains("brother") || combined.contains("hp")) {
            return LanDeviceType.PRINTER
        }
        if (combined.contains("apple") || combined.contains("iphone") || combined.contains("ipad") || combined.contains("android") || combined.contains("samsung") || combined.contains("pixel") || combined.contains("xiaomi") || combined.contains("oneplus") || combined.contains("oppo") || combined.contains("vivo")) {
            return LanDeviceType.PHONE
        }
        if (openPort == 445 || openPort == 139 || openPort == 22 || combined.contains("dell") || combined.contains("lenovo") || combined.contains("asus") || combined.contains("acer") || combined.contains("intel") || combined.contains("microsoft") || combined.contains("macbook") || combined.contains("imac") || combined.contains("linux") || combined.contains("ubuntu") || combined.contains("pc") || combined.contains("desktop")) {
            return LanDeviceType.COMPUTER
        }
        if (openPort == 5000 || combined.contains("synology") || combined.contains("qnap") || combined.contains("nas") || combined.contains("diskstation")) {
            return LanDeviceType.NAS_STORAGE
        }
        if (openPort == 8008 || openPort == 8009 || combined.contains("tv") || combined.contains("roku") || combined.contains("chromecast") || combined.contains("fire") || combined.contains("sonos") || combined.contains("tuya") || combined.contains("alexa") || combined.contains("nest")) {
            return LanDeviceType.SMART_TV_IOT
        }
        if (openPort == 80 || openPort == 443 || openPort == 8080 || combined.contains("router") || combined.contains("gateway") || combined.contains("tp-link") || combined.contains("netgear") || combined.contains("cisco")) {
            return LanDeviceType.ROUTER
        }
        return LanDeviceType.GENERIC
    }

    private fun resolveHostname(ip: String, openPort: Int? = null, vendor: String? = null): String {
        // 1. Query NetBIOS for local machine name (Windows, Samba, NAS, Printers)
        ArpTableHelper.queryNetBiosName(ip, timeoutMs = 250)?.let { netBiosName ->
            if (netBiosName.isNotBlank()) return netBiosName
        }

        // 2. Reverse DNS / mDNS resolution
        try {
            val addr = InetAddress.getByName(ip)
            val canonical = addr.canonicalHostName
            if (!canonical.isNullOrBlank() && canonical != ip) return canonical
            val host = addr.hostName
            if (!host.isNullOrBlank() && host != ip) return host
        } catch (_: Exception) {}

        // 3. Known network topology roles
        if (ip.endsWith(".1")) return "Default Gateway / Router"
        if (ip.endsWith(".254")) return "Subnet Switch / Gateway"

        // 4. Service-based identification from open port
        when (openPort) {
            80, 8080 -> return if (vendor != null) "$vendor Web Interface" else "Router / Web Device (HTTP)"
            443 -> return if (vendor != null) "$vendor Secure Node" else "Secure Gateway (HTTPS)"
            22 -> return if (vendor != null) "$vendor Linux / SSH Host" else "Linux / SSH Server"
            445, 139 -> return if (vendor != null) "$vendor PC" else "Windows / SMB Host"
            53 -> return "DNS Server / Gateway"
            631, 9100 -> return if (vendor != null) "$vendor Printer" else "Network Printer"
            8008, 8009 -> return "Smart Media / Cast Device"
        }

        // 5. Hardware Vendor identification
        if (vendor != null) {
            return "$vendor Network Device"
        }

        return "LAN Device ($ip)"
    }

    companion object {
        fun ipToLong(ip: String): Long? {
            return try {
                val parts = ip.trim().split(".")
                if (parts.size != 4) return null
                var result = 0L
                for (part in parts) {
                    val octet = part.toIntOrNull() ?: return null
                    if (octet !in 0..255) return null
                    result = (result shl 8) or octet.toLong()
                }
                result
            } catch (_: Exception) {
                null
            }
        }

        fun longToIp(value: Long): String {
            return "${(value shr 24) and 0xFF}.${(value shr 16) and 0xFF}.${(value shr 8) and 0xFF}.${value and 0xFF}"
        }
    }
}
