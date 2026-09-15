package com.spyfinder.hiddencamera.detectorapp.utils

import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Preserve unreadable originals before replacing them. A failed backup blocks replacement. */
internal class RecoverableHistoryFile(private val file: File, private val validate: (String) -> Unit) {
    private var savedStamp: Pair<Long, Long>? = null
    fun write(json: String) {
        val stamp = file.lastModified() to file.length()
        if (file.exists() && stamp != savedStamp && runCatching { validate(file.readText()) }.isFailure) {
            val backup = File(file.parentFile, file.name + ".unreadable-" + UUID.randomUUID())
            file.inputStream().use { input ->
                FileOutputStream(backup).use { output -> input.copyTo(output); output.fd.sync() }
            }
        }
        AtomicHistoryFile(file).write(json)
        savedStamp = file.lastModified() to file.length()
    }
}
