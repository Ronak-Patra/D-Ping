package com.dping.app.utils

data class SubnetResult(
    val ip: String,
    val prefix: Int,
    val networkAddress: String,
    val subnetMask: String,
    val wildcardMask: String,
    val broadcastAddress: String,
    val firstUsableHost: String,
    val lastUsableHost: String,
    val totalHosts: Long,
    val usableHosts: Long,
    val cidrNotation: String,
    val binaryMask: String
)

object SubnetCalculator {

    fun calculate(ipStr: String, prefix: Int): SubnetResult? {
        val ipParts = ipStr.trim().split(".")
        if (ipParts.size != 4) return null
        val octets = ipParts.map { it.toIntOrNull() ?: return null }
        if (octets.any { it !in 0..255 } || prefix !in 0..32) return null

        val ipLong = (octets[0].toLong() shl 24) or
                (octets[1].toLong() shl 16) or
                (octets[2].toLong() shl 8) or
                octets[3].toLong()

        val maskLong = if (prefix == 0) 0L else (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
        val wildcardLong = maskLong.inv() and 0xFFFFFFFFL
        val networkLong = ipLong and maskLong
        val broadcastLong = networkLong or wildcardLong

        val totalHosts = if (prefix == 0) 4294967296L else (1L shl (32 - prefix))

        val (firstUsable, lastUsable, usableHosts) = when (prefix) {
            32 -> Triple(longToIp(ipLong), longToIp(ipLong), 1L)
            31 -> Triple(longToIp(networkLong), longToIp(broadcastLong), 2L) // RFC 3021 point-to-point
            else -> Triple(
                longToIp(networkLong + 1),
                longToIp(broadcastLong - 1),
                (totalHosts - 2).coerceAtLeast(0)
            )
        }

        val binaryMask = String.format("%32s", java.lang.Long.toBinaryString(maskLong))
            .replace(' ', '0')
            .chunked(8)
            .joinToString(".")

        return SubnetResult(
            ip = ipStr.trim(),
            prefix = prefix,
            networkAddress = longToIp(networkLong),
            subnetMask = longToIp(maskLong),
            wildcardMask = longToIp(wildcardLong),
            broadcastAddress = longToIp(broadcastLong),
            firstUsableHost = firstUsable,
            lastUsableHost = lastUsable,
            totalHosts = totalHosts,
            usableHosts = usableHosts,
            cidrNotation = "${longToIp(networkLong)}/$prefix",
            binaryMask = binaryMask
        )
    }

    private fun longToIp(value: Long): String {
        return "${(value shr 24) and 0xFF}.${(value shr 16) and 0xFF}.${(value shr 8) and 0xFF}.${value and 0xFF}"
    }
}
