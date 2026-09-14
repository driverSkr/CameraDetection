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
    private const val ARCHIVE_KEY = "scan_archive_v3"
    private const val PREVIOUS_KEY = "scan_archive_v2"
    private const val ARCHIVE_FILE = "scan_history_v3.json"
    private val fileLock = Any()
    private val pending = Channel<Pair<Context, ScanArchive>>(Channel.CONFLATED)
    private val failure = MutableStateFlow(false)
    val saveFailed = failure.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    init {
        scope.launch {
            val encoder = ArchiveEncoder(validate = true)
            var saved: ScanArchive? = null
            for ((context, archive) in pending) {
                if (archive === saved) continue
                failure.value = runCatching {
                    val json = encoder.encode(archive)
                    synchronized(fileLock) { AtomicHistoryFile(java.io.File(context.filesDir, ARCHIVE_FILE)).write(json) }
                    saved = archive
                }.onFailure { Log.e(TAG, "History could not be saved", it) }.isFailure
            }
        }
    }
    fun save(context: Context, archive: ScanArchive) {
        pending.trySend(context.applicationContext to archive)
    }
    suspend fun load(context: Context): ScanArchive = withContext(Dispatchers.IO) {
        val savedFile = runCatching {
            val json = synchronized(fileLock) { AtomicHistoryFile(java.io.File(context.filesDir, ARCHIVE_FILE)).read() }
            json?.let(::decode)
        }.getOrNull()
        if (savedFile != null) return@withContext savedFile.interrupted()
        val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val archive = listOf(ARCHIVE_KEY, PREVIOUS_KEY).firstNotNullOfOrNull { key ->
            prefs.getString(key, null)?.let { runCatching { decode(it) }.getOrNull() }
        }
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
    internal fun encode(archive: ScanArchive): String = ArchiveEncoder(validate = false).encode(archive)

    /** At most two cached immutable records. Changed metadata does not re-encode or re-validate devices. */
    internal class ArchiveEncoder(private val validate: Boolean = true) {
        private data class Cached(val record: ScanRecord, val devices: String, val json: String)
        private val cached = mutableMapOf<String, Cached>()
        fun encode(archive: ScanArchive): String {
            val records = listOfNotNull(archive.recent, archive.complete).associateBy { it.id }
            if (archive.recent != null && archive.recent.id == archive.complete?.id) require(archive.recent == archive.complete)
            cached.keys.retainAll(records.keys)
            val entries = records.map { (id, record) ->
                val previous = cached[id]
                val encoded = if (previous != null && previous.record === record) previous else {
                    val devices = if (previous != null && previous.record.devices === record.devices) previous.devices else {
                        serializeDeviceList(record.devices).toString().also { json ->
                            if (validate) check(deserializeDeviceList(json) == record.devices) { "History device validation failed" }
                        }
                    }
                    val metadata = recordMetadata(record).toString()
                    if (validate) check(decodeRecord(JSONObject(metadata).put("devices", JSONArray())) == record.copy(devices = emptyList())) {
                        "History metadata validation failed"
                    }
                    Cached(record, devices, metadata.dropLast(1) + ",\"devices\":" + devices + "}")
                }
                cached[id] = encoded
                JSONObject.quote(id) + ":" + encoded.json
            }
            val header = JSONObject().put("version", 3)
            archive.recent?.let { header.put("recentId", it.id) }
            archive.complete?.let { header.put("completeId", it.id) }
            return header.toString().dropLast(1) + ",\"records\":{" + entries.joinToString(",") + "}}"
        }
    }
    internal fun decode(json: String): ScanArchive {
        val root = JSONObject(json)
        return when (root.getInt("version")) {
            2 -> ScanArchive(root.optJSONObject("recent")?.let(::decodeRecord), root.optJSONObject("complete")?.let(::decodeRecord))
            3 -> {
                val records = root.getJSONObject("records")
                val decoded = mutableMapOf<String, ScanRecord>()
                fun record(key: String): ScanRecord? {
                    if (!root.has(key)) return null
                    val id = root.getString(key)
                    return decoded.getOrPut(id) { decodeRecord(records.getJSONObject(id)).also { require(it.id == id) } }
                }
                ScanArchive(record("recentId"), record("completeId"))
            }
            else -> error("Unsupported history version")
        }
    }
    private fun recordMetadata(record: ScanRecord) = JSONObject().apply {
        put("id", record.id); put("startedAt", record.startedAt)
        record.endedAt?.let { put("endedAt", it) }
        put("network", record.network); record.status?.let { put("status", it.name) }
        put("planned", record.coverage.planned); put("total", record.coverage.total)
        put("checked", record.coverage.checked); put("analyzed", record.coverage.analyzed)
        put("summary", record.summary)
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
                    riskLevel = if (!jsonObject.optBoolean("isCurrentPhone", false) && jsonObject.optString("finding") == Finding.CAMERA_FEATURES.name) 1 else 0,
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
