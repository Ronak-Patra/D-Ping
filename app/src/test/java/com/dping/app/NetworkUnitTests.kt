package com.dping.app

import com.dping.app.network.iprange.IpRangeScanner
import com.dping.app.utils.ExportManager
import com.dping.app.utils.SubnetCalculator
import org.junit.Assert.*
import org.junit.Test

class NetworkUnitTests {

    @Test
    fun testSubnetCalculatorStandardClassC() {
        val result = SubnetCalculator.calculate("192.168.1.50", 24)
        assertNotNull(result)
        assertEquals("192.168.1.0", result?.networkAddress)
        assertEquals("255.255.255.0", result?.subnetMask)
        assertEquals("0.0.0.255", result?.wildcardMask)
        assertEquals("192.168.1.255", result?.broadcastAddress)
        assertEquals("192.168.1.1", result?.firstUsableHost)
        assertEquals("192.168.1.254", result?.lastUsableHost)
        assertEquals(256L, result?.totalHosts)
        assertEquals(254L, result?.usableHosts)
    }

    @Test
    fun testSubnetCalculatorEdgeCaseSlash31() {
        val result = SubnetCalculator.calculate("10.0.0.0", 31)
        assertNotNull(result)
        assertEquals("10.0.0.0", result?.firstUsableHost)
        assertEquals("10.0.0.1", result?.lastUsableHost)
        assertEquals(2L, result?.usableHosts)
    }

    @Test
    fun testSubnetCalculatorEdgeCaseSlash32() {
        val result = SubnetCalculator.calculate("10.0.0.5", 32)
        assertNotNull(result)
        assertEquals("10.0.0.5", result?.firstUsableHost)
        assertEquals("10.0.0.5", result?.lastUsableHost)
        assertEquals(1L, result?.usableHosts)
    }

    @Test
    fun testIpRangeConversions() {
        val ip = "192.168.1.1"
        val longVal = IpRangeScanner.ipToLong(ip)
        assertNotNull(longVal)
        val convertedBack = IpRangeScanner.longToIp(longVal!!)
        assertEquals(ip, convertedBack)

        // Invalid IP handling
        assertNull(IpRangeScanner.ipToLong("999.1.1.1"))
        assertNull(IpRangeScanner.ipToLong("not.an.ip.address"))
    }

    @Test
    fun testCsvExportEscaping() {
        val records = listOf(
            com.dping.app.data.model.DiagnosticRecord(
                id = 1,
                testType = "Ping",
                target = "example.com",
                timestamp = 1700000000000L,
                durationMs = 120,
                isSuccess = true,
                summary = "Avg: 15.2ms, Loss: 0%",
                countryName = "United States"
            )
        )
        val csv = ExportManager.exportHistoryToCsv(records)
        assertTrue(csv.contains("ID,Test Type,Target"))
        assertTrue(csv.contains("example.com"))
        assertTrue(csv.contains("\"Avg: 15.2ms, Loss: 0%\"")) // comma escaped
    }
}
