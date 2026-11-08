package com.dping.app.network.discovery

import com.dping.app.network.iprange.ArpTableHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

data class LanDiscoveryProgress(
    val scannedIps: Int,
    val totalIps: Int,
    val deviceFound: LanDevice? = null,
    val isFinished: Boolean = false
)

class LanDiscoveryScanner {

    private val commonProbePorts = intArrayOf(80, 443, 22, 53, 445, 8080, 9100, 5000, 8443, 631)

    fun scanLan(
        subnetBase: String,
        localIp: String,
        gatewayIp: String
    ): Flow<LanDiscoveryProgress> = flow {
        val total = 254
        var scanned = 0
        val discoveredIps = mutableSetOf<String>()

        // 1. First yield the current device immediately
        if (localIp.isNotBlank() && localIp != "0.0.0.0") {
            discoveredIps.add(localIp)
            val currentDevice = LanDevice(
                ip = localIp,
                hostname = "This Device (${android.os.Build.MODEL})",
                macAddress = ArpTableHelper.getMacForIp(localIp),
                vendor = android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
                latencyMs = 0.1,
                openServices = listOf("LOCAL"),
                deviceType = LanDeviceType.PHONE,
                isGateway = false,
                isCurrentDevice = true
            )
            emit(
                LanDiscoveryProgress(
                    scannedIps = 0,
                    totalIps = total,
                    deviceFound = currentDevice,
                    isFinished = false
                )
            )
        }

        // 2. Read ARP table right away for previously active devices
        readArpCachedHosts(subnetBase).forEach { cachedIp ->
            if (discoveredIps.add(cachedIp)) {
                val dev = probeDevice(cachedIp, localIp, gatewayIp, timeoutMs = 250)
                if (dev != null) {
                    emit(
                        LanDiscoveryProgress(
                            scannedIps = scanned,
                            totalIps = total,
                            deviceFound = dev,
                            isFinished = false
                        )
                    )
                } else {
                    val mac = ArpTableHelper.getMacForIp(cachedIp)
                    val vendor = ArpTableHelper.getVendorForMac(mac)
                    emit(
                        LanDiscoveryProgress(
                            scannedIps = scanned,
                            totalIps = total,
                            deviceFound = LanDevice(
                                ip = cachedIp,
                                hostname = if (!vendor.isNullOrBlank()) "$vendor Device (Offline)" else "Offline Device",
                                macAddress = mac,
                                vendor = vendor,
                                latencyMs = 0.0,
                                isOnline = false,
                                deviceType = LanDeviceType.GENERIC
                            ),
                            isFinished = false
                        )
                    )
                }
            }
        }

        // 3. Concurrently scan the subnet 1..254
        val dispatcher = Dispatchers.IO.limitedParallelism(32)

        coroutineScope {
            val channel = Channel<LanDevice?>(capacity = 64)

            val scanJobs = (1..total).map { hostNum ->
                val targetIp = "$subnetBase.$hostNum"
                launch(dispatcher) {
                    val device = if (!discoveredIps.contains(targetIp)) {
                        probeDevice(targetIp, localIp, gatewayIp, timeoutMs = 250)
                    } else null
                    channel.send(device)
                }
            }

            launch {
                scanJobs.forEach { it.join() }
                channel.close()
            }

            for (device in channel) {
                scanned++
                if (device != null) {
                    discoveredIps.add(device.ip)
                }
                emit(
                    LanDiscoveryProgress(
                        scannedIps = scanned,
                        totalIps = total,
                        deviceFound = device,
                        isFinished = (scanned == total)
                    )
                )
            }
        }

        // Finish notification
        emit(
            LanDiscoveryProgress(
                scannedIps = total,
                totalIps = total,
                deviceFound = null,
                isFinished = true
            )
        )
    }.flowOn(Dispatchers.IO)

    fun scanRange(
        startIpStr: String,
        endIpStr: String,
        localIp: String,
        gatewayIp: String
    ): Flow<LanDiscoveryProgress> = flow {
        val startLong = ipToLong(startIpStr)
        val endLong = ipToLong(endIpStr)

        if (startLong == null || endLong == null || startLong > endLong) {
            emit(LanDiscoveryProgress(scannedIps = 0, totalIps = 0, isFinished = true))
            return@flow
        }

        val total = (endLong - startLong + 1).toInt().coerceAtMost(1024)
        var scanned = 0

        // Check if local device falls within the scanned range
        val localLong = ipToLong(localIp)
        if (localLong != null && localLong in startLong..endLong) {
            val currentDevice = LanDevice(
                ip = localIp,
                hostname = "This Device (${android.os.Build.MODEL})",
                macAddress = ArpTableHelper.getMacForIp(localIp),
                vendor = android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
                latencyMs = 0.1,
                openServices = listOf("LOCAL"),
                deviceType = LanDeviceType.PHONE,
                isGateway = false,
                isCurrentDevice = true
            )
            emit(
                LanDiscoveryProgress(
                    scannedIps = 0,
                    totalIps = total,
                    deviceFound = currentDevice,
                    isFinished = false
                )
            )
        }

        val dispatcher = Dispatchers.IO.limitedParallelism(32)

        coroutineScope {
            val channel = Channel<LanDevice?>(capacity = 64)

            val scanJobs = (0 until total).map { offset ->
                val ip = longToIp(startLong + offset)
                launch(dispatcher) {
                    if (ip == localIp) {
                        channel.send(null)
                    } else {
                        val device = probeDevice(ip, localIp, gatewayIp, timeoutMs = 300)
                        channel.send(device)
                    }
                }
            }

            launch {
                scanJobs.forEach { it.join() }
                channel.close()
            }

            for (device in channel) {
                scanned++
                emit(
                    LanDiscoveryProgress(
                        scannedIps = scanned,
                        totalIps = total,
                        deviceFound = device,
                        isFinished = (scanned == total)
                    )
                )
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun readArpCachedHosts(subnetBase: String): List<String> {
        val list = mutableListOf<String>()
        try {
            val arp = File("/proc/net/arp")
            if (arp.exists() && arp.canRead()) {
                arp.forEachLine { line ->
                    val parts = line.split("\\s+".toRegex())
                    if (parts.isNotEmpty() && parts[0].startsWith("$subnetBase.")) {
                        val ip = parts[0]
                        if (parts.size >= 4 && parts[3] != "00:00:00:00:00:00") {
                            list.add(ip)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return list
    }

    private fun probeDevice(
        ip: String,
        localIp: String,
        gatewayIp: String,
        timeoutMs: Int
    ): LanDevice? {
        val isGw = (ip == gatewayIp)
        val isCurrent = (ip == localIp)
        val openServices = mutableListOf<String>()
        var isAlive = isGw || isCurrent
        var bestRtt = 0.0

        // Probe common ports
        val startNano = System.nanoTime()
        for (port in commonProbePorts) {
            try {
                Socket().use { sock ->
                    sock.connect(InetSocketAddress(ip, port), timeoutMs)
                    isAlive = true
                    val rtt = (System.nanoTime() - startNano) / 1_000_000.0
                    if (bestRtt == 0.0 || rtt < bestRtt) bestRtt = rtt
                    openServices.add(serviceNameForPort(port))
                }
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if (msg.contains("refused", ignoreCase = true) || msg.contains("ECONNREFUSED", ignoreCase = true)) {
                    isAlive = true
                    val rtt = (System.nanoTime() - startNano) / 1_000_000.0
                    if (bestRtt == 0.0 || rtt < bestRtt) bestRtt = rtt
                }
            }
            if (openServices.size >= 2) break
        }

        // Fallback: ICMP reachability check
        if (!isAlive) {
            try {
                val addr = InetAddress.getByName(ip)
                val icmpStart = System.nanoTime()
                if (addr.isReachable(timeoutMs)) {
                    isAlive = true
                    bestRtt = (System.nanoTime() - icmpStart) / 1_000_000.0
                }
            } catch (_: Exception) {}
        }

        if (!isAlive) return null

        val mac = ArpTableHelper.getMacForIp(ip)
        val vendor = ArpTableHelper.getVendorForMac(mac)
        val hostname = resolveHostname(ip, vendor, isGw)
        val deviceType = classifyDevice(isGw, isCurrent, vendor, hostname, openServices)

        return LanDevice(
            ip = ip,
            hostname = hostname,
            macAddress = mac,
            vendor = vendor,
            latencyMs = if (bestRtt > 0) bestRtt else 1.5,
            openServices = openServices,
            deviceType = deviceType,
            isGateway = isGw,
            isCurrentDevice = isCurrent
        )
    }

    private fun serviceNameForPort(port: Int): String = when (port) {
        80 -> "HTTP:80"
        443 -> "HTTPS:443"
        22 -> "SSH:22"
        53 -> "DNS:53"
        445 -> "SMB:445"
        8080 -> "Web:8080"
        9100 -> "Printer:9100"
        5000 -> "UPnP:5000"
        8443 -> "HTTPS:8443"
        631 -> "IPP:631"
        else -> "Port:$port"
    }

    private fun resolveHostname(ip: String, vendor: String?, isGateway: Boolean): String {
        // NetBIOS query
        ArpTableHelper.queryNetBiosName(ip, timeoutMs = 250)?.let { netBios ->
            if (netBios.isNotBlank() && !netBios.startsWith("Host-")) return netBios
        }

        // Reverse DNS lookup
        try {
            val addr = InetAddress.getByName(ip)
            val canonical = addr.canonicalHostName
            if (!canonical.isNullOrBlank() && canonical != ip) {
                return canonical
            }
            val host = addr.hostName
            if (!host.isNullOrBlank() && host != ip) {
                return host
            }
        } catch (_: Exception) {}

        if (isGateway) return "Default Gateway / Router"
        if (!vendor.isNullOrBlank()) return "$vendor Device"
        return "Network Device"
    }

    private fun classifyDevice(
        isGateway: Boolean,
        isCurrentDevice: Boolean,
        vendor: String?,
        hostname: String,
        openServices: List<String>
    ): LanDeviceType {
        if (isGateway) return LanDeviceType.ROUTER
        if (isCurrentDevice) return LanDeviceType.PHONE

        val combined = "${vendor.orEmpty()} $hostname".lowercase()

        if (openServices.any { it.contains("9100") || it.contains("631") } ||
            combined.contains("printer") || combined.contains("canon") ||
            combined.contains("epson") || combined.contains("brother") || combined.contains("hp")
        ) {
            return LanDeviceType.PRINTER
        }

        if (combined.contains("apple") || combined.contains("iphone") ||
            combined.contains("ipad") || combined.contains("android") ||
            combined.contains("samsung") || combined.contains("pixel") ||
            combined.contains("xiaomi") || combined.contains("oneplus") ||
            combined.contains("huawei") || combined.contains("oppo") ||
            combined.contains("vivo")
        ) {
            return LanDeviceType.PHONE
        }

        if (combined.contains("dell") || combined.contains("lenovo") ||
            combined.contains("asus") || combined.contains("acer") ||
            combined.contains("intel") || combined.contains("microsoft") ||
            combined.contains("macbook") || combined.contains("imac") ||
            combined.contains("linux") || combined.contains("ubuntu") ||
            combined.contains("thinkpad")
        ) {
            return LanDeviceType.COMPUTER
        }

        if (combined.contains("synology") || combined.contains("qnap") ||
            combined.contains("nas") || combined.contains("diskstation") ||
            combined.contains("truenas")
        ) {
            return LanDeviceType.NAS_STORAGE
        }

        if (combined.contains("tv") || combined.contains("roku") ||
            combined.contains("chromecast") || combined.contains("fire") ||
            combined.contains("sonos") || combined.contains("espressif") ||
            combined.contains("tuya") || combined.contains("tasmota") ||
            combined.contains("amazon") || combined.contains("alexa") ||
            combined.contains("nest") || combined.contains("ring")
        ) {
            return LanDeviceType.SMART_TV_IOT
        }

        if (combined.contains("router") || combined.contains("gateway") ||
            combined.contains("tp-link") || combined.contains("netgear") ||
            combined.contains("cisco") || combined.contains("asus") ||
            combined.contains("ubiquiti") || combined.contains("mikrotik")
        ) {
            return LanDeviceType.ROUTER
        }

        return LanDeviceType.GENERIC
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
