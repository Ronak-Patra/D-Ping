package com.dping.app.network.dns

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

data class DnsRecordItem(
    val type: String,
    val value: String,
    val ttl: Int = 0
)

data class DnsQueryResult(
    val query: String,
    val type: String,
    val records: List<DnsRecordItem>,
    val durationMs: Double,
    val isSuccess: Boolean,
    val errorMessage: String = "",
    val resolver: String = "Local System Resolver"
)

class DnsService {

    suspend fun queryDns(target: String, recordType: String = "A"): DnsQueryResult = withContext(Dispatchers.IO) {
        val cleanTarget = target.trim()
        val start = System.nanoTime()

        // Handle basic A and AAAA via native resolver first
        if (recordType == "A" || recordType == "AAAA") {
            try {
                val addresses = InetAddress.getAllByName(cleanTarget)
                val items = addresses.mapNotNull { addr ->
                    val host = addr.hostAddress ?: return@mapNotNull null
                    val isIpv6 = host.contains(":")
                    if (recordType == "A" && !isIpv6) {
                        DnsRecordItem("A", host)
                    } else if (recordType == "AAAA" && isIpv6) {
                        DnsRecordItem("AAAA", host)
                    } else null
                }
                val duration = (System.nanoTime() - start) / 1_000_000.0
                return@withContext DnsQueryResult(
                    query = cleanTarget,
                    type = recordType,
                    records = items,
                    durationMs = duration,
                    isSuccess = items.isNotEmpty(),
                    errorMessage = if (items.isEmpty()) "No matching $recordType records found" else "",
                    resolver = "System DNS Resolver"
                )
            } catch (e: Exception) {
                val duration = (System.nanoTime() - start) / 1_000_000.0
                return@withContext DnsQueryResult(
                    query = cleanTarget,
                    type = recordType,
                    records = emptyList(),
                    durationMs = duration,
                    isSuccess = false,
                    errorMessage = e.localizedMessage ?: "DNS resolution failed",
                    resolver = "System DNS Resolver"
                )
            }
        }

        // For MX, TXT, NS, CNAME, PTR: use standard DNS-over-HTTPS (Cloudflare DoH)
        try {
            val typeParam = recordType.uppercase()
            val url = URL("https://cloudflare-dns.com/dns-query?name=${cleanTarget}&type=${typeParam}")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/dns-json")
            conn.connectTimeout = 4000
            conn.readTimeout = 4000

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val status = json.optInt("Status", -1)
                val answers = json.optJSONArray("Answer")

                val recordsList = mutableListOf<DnsRecordItem>()
                if (answers != null) {
                    for (i in 0 until answers.length()) {
                        val obj = answers.getJSONObject(i)
                        val data = obj.optString("data", "").trim('"')
                        val ttl = obj.optInt("TTL", 0)
                        recordsList.add(DnsRecordItem(type = typeParam, value = data, ttl = ttl))
                    }
                }

                val duration = (System.nanoTime() - start) / 1_000_000.0
                val isSuccess = status == 0 && recordsList.isNotEmpty()
                val errorMsg = when {
                    status == 3 -> "Domain does not exist (NXDOMAIN)"
                    status != 0 -> "Resolver returned status code $status"
                    recordsList.isEmpty() -> "No matching $recordType records found"
                    else -> ""
                }

                return@withContext DnsQueryResult(
                    query = cleanTarget,
                    type = typeParam,
                    records = recordsList,
                    durationMs = duration,
                    isSuccess = isSuccess,
                    errorMessage = errorMsg,
                    resolver = "Cloudflare DoH (1.1.1.1)"
                )
            }
        } catch (e: Exception) {
            val duration = (System.nanoTime() - start) / 1_000_000.0
            return@withContext DnsQueryResult(
                query = cleanTarget,
                type = recordType,
                records = emptyList(),
                durationMs = duration,
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "DoH query error",
                resolver = "Cloudflare DoH"
            )
        }

        val duration = (System.nanoTime() - start) / 1_000_000.0
        DnsQueryResult(
            query = cleanTarget,
            type = recordType,
            records = emptyList(),
            durationMs = duration,
            isSuccess = false,
            errorMessage = "Lookup timed out or failed"
        )
    }

    suspend fun reverseDns(ip: String): DnsQueryResult = withContext(Dispatchers.IO) {
        val cleanIp = ip.trim()
        val start = System.nanoTime()

        try {
            val addr = InetAddress.getByName(cleanIp)
            val canonical = addr.canonicalHostName
            val duration = (System.nanoTime() - start) / 1_000_000.0

            if (!canonical.isNullOrBlank() && canonical != cleanIp) {
                return@withContext DnsQueryResult(
                    query = cleanIp,
                    type = "PTR",
                    records = listOf(DnsRecordItem(type = "PTR", value = canonical)),
                    durationMs = duration,
                    isSuccess = true,
                    resolver = "System Reverse Resolver"
                )
            }
        } catch (_: Exception) {}

        // Fallback to DoH in-addr.arpa
        val reversed = cleanIp.split(".").reversed().joinToString(".") + ".in-addr.arpa"
        return@withContext queryDns(reversed, "PTR")
    }
}
