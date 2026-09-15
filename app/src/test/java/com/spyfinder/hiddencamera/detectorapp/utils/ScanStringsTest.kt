package com.spyfinder.hiddencamera.detectorapp.utils

import com.spyfinder.hiddencamera.detectorapp.R
import org.junit.Assert.*
import org.junit.Test

class ScanStringsTest {
    private fun lookup(id: Int, args: List<String>): String = when (id) {
        R.string.scan_complete_prefix -> "扫描完成。"
        R.string.scan_summary -> "已检查 ${args[0]}/${args[1]} 个地址，${args[2]} 台响应。"
        R.string.scan_incomplete_count -> "${args[0]} 台未完成。"
        R.string.scan_limits_note -> "结果仅供参考。"
        R.string.evidence_rtsp -> "端口 ${args[0]} 响应视频协议"
        R.string.evidence_source_local -> "本机"
        R.string.current_phone -> "当前手机"
        R.string.scan_discovery_progress -> "已检查 ${args[0]}/${args[1]}"
        R.string.scan_unresolved_services -> "${args[0]} 条公告未解析。"
        R.string.scan_channel_send_failed -> "${args[0]} 请求发送失败，请重试。"
        R.string.scan_analysis_remaining -> "待分析 ${args[0]} 台"
        else -> error("Unexpected resource $id")
    }

    @Test fun translatesExistingHistoryWithoutLosingMetadataOrCounts() {
        val value = "2026-09-14 10:00:00 · 192.168.1.2/24\nScan completed. Checked 254 of 254 IPv4 addresses; 3 devices responded. 1 devices could not be fully analyzed. Devices that do not respond or are isolated by the network may be missed. No result proves a room is safe."
        val formatted = ScanStrings.format(value, ::lookup)
        assertEquals("2026-09-14 10:00:00 · 192.168.1.2/24\n扫描完成。已检查 254/254 个地址，3 台响应。1 台未完成。结果仅供参考。", formatted)
    }

    @Test fun translatesEvidenceWithoutChangingItsProtocolOrPort() {
        assertEquals("RTSP: 端口 8554 响应视频协议", ScanStrings.format("RTSP: Video protocol responded on port 8554; verify the device manually", ::lookup))
        assertEquals("本机: 当前手机", ScanStrings.format("Local: Current phone", ::lookup))
    }

    @Test fun preservesExternalServiceNamesAndAddresses() {
        val value = "mDNS: _rtsp._tcp.local: Camera ABC._rtsp._tcp.local"
        assertEquals(value, ScanStrings.format(value, ::lookup))
        assertEquals("192.168.1.18", ScanStrings.format("192.168.1.18", ::lookup))
    }

    @Test fun progressUsesActualCounts() {
        assertEquals("已检查 128/1024", ScanStrings.format("Discovering: 128/1024 addresses checked", ::lookup))
    }
    @Test fun unresolvedServiceCountIsLocalized() {
        assertEquals("扫描完成。2 条公告未解析。", ScanStrings.format("Scan completed. 2 service announcements could not be resolved to an in-scope IPv4 endpoint.", ::lookup))
    }
    @Test fun channelFailureSurvivesHistoryFormatting() {
        val formatted = ScanStrings.format("Scan completed. 2 service announcements could not be resolved to an in-scope IPv4 endpoint.\nmDNS discovery request could not be sent. Results may be incomplete. Reconnect to Wi-Fi and retry.", ::lookup)
        assertEquals("扫描完成。2 条公告未解析。\nmDNS 请求发送失败，请重试。", formatted)
        assertEquals("待分析 12 台", ScanStrings.format("Devices awaiting analysis: 12", ::lookup))
    }
}
