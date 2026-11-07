// SPDX-License-Identifier: Apache-2.0
package org.zoocode.jetbrains.editor

import com.intellij.openapi.editor.Document

/** Extension-host range coordinates are one-based, unlike IDEA's line indices. */
internal fun decodeEditorRange(value: Any?): Range? {
    val map = value as? Map<*, *> ?: return null
    val line = (map["startLineNumber"] as? Number)?.toInt() ?: return null
    val column = (map["startColumn"] as? Number)?.toInt() ?: return null
    return Range(
        line, column,
        (map["endLineNumber"] as? Number)?.toInt() ?: line,
        (map["endColumn"] as? Number)?.toInt() ?: column
    )
}

internal fun decodeEditorSelection(value: Any?): Selection? {
    val map = value as? Map<*, *> ?: return null
    return Selection(
        (map["selectionStartLineNumber"] as? Number)?.toInt() ?: return null,
        (map["selectionStartColumn"] as? Number)?.toInt() ?: return null,
        (map["positionLineNumber"] as? Number)?.toInt() ?: return null,
        (map["positionColumn"] as? Number)?.toInt() ?: return null
    )
}

/** Old chat links can refer past EOF or past the end of a line after edits. */
internal fun navigationOffset(document: Document, lineNumber: Int, column: Int): Int {
    val line = (lineNumber.toLong() - 1).coerceIn(0, (document.lineCount - 1).coerceAtLeast(0).toLong()).toInt()
    val start = document.getLineStartOffset(line)
    val length = document.getLineEndOffset(line) - start
    return start + (column.toLong() - 1).coerceIn(0, length.toLong()).toInt()
}
