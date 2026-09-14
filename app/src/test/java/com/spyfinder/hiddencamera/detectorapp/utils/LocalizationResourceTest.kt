package com.spyfinder.hiddencamera.detectorapp.utils

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

class LocalizationResourceTest {
    private fun strings(folder: String): Map<String, String> {
        val base = File("src/main/res").takeIf { it.exists() } ?: File("app/src/main/res")
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(base, "$folder/strings.xml")).getElementsByTagName("string")
        return (0 until nodes.length).associate {
            val element = nodes.item(it) as Element
            element.getAttribute("name") to element.textContent.trim('"')
        }
    }

    @Test fun bothLanguagesHaveTheSameNonEmptyResources() {
        val english = strings("values")
        val chinese = strings("values-zh")
        assertEquals(english.keys, chinese.keys)
        (english + chinese).forEach { (key, value) -> assertTrue(key, value.isNotBlank()) }
    }

    @Test fun translationsPreserveFormatArguments() {
        val english = strings("values")
        val chinese = strings("values-zh")
        val placeholder = Regex("%[0-9]+\\$[sd]")
        english.forEach { (key, value) ->
            assertEquals(key, placeholder.findAll(value).map { it.value }.toList().sorted(),
                placeholder.findAll(chinese.getValue(key)).map { it.value }.toList().sorted())
        }
    }

    @Test fun defaultResourcesAreEnglishAndChineseResourcesAreChinese() {
        assertEquals("Start", strings("values").getValue("action_start"))
        assertEquals("开始", strings("values-zh").getValue("action_start"))
        assertEquals("Scan cancelled. Results are incomplete.", strings("values").getValue("scan_cancelled"))
        assertEquals("扫描已取消，结果不完整。", strings("values-zh").getValue("scan_cancelled"))
    }
}
