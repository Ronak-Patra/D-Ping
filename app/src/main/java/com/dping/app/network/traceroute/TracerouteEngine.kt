package com.dping.app.network.traceroute

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.util.regex.Pattern

data class HopInfo(
    val hopNumber: Int,
    val ip: String = "*",
    val hostname: String = "",
    val rttMs: Double = 0.0,
    val isTimeout: Boolean = false,
    val isDestinationReached: Boolean = false
)

class TracerouteEngine {

    companion object {
        private const val MAX_HOPS = 30
        private const val MAX_CONSECUTIVE_TIMEOUTS = 8
        private val IP_PATTERN = Pattern.compile("""\b(?:[0-9]{1,3}\.){3}[0-9]{1,3}\b""")
        private val TIME_PATTERN = Pattern.compile("""time=([\d.]+)\s*ms""")
        private val BYTES_FROM_PATTERN = Pattern.compile("""(?i)\bbytes\s+from\s+([0-9.]+)""")
        private val FROM_ROUTER_PATTERN = Pattern.compile("""(?i)From\s+([0-9.]+)""")
    }

    fun startTraceroute(target: String): Flow<HopInfo> = flow {
        val cleanTarget = target.trim()
        val destinationIp: String
        try {
            val addr = InetAddress.getByName(cleanTarget)
            destinationIp = addr.hostAddress ?: cleanTarget
        } catch (e: Exception) {
            emit(
                HopInfo(
                    hopNumber = 1,
                    ip = "*",
                    hostname = "Resolution failed: ${e.localizedMessage ?: "Unknown host"}",
                    isTimeout = true,
                    isDestinationReached = false
                )
            )
            return@flow
        }

        var consecutiveTimeouts = 0

        for (ttl in 1..MAX_HOPS) {
            if (!currentCoroutineContext().isActive) break

            val hop = executeTtlProbe(ttl, cleanTarget, destinationIp)
            emit(hop)

            if (hop.isDestinationReached) {
                break
            }

            if (hop.isTimeout) {
                consecutiveTimeouts++
                // Stop early if target network is completely silent/unreachable for 8 hops
                if (consecutiveTimeouts >= MAX_CONSECUTIVE_TIMEOUTS) {
                    break
                }
            } else {
                consecutiveTimeouts = 0
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun executeTtlProbe(ttl: Int, cleanTarget: String, destinationIp: String): HopInfo {
        var process: Process? = null
        var reader: BufferedReader? = null
        val startTime = System.nanoTime()

        try {
            // Ping target using destination IP to avoid redundant DNS lookups on every TTL hop
            val cmd = listOf("/system/bin/ping", "-c", "1", "-t", ttl.toString(), "-W", "2", destinationIp)
            val pb = ProcessBuilder(cmd)
            pb.redirectErrorStream(true)
            process = pb.start()
            reader = BufferedReader(InputStreamReader(process.inputStream))

            var detectedIp = ""
            var reportedTimeMs = 0.0
            var isEchoReply = false
            var isTtlExceeded = false
            var isUnreachable = false
            var unreachableReason = ""
            var line: String? = null

            while (reader.readLine().also { line = it } != null) {
                val l = line?.trim() ?: break
                if (l.isEmpty()) continue

                // Skip ping header lines & summary statistics
                if (l.startsWith("PING ", ignoreCase = true)) continue
                if (l.contains("ping statistics", ignoreCase = true)) continue
                if (l.contains("packets transmitted", ignoreCase = true)) continue
                if (l.contains("rtt min/avg", ignoreCase = true)) continue

                // Check for Echo Reply: e.g. "64 bytes from 8.8.8.8: icmp_seq=1 ttl=118 time=14.2 ms"
                if (l.contains("bytes from", ignoreCase = true)) {
                    val bfMatcher = BYTES_FROM_PATTERN.matcher(l)
                    if (bfMatcher.find()) {
                        detectedIp = bfMatcher.group(1) ?: destinationIp
                    } else {
                        val ipM = IP_PATTERN.matcher(l)
                        if (ipM.find()) detectedIp = ipM.group(0) ?: destinationIp
                    }

                    val timeMatcher = TIME_PATTERN.matcher(l)
                    if (timeMatcher.find()) {
                        reportedTimeMs = timeMatcher.group(1)?.toDoubleOrNull() ?: 0.0
                    }
                    isEchoReply = true
                    break
                }

                // Check for Intermediate Hop: e.g. "From 192.168.1.1: icmp_seq=1 Time to live exceeded"
                if (l.contains("Time to live exceeded", ignoreCase = true) ||
                    l.contains("Time to: live exceeded", ignoreCase = true)
                ) {
                    val fromM = FROM_ROUTER_PATTERN.matcher(l)
                    if (fromM.find()) {
                        detectedIp = fromM.group(1) ?: ""
                    } else {
                        val ipM = IP_PATTERN.matcher(l)
                        if (ipM.find()) detectedIp = ipM.group(0) ?: ""
                    }

                    val timeMatcher = TIME_PATTERN.matcher(l)
                    if (timeMatcher.find()) {
                        reportedTimeMs = timeMatcher.group(1)?.toDoubleOrNull() ?: 0.0
                    }
                    isTtlExceeded = true
                    break
                }

                // Check for Unreachable error: e.g. "From 192.168.1.1: Destination Host Unreachable"
                if (l.contains("Destination Host Unreachable", ignoreCase = true) ||
                    l.contains("Destination Net Unreachable", ignoreCase = true) ||
                    l.contains("Destination Protocol Unreachable", ignoreCase = true) ||
                    l.contains("Destination Port Unreachable", ignoreCase = true) ||
                    l.contains("Packet filtered", ignoreCase = true)
                ) {
                    val fromM = FROM_ROUTER_PATTERN.matcher(l)
                    if (fromM.find()) {
                        detectedIp = fromM.group(1) ?: ""
                    }
                    isUnreachable = true
                    unreachableReason = when {
                        l.contains("Host Unreachable", ignoreCase = true) -> "Host Unreachable"
                        l.contains("Net Unreachable", ignoreCase = true) -> "Network Unreachable"
                        l.contains("Packet filtered", ignoreCase = true) -> "Packet Filtered"
                        else -> "Destination Unreachable"
                    }
                    break
                }
            }

            process.waitFor()
            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000.0
            val effectiveRtt = if (reportedTimeMs > 0.0) reportedTimeMs else elapsedMs

            // 1. Genuine Echo Reply reached destination
            if (isEchoReply) {
                val ip = if (detectedIp.isNotBlank()) detectedIp else destinationIp
                val isReached = (ip == destinationIp || ip == cleanTarget)
                return HopInfo(
                    hopNumber = ttl,
                    ip = ip,
                    hostname = resolveHostname(ip),
                    rttMs = effectiveRtt,
                    isTimeout = false,
                    isDestinationReached = isReached
                )
            }

            // 2. Intermediate router encountered (TTL exceeded in transit)
            if (isTtlExceeded && detectedIp.isNotBlank()) {
                return HopInfo(
                    hopNumber = ttl,
                    ip = detectedIp,
                    hostname = resolveHostname(detectedIp),
                    rttMs = effectiveRtt,
                    isTimeout = false,
                    isDestinationReached = false // Intermediate hop NEVER reached destination
                )
            }

            // 3. Router reported unreachable
            if (isUnreachable) {
                return HopInfo(
                    hopNumber = ttl,
                    ip = if (detectedIp.isNotBlank()) detectedIp else "*",
                    hostname = unreachableReason,
                    rttMs = effectiveRtt,
                    isTimeout = true,
                    isDestinationReached = false
                )
            }

            // 4. Request timed out (no response)
            return HopInfo(
                hopNumber = ttl,
                ip = "*",
                hostname = "Request timed out",
                rttMs = 0.0,
                isTimeout = true,
                isDestinationReached = false
            )

        } catch (e: Exception) {
            return HopInfo(
                hopNumber = ttl,
                ip = "*",
                hostname = e.localizedMessage ?: "Probe error",
                rttMs = 0.0,
                isTimeout = true,
                isDestinationReached = false
            )
        } finally {
            try {
                reader?.close()
            } catch (_: Exception) {}
            try {
                process?.destroy()
            } catch (_: Exception) {}
        }
    }

    private fun resolveHostname(ip: String): String {
        return try {
            val hopAddr = InetAddress.getByName(ip)
            val resolved = hopAddr.canonicalHostName
            if (!resolved.isNullOrBlank() && resolved != ip) {
                resolved
            } else {
                val host = hopAddr.hostName
                if (!host.isNullOrBlank() && host != ip) host else ip
            }
        } catch (_: Exception) {
            ip
        }
    }
}
