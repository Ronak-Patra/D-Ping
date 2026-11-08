package com.dping.app.network.http

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class HttpCheckResult(
    val requestedUrl: String,
    val finalUrl: String = "",
    val statusCode: Int = 0,
    val statusMessage: String = "",
    val durationMs: Double = 0.0,
    val redirects: List<String> = emptyList(),
    val headers: Map<String, String> = emptyMap(),
    val isSuccess: Boolean = false,
    val errorMessage: String = ""
)

class HttpChecker {

    suspend fun checkUrl(inputUrl: String): HttpCheckResult = withContext(Dispatchers.IO) {
        val trimmed = inputUrl.trim()
        val formattedUrl = if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            "https://$trimmed"
        } else {
            trimmed
        }

        val startTime = System.nanoTime()
        val redirects = mutableListOf<String>()

        try {
            var currentUrl = URL(formattedUrl)
            var connection: HttpURLConnection
            var redirectCount = 0

            while (redirectCount < 5) {
                connection = currentUrl.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("User-Agent", "D-Ping-Android/1.0")
                connection.requestMethod = "GET"

                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location") ?: break
                    redirects.add("$code -> $location")
                    currentUrl = URL(currentUrl, location)
                    redirectCount++
                    connection.disconnect()
                } else {
                    val duration = (System.nanoTime() - startTime) / 1_000_000.0
                    val headerMap = mutableMapOf<String, String>()
                    connection.headerFields.forEach { (key, values) ->
                        if (key != null && values.isNotEmpty()) {
                            headerMap[key] = values.joinToString(", ")
                        }
                    }

                    val result = HttpCheckResult(
                        requestedUrl = formattedUrl,
                        finalUrl = currentUrl.toString(),
                        statusCode = code,
                        statusMessage = connection.responseMessage ?: "",
                        durationMs = duration,
                        redirects = redirects,
                        headers = headerMap,
                        isSuccess = (code in 200..299)
                    )
                    connection.disconnect()
                    return@withContext result
                }
            }

            val duration = (System.nanoTime() - startTime) / 1_000_000.0
            HttpCheckResult(
                requestedUrl = formattedUrl,
                finalUrl = currentUrl.toString(),
                statusCode = 310,
                statusMessage = "Too Many Redirects",
                durationMs = duration,
                redirects = redirects,
                isSuccess = false,
                errorMessage = "Exceeded maximum redirect limit (5)"
            )
        } catch (e: Exception) {
            val duration = (System.nanoTime() - startTime) / 1_000_000.0
            HttpCheckResult(
                requestedUrl = formattedUrl,
                durationMs = duration,
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Connection error"
            )
        }
    }
}
