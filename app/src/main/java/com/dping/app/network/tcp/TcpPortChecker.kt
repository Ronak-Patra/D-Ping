package com.dping.app.network.tcp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException

enum class TcpPortStatus {
    ESTABLISHED, REFUSED, TIMED_OUT, RESOLUTION_FAILED, ERROR
}

data class TcpCheckResult(
    val host: String,
    val port: Int,
    val status: TcpPortStatus,
    val durationMs: Double,
    val message: String
)

class TcpPortChecker {

    suspend fun checkPort(host: String, port: Int, timeoutMs: Int = 3000): TcpCheckResult = withContext(Dispatchers.IO) {
        val raw = host.trim().removePrefix("https://").removePrefix("http://").split("/")[0]
        val parts = raw.split(":")
        val cleanHost = parts[0]
        val targetPort = if (parts.size > 1) parts[1].toIntOrNull() ?: port else port
        val startTime = System.nanoTime()

        try {
            // Test DNS resolution first
            InetAddress.getByName(cleanHost)

            Socket().use { socket ->
                socket.connect(InetSocketAddress(cleanHost, targetPort), timeoutMs)
                val duration = (System.nanoTime() - startTime) / 1_000_000.0
                return@withContext TcpCheckResult(
                    host = cleanHost,
                    port = targetPort,
                    status = TcpPortStatus.ESTABLISHED,
                    durationMs = duration,
                    message = "Connection established successfully"
                )
            }
        } catch (e: UnknownHostException) {
            val duration = (System.nanoTime() - startTime) / 1_000_000.0
            return@withContext TcpCheckResult(
                host = cleanHost,
                port = port,
                status = TcpPortStatus.RESOLUTION_FAILED,
                durationMs = duration,
                message = "Hostname resolution failed"
            )
        } catch (e: SocketTimeoutException) {
            val duration = (System.nanoTime() - startTime) / 1_000_000.0
            return@withContext TcpCheckResult(
                host = cleanHost,
                port = port,
                status = TcpPortStatus.TIMED_OUT,
                durationMs = duration,
                message = "Connection timed out after ${timeoutMs}ms (Host silent or port filtered)"
            )
        } catch (e: Exception) {
            val duration = (System.nanoTime() - startTime) / 1_000_000.0
            val msg = e.localizedMessage ?: ""
            val isRefused = msg.contains("refused", ignoreCase = true) || msg.contains("ECONNREFUSED", ignoreCase = true)

            return@withContext TcpCheckResult(
                host = cleanHost,
                port = port,
                status = if (isRefused) TcpPortStatus.REFUSED else TcpPortStatus.ERROR,
                durationMs = duration,
                message = if (isRefused) "Connection refused (Port closed by target)" else msg
            )
        }
    }
}
