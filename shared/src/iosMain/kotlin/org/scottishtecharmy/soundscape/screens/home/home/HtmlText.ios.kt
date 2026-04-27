package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle

private val linkStyles = TextLinkStyles(
    style = SpanStyle(textDecoration = TextDecoration.Underline)
)

actual fun parseHtmlToAnnotatedString(html: String): AnnotatedString = buildAnnotatedString {
    val tokens = tokenizeHtml(html)
    val openTags = ArrayDeque<Tag>()
    var pendingNewlines = 0

    fun flushNewlines() {
        if (pendingNewlines > 0) {
            repeat(pendingNewlines) { append('\n') }
            pendingNewlines = 0
        }
    }

    for (token in tokens) {
        when (token) {
            is HtmlToken.Text -> {
                if (token.value.isNotEmpty()) {
                    flushNewlines()
                    appendStyledText(token.value, openTags)
                }
            }
            is HtmlToken.Open -> {
                when (token.tag.name) {
                    "p", "div" -> if (length > 0) pendingNewlines = maxOf(pendingNewlines, 2)
                    "br" -> {
                        flushNewlines()
                        append('\n')
                    }
                    "li" -> {
                        if (length > 0) append('\n')
                        append("• ")
                    }
                    "ul", "ol" -> if (length > 0) pendingNewlines = maxOf(pendingNewlines, 1)
                }
                if (!token.selfClosing && token.tag.name !in voidElements) {
                    openTags.addLast(token.tag)
                }
            }
            is HtmlToken.Close -> {
                if (openTags.isNotEmpty() && openTags.last().name == token.name) {
                    openTags.removeLast()
                }
                when (token.name) {
                    "p", "div" -> pendingNewlines = maxOf(pendingNewlines, 2)
                    "li", "ul", "ol" -> pendingNewlines = maxOf(pendingNewlines, 1)
                }
            }
        }
    }
}

private fun AnnotatedString.Builder.appendStyledText(text: String, openTags: ArrayDeque<Tag>) {
    val style = SpanStyle(
        fontWeight = if (openTags.any { it.name in boldTags }) FontWeight.Bold else null,
        fontStyle = if (openTags.any { it.name in italicTags }) FontStyle.Italic else null,
        textDecoration = if (openTags.any { it.name == "u" }) TextDecoration.Underline else null,
        color = if (openTags.any { it.name == "code" }) Color.Unspecified else Color.Unspecified,
    )

    val href = openTags.lastOrNull { it.name == "a" }?.attrs?.get("href")
    if (href != null) {
        withLink(LinkAnnotation.Url(href, linkStyles)) {
            withStyle(style) { append(text) }
        }
    } else {
        withStyle(style) { append(text) }
    }
}

private val boldTags = setOf("b", "strong")
private val italicTags = setOf("i", "em")
private val voidElements = setOf("br", "hr", "img", "meta", "link", "input")

private data class Tag(val name: String, val attrs: Map<String, String>)

private sealed interface HtmlToken {
    data class Text(val value: String) : HtmlToken
    data class Open(val tag: Tag, val selfClosing: Boolean) : HtmlToken
    data class Close(val name: String) : HtmlToken
}

private fun tokenizeHtml(html: String): List<HtmlToken> {
    val out = mutableListOf<HtmlToken>()
    var i = 0
    val n = html.length
    val text = StringBuilder()

    fun flushText() {
        if (text.isNotEmpty()) {
            out.add(HtmlToken.Text(decodeEntities(text.toString())))
            text.clear()
        }
    }

    while (i < n) {
        val c = html[i]
        if (c == '<' && i + 1 < n) {
            val end = html.indexOf('>', i + 1)
            if (end == -1) {
                text.append(c)
                i++
                continue
            }
            flushText()
            val raw = html.substring(i + 1, end).trim()
            if (raw.startsWith("/")) {
                out.add(HtmlToken.Close(raw.substring(1).lowercase().substringBefore(' ')))
            } else {
                val selfClosing = raw.endsWith("/")
                val body = if (selfClosing) raw.dropLast(1).trim() else raw
                val parts = splitTagBody(body)
                val name = parts.first().lowercase()
                val attrs = parseAttrs(parts.drop(1))
                out.add(HtmlToken.Open(Tag(name, attrs), selfClosing))
            }
            i = end + 1
        } else {
            text.append(c)
            i++
        }
    }
    flushText()
    return out
}

private fun splitTagBody(body: String): List<String> {
    val parts = mutableListOf<String>()
    val cur = StringBuilder()
    var i = 0
    var inQuote: Char? = null
    while (i < body.length) {
        val c = body[i]
        if (inQuote != null) {
            cur.append(c)
            if (c == inQuote) inQuote = null
        } else if (c == '"' || c == '\'') {
            cur.append(c)
            inQuote = c
        } else if (c.isWhitespace()) {
            if (cur.isNotEmpty()) {
                parts.add(cur.toString())
                cur.clear()
            }
        } else {
            cur.append(c)
        }
        i++
    }
    if (cur.isNotEmpty()) parts.add(cur.toString())
    return parts
}

private fun parseAttrs(parts: List<String>): Map<String, String> {
    val out = mutableMapOf<String, String>()
    for (p in parts) {
        val eq = p.indexOf('=')
        if (eq <= 0) {
            out[p.lowercase()] = ""
        } else {
            val k = p.substring(0, eq).lowercase()
            var v = p.substring(eq + 1)
            if (v.length >= 2 && (v.first() == '"' || v.first() == '\'') && v.last() == v.first()) {
                v = v.substring(1, v.length - 1)
            }
            out[k] = decodeEntities(v)
        }
    }
    return out
}

private fun decodeEntities(input: String): String {
    if ('&' !in input) return input
    val sb = StringBuilder(input.length)
    var i = 0
    while (i < input.length) {
        val c = input[i]
        if (c == '&') {
            val semi = input.indexOf(';', i + 1)
            if (semi > i && semi - i <= 10) {
                val entity = input.substring(i + 1, semi)
                val decoded = decodeEntity(entity)
                if (decoded != null) {
                    sb.append(decoded)
                    i = semi + 1
                    continue
                }
            }
        }
        sb.append(c)
        i++
    }
    return sb.toString()
}

private fun decodeEntity(entity: String): String? = when {
    entity == "amp" -> "&"
    entity == "lt" -> "<"
    entity == "gt" -> ">"
    entity == "quot" -> "\""
    entity == "apos" -> "'"
    entity == "nbsp" -> " "
    entity.startsWith("#x") || entity.startsWith("#X") -> {
        val cp = entity.substring(2).toIntOrNull(16)
        cp?.let { Char(it).toString() }
    }
    entity.startsWith("#") -> {
        val cp = entity.substring(1).toIntOrNull()
        cp?.let { Char(it).toString() }
    }
    else -> null
}
