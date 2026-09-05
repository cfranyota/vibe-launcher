package com.vibelauncher.app.ui.notes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle

internal const val BOLD_TOKEN = "**"
internal const val UNDERLINE_TOKEN = "__"
internal const val ITALIC_TOKEN = "*"

/** Parses a block's raw stored text (markdown-style tokens embedded directly in the
 *  string) into a styled [AnnotatedString]. The token characters themselves stay visible
 *  (de-emphasized) rather than being hidden - this keeps the transform a same-length,
 *  same-offsets mapping (OffsetMapping.Identity is safe), avoiding a much more fragile
 *  custom offset-mapping implementation for what's still a fully real, rendered,
 *  persisted bold/italic/underline. Single-pass, non-overlapping: bold (**) and underline
 *  (__) are matched first, then italic (*) only outside their consumed ranges. */
internal fun parseFormattedText(raw: String, tokenColor: Color): AnnotatedString {
    data class Range(val start: Int, val end: Int, val style: SpanStyle)

    val ranges = mutableListOf<Range>()
    val consumed = BooleanArray(raw.length)

    fun findPairs(token: String, style: SpanStyle) {
        var i = 0
        while (i <= raw.length - token.length) {
            if (raw.regionMatches(i, token, 0, token.length) && !consumed[i]) {
                val closeStart = raw.indexOf(token, i + token.length)
                if (closeStart == -1) break
                val closeEnd = closeStart + token.length
                if ((i until closeEnd).none { consumed[it] }) {
                    ranges += Range(i, closeEnd, style)
                    for (k in i until closeEnd) consumed[k] = true
                    i = closeEnd
                    continue
                }
            }
            i++
        }
    }

    findPairs(BOLD_TOKEN, SpanStyle(fontWeight = FontWeight.Bold))
    findPairs(UNDERLINE_TOKEN, SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))

    // Italic - single '*' not already consumed by a bold/underline match above.
    run {
        var i = 0
        while (i < raw.length) {
            if (raw[i] == '*' && !consumed[i]) {
                val closeStart = (i + 1 until raw.length).firstOrNull { raw[it] == '*' && !consumed[it] }
                if (closeStart != null) {
                    ranges += Range(i, closeStart + 1, SpanStyle(fontStyle = FontStyle.Italic))
                    for (k in i..closeStart) consumed[k] = true
                    i = closeStart + 1
                    continue
                }
            }
            i++
        }
    }

    return AnnotatedString.Builder(raw).apply {
        for (range in ranges.sortedBy { it.start }) {
            addStyle(range.style, range.start, range.end)
            // De-emphasize just the token characters themselves within the styled range.
            addStyle(SpanStyle(color = tokenColor), range.start, minOf(range.start + tokenLength(range.style), range.end))
        }
    }.toAnnotatedString()
}

private fun tokenLength(style: SpanStyle): Int = when {
    style.fontWeight == FontWeight.Bold -> BOLD_TOKEN.length
    style.textDecoration != null -> UNDERLINE_TOKEN.length
    else -> ITALIC_TOKEN.length
}

internal fun noteFormattingTransformation(tokenColor: Color): VisualTransformation = VisualTransformation { text ->
    TransformedText(parseFormattedText(text.text, tokenColor), OffsetMapping.Identity)
}
