package com.spyfinder.hiddencamera.detectorapp.utils

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/** Write and sync a sibling file before an atomic replacement; a failed write preserves the old record. */
internal class AtomicHistoryFile(private val file: File,
    private val replace: (Path, Path) -> Unit = { source, target ->
        Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }) {
    fun read(): String? = if (file.isFile) file.readText(Charsets.UTF_8) else null
    fun write(json: String) {
        val temporary = File(file.parentFile, file.name + ".tmp")
        try {
            FileOutputStream(temporary).use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
                stream.fd.sync()
            }
            replace(temporary.toPath(), file.toPath())
        } finally {
            Files.deleteIfExists(temporary.toPath())
        }
    }
}
