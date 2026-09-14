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
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import android.system.ErrnoException
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class WifiTarget(val network: Network, val ip: String, val prefix: Int, val gateway: String?)
data class ScanOutput(val devices: List<WifiDevice>, val partial: Boolean, val message: String)
data class ScanCoverage(val planned: Int = 0, val total: Long = 0, val checked: Int = 0, val analyzed: Int = 0)

class NetworkScanner(private val context: Context, val resources: ScanResources = ScanResources()) {
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val found = ConcurrentHashMap<String, List<Evidence>>()
    private data class Analysis(val evidence: List<Evidence>, val complete: Boolean)
    private data class Pending(val ip: String, val priority: Int, val order: Int) : Comparable<Pending> {
        override fun compareTo(other: Pending) = compareValuesBy(this, other, { it.priority }, { it.order })
    }
    private val analyzed = ConcurrentHashMap<String, Analysis>()
    private val pending = PriorityBlockingQueue<Pending>()
    private val sequence = AtomicInteger()
    private val discoveryCount = AtomicInteger()
    private val limited = AtomicBoolean()
    @Volatile private var planned = 0
    @Volatile private var total = 0L
    @Volatile private var scanDeadline = Long.MAX_VALUE
    private var target: WifiTarget? = null
    fun coverage() = ScanCoverage(planned, total, discoveryCount.get(), analyzed.size)
    fun snapshot(): List<WifiDevice> = synchronized(found) {
        found.map { (ip, advertised) ->
            val result = analyzed[ip]
            device(ip, (advertised + result?.evidence.orEmpty()).distinct(), result?.complete == true, result?.complete != true)
        }.sortedBy { ScanRules.ipv4(it.ip) }
    }
    private fun discovered(ip: String, evidence: List<Evidence>) = synchronized(found) {
        val old = found[ip]
        if (old == null && found.size >= 1024) { limited.set(true); return@synchronized }
        val combined = (old.orEmpty() + evidence).distinct()
        if (combined.size > 64) limited.set(true)
        found[ip] = combined.take(64)
        if (old == null) pending.offer(Pending(ip, if (evidence.any { it.cameraRelated }) 0 else 1, sequence.incrementAndGet()))
        else if (evidence.any { it.cameraRelated }) {
            pending.firstOrNull { it.ip == ip && it.priority != 0 }?.let {
                if (pending.remove(it)) pending.offer(it.copy(priority = 0))
            }
        }
        Unit
    }

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
        val started = SystemClock.elapsedRealtime()
        scanDeadline = started + 45_000
        val discoveryDeadline = started + 33_000
        val targets = ScanRules.targets(t.ip, t.prefix, t.gateway)
        planned = targets.addresses.size
        total = targets.total
        discovered(t.ip, listOf(Evidence("Local", "Current phone")))
        onProgress("Discovering devices in ${t.ip}/${t.prefix}…", snapshot(), 0)
        coroutineScope {
            val reporter = launch {
                while (isActive) {
                    delay(200)
                    val coverage = coverage()
                    val message = "Discovering: ${coverage.checked}/$planned addresses checked\nAnalyzing services: ${coverage.analyzed}/${found.size} devices"
                    val progress = coverage.checked * 60 / planned.coerceAtLeast(1) + coverage.analyzed * 39 / found.size.coerceAtLeast(1)
                    onProgress(message, snapshot(), progress.coerceAtMost(99))
                }
            }
            try {
                ScanPipeline(SystemClock::elapsedRealtime, discoveryDeadline, scanDeadline, { resources.closed }).run(
                    planned, { discoverServices(t) }, { index ->
                        val ip = targets.addresses[index]
                        if (found.containsKey(ip)) discoveryCount.incrementAndGet()
                        else {
                            val attempt = ProbeSupport.discover({ !resources.closed && SystemClock.elapsedRealtime() < discoveryDeadline }) {
                                probe(t, ip, it, discoveryDeadline)
                            }
                            if (attempt.uncertain) limited.set(true)
                            if (attempt.responded) discovered(ip, emptyList())
                            if (attempt.checked) discoveryCount.incrementAndGet()
                        }
                    }, { pending.poll()?.ip }, { ip -> analyzed[ip] = analyze(t, ip) })
            } finally { reporter.cancelAndJoin() }
        }
        val devices = snapshot()
        val incomplete = devices.count { !it.analysisComplete }
        val partial = targets.limited || limited.get() || discoveryCount.get() < planned || incomplete > 0
        ScanOutput(devices, partial, buildString {
            append(if (partial) "Partially completed. " else "Scan completed. ")
            append("Checked ${discoveryCount.get()} of ${targets.total} IPv4 addresses; ${devices.size} devices responded.")
            if (incomplete > 0) append(" $incomplete devices could not be fully analyzed.")
            append(" Devices that do not respond or are isolated by the network may be missed. No result proves a room is safe.")
        })
    }

    private fun errorResult(error: Throwable): ProbeResult {
        val causes = generateSequence(error) { it.cause }.take(16)
        val errno = causes.filterIsInstance<ErrnoException>().firstOrNull()?.errno
        return ProbeSupport.classify(error, errno, resources.closed)
    }
    private fun probe(t: WifiTarget, ip: String, port: Int, deadline: Long): ProbeResult {
        if (resources.closed) throw CancellationException()
        val socket = resources.track(t.network.socketFactory.createSocket())
        return try {
            socket.connect(InetSocketAddress(ip, port), (deadline - SystemClock.elapsedRealtime()).coerceIn(1, 450).toInt())
            ProbeResult.OPEN
        } catch (e: java.io.IOException) {
            errorResult(e)
        } finally { resources.release(socket) }
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

    private fun analyze(t: WifiTarget, ip: String): Analysis {
        val evidence = mutableListOf<Evidence>()
        if (ip == t.ip) return Analysis(evidence, true)
        if (ip == t.gateway) evidence.add(Evidence("Network", "Configured Wi-Fi gateway"))
        val deadline = minOf(scanDeadline, SystemClock.elapsedRealtime() + 8_000)
        var incomplete = false
        for (port in ProbeSupport.ports) {
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
                    val response = ProbeSupport.readStatusLine(socket.getInputStream(),
                        minOf(deadline, SystemClock.elapsedRealtime() + 1500), SystemClock::elapsedRealtime,
                        { socket.soTimeout = it }, { resources.closed })
                    if (DiscoveryProtocols.isRtsp(response)) evidence.add(Evidence("RTSP", "Video protocol responded on port $port; verify the device manually", true))
                    else if (response.startsWith("HTTP/1.")) evidence.add(Evidence("HTTP", "Web service responded on port $port"))
                }
            } catch (e: java.io.IOException) {
                when (errorResult(e)) {
                    ProbeResult.REFUSED -> Unit
                    ProbeResult.CANCELLED -> throw CancellationException()
                    else -> incomplete = true
                }
            } finally { resources.release(socket) }
        }
        return Analysis(evidence.distinct(), !incomplete)
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
                if (evidence.isNotEmpty()) discovered(ip, evidence)
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
