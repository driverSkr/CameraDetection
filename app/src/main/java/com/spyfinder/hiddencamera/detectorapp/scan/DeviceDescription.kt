package com.spyfinder.hiddencamera.detectorapp.scan

import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory
import java.io.ByteArrayInputStream

object DeviceDescription {
    fun localUrl(value: String, ip: String): String? = runCatching {
        val uri = URI(value)
        require(uri.scheme == "http" && uri.host == ip && ScanRules.ipv4(ip) != null)
        require(uri.userInfo == null && uri.fragment == null && (uri.port == -1 || uri.port in 1..65535))
        require(value.length <= 2048)
        uri.toASCIIString()
    }.getOrNull()

    fun parse(bytes: ByteArray, onvif: Boolean): Map<String, String> = runCatching {
        require(bytes.size <= 65536)
        val text = bytes.toString(Charsets.UTF_8)
        require(!text.contains("<!DOCTYPE", true) && !text.contains("<!ENTITY", true))
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isExpandEntityReferences = false
        }
        // Parse the checked UTF-8 text, so alternate byte encodings cannot bypass the declaration checks.
        val doc = factory.newDocumentBuilder().parse(ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)))
        val namespace = if (onvif) "http://www.onvif.org/ver10/device/wsdl" else "urn:schemas-upnp-org:device-1-0"
        val nodes = doc.getElementsByTagNameNS(namespace, if (onvif) "GetDeviceInformationResponse" else "device")
        val parent = nodes.item(0) ?: return@runCatching emptyMap()
        val fields = if (onvif) mapOf("Manufacturer" to "identity_manufacturer", "Model" to "identity_model", "FirmwareVersion" to "identity_firmware")
            else mapOf("friendlyName" to "upnp_name", "deviceType" to "upnp_type", "manufacturer" to "identity_manufacturer", "modelName" to "identity_model")
        val result = mutableMapOf<String, String>()
        for (index in 0 until parent.childNodes.length) {
            val child = parent.childNodes.item(index)
            val key = fields[child.localName] ?: continue
            if (child.namespaceURI != namespace) continue
            ServiceMetadata.clean(child.textContent).takeIf { it.isNotBlank() }?.let { result[key] = it }
        }
        if (result.isNotEmpty()) result["identity_source"] = if (onvif) "ONVIF" else "UPnP"
        result
    }.getOrDefault(emptyMap())
}
