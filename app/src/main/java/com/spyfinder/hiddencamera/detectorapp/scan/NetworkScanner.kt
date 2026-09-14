package com.spyfinder.hiddencamera.detectorapp.scan

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.SystemClock
import android.util.Xml
import com.spyfinder.hiddencamera.detectorapp.R
import com.spyfinder.hiddencamera.detectorapp.model.WifiDevice
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class WifiTarget(val network: Network, val ip: String, val prefix: Int, val gateway: String?)
data class ScanOutput(val devices: List<WifiDevice>, val partial: Boolean, val message: String)

class NetworkScanner(private val context: Context, val resources: ScanResources = ScanResources()) {
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val found = ConcurrentHashMap<String, List<Evidence>>()
    private val analyzed = ConcurrentHashMap<String, WifiDevice>()
    private val checked = AtomicInteger()
    private var planned = 0
    private var reachable = 0
    fun snapshot(): List<WifiDevice> = found.keys.map { ip -> analyzed[ip] ?: device(ip, found[ip].orEmpty(), false, true) }.sortedBy { it.ip }
    private var target: WifiTarget? = null

    fun selectNetwork(): WifiTarget {
        val networks = cm.allNetworks.filter { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }
        val network = networks.firstOrNull { it == cm.activeNetwork } ?: networks.singleOrNull()
            ?: throw IllegalStateException(if (networks.isEmpty()) "Connect to Wi-Fi before scanning." else "Multiple Wi-Fi networks are available. Select one network and retry.")
        val properties = cm.getLinkProperties(network) ?: error("Wi-Fi address is unavailable. Reconnect and retry.")
        val address = properties.linkAddresses.firstOrNull { it.address is Inet4Address }
            ?: error("This network has no IPv4 address. IPv6 scanning is not supported yet.")
        return WifiTarget(network, address.address.hostAddress!!, address.prefixLength,
            properties.routes.firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }?.gateway?.hostAddress).also { target = it }
    }

    fun networkUnchanged(t: WifiTarget): Boolean {
        if (cm.getNetworkCapabilities(t.network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) != true) return false
        return cm.getLinkProperties(t.network)?.linkAddresses?.any { it.address.hostAddress == t.ip && it.prefixLength == t.prefix } == true
    }

    suspend fun scan(t: WifiTarget, onProgress: (String, List<WifiDevice>, Int) -> Unit): ScanOutput = withContext(Dispatchers.IO) {
        target = t
        val targets = ScanRules.targets(t.ip, t.prefix, t.gateway)
        planned = targets.addresses.size
        found[t.ip] = listOf(Evidence("Local", "Current phone"))
        onProgress("Discovering devices in ${t.ip}/${t.prefix}…", snapshot(), 0)
        discoverServices(t)
        // TCP discovery works even when ICMP is blocked. Refusal also proves the host answered.
        val discoveryCount = AtomicInteger()
        bounded(targets.addresses, 32) { ip ->
            if (!found.containsKey(ip)) {
                for (port in listOf(80, 443, 554)) {
                    if (reachable(t, ip, port)) { found.putIfAbsent(ip, emptyList()); break }
                }
            }
            val count = discoveryCount.incrementAndGet()
            if (count % 16 == 0 || count == planned) onProgress("Discovering: $count/$planned addresses checked", snapshot(), count * 60 / planned.coerceAtLeast(1))
        }
        reachable = found.size
        onProgress("Analyzing services: 0/$reachable devices", snapshot(), 60)
        bounded(found.keys.toList(), 8) { ip ->
            analyzed[ip] = analyze(t, ip, found[ip].orEmpty())
            val count = checked.incrementAndGet()
            onProgress("Analyzing services: $count/$reachable devices", snapshot(), 60 + count * 39 / reachable.coerceAtLeast(1))
        }
        val devices = snapshot()
        val incomplete = devices.count { !it.analysisComplete }
        val partial = targets.limited || incomplete > 0
        ScanOutput(devices, partial, buildString {
            append(if (partial) "Partially completed. " else "Scan completed. ")
            append("Checked $planned of ${targets.total} IPv4 addresses; ${devices.size} devices responded.")
            if (incomplete > 0) append(" $incomplete devices could not be fully analyzed.")
            append(" Devices that do not respond or are isolated by the network may be missed. No result proves a room is safe.")
        })
    }

    private suspend fun <T> bounded(items: List<T>, workers: Int, block: suspend (T) -> Unit) = coroutineScope {
        val queue = Channel<T>(workers)
        launch { try { for (item in items) queue.send(item) } finally { queue.close() } }
        repeat(minOf(workers, items.size)) { launch { for (item in queue) { ensureActive(); if (resources.closed) throw CancellationException(); block(item) } } }
    }

    private fun reachable(t: WifiTarget, ip: String, port: Int): Boolean {
        val socket = resources.track(t.network.socketFactory.createSocket())
        return try { socket.connect(InetSocketAddress(ip, port), 450); true }
        catch (_: ConnectException) { true }
        catch (_: java.io.IOException) { false }
        finally { resources.release(socket) }
    }

    private fun device(ip: String, evidence: List<Evidence>, complete: Boolean, incomplete: Boolean): WifiDevice {
        val self = ip == target?.ip
        val finding = ScanRules.finding(evidence, incomplete)
        val type = when { self -> "Phone"; ip == target?.gateway -> "Router"; finding == Finding.CAMERA_FEATURES -> "Video service"; else -> "Unknown" }
        return WifiDevice(if (self) "Current phone" else type, type, ip, R.drawable.svg_icon_sensor, 0, 0,
            riskLevel = if (!self && finding == Finding.CAMERA_FEATURES) 1 else 0,
            finding = finding, evidence = evidence.map { "${it.source}: ${it.detail}" }, isCurrentPhone = self,
            analysisComplete = complete, ruleVersion = ScanRules.VERSION)
    }

    private fun analyze(t: WifiTarget, ip: String, discovered: List<Evidence>): WifiDevice {
        val evidence = discovered.toMutableList()
        if (ip == t.ip) return device(ip, evidence, true, false)
        if (ip == t.gateway) evidence.add(Evidence("Network", "Configured Wi-Fi gateway"))
        val deadline = SystemClock.elapsedRealtime() + 8_000
        var incomplete = false
        for (port in listOf(554, 8554, 80, 443, 5000)) {
            if (resources.closed) throw CancellationException()
            val remaining = deadline - SystemClock.elapsedRealtime()
            if (remaining <= 0) { incomplete = true; break }
            val socket = resources.track(t.network.socketFactory.createSocket())
            try {
                socket.soTimeout = minOf(1500L, remaining).toInt()
                socket.connect(InetSocketAddress(ip, port), minOf(1500L, remaining).toInt())
                evidence.add(Evidence("TCP", "Port $port is open (service not confirmed)"))
                if (port in listOf(554, 8554, 80, 5000)) {
                    val request = if (port == 554 || port == 8554) "OPTIONS rtsp://$ip:$port/ RTSP/1.0\r\nCSeq: 1\r\n\r\n"
                        else "HEAD / HTTP/1.1\r\nHost: $ip\r\nConnection: close\r\n\r\n"
                    socket.getOutputStream().write(request.toByteArray(Charsets.US_ASCII))
                    socket.soTimeout = (deadline - SystemClock.elapsedRealtime()).coerceIn(1, 1500).toInt()
                    val bytes = ByteArray(4096)
                    val n = socket.getInputStream().read(bytes)
                    val response = if (n > 0) String(bytes, 0, n, Charsets.UTF_8) else ""
                    if (DiscoveryProtocols.isRtsp(response)) evidence.add(Evidence("RTSP", "Video protocol responded on port $port; verify the device manually", true))
                    else if (response.startsWith("HTTP/1.")) evidence.add(Evidence("HTTP", "Web service responded on port $port"))
                }
            } catch (_: SocketTimeoutException) { incomplete = true }
            catch (_: ConnectException) { /* Refused port is a completed negative probe. */ }
            catch (_: java.io.IOException) { incomplete = true }
            finally { resources.release(socket) }
        }
        return device(ip, evidence.distinct(), !incomplete, incomplete)
    }

    private suspend fun discoverServices(t: WifiTarget) = coroutineScope {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val lock = wifi.createMulticastLock("camera-discovery").apply { setReferenceCounted(false) }
        try {
            lock.acquire()
            listOf("mdns", "ssdp", "onvif").map { kind -> async { udp(t, kind) } }.awaitAll()
        } finally { if (lock.isHeld) lock.release() }
    }

    private fun udp(t: WifiTarget, kind: String) {
        val socket = resources.track(DatagramSocket(null))
        try {
            socket.reuseAddress = true
            t.network.bindSocket(socket)
            socket.bind(InetSocketAddress(t.ip, 0))
            val port = if (kind == "mdns") 5353 else if (kind == "ssdp") 1900 else 3702
            val host = if (kind == "mdns") "224.0.0.251" else "239.255.255.250"
            val requestId = "uuid:${UUID.randomUUID()}"
            val data = when (kind) {
                "mdns" -> DiscoveryProtocols.mdnsQuery()
                "ssdp" -> "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 1\r\nST: ssdp:all\r\n\r\n".toByteArray()
                else -> "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:d=\"http://schemas.xmlsoap.org/ws/2005/04/discovery\" xmlns:dn=\"http://www.onvif.org/ver10/network/wsdl\"><s:Header><a:MessageID>$requestId</a:MessageID><a:To>urn:schemas-xmlsoap-org:ws:2005:04:discovery</a:To><a:Action>http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</a:Action></s:Header><s:Body><d:Probe><d:Types>dn:NetworkVideoTransmitter</d:Types></d:Probe></s:Body></s:Envelope>".toByteArray()
            }
            socket.send(DatagramPacket(data, data.size, InetAddress.getByName(host), port))
            val end = SystemClock.elapsedRealtime() + 2_000
            while (!resources.closed && SystemClock.elapsedRealtime() < end) {
                socket.soTimeout = (end - SystemClock.elapsedRealtime()).coerceIn(1, 500).toInt()
                val packet = DatagramPacket(ByteArray(16_384), 16_384)
                try { socket.receive(packet) } catch (_: SocketTimeoutException) { continue }
                val ip = packet.address.hostAddress ?: continue
                if (!ScanRules.inSubnet(ip, t.ip, t.prefix)) continue
                val payload = packet.data.copyOf(packet.length)
                val evidence = when (kind) {
                    "mdns" -> DiscoveryProtocols.mdnsEvidence(payload)
                    "ssdp" -> DiscoveryProtocols.ssdpEvidence(String(payload, Charsets.UTF_8))
                    else -> onvifEvidence(String(payload, Charsets.UTF_8), requestId)
                }
                if (evidence.isNotEmpty()) found.compute(ip) { _, old -> (old.orEmpty() + evidence).distinct() }
            }
        } catch (_: java.io.IOException) { /* No multicast response does not imply no devices. */ }
        finally { resources.release(socket) }
    }

    private fun onvifEvidence(xml: String, requestId: String): List<Evidence> = runCatching {
        require(!xml.contains("<!DOCTYPE", true) && !xml.contains("<!ENTITY", true))
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(StringReader(xml))
        var related = false
        var video = false
        var match = false
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                if (parser.name == "ProbeMatch" && parser.namespace == "http://schemas.xmlsoap.org/ws/2005/04/discovery") match = true
                if (parser.name == "RelatesTo") related = parser.nextText().trim() == requestId
                if (parser.name == "Types" && parser.namespace == "http://schemas.xmlsoap.org/ws/2005/04/discovery") {
                    val namespaces = (0 until parser.getNamespaceCount(parser.depth)).associate { parser.getNamespacePrefix(it).orEmpty() to parser.getNamespaceUri(it) }
                    video = parser.nextText().trim().split(Regex("\\s+")).any {
                        it.substringAfter(':') == "NetworkVideoTransmitter" && namespaces[it.substringBefore(':', "")] == "http://www.onvif.org/ver10/network/wsdl"
                    } || video
                }
            }
            parser.next()
        }
        if (related && match && video) listOf(Evidence("ONVIF", "Device advertises a network video transmitter; verify manually", true)) else emptyList()
    }.getOrDefault(emptyList())
}
