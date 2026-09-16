package com.spyfinder.hiddencamera.detectorapp.scan

/** Hardware identity is separate from the presence of a video protocol. */
object DeviceIdentity {
    // Compile once: result rows and category filters classify the same records repeatedly.
    private val modelRules = listOf(
        "Likely video recorder" to "nvr|dvr|network video recorder|digital video recorder|录像机",
        "Likely network camera" to "ip camera|network camera|ipcam|网络摄像机",
        "Likely computer" to "macbook(?: pro| air)?|mac mini|imac|mac pro|thinkpad|desktop|laptop",
        "Likely phone or tablet" to "iphone|ipad|pixel [0-9]+",
        "Likely television" to "smart tv|android tv|电视|智慧屏",
        "Likely storage device" to "nas|diskstation|rackstation|network attached storage"
    ).map { (type, words) -> type to Regex("(?i)(?:^|[^a-z0-9])(?:$words)(?:[^a-z]|$)") }
    private val hostRules = listOf(
        "Likely computer" to Regex("(?i)(?:^|[^a-z0-9])(?:macbook|imac|mac[ -]?mini|thinkpad|desktop|laptop)(?:[-_. 0-9]|$)"),
        "Likely phone or tablet" to Regex("(?i)(?:^|[^a-z0-9])(?:iphone|ipad)(?:[-_. 0-9]|$)")
    )
    private val upnpType = Regex("urn:schemas-upnp-org:device:(MediaRenderer|MediaServer|InternetGatewayDevice|Printer):[0-9]+")
    private fun declaredTypes(details: Map<String, String>) =
        listOf("upnp_type", "ssdp_st").flatMap { details[it].orEmpty().lines() }
            .mapNotNull { upnpType.matchEntire(it.trim())?.groupValues?.get(1) }.distinct()
    fun forDevice(device: com.spyfinder.hiddencamera.detectorapp.model.WifiDevice): Result = classify(
        device.isCurrentPhone, device.details["identity_basis"] == "Configured Wi-Fi gateway" ||
            device.evidence.any { it == "Network: Configured Wi-Fi gateway" }, device.details,
        device.finding == Finding.CAMERA_FEATURES).let { it.copy(capabilities = capabilities(device.details)) }
    data class Result(val type: String, val basis: String, val capabilities: List<String> = emptyList())
    fun capabilities(details: Map<String, String>): List<String> = buildList {
        declaredTypes(details).forEach { declared ->
            when (declared) {
                "MediaRenderer" -> add("Media playback service")
                "MediaServer" -> add("Media server service")
                "Printer" -> add("Printing service")
            }
        }
        details["mdns_device_type"].orEmpty().lines().forEach {
            when (it) { "Printer" -> add("Printing service"); "Media playback device" -> add("Media playback service") }
        }
    }.distinct().sorted()
    fun classify(self: Boolean, gateway: Boolean, details: Map<String, String>, video: Boolean): Result {
        if (self) return Result("Phone", "Current phone")
        if (gateway) return Result("Router", "Configured Wi-Fi gateway")
        if (details["identity_conflict"] == "true") return Result("Device type unconfirmed", "Conflicting identity clues")
        val declared = details["upnp_type"].orEmpty()
        val hardwareTypes = declaredTypes(details).mapNotNull {
            when (it) {
                "InternetGatewayDevice" -> "Router"
                "Printer" -> "Printer"
                else -> null
            }
        }.distinct()
        if (hardwareTypes.size > 1) return Result("Device type unconfirmed", "Conflicting identity clues")
        val type = hardwareTypes.singleOrNull()
        if (type != null) {
            val fromDescription = declared.lines().any {
                upnpType.matchEntire(it.trim())?.groupValues?.get(1) in setOf("InternetGatewayDevice", "Printer")
            }
            return Result(type, if (fromDescription) "UPnP device type" else "SSDP device type")
        }
        // Use explicit model words only, not ports, manufacturer names, or software banners.
        val model = details["identity_model"].orEmpty()
        val modelMatches = modelRules.filter { it.second.containsMatchIn(model) }.map { it.first }.distinct()
        if (modelMatches.size > 1) return Result("Device type unconfirmed", "Conflicting identity clues")
        modelMatches.singleOrNull()?.let { return Result(it, "Device-reported model") }
        val serviceTypes = details["mdns_device_type"].orEmpty().lines().filter { it in setOf("Printer", "Media playback device") }.distinct()
        if (serviceTypes == listOf("Printer")) return Result("Printer", "mDNS service type")
        // Names are user-editable. Only narrow computer/mobile hints are used, always tentative.
        // Never infer cameras, recorders, storage or safety from a friendly name or a port alone.
        val names = listOf("mdns_host", "mdns_name", "upnp_name").flatMap { details[it].orEmpty().lines() }
        val hostMatches = hostRules.filter { rule -> names.any { rule.second.containsMatchIn(it) } }.map { it.first }.distinct()
        if (hostMatches.size > 1) return Result("Device type unconfirmed", "Conflicting identity clues")
        hostMatches.singleOrNull()?.let { return Result(it, "Device name suggests type; verify manually") }
        if (serviceTypes.size > 1) return Result("Device type unconfirmed", "Multiple service capabilities; hardware type unconfirmed")
        if (capabilities(details).isNotEmpty()) return Result("Device type unconfirmed", "Services advertised; hardware type unconfirmed")
        val reason = when {
            details["identity_query"] == "Authentication required" -> "Authentication required"
            model.isNotBlank() || declared.isNotBlank() -> "Reported identity not matched"
            details["identity_query"] == "Device description unavailable" -> "Device description unavailable"
            video -> "Video capability detected; hardware type unconfirmed"
            details.any { (key, value) -> key.startsWith("port_") && value != "TCP" } ||
                details.keys.any { it in setOf("mdns_name", "mdns_host", "ssdp_st", "ssdp_server") } -> "Network services only; no device identity"
            else -> "Online response only; no device identity"
        }
        return Result("Device type unconfirmed", reason)
    }
    fun name(details: Map<String, String>): String? = listOf("upnp_name", "mdns_name", "identity_model")
        .firstNotNullOfOrNull { details[it]?.lineSequence()?.firstOrNull(String::isNotBlank) }
}
