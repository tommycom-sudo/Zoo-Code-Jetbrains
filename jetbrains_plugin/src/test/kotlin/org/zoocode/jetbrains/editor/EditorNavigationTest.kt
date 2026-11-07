// SPDX-License-Identifier: Apache-2.0
package org.zoocode.jetbrains.editor

import com.intellij.openapi.editor.Document
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.lang.reflect.Proxy

class EditorNavigationTest {
    private fun document(lines: List<String>): Document {
        val starts = lines.indices.map { index -> lines.take(index).sumOf { it.length + 1 } }
        return Proxy.newProxyInstance(Document::class.java.classLoader, arrayOf(Document::class.java)) { _, method, args ->
            when (method.name) {
                "getLineCount" -> lines.size
                "getLineStartOffset" -> starts[args!![0] as Int]
                "getLineEndOffset" -> (args!![0] as Int).let { starts[it] + lines[it].length }
                else -> error("Unexpected document call: ${method.name}")
            }
        } as Document
    }

    @Test
    fun `chat link line 118 maps to the start of IDEA line index 117`() {
        val range = decodeEditorRange(mapOf(
            "startLineNumber" to 118.0, "startColumn" to 1.0,
            "endLineNumber" to 118.0, "endColumn" to 1.0
        ))!!
        assertEquals(234, navigationOffset(document(List(130) { "x" }), range.startLineNumber, range.startColumn))
    }

    @Test
    fun `first line and nonzero column use one based wire coordinates`() {
        val doc = document(listOf("abcd", "ef"))
        assertEquals(0, navigationOffset(doc, 1, 1))
        assertEquals(6, navigationOffset(doc, 2, 2))
    }

    @Test
    fun `stale and invalid positions clamp to document boundaries`() {
        val doc = document(listOf("abc", "de"))
        assertEquals(6, navigationOffset(doc, Int.MAX_VALUE, Int.MAX_VALUE))
        assertEquals(0, navigationOffset(doc, Int.MIN_VALUE, Int.MIN_VALUE))
        assertEquals(3, navigationOffset(doc, 1, 500))
        assertEquals(0, navigationOffset(document(listOf("")), 118, 1))
    }

    @Test
    fun `missing or malformed selection leaves the editor position alone`() {
        assertNull(decodeEditorRange(null))
        assertNull(decodeEditorRange(mapOf("startLineNumber" to "118", "startColumn" to 1)))
        assertNull(decodeEditorRange(emptyMap<String, Any>()))
    }

    @Test
    fun `selection wire format preserves reversed anchor and cursor`() {
        assertEquals(Selection(5, 8, 2, 3), decodeEditorSelection(mapOf(
            "selectionStartLineNumber" to 5.0, "selectionStartColumn" to 8.0,
            "positionLineNumber" to 2.0, "positionColumn" to 3.0
        )))
        assertNull(decodeEditorSelection(mapOf("positionLineNumber" to 118)))
    }
}
