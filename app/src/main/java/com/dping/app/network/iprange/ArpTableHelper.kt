package com.dping.app.network.iprange

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface

object ArpTableHelper {

    fun getMacForIp(ip: String): String? {
        // 1. Try local device interfaces first
        getLocalMac(ip)?.let { return it }

        // 2. Try reading /proc/net/arp (standard Linux/Android ARP table)
        try {
            val file = File("/proc/net/arp")
            if (file.exists() && file.canRead()) {
                BufferedReader(FileReader(file)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val parts = line!!.split("\\s+".toRegex())
                        if (parts.size >= 4 && parts[0] == ip) {
                            val mac = parts[3].trim().uppercase()
                            if (isValidMac(mac)) {
                                return mac
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Try runtime `ip neigh` command
        try {
            val process = Runtime.getRuntime().exec(arrayOf("ip", "neigh"))
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val parts = line!!.split("\\s+".toRegex())
                    if (parts.size >= 5 && parts[0] == ip) {
                        val lladdrIdx = parts.indexOf("lladdr")
                        if (lladdrIdx != -1 && lladdrIdx + 1 < parts.size) {
                            val mac = parts[lladdrIdx + 1].trim().uppercase()
                            if (isValidMac(mac)) return mac
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return null
    }

    private fun getLocalMac(ip: String): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (ni in interfaces) {
                for (addr in ni.inetAddresses) {
                    if (addr.hostAddress == ip) {
                        val macBytes = ni.hardwareAddress ?: continue
                        val sb = StringBuilder()
                        for (i in macBytes.indices) {
                            sb.append(String.format("%02X", macBytes[i]))
                            if (i < macBytes.size - 1) sb.append(":")
                        }
                        val mac = sb.toString()
                        if (isValidMac(mac)) return mac
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    fun isValidMac(mac: String): Boolean {
        if (mac.isBlank()) return false
        if (mac.equals("00:00:00:00:00:00", ignoreCase = true)) return false
        if (mac.equals("02:00:00:00:00:00", ignoreCase = true)) return false
        return mac.matches("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})".toRegex())
    }

    fun getVendorForMac(mac: String?): String? {
        if (mac == null || !isValidMac(mac)) return null
        val prefix = mac.replace(":", "").replace("-", "").take(6).uppercase()
        return when (prefix) {
            "005056", "000C29", "000569" -> "VMware"
            "001A11", "F09FC2", "B827EB", "DCA632", "E45F01" -> "Raspberry Pi"
            "00155D" -> "Microsoft Hyper-V"
            "080027", "0A0027" -> "VirtualBox"
            "ACDE48", "F4F5DB", "3C0630", "F01898", "64200C" -> "Apple Inc."
            "50EC50", "44650D", "D46A6A", "BC8CCD" -> "Samsung"
            "001B63", "843838", "A4B1E9", "BC5FF4" -> "Apple Inc."
            "7085C2", "D89695", "9C35EB", "A0F3C1" -> "Intel"
            "E4AAEC", "50D4F7", "C46E1F", "E848B8" -> "TP-Link"
            "0014D1", "0024A5", "0026F2", "204E7F" -> "NETGEAR"
            "00000C", "000142", "000143", "0001C7" -> "Cisco"
            "74DA38", "A020A6", "E06995" -> "Huawei"
            "54E43A", "8828B3", "B40E7D" -> "Xiaomi"
            "001E8C", "001A70", "00180A" -> "Asus"
            "001018", "0009B7" -> "Broadcom"
            else -> null
        }
    }

    fun queryNetBiosName(ip: String, timeoutMs: Int = 300): String? {
        try {
            DatagramSocket().use { socket ->
                socket.soTimeout = timeoutMs
                val address = InetAddress.getByName(ip)
                val query = byteArrayOf(
                    0x82.toByte(), 0x28.toByte(),
                    0x00, 0x00,
                    0x00, 0x01,
                    0x00, 0x00, 0x00, 0x00,
                    0x20,
                    0x43, 0x4B, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
                    0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
                    0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
                    0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
                    0x00,
                    0x00, 0x21,
                    0x00, 0x01
                )
                val packet = DatagramPacket(query, query.size, address, 137)
                socket.send(packet)

                val buffer = ByteArray(1024)
                val recvPacket = DatagramPacket(buffer, buffer.size)
                socket.receive(recvPacket)

                val data = recvPacket.data
                val len = recvPacket.length
                if (len > 57) {
                    val numNames = data[56].toInt() and 0xFF
                    if (numNames > 0 && len >= 57 + 18) {
                        val nameBytes = data.copyOfRange(57, 57 + 15)
                        val rawName = String(nameBytes, Charsets.US_ASCII).trim()
                        if (rawName.isNotBlank() && rawName.all { it.isLetterOrDigit() || it == '-' || it == '_' }) {
                            return rawName
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
