package com.spyfinder.hiddencamera.detectorapp.utils

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.spyfinder.hiddencamera.detectorapp.model.WifiDevice
import org.json.JSONArray
import org.json.JSONObject
import com.spyfinder.hiddencamera.detectorapp.scan.Finding
import com.spyfinder.hiddencamera.detectorapp.scan.ScanStatus
import com.spyfinder.hiddencamera.detectorapp.scan.ScanCoverage
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ScanHistoryStore {
    private const val TAG = "ScanHistoryStore"
    private const val PREF_NAME = "sp_detect_scan_history"
    private const val ARCHIVE_KEY = "scan_archive_v2"
    private val pending = Channel<Pair<Context, ScanArchive>>(Channel.CONFLATED)
    private val failure = MutableStateFlow(false)
    val saveFailed = failure.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    init {
        scope.launch {
            for ((context, archive) in pending) {
                failure.value = runCatching {
                    val json = encode(archive)
                    check(decode(json) == archive) { "History validation failed" }
                    check(context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                        .putString(ARCHIVE_KEY, json).commit()) { "History write failed" }
                }.onFailure { Log.e(TAG, "History could not be saved", it) }.isFailure
            }
        }
    }
    fun save(context: Context, archive: ScanArchive) {
        pending.trySend(context.applicationContext to archive)
    }
    suspend fun load(context: Context): ScanArchive = withContext(Dispatchers.IO) {
        val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val archive = prefs.getString(ARCHIVE_KEY, null)?.let { runCatching { decode(it) }.getOrNull() }
        if (archive != null) return@withContext archive.interrupted()
        // Keep legacy keys intact: migration must never delete the only recoverable copy.
        runCatching {
            val suspicious = prefs.getString("key_suspicious_devices", null) ?: return@runCatching ScanArchive()
            val trusted = prefs.getString("key_trusted_devices", null) ?: return@runCatching ScanArchive()
            legacy(suspicious, trusted, prefs.getString("summary", null))
        }.getOrElse { ScanArchive() }
    }
    internal fun legacy(suspicious: String, trusted: String, summary: String?): ScanArchive {
        val record = ScanRecord("legacy", 0, null, "", null, ScanCoverage(),
            summary ?: "Legacy scan record — rescan to obtain evidence.",
            deserializeDeviceList(suspicious) + deserializeDeviceList(trusted))
        // Protect the previous record during migration, without claiming its completion is known.
        return ScanArchive(recent = record, complete = record)
    }
    internal fun encode(archive: ScanArchive): String = JSONObject().apply {
        put("version", 2)
        archive.recent?.let { put("recent", encodeRecord(it)) }
        archive.complete?.let { put("complete", encodeRecord(it)) }
    }.toString()
    internal fun decode(json: String): ScanArchive {
        val root = JSONObject(json)
        require(root.getInt("version") == 2)
        return ScanArchive(root.optJSONObject("recent")?.let(::decodeRecord), root.optJSONObject("complete")?.let(::decodeRecord))
    }
    private fun encodeRecord(record: ScanRecord) = JSONObject().apply {
        put("id", record.id); put("startedAt", record.startedAt)
        record.endedAt?.let { put("endedAt", it) }
        put("network", record.network); record.status?.let { put("status", it.name) }
        put("planned", record.coverage.planned); put("total", record.coverage.total)
        put("checked", record.coverage.checked); put("analyzed", record.coverage.analyzed)
        put("summary", record.summary); put("devices", serializeDeviceList(record.devices))
    }
    private fun decodeRecord(json: JSONObject) = ScanRecord(
        json.getString("id"), json.getLong("startedAt"), if (json.has("endedAt")) json.getLong("endedAt") else null,
        json.getString("network"), if (json.has("status")) ScanStatus.valueOf(json.getString("status")) else null,
        ScanCoverage(json.getInt("planned"), json.getLong("total"), json.getInt("checked"), json.getInt("analyzed")),
        json.getString("summary"), deserializeDeviceList(json.getJSONArray("devices").toString()))

    private fun serializeDeviceList(devices: List<WifiDevice>): JSONArray {
        val jsonArray = JSONArray()
        devices.forEach { device ->
            jsonArray.put(
                JSONObject().apply {
                    put("name", device.name)
                    put("type", device.type)
                    put("ip", device.ip)
                    put("iconRes", device.iconRes)
                    put("signal", device.signal)
                    put("signalColor", device.signalColor)
                    put("brandModel", device.brandModel)
                    put("mac", device.mac)
                    put("connected", device.connected)
                    put("rssi", device.rssi)
                    put("riskLevel", device.riskLevel)
                    put("finding", device.finding.name)
                    put("evidence", JSONArray(device.evidence))
                    put("userTrusted", device.userTrusted)
                    put("isCurrentPhone", device.isCurrentPhone)
                    put("analysisComplete", device.analysisComplete)
                    put("ruleVersion", device.ruleVersion)
                }
            )
        }
        return jsonArray
    }

    private fun deserializeDeviceList(json: String): List<WifiDevice> {
        val result = mutableListOf<WifiDevice>()
        val jsonArray = JSONArray(json)
        for (index in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.optJSONObject(index) ?: continue
            result.add(
                WifiDevice(
                    name = jsonObject.optString("name"),
                    type = jsonObject.optString("type"),
                    ip = jsonObject.optString("ip"),
                    iconRes = jsonObject.optInt("iconRes"),
                    signal = jsonObject.optInt("signal"),
                    signalColor = jsonObject.optInt("signalColor"),
                    brandModel = jsonObject.optString("brandModel"),
                    mac = jsonObject.optString("mac"),
                    connected = jsonObject.optBoolean("connected", false),
                    rssi = jsonObject.optInt("rssi"),
                    riskLevel = if (jsonObject.optString("finding") == Finding.CAMERA_FEATURES.name) 1 else 0,
                    finding = runCatching { Finding.valueOf(jsonObject.optString("finding")) }.getOrDefault(Finding.LEGACY),
                    evidence = jsonObject.optJSONArray("evidence")?.let { array -> (0 until array.length()).map { array.optString(it) } }.orEmpty(),
                    userTrusted = jsonObject.optBoolean("userTrusted", false),
                    isCurrentPhone = jsonObject.optBoolean("isCurrentPhone", false),
                    analysisComplete = jsonObject.optBoolean("analysisComplete", false),
                    ruleVersion = jsonObject.optInt("ruleVersion", 0)
                )
            )
        }
        return result
    }
}
