package com.spyfinder.hiddencamera.detectorapp.utils

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.io.File

class AtomicHistoryFileTest {
    @get:Rule val folder = TemporaryFolder()
    @Test fun commitsCompleteContentsAndReplacesExistingFile() {
        val file = File(folder.root, "history.json")
        val store = AtomicHistoryFile(file)
        assertNull(store.read())
        store.write("old history")
        store.write("new history 中文")
        assertEquals("new history 中文", store.read())
        assertFalse(File(folder.root, "history.json.tmp").exists())
    }
    @Test fun failedReplacementPreservesThePreviousHistoryAndCanBeRetried() {
        val file = File(folder.root, "history.json")
        AtomicHistoryFile(file).write("previous complete scan")
        val failing = AtomicHistoryFile(file) { _, _ -> throw IOException("injected disk failure") }
        assertTrue(runCatching { failing.write("new scan") }.isFailure)
        assertEquals("previous complete scan", file.readText())
        assertFalse(File(folder.root, "history.json.tmp").exists())
        AtomicHistoryFile(file).write("retried scan")
        assertEquals("retried scan", file.readText())
    }
}
