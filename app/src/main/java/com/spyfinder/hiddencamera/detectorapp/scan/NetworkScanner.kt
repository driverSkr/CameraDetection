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
    private data class Analysis(val evidence: List<Evidence>, val complete: Boolean, val details: Map<String, String> = emptyMap())
    private val metadata = ConcurrentHashMap<String, Map<String, String>>()
    private data class Pending(val ip: String, val priority: Int, val order: Int) : Comparable<Pending> {
        override fun compareTo(other: Pending) = compareValuesBy(this, other, { it.priority }, { it.order })
    }
    private val analyzed = ConcurrentHashMap<String, Analysis>()
    // Session observations survive invalidation of a completed analysis and failed retries.
    private data class Observations(val evidence: List<Evidence>, val details: Map<String, String>)
    private val observations = ConcurrentHashMap<String, Observations>()
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
        rows.update(device(ip, (found[ip].orEmpty() + observations[ip]?.evidence.orEmpty()).distinct(), result?.complete == true, result?.complete != true))
    }
    private fun recordAnalysis(ip: String, probes: Set<ServiceProbe>, result: Analysis) = synchronized(found) {
        val previous = observations[ip]
        observations[ip] = Observations(
            (previous?.evidence.orEmpty() + result.evidence).distinct(),
            ServiceMetadata.mergeProbeDetails(listOf(previous?.details.orEmpty(),
                result.details.filterKeys { it.startsWith("port_") || it.startsWith("server_") })))
        if (advertisedProbes[ip].orEmpty() != probes) {
            // Keep positive observations even when a late announcement requires another pass.
            pending.offer(Pending(ip, 0, sequence.incrementAndGet()))
        } else {
            analyzed[ip] = result
            progress.analyzed(ip)
        }
        refreshRow(ip)
    }
    private fun discovered(ip: String, evidence: List<Evidence>, probe: ServiceProbe? = null,
        details: Map<String, String> = emptyMap()) = synchronized(found) {
        val old = found[ip]
        if (old == null && found.size >= ScanRules.MAX_TARGETS) { limited.set(true); return@synchronized }
        val combined = (old.orEmpty() + evidence).distinct()
        if (combined.size > 64) limited.set(true)
        found[ip] = combined.sortedByDescending { it.cameraRelated }.take(64)
        if (details.isNotEmpty()) metadata[ip] = ServiceMetadata.merge(metadata[ip].orEmpty(), details).entries.take(24).associate { it.toPair() }
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
        val networks = cm.allNetworks.toList()
        val local = networks.filter { isLocalWifi(cm.getNetworkCapabilities(it)) }
        val hasWifi = networks.any { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }
        val hasVpn = networks.any { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true }
        val airplane = android.provider.Settings.Global.getInt(
            context.contentResolver, android.provider.Settings.Global.AIRPLANE_MODE_ON, 0) != 0
        scanPreflightMessage(airplane, local.size, hasWifi, hasVpn)?.let { throw IllegalStateException(it) }
        val network = local.firstOrNull { it == cm.activeNetwork } ?: local.single()
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
                            recordAnalysis(ip, probes, result)
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
        if (result == ProbeResult.OPEN) synchronized(found) {
            metadata[ip] = metadata[ip].orEmpty() + ("port_$port" to "TCP")
            if (found.containsKey(ip)) refreshRow(ip)
        }
        return result
    }

    private fun device(ip: String, evidence: List<Evidence>, complete: Boolean, incomplete: Boolean): WifiDevice {
        val self = ip == target?.ip
        val finding = ScanRules.finding(evidence, incomplete)
        val details = metadata[ip].orEmpty() + analyzed[ip]?.details.orEmpty() + observations[ip]?.details.orEmpty()
        val identity = DeviceIdentity.classify(self, ip == target?.gateway, details, finding == Finding.CAMERA_FEATURES)
        val type = identity.type
        return WifiDevice(if (self) "Current phone" else DeviceIdentity.name(details) ?: "$type · $ip", type, ip, R.drawable.svg_icon_sensor, 0, 0,
            riskLevel = if (!self && finding == Finding.CAMERA_FEATURES) 1 else 0,
            finding = finding, evidence = evidence.map { "${it.source}: ${it.detail}" }, isCurrentPhone = self,
            analysisComplete = complete, ruleVersion = ScanRules.VERSION,
            brandModel = if (self) "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}" else listOfNotNull(details["identity_manufacturer"], details["identity_model"]).joinToString(" "),
            details = details.filterKeys { !it.endsWith("_url") } + ("identity_basis" to identity.basis))
    }

    private suspend fun analyze(t: WifiTarget, ip: String, advertised: Set<ServiceProbe>): Analysis {
        val evidence = mutableListOf<Evidence>()
        if (ip == t.ip) return Analysis(evidence, true)
        if (ip == t.gateway) evidence.add(Evidence("Network", "Configured Wi-Fi gateway"))
        val probes = (advertised + ProbeSupport.ports.filter { port -> advertised.none { it.port == port && it.protocol == "TCP" } }.map { port -> ServiceProbe(port,
            when (port) { 554, 8554 -> "RTSP"; 80, 5000 -> "HTTP"; else -> "TCP" }) }).distinct()
        val results = serviceRunner.run(probes) { (port, protocol, descriptionUrl) ->
            if (protocol == "UPNP_INFO" || protocol == "ONVIF_INFO") {
                val url = descriptionUrl
                return@run Analysis(emptyList(), true, url?.let { readDescription(t, ip, it, protocol == "ONVIF_INFO") }.orEmpty())
            }
            val evidence = mutableListOf<Evidence>()
            val details = mutableMapOf<String, String>()
            var incomplete = false
            if (resources.closed) throw CancellationException()
            val socket = resources.track(t.network.socketFactory.createSocket())
            try {
                socket.soTimeout = 1500
                socket.connect(InetSocketAddress(ip, port), 1500)
                evidence.add(Evidence("TCP", "Port $port is open (service not confirmed)"))
                details["port_$port"] = "TCP"
                if (protocol != "TCP") {
                    val request = if (protocol == "RTSP") "OPTIONS rtsp://$ip:$port/ RTSP/1.0\r\nCSeq: 1\r\n\r\n"
                        else "HEAD / HTTP/1.1\r\nHost: $ip\r\nConnection: close\r\n\r\n"
                    socket.getOutputStream().write(request.toByteArray(Charsets.US_ASCII))
                    val input = socket.getInputStream().buffered()
                    val deadline = SystemClock.elapsedRealtime() + 1500
                    val response = ProbeSupport.readLine(input,
                        deadline, SystemClock::elapsedRealtime,
                        { socket.soTimeout = it }, { resources.closed })
                    if (DiscoveryProtocols.isRtsp(response)) evidence.add(Evidence("RTSP", "Video protocol responded on port $port; verify the device manually", true))
                    else if (response.startsWith("HTTP/1.")) evidence.add(Evidence("HTTP", "Web service responded on port $port"))
                    if (DiscoveryProtocols.isRtsp(response) || response.startsWith("HTTP/1.")) {
                        details["port_$port"] = if (DiscoveryProtocols.isRtsp(response)) "RTSP" else "HTTP"
                        val headers = ProbeSupport.readHeaders(input, deadline, SystemClock::elapsedRealtime,
                            { socket.soTimeout = it }, { resources.closed })
                        ServiceMetadata.headers(response + headers)["server"]?.takeIf { it.isNotBlank() }?.let {
                            details["server_$port"] = it
                        }
                    }
                }
            } catch (e: java.io.IOException) {
                when (errorResult(e)) {
                    ProbeResult.REFUSED -> Unit
                    ProbeResult.CANCELLED -> throw CancellationException()
                    else -> incomplete = true
                }
            } finally { resources.release(socket); liveness.activity() }
            Analysis(evidence, !incomplete, details)
        }
        val descriptions = probes.zip(results).filter { it.first.url != null }.map { it.second.details }
        val serviceDetails = ServiceMetadata.mergeProbeDetails(probes.zip(results).filter { it.first.url == null }.map { it.second.details })
        return Analysis((evidence + results.flatMap { it.evidence }).distinct(), results.all { it.complete }, serviceDetails + DeviceDescription.mergeReports(descriptions))
    }

    private fun readDescription(t: WifiTarget, ip: String, value: String, onvif: Boolean): Map<String, String> {
        val url = DeviceDescription.localUrl(value, ip) ?: return emptyMap()
        val uri = java.net.URI(url)
        val socket = resources.track(t.network.socketFactory.createSocket())
        val deadline = SystemClock.elapsedRealtime() + 2500
        return try {
            socket.connect(InetSocketAddress(ip, uri.port.takeIf { it > 0 } ?: 80), 1000)
            val body = if (onvif) "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\"><s:Body><GetDeviceInformation xmlns=\"http://www.onvif.org/ver10/device/wsdl\"/></s:Body></s:Envelope>".toByteArray() else byteArrayOf()
            val path = uri.rawPath.ifEmpty { "/" } + (uri.rawQuery?.let { "?$it" } ?: "")
            val request = "${if (onvif) "POST" else "GET"} $path HTTP/1.1\r\nHost: ${uri.rawAuthority}\r\nConnection: close\r\nAccept-Encoding: identity\r\n" +
                (if (onvif) "Content-Type: application/soap+xml; charset=utf-8; action=\"http://www.onvif.org/ver10/device/wsdl/GetDeviceInformation\"\r\nContent-Length: ${body.size}\r\n" else "") + "\r\n"
            socket.getOutputStream().write(request.toByteArray(Charsets.US_ASCII) + body)
            val input = socket.getInputStream().buffered()
            fun remaining() {
                if (resources.closed) throw CancellationException()
                val time = deadline - SystemClock.elapsedRealtime()
                if (time <= 0) throw java.net.SocketTimeoutException()
                socket.soTimeout = time.toInt()
            }
            fun line() = ProbeSupport.readLine(input, deadline, SystemClock::elapsedRealtime,
                { socket.soTimeout = it }, { resources.closed })
            val status = line()
            val headers = ServiceMetadata.headers(status + ProbeSupport.readHeaders(input, deadline,
                SystemClock::elapsedRealtime, { socket.soTimeout = it }, { resources.closed }))
            if (!status.matches(Regex("HTTP/1\\.[01] 200(?: .*|\\r\\n)\\r?\\n?")))
                return mapOf("identity_query" to if (Regex("^HTTP/1\\.[01] (401|403) ").containsMatchIn(status)) "Authentication required" else "Device description unavailable")
            require(headers["content-encoding"].let { it == null || it.equals("identity", true) })
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(2048)
            fun read(count: Int) {
                require(count >= 0 && output.size().toLong() + count <= 65536)
                var left = count
                while (left > 0) {
                    remaining()
                    val n = input.read(buffer, 0, minOf(left, buffer.size))
                    if (n < 0) throw java.io.IOException("Truncated description")
                    output.write(buffer, 0, n); left -= n
                }
            }
            if (headers["transfer-encoding"].equals("chunked", true)) {
                while (true) {
                    val count = line().trim().substringBefore(';').toInt(16)
                    if (count == 0) break
                    read(count)
                    require(line() == "\r\n")
                }
            } else if (headers["content-length"] != null) read(headers.getValue("content-length").toInt())
            else {
                require(headers["transfer-encoding"] == null)
                while (true) {
                    remaining()
                    val n = input.read(buffer)
                    if (n < 0) break
                    require(output.size() + n <= 65536)
                    output.write(buffer, 0, n)
                }
            }
            DeviceDescription.parse(output.toByteArray(), onvif).ifEmpty { mapOf("identity_query" to "Device description unavailable") }
        } catch (e: CancellationException) { throw e }
          catch (_: Exception) { mapOf("identity_query" to "Device description unavailable") }
        finally { resources.release(socket); liveness.activity() }
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
            val destination = InetAddress.getByName(host)
            fun send(bytes: ByteArray) {
                if (resources.closed) throw CancellationException()
                stage(DiscoveryHealth.Stage.SENDING)
                socket.send(DatagramPacket(bytes, bytes.size, destination, port))
                stage(DiscoveryHealth.Stage.RECEIVING)
            }
            val started = SystemClock.elapsedRealtime()
            val end = started + 3_000
            var initialSends = 0
            while (!resources.closed && SystemClock.elapsedRealtime() < end) {
                if (initialSends < 3 && SystemClock.elapsedRealtime() - started >= initialSends * 750L) {
                    send(data)
                    initialSends++
                }
                val questions = mdns?.questions().orEmpty()
                val encodable = questions.filter { runCatching { MdnsPacket.query(listOf(it)) }.isSuccess }
                if (encodable.isNotEmpty()) send(MdnsPacket.query(encodable))
                socket.soTimeout = (end - SystemClock.elapsedRealtime()).coerceIn(1, 250).toInt()
                val packet = DatagramPacket(ByteArray(16_384), 16_384)
                try { socket.receive(packet) } catch (_: SocketTimeoutException) { continue }
                val ip = packet.address.hostAddress ?: continue
                if (!ScanRules.inSubnet(ip, t.ip, t.prefix)) continue
                val payload = packet.data.copyOf(packet.length)
                if (mdns != null) {
                    mdns.accept(payload)
                    mdns.changedEndpoints(t.ip, t.prefix).forEach { discovered(it.ip, listOf(it.evidence), it.probe, it.details) }
                    continue
                }
                val evidence = when (kind) {
                    "ssdp" -> DiscoveryProtocols.ssdpEvidence(String(payload, Charsets.UTF_8))
                    else -> onvifEvidence(String(payload, Charsets.UTF_8), requestId)
                }
                if (evidence.isNotEmpty()) {
                    val response = String(payload, Charsets.UTF_8)
                    val details = (if (kind == "ssdp") ServiceMetadata.ssdp(response) else emptyMap()).toMutableMap()
                    val advertisedUrl = if (kind == "ssdp") ServiceMetadata.headers(response)["location"] else
                        Regex("<(?:[A-Za-z0-9_]+:)?XAddrs(?:\\s[^>]*)?>([^<]+)</").find(response)?.groupValues?.get(1)?.trim()?.split(Regex("\\s+"))?.firstOrNull { DeviceDescription.localUrl(it, ip) != null }
                    val url = advertisedUrl?.let { DeviceDescription.localUrl(it, ip) }
                    val probe = url?.let {
                        ServiceProbe(java.net.URI(it).port.takeIf { p -> p > 0 } ?: 80, if (kind == "ssdp") "UPNP_INFO" else "ONVIF_INFO", it)
                    }
                    discovered(ip, evidence, probe, details)
                }
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
