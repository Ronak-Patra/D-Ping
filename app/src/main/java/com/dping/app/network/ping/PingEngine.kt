package com.dping.app.network.ping

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

enum class PingPacketStatus {
    SUCCESS, TIMEOUT, UNREACHABLE, ERROR
}

data class PingPacket(
    val sequence: Int,
    val bytes: Int = 64,
    val ip: String = "",
    val ttl: Int = 0,
    val rttMs: Double = 0.0,
    val status: PingPacketStatus = PingPacketStatus.SUCCESS,
    val message: String = ""
)

data class PingStats(
    val sent: Int = 0,
    val received: Int = 0,
    val lost: Int = 0,
    val lossPercentage: Double = 0.0,
    val minRtt: Double = 0.0,
    val avgRtt: Double = 0.0,
    val maxRtt: Double = 0.0,
    val mdevRtt: Double = 0.0,
    val isRunning: Boolean = false,
    val resolvedIp: String = "",
    val target: String = ""
)

class PingEngine {

    companion object {
        // Regex to parse: 64 bytes from 8.8.8.8: icmp_seq=1 ttl=117 time=14.2 ms
        private val PING_SUCCESS_PATTERN = Pattern.compile(
            """(\d+)\s+bytes\s+from\s+([^\s:]+):(?:\s+icmp_seq=(\d+))?(?:\s+ttl=(\d+))?\s+time=([\d.]+)\s*ms""",
            Pattern.CASE_INSENSITIVE
        )
        // Alternative pattern without sequence or slightly different format
        private val PING_FALLBACK_PATTERN = Pattern.compile(
            """time=([\d.]+)\s*ms""",
            Pattern.CASE_INSENSITIVE
        )
    }

    fun startPing(
        target: String,
        packetSize: Int = 64,
        intervalMs: Long = 1000L,
        count: Int = 0, // 0 means continuous
        resolveHostname: Boolean = true
    ): Flow<PingPacket> = flow {
        var resolvedTarget = target.trim()
        var resolvedIp = ""

        try {
            val address = InetAddress.getByName(resolvedTarget)
            resolvedIp = address.hostAddress ?: resolvedTarget
        } catch (e: Exception) {
            emit(
                PingPacket(
                    sequence = 1,
                    status = PingPacketStatus.ERROR,
                    message = "Cannot resolve host: ${e.localizedMessage ?: "Unknown host"}"
                )
            )
            return@flow
        }

        val targetToPing = if (resolveHostname && resolvedIp.isNotBlank()) resolvedIp else resolvedTarget
        val intervalSec = (intervalMs / 1000.0).coerceAtLeast(0.2)

        val cmd = mutableListOf<String>()
        cmd.add("/system/bin/ping")
        cmd.add("-s")
        cmd.add(packetSize.toString())
        cmd.add("-i")
        cmd.add(String.format(java.util.Locale.US, "%.1f", intervalSec))
        if (count > 0) {
            cmd.add("-c")
            cmd.add(count.toString())
        }
        cmd.add(targetToPing)

        var process: Process? = null
        var reader: BufferedReader? = null

        try {
            val processBuilder = ProcessBuilder(cmd)
            processBuilder.redirectErrorStream(true)
            process = processBuilder.start()
            reader = BufferedReader(InputStreamReader(process.inputStream))

            var seqCounter = 1
            var line: String? = null

            while (currentCoroutineContext().isActive && reader.readLine().also { line = it } != null) {
                val currentLine = line ?: break

                // Ignore header line like "PING 8.8.8.8 (8.8.8.8) 64(92) bytes of data."
                if (currentLine.startsWith("PING ", ignoreCase = true) || currentLine.isBlank()) {
                    continue
                }

                // Check for summary footer line
                if (currentLine.contains("ping statistics", ignoreCase = true) ||
                    currentLine.contains("packets transmitted", ignoreCase = true) ||
                    currentLine.contains("rtt min/avg/max", ignoreCase = true)
                ) {
                    continue
                }

                val matcher = PING_SUCCESS_PATTERN.matcher(currentLine)
                if (matcher.find()) {
                    val bytes = matcher.group(1)?.toIntOrNull() ?: packetSize
                    val ip = matcher.group(2) ?: targetToPing
                    val seq = matcher.group(3)?.toIntOrNull() ?: seqCounter++
                    val ttl = matcher.group(4)?.toIntOrNull() ?: 64
                    val time = matcher.group(5)?.toDoubleOrNull() ?: 0.0

                    emit(
                        PingPacket(
                            sequence = seq,
                            bytes = bytes,
                            ip = ip,
                            ttl = ttl,
                            rttMs = time,
                            status = PingPacketStatus.SUCCESS,
                            message = currentLine
                        )
                    )
                } else if (currentLine.contains("Request timeout", ignoreCase = true) ||
                    currentLine.contains("Destination Host Unreachable", ignoreCase = true) ||
                    currentLine.contains("Time to live exceeded", ignoreCase = true) ||
                    currentLine.contains("no answer yet", ignoreCase = true)
                ) {
                    emit(
                        PingPacket(
                            sequence = seqCounter++,
                            status = PingPacketStatus.TIMEOUT,
                            message = currentLine.trim()
                        )
                    )
                } else {
                    // Try fallback time match
                    val fbMatcher = PING_FALLBACK_PATTERN.matcher(currentLine)
                    if (fbMatcher.find()) {
                        val time = fbMatcher.group(1)?.toDoubleOrNull() ?: 0.0
                        emit(
                            PingPacket(
                                sequence = seqCounter++,
                                bytes = packetSize,
                                ip = targetToPing,
                                rttMs = time,
                                status = PingPacketStatus.SUCCESS,
                                message = currentLine
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            emit(
                PingPacket(
                    sequence = 1,
                    status = PingPacketStatus.ERROR,
                    message = "Ping error: ${e.localizedMessage ?: "Execution failed"}"
                )
            )
        } finally {
            try {
                reader?.close()
            } catch (_: Exception) {}
            try {
                process?.destroy()
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)
}
