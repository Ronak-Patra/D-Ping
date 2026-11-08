package com.dping.app.network.http

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.cert.X509Certificate
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

data class TlsCertInfo(
    val host: String,
    val port: Int,
    val subject: String,
    val issuer: String,
    val validFrom: Date,
    val validUntil: Date,
    val daysRemaining: Long,
    val isExpired: Boolean,
    val isNotYetValid: Boolean,
    val algorithm: String,
    val serialNumber: String,
    val sans: List<String> = emptyList(),
    val isHostnameMatch: Boolean = true,
    val isSuccess: Boolean = true,
    val errorMessage: String = ""
)

class TlsInspector {

    suspend fun inspectCertificate(host: String, port: Int = 443, timeoutMs: Int = 5000): TlsCertInfo = withContext(Dispatchers.IO) {
        val raw = host.trim().removePrefix("https://").removePrefix("http://").split("/")[0]
        val parts = raw.split(":")
        val cleanHost = parts[0]
        val actualPort = if (parts.size > 1) parts[1].toIntOrNull() ?: port else port

        try {
            val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val socket = factory.createSocket(cleanHost, actualPort) as SSLSocket
            socket.soTimeout = timeoutMs
            socket.startHandshake()

            val session = socket.session
            val certs = session.peerCertificates
            if (certs.isEmpty() || certs[0] !is X509Certificate) {
                socket.close()
                return@withContext TlsCertInfo(
                    host = cleanHost,
                    port = actualPort,
                    subject = "",
                    issuer = "",
                    validFrom = Date(),
                    validUntil = Date(),
                    daysRemaining = 0,
                    isExpired = false,
                    isNotYetValid = false,
                    algorithm = "",
                    serialNumber = "",
                    isSuccess = false,
                    errorMessage = "No X.509 certificate found on server"
                )
            }

            val cert = certs[0] as X509Certificate
            val now = Date()
            val validFrom = cert.notBefore
            val validUntil = cert.notAfter
            val diffMs = validUntil.time - now.time
            val daysRemaining = TimeUnit.MILLISECONDS.toDays(diffMs)

            val isExpired = now.after(validUntil)
            val isNotYetValid = now.before(validFrom)

            // Subject Alternative Names
            val sanList = mutableListOf<String>()
            try {
                cert.subjectAlternativeNames?.forEach { item ->
                    if (item.size >= 2 && item[1] is String) {
                        sanList.add(item[1] as String)
                    }
                }
            } catch (_: Exception) {}

            // Check hostname verification with default verifier
            val verifier = HttpsURLConnection.getDefaultHostnameVerifier()
            val isMatch = verifier.verify(cleanHost, session)

            socket.close()

            TlsCertInfo(
                host = cleanHost,
                port = actualPort,
                subject = cert.subjectX500Principal.name,
                issuer = cert.issuerX500Principal.name,
                validFrom = validFrom,
                validUntil = validUntil,
                daysRemaining = daysRemaining,
                isExpired = isExpired,
                isNotYetValid = isNotYetValid,
                algorithm = cert.sigAlgName,
                serialNumber = cert.serialNumber.toString(16).uppercase(),
                sans = sanList,
                isHostnameMatch = isMatch,
                isSuccess = true
            )
        } catch (e: Exception) {
            TlsCertInfo(
                host = cleanHost,
                port = actualPort,
                subject = "",
                issuer = "",
                validFrom = Date(),
                validUntil = Date(),
                daysRemaining = 0,
                isExpired = false,
                isNotYetValid = false,
                algorithm = "",
                serialNumber = "",
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "TLS Handshake or Certificate validation error"
            )
        }
    }
}
