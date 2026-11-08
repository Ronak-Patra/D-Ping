package com.dping.app.network.discovery

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URL

data class MyIpInfo(
    val wifiIp: String = "0.0.0.0",
    val localIpv6: String = "",
    val publicIp: String = "Unavailable",
    val country: String = "Unavailable",
    val countryCode: String = "",
    val flagEmoji: String = "",
    val isp: String = "",
    val city: String = "",
    val region: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timezone: String = "",
    val asn: String = "",
    val reverseDns: String = "",
    val gatewayIp: String = "192.168.1.1",
    val subnetMask: String = "255.255.255.0",
    val dnsServers: List<String> = emptyList(),
    val isConnected: Boolean = false,
    val connectionType: String = "Disconnected"
)

data class GatewayInfo(
    val gatewayIp: String = "Unavailable",
    val localIp: String = "Unavailable",
    val dnsServers: List<String> = emptyList(),
    val isReachable: Boolean = false,
    val latencyMs: Double = 0.0
)

data class InterfaceDetails(
    val name: String,
    val displayName: String,
    val isUp: Boolean,
    val isLoopback: Boolean,
    val isVpn: Boolean,
    val mtu: Int,
    val ipv4Addresses: List<String>,
    val ipv6Addresses: List<String>
)

class NetworkInfoProvider(private val context: Context) {

    suspend fun getMyIpInfo(): MyIpInfo = withContext(Dispatchers.IO) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = activeNetwork?.let { cm.getNetworkCapabilities(it) }

        var isConnected = false
        var connType = "Disconnected"

        if (caps != null) {
            isConnected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            connType = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                else -> "Active Network"
            }
        }

        // Local IPv4 & IPv6
        val localIpv4 = getLocalIpv4()
        val localIpv6 = getLocalIpv6()

        // Attempt geo/IP lookup if connected or if device has a local IP assigned
        val hasLocalAddress = localIpv4 != "Unavailable" && localIpv4 != "0.0.0.0" && localIpv4 != "127.0.0.1"
        val shouldAttempt = isConnected || hasLocalAddress

        val geo = if (shouldAttempt) {
            fetchGeoLocation()
        } else {
            GeoData()
        }

        // If geo lookup succeeded, device is definitely connected
        if (geo.publicIp != "Unavailable" && geo.publicIp.isNotBlank()) {
            isConnected = true
            if (connType == "Disconnected") {
                connType = if (localIpv4.startsWith("192.168.") || localIpv4.startsWith("10.")) "Wi-Fi" else "Connected"
            }
        }

        var reverseDns = ""
        if (geo.publicIp != "Unavailable" && geo.publicIp.isNotBlank()) {
            try {
                val addr = InetAddress.getByName(geo.publicIp)
                val host = addr.canonicalHostName
                if (!host.isNullOrBlank() && host != geo.publicIp) {
                    reverseDns = host
                }
            } catch (_: Exception) {}
        }

        val gwInfo = getGatewayInfo()

        MyIpInfo(
            wifiIp = localIpv4,
            localIpv6 = localIpv6,
            publicIp = geo.publicIp,
            country = geo.country,
            countryCode = geo.countryCode,
            flagEmoji = geo.flagEmoji,
            isp = geo.isp,
            city = geo.city,
            region = geo.region,
            latitude = geo.latitude,
            longitude = geo.longitude,
            timezone = geo.timezone,
            asn = geo.asn,
            reverseDns = reverseDns,
            gatewayIp = if (gwInfo.gatewayIp != "Unavailable") gwInfo.gatewayIp else "192.168.1.1",
            subnetMask = "255.255.255.0",
            dnsServers = gwInfo.dnsServers.ifEmpty { listOf("1.1.1.1", "8.8.8.8") },
            isConnected = isConnected,
            connectionType = connType
        )
    }

    fun getLocalIpv4(): String {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val linkProps = cm?.getLinkProperties(cm.activeNetwork)
            if (linkProps != null) {
                for (linkAddr in linkProps.linkAddresses) {
                    val addr = linkAddr.address
                    if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                        val host = addr.hostAddress
                        if (!host.isNullOrBlank() && host != "127.0.0.1") return host
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "0.0.0.0"
            var fallbackIp: String? = null
            while (interfaces.hasMoreElements()) {
                val nif = interfaces.nextElement()
                if (nif.isLoopback || !nif.isUp) continue
                val addresses = nif.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress
                        if (!host.isNullOrBlank() && host != "127.0.0.1") {
                            if (!addr.isLinkLocalAddress) return host
                            if (fallbackIp == null) fallbackIp = host
                        }
                    }
                }
            }
            if (fallbackIp != null) return fallbackIp
        } catch (_: Exception) {}
        return "0.0.0.0"
    }

    fun getLocalIpv6(): String {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val linkProps = cm?.getLinkProperties(cm.activeNetwork)
            if (linkProps != null) {
                for (linkAddr in linkProps.linkAddresses) {
                    val addr = linkAddr.address
                    if (addr is Inet6Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress?.split("%")?.get(0)
                        if (!host.isNullOrBlank() && host != "::1") return host
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return ""
            while (interfaces.hasMoreElements()) {
                val nif = interfaces.nextElement()
                if (nif.isLoopback || !nif.isUp) continue
                val addresses = nif.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet6Address) {
                        val host = addr.hostAddress?.split("%")?.get(0)
                        if (!host.isNullOrBlank() && host != "::1") {
                            return host
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return ""
    }

    suspend fun getGatewayInfo(): GatewayInfo = withContext(Dispatchers.IO) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val linkProps = cm?.getLinkProperties(cm.activeNetwork)

        var gatewayIp = "Unavailable"
        val dnsList = mutableListOf<String>()

        if (linkProps != null) {
            for (route in linkProps.routes) {
                if (route.isDefaultRoute && route.gateway != null) {
                    gatewayIp = route.gateway?.hostAddress ?: "Unavailable"
                    break
                }
            }
            linkProps.dnsServers.forEach { dns ->
                dns.hostAddress?.let { dnsList.add(it) }
            }
        }

        var isReachable = false
        var latencyMs = 0.0

        if (gatewayIp != "Unavailable") {
            try {
                val start = System.nanoTime()
                val addr = InetAddress.getByName(gatewayIp)
                if (addr.isReachable(1500)) {
                    isReachable = true
                    latencyMs = (System.nanoTime() - start) / 1_000_000.0
                }
            } catch (_: Exception) {}
        }

        val localIp = getLocalIpv4()

        GatewayInfo(
            gatewayIp = gatewayIp,
            localIp = if (localIp != "0.0.0.0") localIp else "Unavailable",
            dnsServers = dnsList,
            isReachable = isReachable,
            latencyMs = latencyMs
        )
    }

    fun getAllInterfaces(): List<InterfaceDetails> {
        val result = mutableListOf<InterfaceDetails>()
        try {
            val list = NetworkInterface.getNetworkInterfaces()
            while (list.hasMoreElements()) {
                val nif = list.nextElement()
                val ipv4 = mutableListOf<String>()
                val ipv6 = mutableListOf<String>()

                val addresses = nif.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address) {
                        addr.hostAddress?.let { ipv4.add(it) }
                    } else if (addr is Inet6Address) {
                        addr.hostAddress?.split("%")?.get(0)?.let { ipv6.add(it) }
                    }
                }

                result.add(
                    InterfaceDetails(
                        name = nif.name,
                        displayName = nif.displayName ?: nif.name,
                        isUp = nif.isUp,
                        isLoopback = nif.isLoopback,
                        isVpn = nif.name.startsWith("tun") || nif.name.startsWith("ppp"),
                        mtu = try { nif.mtu } catch (_: Exception) { 1500 },
                        ipv4Addresses = ipv4,
                        ipv6Addresses = ipv6
                    )
                )
            }
        } catch (_: Exception) {}
        return result.sortedWith(
            compareByDescending<InterfaceDetails> { getInterfacePriority(it) }
                .thenBy { it.name }
        )
    }

    private fun getInterfacePriority(iface: InterfaceDetails): Int {
        val name = iface.name.lowercase()
        val hasRealIpv4 = iface.isUp && iface.ipv4Addresses.any {
            it != "127.0.0.1" && !it.startsWith("169.254.") && !it.startsWith("0.")
        }

        return when {
            // Connected Wi-Fi or Ethernet with active LAN IPv4 (e.g. wlan0 with 192.168.0.102) -> FIRST!
            hasRealIpv4 && (name.startsWith("wlan") || name.startsWith("wifi") || name.startsWith("eth")) -> 100
            // Connected Cellular modem with active IPv4
            hasRealIpv4 && (name.startsWith("rmnet") || name.startsWith("ccmni") || name.startsWith("pdp")) -> 90
            // Any other interface with active IPv4
            hasRealIpv4 -> 85
            // Active VPN tunnels
            iface.isVpn || name.startsWith("tun") || name.startsWith("ppp") || name.startsWith("wg") -> 70
            // Physical adapters (Wi-Fi, Ethernet, Cellular) without IP
            name.startsWith("wlan") || name.startsWith("wifi") -> 60
            name.startsWith("eth") -> 55
            name.startsWith("rmnet") || name.startsWith("ccmni") || name.startsWith("pdp") -> 50
            name.startsWith("rndis") || name.startsWith("usb") -> 45
            name.startsWith("bt") || name.startsWith("bnep") -> 40
            name.startsWith("p2p") -> 35
            // Loopback interface
            iface.isLoopback || name == "lo" -> 20
            // Virtual dummy / tunnel interfaces -> at the bottom
            name.startsWith("dummy") -> 10
            name.startsWith("sit") || name.startsWith("ip6tnl") || name.startsWith("vti") -> 5
            else -> 30
        }
    }

    private data class GeoData(
        val publicIp: String = "Unavailable",
        val country: String = "Unavailable",
        val countryCode: String = "",
        val flagEmoji: String = "",
        val city: String = "",
        val region: String = "",
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val timezone: String = "",
        val asn: String = "",
        val isp: String = ""
    )

    private fun fetchGeoLocation(): GeoData {
        val userAgent = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        // 1. Primary: ipwho.is (fast, HTTPS, generous limits, returns rich geo + flag emoji + ISP/ASN)
        try {
            val url = URL("https://ipwho.is/")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                if (json.optBoolean("success", true)) {
                    val ip = json.optString("ip", "")
                    if (ip.isNotBlank()) {
                        val code = json.optString("country_code", "")
                        val emoji = json.optJSONObject("flag")?.optString("emoji", "") ?: ""
                        val connObj = json.optJSONObject("connection")
                        val tzObj = json.optJSONObject("timezone")
                        val asnVal = connObj?.opt("asn")?.toString() ?: ""
                        return GeoData(
                            publicIp = ip,
                            country = json.optString("country", "Unavailable"),
                            countryCode = code,
                            flagEmoji = if (emoji.isNotBlank()) emoji else if (code.isNotBlank()) countryCodeToEmoji(code) else "",
                            city = json.optString("city", ""),
                            region = json.optString("region", ""),
                            latitude = json.optDouble("latitude", 0.0),
                            longitude = json.optDouble("longitude", 0.0),
                            timezone = tzObj?.optString("id", "") ?: "",
                            asn = if (asnVal.isNotBlank() && !asnVal.startsWith("AS", ignoreCase = true)) "AS$asnVal" else asnVal,
                            isp = connObj?.optString("isp", "") ?: json.optString("org", "")
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Secondary fallback: freeipapi.com (high rate limit, reliable, structured)
        try {
            val url = URL("https://freeipapi.com/api/json")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val ip = json.optString("ipAddress", "")
                if (ip.isNotBlank()) {
                    val code = json.optString("countryCode", "")
                    val tzArray = json.optJSONArray("timeZones")
                    val tz = if (tzArray != null && tzArray.length() > 0) tzArray.optString(0, "") else ""
                    val asnVal = json.optString("asn", "")
                    return GeoData(
                        publicIp = ip,
                        country = json.optString("countryName", "Unavailable"),
                        countryCode = code,
                        flagEmoji = if (code.isNotBlank()) countryCodeToEmoji(code) else "",
                        city = json.optString("cityName", ""),
                        region = json.optString("regionName", ""),
                        latitude = json.optDouble("latitude", 0.0),
                        longitude = json.optDouble("longitude", 0.0),
                        timezone = tz,
                        asn = if (asnVal.isNotBlank() && !asnVal.startsWith("AS", ignoreCase = true)) "AS$asnVal" else asnVal,
                        isp = json.optString("asnOrganization", "")
                    )
                }
            }
        } catch (_: Exception) {}

        // 3. Tertiary fallback: ipapi.co
        try {
            val url = URL("https://ipapi.co/json/")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                if (!json.has("error")) {
                    val ip = json.optString("ip", "")
                    if (ip.isNotBlank()) {
                        val code = json.optString("country_code", "")
                        return GeoData(
                            publicIp = ip,
                            country = json.optString("country_name", "Unavailable"),
                            countryCode = code,
                            flagEmoji = if (code.isNotBlank()) countryCodeToEmoji(code) else "",
                            city = json.optString("city", ""),
                            region = json.optString("region", ""),
                            latitude = json.optDouble("latitude", 0.0),
                            longitude = json.optDouble("longitude", 0.0),
                            timezone = json.optString("timezone", ""),
                            asn = json.optString("asn", ""),
                            isp = json.optString("org", "")
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        // 4. Quaternary fallback: ipinfo.io
        try {
            val url = URL("https://ipinfo.io/json")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val ip = json.optString("ip", "")
                if (ip.isNotBlank()) {
                    val code = json.optString("country", "")
                    val loc = json.optString("loc", "")
                    var lat = 0.0
                    var lon = 0.0
                    if (loc.contains(",")) {
                        val parts = loc.split(",")
                        lat = parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
                        lon = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
                    }
                    return GeoData(
                        publicIp = ip,
                        country = if (code.isNotBlank()) java.util.Locale("", code).displayCountry else "Unavailable",
                        countryCode = code,
                        flagEmoji = if (code.isNotBlank()) countryCodeToEmoji(code) else "",
                        city = json.optString("city", ""),
                        region = json.optString("region", ""),
                        latitude = lat,
                        longitude = lon,
                        timezone = json.optString("timezone", ""),
                        asn = "",
                        isp = json.optString("org", "")
                    )
                }
            }
        } catch (_: Exception) {}

        // 5. Raw IP-only fallback if all geo endpoints were unreachable
        val fallbackIp = try {
            val url = URL("https://api64.ipify.org")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
            }
            if (conn.responseCode == 200) conn.inputStream.bufferedReader().use { it.readLine() ?: "Unavailable" } else "Unavailable"
        } catch (_: Exception) { "Unavailable" }

        return GeoData(publicIp = fallbackIp)
    }

    companion object {
        fun countryCodeToEmoji(countryCode: String): String {
            if (countryCode.length != 2) return ""
            val upper = countryCode.uppercase()
            val firstChar = Character.codePointAt(upper, 0) - 0x41 + 0x1F1E6
            val secondChar = Character.codePointAt(upper, 1) - 0x41 + 0x1F1E6
            return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
        }
    }
}
