package org.scottishtecharmy.soundscape.geoengine.utils.gpx

fun parseGpx(input: String): GpxData {
    val tokens = tokenize(input)
    return buildGpx(tokens)
}

private sealed class XmlToken {
    data class StartTag(val localName: String, val attributes: Map<String, String>) : XmlToken()
    data class EndTag(val localName: String) : XmlToken()
    data class Text(val content: String) : XmlToken()
}

private fun tokenize(xml: String): List<XmlToken> {
    val tokens = mutableListOf<XmlToken>()
    var i = 0
    while (i < xml.length) {
        if (xml[i] == '<') {
            val end = xml.indexOf('>', i)
            if (end == -1) break
            val tag = xml.substring(i + 1, end).trim()
            i = end + 1

            if (tag.startsWith("?") || tag.startsWith("!")) continue

            if (tag.startsWith("/")) {
                tokens.add(XmlToken.EndTag(stripNamespace(tag.substring(1).trim())))
            } else {
                val selfClosing = tag.endsWith("/")
                val content = if (selfClosing) tag.dropLast(1).trim() else tag
                val parts = splitTagContent(content)
                val localName = stripNamespace(parts.first)
                tokens.add(XmlToken.StartTag(localName, parts.second))
                if (selfClosing) {
                    tokens.add(XmlToken.EndTag(localName))
                }
            }
        } else {
            val nextTag = xml.indexOf('<', i)
            val textEnd = if (nextTag == -1) xml.length else nextTag
            val text = xml.substring(i, textEnd)
            if (text.isNotBlank()) {
                tokens.add(XmlToken.Text(decodeEntities(text.trim())))
            }
            i = textEnd
        }
    }
    return tokens
}

private fun stripNamespace(name: String): String {
    val colon = name.indexOf(':')
    return if (colon >= 0) name.substring(colon + 1) else name
}

private fun splitTagContent(tag: String): Pair<String, Map<String, String>> {
    val attrs = mutableMapOf<String, String>()
    val nameEnd = tag.indexOfFirst { it.isWhitespace() }
    if (nameEnd == -1) return Pair(tag, attrs)

    val name = tag.substring(0, nameEnd)
    var rest = tag.substring(nameEnd).trim()

    while (rest.isNotEmpty()) {
        val eq = rest.indexOf('=')
        if (eq == -1) break
        val key = rest.substring(0, eq).trim()
        rest = rest.substring(eq + 1).trim()
        if (rest.isEmpty()) break

        val quote = rest[0]
        if (quote != '"' && quote != '\'') break
        val closeQuote = rest.indexOf(quote, 1)
        if (closeQuote == -1) break

        attrs[stripNamespace(key)] = decodeEntities(rest.substring(1, closeQuote))
        rest = rest.substring(closeQuote + 1).trim()
    }
    return Pair(name, attrs)
}

private fun decodeEntities(s: String): String {
    return s.replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
}

private class TokenReader(private val tokens: List<XmlToken>) {
    var pos = 0
    fun hasNext() = pos < tokens.size
    fun peek(): XmlToken? = if (hasNext()) tokens[pos] else null
    fun next(): XmlToken = tokens[pos++]

    fun skipToEndTag(name: String) {
        var depth = 1
        while (hasNext() && depth > 0) {
            when (val t = next()) {
                is XmlToken.StartTag -> if (t.localName == name) depth++
                is XmlToken.EndTag -> if (t.localName == name) depth--
                is XmlToken.Text -> {}
            }
        }
    }

    fun readTextContent(tagName: String): String {
        val sb = StringBuilder()
        while (hasNext()) {
            when (val t = peek()!!) {
                is XmlToken.EndTag -> {
                    if (t.localName == tagName) { next(); return sb.toString() }
                    break
                }
                is XmlToken.Text -> { sb.append(t.content); next() }
                is XmlToken.StartTag -> { next(); skipToEndTag(t.localName) }
            }
        }
        return sb.toString()
    }
}

private fun buildGpx(tokens: List<XmlToken>): GpxData {
    val reader = TokenReader(tokens)
    var metadata = GpxMetadata()
    val waypoints = mutableListOf<GpxWaypoint>()
    val routes = mutableListOf<GpxRoute>()
    val tracks = mutableListOf<GpxTrack>()

    while (reader.hasNext()) {
        when (val t = reader.next()) {
            is XmlToken.StartTag -> when (t.localName) {
                "metadata" -> metadata = readMetadata(reader)
                "wpt" -> waypoints.add(readWaypoint(reader, "wpt", t.attributes))
                "rte" -> routes.add(readRoute(reader))
                "trk" -> tracks.add(readTrack(reader))
                else -> {}
            }
            else -> {}
        }
    }
    return GpxData(metadata, waypoints, routes, tracks)
}

private fun readMetadata(reader: TokenReader): GpxMetadata {
    var name = ""
    var desc = ""
    while (reader.hasNext()) {
        when (val t = reader.next()) {
            is XmlToken.StartTag -> when (t.localName) {
                "name" -> name = reader.readTextContent("name")
                "desc" -> desc = reader.readTextContent("desc")
                else -> reader.skipToEndTag(t.localName)
            }
            is XmlToken.EndTag -> if (t.localName == "metadata") break
            is XmlToken.Text -> {}
        }
    }
    return GpxMetadata(name, desc)
}

private fun readWaypoint(reader: TokenReader, endTag: String, attrs: Map<String, String>): GpxWaypoint {
    val lat = attrs["lat"]?.toDoubleOrNull() ?: 0.0
    val lon = attrs["lon"]?.toDoubleOrNull() ?: 0.0
    var name = ""
    var desc = ""
    var ele: Double? = null
    var time: String? = null

    while (reader.hasNext()) {
        when (val t = reader.next()) {
            is XmlToken.StartTag -> when (t.localName) {
                "name" -> name = reader.readTextContent("name")
                "desc" -> desc = reader.readTextContent("desc")
                "ele" -> ele = reader.readTextContent("ele").toDoubleOrNull()
                "time" -> time = reader.readTextContent("time")
                else -> reader.skipToEndTag(t.localName)
            }
            is XmlToken.EndTag -> if (t.localName == endTag) break
            is XmlToken.Text -> {}
        }
    }
    return GpxWaypoint(lat, lon, name, desc, ele, time)
}

private fun readRoute(reader: TokenReader): GpxRoute {
    var name = ""
    val points = mutableListOf<GpxWaypoint>()
    while (reader.hasNext()) {
        when (val t = reader.next()) {
            is XmlToken.StartTag -> when (t.localName) {
                "name" -> name = reader.readTextContent("name")
                "rtept" -> points.add(readWaypoint(reader, "rtept", t.attributes))
                else -> reader.skipToEndTag(t.localName)
            }
            is XmlToken.EndTag -> if (t.localName == "rte") break
            is XmlToken.Text -> {}
        }
    }
    return GpxRoute(name, points)
}

private fun readTrack(reader: TokenReader): GpxTrack {
    var name = ""
    val segments = mutableListOf<GpxTrackSegment>()
    while (reader.hasNext()) {
        when (val t = reader.next()) {
            is XmlToken.StartTag -> when (t.localName) {
                "name" -> name = reader.readTextContent("name")
                "trkseg" -> segments.add(readTrackSegment(reader))
                else -> reader.skipToEndTag(t.localName)
            }
            is XmlToken.EndTag -> if (t.localName == "trk") break
            is XmlToken.Text -> {}
        }
    }
    return GpxTrack(name, segments)
}

private fun readTrackSegment(reader: TokenReader): GpxTrackSegment {
    val points = mutableListOf<GpxTrackPoint>()
    while (reader.hasNext()) {
        when (val t = reader.next()) {
            is XmlToken.StartTag -> when (t.localName) {
                "trkpt" -> points.add(readTrackPoint(reader, t.attributes))
                else -> reader.skipToEndTag(t.localName)
            }
            is XmlToken.EndTag -> if (t.localName == "trkseg") break
            is XmlToken.Text -> {}
        }
    }
    return GpxTrackSegment(points)
}

private fun readTrackPoint(reader: TokenReader, attrs: Map<String, String>): GpxTrackPoint {
    val lat = attrs["lat"]?.toDoubleOrNull() ?: 0.0
    val lon = attrs["lon"]?.toDoubleOrNull() ?: 0.0
    var ele: Double? = null
    var time: String? = null
    var speed: Float? = null
    var bearing: Float? = null

    while (reader.hasNext()) {
        when (val t = reader.next()) {
            is XmlToken.StartTag -> when (t.localName) {
                "ele" -> ele = reader.readTextContent("ele").toDoubleOrNull()
                "time" -> time = reader.readTextContent("time")
                "speed" -> speed = reader.readTextContent("speed").toFloatOrNull()
                "bearing" -> bearing = reader.readTextContent("bearing").toFloatOrNull()
                else -> reader.skipToEndTag(t.localName)
            }
            is XmlToken.EndTag -> if (t.localName == "trkpt") break
            is XmlToken.Text -> {}
        }
    }
    return GpxTrackPoint(lat, lon, ele, time, speed, bearing)
}
