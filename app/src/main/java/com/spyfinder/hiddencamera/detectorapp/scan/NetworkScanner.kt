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
    private var progress = ScanWorkProgress(emptySet())
    private val liveness = ScanLiveness(SystemClock::elapsedRealtime)
    private val uncertainAddresses = AtomicInteger()
    private val unresolvedServices = AtomicInteger()
    private val advertisedProbes = mutableMapOf<String, Set<ServiceProbe>>()
    private val rows = DeviceSnapshotCache()
    private val discoveryHealth = DiscoveryHealth()
    private val serviceRunner = ServiceProbeRunner()
    private val discoveryPolicy = DiscoveryPolicy()
    private val probeCounts = java.util.concurrent.ConcurrentHashMap<ProbeResult, AtomicInteger>()
    private val analysisMillis = java.util.concurrent.atomic.AtomicLong()
    private val discoveryMillis = java.util.concurrent.atomic.AtomicLong()
    fun withWarnings(message: String): String = listOf(message, discoveryHealth.warnings()).filter { it.isNotBlank() }.joinToString("\n")
    fun stalled() = liveness.stalled()
    private var target: WifiTarget? = null
    fun coverage() = ScanCoverage(planned, total, discoveryCount.get(), analyzed.size)
    fun snapshot(): List<WifiDevice> = rows.snapshot()
    private fun refreshRow(ip: String) {
        val result = analyzed[ip]
        rows.update(device(ip, (found[ip].orEmpty() + result?.evidence.orEmpty()).distinct(), result?.complete == true, result?.complete != true))
    }
    private fun discovered(ip: String, evidence: List<Evidence>, probe: ServiceProbe? = null) = synchronized(found) {
        val old = found[ip]
        if (old == null && found.size >= ScanRules.MAX_TARGETS) { limited.set(true); return@synchronized }
        val combined = (old.orEmpty() + evidence).distinct()
        if (combined.size > 64) limited.set(true)
        found[ip] = combined.sortedByDescending { it.cameraRelated }.take(64)
        if (probe != null) {
            val previous = advertisedProbes[ip].orEmpty()
            if (probe !in previous && previous.size >= 8) limited.set(true)
            else if (probe !in previous) {
                advertisedProbes[ip] = previous + probe
                if (analyzed.remove(ip) != null) pending.offer(Pending(ip, 0, sequence.incrementAndGet()))
            }
        }
        refreshRow(ip)
        progress.discovered(ip)
        liveness.activity()
        if (old == null) pending.offer(Pending(ip, if (evidence.any { it.cameraRelated }) 0 else 1, sequence.incrementAndGet()))
        else if (evidence.any { it.cameraRelated }) {
            pending.firstOrNull { it.ip == ip && it.priority != 0 }?.let {
                if (pending.remove(it)) pending.offer(it.copy(priority = 0))
            }
        }
        Unit
    }

    fun selectNetwork(): WifiTarget {
        val networks = cm.allNetworks.filter { isLocalWifi(cm.getNetworkCapabilities(it)) }
        val network = networks.firstOrNull { it == cm.activeNetwork } ?: networks.singleOrNull()
            ?: throw IllegalStateException(if (networks.isEmpty()) "Connect to Wi-Fi before scanning." else "Multiple Wi-Fi networks are available. Select one network and retry.")
        val properties = cm.getLinkProperties(network) ?: error("Wi-Fi address is unavailable. Reconnect and retry.")
        val address = properties.linkAddresses.firstOrNull { it.address is Inet4Address }
            ?: error("This network has no IPv4 address. IPv6 scanning is not supported yet.")
        return WifiTarget(network, address.address.hostAddress!!, address.prefixLength,
            properties.routes.firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }?.gateway?.hostAddress).also { target = it }
    }

    fun networkUnchanged(t: WifiTarget): Boolean {
        if (!isLocalWifi(cm.getNetworkCapabilities(t.network))) return false
        return cm.getLinkProperties(t.network)?.linkAddresses?.any { it.address.hostAddress == t.ip && it.prefixLength == t.prefix } == true
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun scan(t: WifiTarget, onProgress: (String, List<WifiDevice>, Int) -> Unit): ScanOutput = withContext(Dispatchers.IO) {
        target = t
        val scanStarted = SystemClock.elapsedRealtime()
        liveness.activity()
        val targets = ScanRules.targets(t.ip, t.prefix, t.gateway)
        planned = targets.addresses.size
        total = targets.total
        progress = ScanWorkProgress(targets.addresses.toSet())
        discovered(t.ip, listOf(Evidence("Local", "Current phone")))
        onProgress("Discovering devices in ${t.ip}/${t.prefix}…", snapshot(), 0)
        coroutineScope {
            val reporter = launch {
                while (isActive) {
                    delay(200)
                    val coverage = coverage()
                    val remaining = (found.size - coverage.analyzed).coerceAtLeast(0)
                    val message = "Discovering: ${coverage.checked}/$planned addresses checked\nAnalyzing services: ${coverage.analyzed}/${found.size} devices\nDevices awaiting analysis: $remaining"
                    onProgress(withWarnings(message), snapshot(), progress.percent())
                }
            }
            try {
                // Blocking connects need enough IO slots; reserve headroom for multicast and coordinators.
                withContext(Dispatchers.IO.limitedParallelism(discoveryPolicy.workers(planned) + 12)) {
                    ScanPipeline({ resources.closed }, discoveryWorkers = discoveryPolicy.workers(planned)).run(
                        planned, { try { discoverServices(t) } finally { progress.multicastFinished(); liveness.activity() } }, { index ->
                            val ip = targets.addresses[index]
                            if (!found.containsKey(ip)) {
                                val attempt = discoveryPolicy.discover({ !resources.closed }) { port, timeout ->
                                    probe(t, ip, port, timeout)
                                }
                                if (attempt.uncertain) uncertainAddresses.incrementAndGet()
                                if (attempt.responded) discovered(ip, emptyList())
                                if (!attempt.checked) throw CancellationException()
                            }
                            discoveryCount.incrementAndGet()
                            progress.checked(ip)
                            liveness.activity()
                        }, { pending.poll()?.ip }, { ip ->
                            val probes = synchronized(found) { advertisedProbes[ip].orEmpty() }
                            val analysisStarted = SystemClock.elapsedRealtime()
                            val result = try { analyze(t, ip, probes) } finally {
                                analysisMillis.addAndGet(SystemClock.elapsedRealtime() - analysisStarted)
                            }
                            synchronized(found) {
                                if (advertisedProbes[ip].orEmpty() != probes) {
                                    // A late announcement must be analyzed even if this address was already in flight.
                                    pending.offer(Pending(ip, 0, sequence.incrementAndGet()))
                                } else {
                                    analyzed[ip] = result
                                    refreshRow(ip)
                                    progress.analyzed(ip)
                                }
                            }
                            liveness.activity()
                        })
                }
            } finally {
                reporter.cancelAndJoin()
                android.util.Log.i("ScanMetrics", "elapsed_ms=${SystemClock.elapsedRealtime() - scanStarted} " +
                    "discovery_probe_ms=${discoveryMillis.get()} analysis_device_ms=${analysisMillis.get()} " +
                    "checked=${discoveryCount.get()} analyzed=${analyzed.size} remaining=${(found.size - analyzed.size).coerceAtLeast(0)} " +
                    "probes=$probeCounts channels=${discoveryHealth.snapshot()}")
            }
        }
        val devices = snapshot()
        val incomplete = devices.count { !it.analysisComplete }
        val partial = targets.limited || limited.get() || discoveryCount.get() < planned
        ScanOutput(devices, partial, withWarnings(buildString {
            append(if (partial) "Partially completed. " else "Scan completed. ")
            append("Checked ${discoveryCount.get()} of ${targets.total} IPv4 addresses; ${devices.size} devices responded.")
            if (incomplete > 0) append(" $incomplete devices could not be fully analyzed.")
            if (uncertainAddresses.get() > 0) append(" ${uncertainAddresses.get()} addresses could not be verified due to network errors.")
            if (unresolvedServices.get() > 0) append(" ${unresolvedServices.get()} service announcements could not be resolved to an in-scope IPv4 endpoint.")
            if (targets.limited || limited.get()) append(" Coverage or evidence storage was limited; review the recorded scope.")
            append(" Devices that do not respond or are isolated by the network may be missed. No result proves a room is safe.")
        }))
    }

    private fun errorResult(error: Throwable): ProbeResult {
        val causes = generateSequence(error) { it.cause }.take(16)
        val errno = causes.filterIsInstance<ErrnoException>().firstOrNull()?.errno
        return ProbeSupport.classify(error, errno, resources.closed)
    }
    private fun probe(t: WifiTarget, ip: String, port: Int, timeoutMillis: Int): ProbeResult {
        if (resources.closed) throw CancellationException()
        val socket = resources.track(t.network.socketFactory.createSocket())
        val started = SystemClock.elapsedRealtime()
        val result = try {
            socket.connect(InetSocketAddress(ip, port), timeoutMillis)
            ProbeResult.OPEN
        } catch (e: java.io.IOException) {
            errorResult(e)
        } finally {
            resources.release(socket); liveness.activity()
            discoveryMillis.addAndGet(SystemClock.elapsedRealtime() - started)
        }
        probeCounts.getOrPut(result) { AtomicInteger() }.incrementAndGet()
        return result
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

    private suspend fun analyze(t: WifiTarget, ip: String, advertised: Set<ServiceProbe>): Analysis {
        val evidence = mutableListOf<Evidence>()
        if (ip == t.ip) return Analysis(evidence, true)
        if (ip == t.gateway) evidence.add(Evidence("Network", "Configured Wi-Fi gateway"))
        val probes = (advertised + ProbeSupport.ports.map { port -> ServiceProbe(port,
            when (port) { 554, 8554 -> "RTSP"; 80, 5000 -> "HTTP"; else -> "TCP" }) }).distinct()
        val results = serviceRunner.run(probes) { (port, protocol) ->
            val evidence = mutableListOf<Evidence>()
            var incomplete = false
            if (resources.closed) throw CancellationException()
            val socket = resources.track(t.network.socketFactory.createSocket())
            try {
                socket.soTimeout = 1500
                socket.connect(InetSocketAddress(ip, port), 1500)
                evidence.add(Evidence("TCP", "Port $port is open (service not confirmed)"))
                if (protocol != "TCP") {
                    val request = if (protocol == "RTSP") "OPTIONS rtsp://$ip:$port/ RTSP/1.0\r\nCSeq: 1\r\n\r\n"
                        else "HEAD / HTTP/1.1\r\nHost: $ip\r\nConnection: close\r\n\r\n"
                    socket.getOutputStream().write(request.toByteArray(Charsets.US_ASCII))
                    val response = ProbeSupport.readStatusLine(socket.getInputStream(),
                        SystemClock.elapsedRealtime() + 1500, SystemClock::elapsedRealtime,
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
            } finally { resources.release(socket); liveness.activity() }
            Analysis(evidence, !incomplete)
        }
        return Analysis((evidence + results.flatMap { it.evidence }).distinct(), results.all { it.complete })
    }

    private suspend fun discoverServices(t: WifiTarget) = coroutineScope {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val lock = wifi.createMulticastLock("camera-discovery").apply { setReferenceCounted(false) }
        try {
            var acquired = false
            discoveryHealth.run("Multicast", { resources.closed }) { lock.acquire(); acquired = true }
            if (!acquired) return@coroutineScope
            listOf("mdns", "ssdp", "onvif").map { kind -> async { udp(t, kind) } }.awaitAll()
        } finally { if (lock.isHeld) lock.release() }
    }

    private fun udp(t: WifiTarget, kind: String) {
        var ownedSocket: DatagramSocket? = null
        val mdns = if (kind == "mdns") MdnsDiscovery() else null
        try {
            discoveryHealth.run(when (kind) { "mdns" -> "mDNS"; "ssdp" -> "SSDP"; else -> "ONVIF" }, { resources.closed }) { stage ->
            val socket = resources.track(DatagramSocket(null)).also { ownedSocket = it }
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
            stage(DiscoveryHealth.Stage.SENDING)
            socket.send(DatagramPacket(data, data.size, InetAddress.getByName(host), port))
            stage(DiscoveryHealth.Stage.RECEIVING)
            val end = SystemClock.elapsedRealtime() + 2_000
            while (!resources.closed && SystemClock.elapsedRealtime() < end) {
                socket.soTimeout = (end - SystemClock.elapsedRealtime()).coerceIn(1, 500).toInt()
                val packet = DatagramPacket(ByteArray(16_384), 16_384)
                try { socket.receive(packet) } catch (_: SocketTimeoutException) { continue }
                val ip = packet.address.hostAddress ?: continue
                if (!ScanRules.inSubnet(ip, t.ip, t.prefix)) continue
                val payload = packet.data.copyOf(packet.length)
                if (mdns != null) {
                    mdns.accept(payload)
                    mdns.endpoints(t.ip, t.prefix).forEach { discovered(it.ip, listOf(it.evidence), it.probe) }
                    val questions = mdns.questions()
                    if (questions.isNotEmpty()) {
                        val query = runCatching { MdnsPacket.query(questions) }.getOrNull()
                        if (query != null) {
                            stage(DiscoveryHealth.Stage.SENDING)
                            socket.send(DatagramPacket(query, query.size, InetAddress.getByName(host), port))
                            stage(DiscoveryHealth.Stage.RECEIVING)
                        }
                    }
                    continue
                }
                val evidence = when (kind) {
                    "ssdp" -> DiscoveryProtocols.ssdpEvidence(String(payload, Charsets.UTF_8))
                    else -> onvifEvidence(String(payload, Charsets.UTF_8), requestId)
                }
                if (evidence.isNotEmpty()) discovered(ip, evidence)
            }
            }
        }
        finally {
            if (mdns != null) {
                unresolvedServices.set(mdns.unresolved(t.ip, t.prefix))
                if (mdns.limited) limited.set(true)
            }
            ownedSocket?.let { resources.release(it) }; liveness.activity()
        }
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
