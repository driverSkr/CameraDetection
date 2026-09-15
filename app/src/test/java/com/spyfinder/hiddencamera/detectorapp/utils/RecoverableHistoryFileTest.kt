package com.spyfinder.hiddencamera.detectorapp.utils

import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class RecoverableHistoryFileTest {
    @get:Rule val folder = TemporaryFolder()
    @Test fun preservesUnreadableBytesBeforeWritingNewHistory() {
        val file = folder.newFile("history.json")
        val original = byteArrayOf(0, 1, 2, -1)
        file.writeBytes(original)
        RecoverableHistoryFile(file) { require(it == "valid") }.write("valid")
        assertEquals("valid", file.readText())
        val backup = folder.root.listFiles()!!.single { it.name.startsWith("history.json.unreadable-") }
        assertArrayEquals(original, backup.readBytes())
    }
    @Test fun repeatedCheckpointsDoNotRevalidateUnchangedSavedHistory() {
        val file = folder.newFile("history.json").apply { writeText("valid") }
        var validations = 0
        val writer = RecoverableHistoryFile(file) { validations++; require(it == "valid") }
        repeat(3) { writer.write("valid") }
        assertEquals(1, validations)
        assertEquals(1, folder.root.listFiles()!!.size)
    }
}
