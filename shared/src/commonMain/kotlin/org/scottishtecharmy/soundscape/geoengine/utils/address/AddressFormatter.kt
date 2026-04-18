package org.scottishtecharmy.soundscape.geoengine.utils.address

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.scottishtecharmy.soundscape.platform.readResourceText

class AddressFormatter(
    private val abbreviate: Boolean = false,
    private val appendCountry: Boolean = false,
    private val appendUnknown: Boolean = false
) {
    fun format(json: String, fallbackCountryCode: String? = null): String {
        var components = parseJson(json)
        components = normalizeFields(components)

        if (fallbackCountryCode != null) {
            components["country_code"] = fallbackCountryCode
        }

        components = determineCountryCode(components, fallbackCountryCode)
        val countryCode = components["country_code"]!!

        if (appendCountry) {
            val countryNames = Templates.countryNames
            if (countryNames.containsKey(countryCode) && components["country"] == null) {
                components["country"] = countryNames[countryCode]!!.jsonPrimitive.content
            }
        } else {
            components.remove("country")
        }

        components = applyAliases(components)
        val template = findTemplate(components)
        val replaceNode = (template as? JsonObject)?.get("replace")
        components = cleanupInput(components, replaceNode)
        return renderTemplate(template, components)
    }

    private fun parseJson(json: String): MutableMap<String, String> {
        val obj = Json.parseToJsonElement(json).jsonObject
        val map = mutableMapOf<String, String>()
        for ((key, value) in obj) {
            val str = when (value) {
                is JsonPrimitive -> value.content
                else -> value.toString()
            }
            map[key] = str
        }
        return map
    }

    private fun normalizeFields(components: MutableMap<String, String>): MutableMap<String, String> {
        val normalized = mutableMapOf<String, String>()
        for ((key, value) in components) {
            val newKey = camelToSnake(key)
            if (!normalized.containsKey(newKey)) {
                normalized[newKey] = value
            }
        }
        return normalized
    }

    private fun determineCountryCode(
        components: MutableMap<String, String>,
        fallbackCountryCode: String?
    ): MutableMap<String, String> {
        var countryCode = components["country_code"]
            ?: fallbackCountryCode
            ?: error("No country code provided. Use fallbackCountryCode?")

        countryCode = countryCode.uppercase()

        if (!Templates.worldwide.containsKey(countryCode) || countryCode.length != 2) {
            error("Invalid country code: $countryCode")
        }

        if (countryCode == "UK") countryCode = "GB"

        val country = Templates.worldwide[countryCode]?.jsonObject
        if (country != null && country.containsKey("use_country")) {
            val oldCountryCode = countryCode
            countryCode = country["use_country"]!!.jsonPrimitive.content.uppercase()

            if (country.containsKey("change_country")) {
                var newCountry = country["change_country"]!!.jsonPrimitive.content
                val varRegex = Regex("\\$(\\w+)")
                val match = varRegex.find(newCountry)
                if (match != null) {
                    val varName = match.groupValues[1]
                    val replacement = components[varName] ?: ""
                    newCountry = newCountry.replace("\$$varName", replacement)
                }
                components["country"] = newCountry
            }

            val oldCountry = Templates.worldwide[oldCountryCode]?.jsonObject
            val addComponent = oldCountry?.get("add_component")
            if (addComponent != null) {
                val text = addComponent.jsonPrimitive.content
                if ("=" in text) {
                    val (k, v) = text.split("=", limit = 2)
                    if (k == "state") components["state"] = v
                }
            }
        }

        val state = components["state"]
        if (countryCode == "NL" && state != null) {
            when {
                state == "Curaçao" -> {
                    countryCode = "CW"
                    components["country"] = "Curaçao"
                }
                state.contains("sint maarten", ignoreCase = true) -> {
                    countryCode = "SX"
                    components["country"] = "Sint Maarten"
                }
                state.contains("aruba", ignoreCase = true) -> {
                    countryCode = "AW"
                    components["country"] = "Aruba"
                }
            }
        }

        components["country_code"] = countryCode
        return components
    }

    private fun applyAliases(components: MutableMap<String, String>): MutableMap<String, String> {
        val aliased = mutableMapOf<String, String>()
        for ((key, value) in components) {
            var newKey = key
            for (alias in Templates.aliases) {
                val obj = alias.jsonObject
                if (obj["alias"]!!.jsonPrimitive.content == key &&
                    !components.containsKey(obj["name"]!!.jsonPrimitive.content)
                ) {
                    newKey = obj["name"]!!.jsonPrimitive.content
                    break
                }
            }
            aliased[key] = value
            aliased[newKey] = value
        }
        return aliased
    }

    private fun findTemplate(components: MutableMap<String, String>): JsonElement {
        val cc = components["country_code"]!!
        return Templates.worldwide[cc] ?: Templates.worldwide["default"]!!
    }

    private fun chooseTemplateText(
        template: JsonElement,
        components: Map<String, String>
    ): String {
        val obj = template.jsonObject
        var selected: String? = null

        if (obj.containsKey("address_template")) {
            val ref = obj["address_template"]!!.jsonPrimitive.content
            val resolved = Templates.worldwide[ref]
            selected = if (resolved is JsonPrimitive) resolved.content else ref
        }
        if (selected == null) {
            val defaults = Templates.worldwide["default"]!!.jsonObject
            val ref = defaults["address_template"]!!.jsonPrimitive.content
            selected = Templates.worldwide[ref]!!.jsonPrimitive.content
        }

        val required = listOf("road", "postcode")
        val missingCount = required.count { !components.containsKey(it) }
        if (missingCount == 2) {
            if (obj.containsKey("fallback_template")) {
                val ref = obj["fallback_template"]!!.jsonPrimitive.content
                val resolved = Templates.worldwide[ref]
                selected = if (resolved is JsonPrimitive) resolved.content else ref
            } else {
                val defaults = Templates.worldwide["default"]!!.jsonObject
                val ref = defaults["fallback_template"]!!.jsonPrimitive.content
                selected = Templates.worldwide[ref]!!.jsonPrimitive.content
            }
        }
        return selected!!
    }

    private fun cleanupInput(
        components: MutableMap<String, String>,
        replacementsNode: JsonElement?
    ): MutableMap<String, String> {
        val country = components["country"]
        val state = components["state"]

        if (country != null && state != null && country.toIntOrNull() != null) {
            components["country"] = state
            components.remove("state")
        }

        if (replacementsNode is JsonArray && replacementsNode.size > 0) {
            for (key in components.keys.toList()) {
                for (replacement in replacementsNode) {
                    val arr = replacement.jsonArray
                    val pattern = arr[0].jsonPrimitive.content
                    val repl = arr[1].jsonPrimitive.content
                    val componentRegex = Regex("^${Regex.escape(key)}=")
                    if (componentRegex.containsMatchIn(pattern)) {
                        val value = componentRegex.replace(pattern, "")
                        if (components[key] == value) {
                            components[key] = repl
                        }
                    } else {
                        val currentValue = components[key] ?: continue
                        components[key] = Regex(pattern).replace(currentValue, repl)
                    }
                }
            }
        }

        if (!components.containsKey("state_code") && components.containsKey("state")) {
            val stateCode = getStateCode(components["state"]!!, components["country_code"]!!)
            if (stateCode != null) components["state_code"] = stateCode

            val stateVal = components["state"]!!
            if (Regex("^washington,? d\\.?c\\.?", RegexOption.IGNORE_CASE).containsMatchIn(stateVal)) {
                components["state_code"] = "DC"
                components["state"] = "District of Columbia"
                components["city"] = "Washington"
            }
        }

        if (!components.containsKey("county_code") && components.containsKey("county")) {
            val countyCode = getCountyCode(components["county"]!!, components["country_code"]!!)
            if (countyCode != null) components["county_code"] = countyCode
        }

        val unknownComponents = components.filter { !Templates.knownComponents.contains(it.key) }
            .values.toList()
        if (appendUnknown && unknownComponents.isNotEmpty()) {
            components["attention"] = unknownComponents.joinToString(", ")
        }

        val postcode = components["postcode"]
        if (postcode != null) {
            when {
                postcode.length > 20 -> components.remove("postcode")
                Regex("\\d+;\\d+").matches(postcode) -> components.remove("postcode")
                else -> {
                    val m = Regex("^(\\d{5}),\\d{5}").find(postcode)
                    if (m != null) components["postcode"] = m.groupValues[1]
                }
            }
        }

        if (abbreviate && components.containsKey("country_code")) {
            val cc = components["country_code"]!!
            val languages = Templates.country2lang[cc]?.jsonArray
            if (languages != null) {
                for (lang in languages) {
                    val langKey = lang.jsonPrimitive.content
                    val langAbbrevs = Templates.abbreviations[langKey]?.jsonArray ?: continue
                    for (abbrevEntry in langAbbrevs) {
                        val obj = abbrevEntry.jsonObject
                        val component = obj["component"]?.jsonPrimitive?.content ?: continue
                        val currentVal = components[component] ?: continue
                        val abbrevsArray = obj["replacements"]?.jsonArray ?: continue
                        var updated = currentVal
                        for (r in abbrevsArray) {
                            val src = r.jsonObject["src"]!!.jsonPrimitive.content
                            val dest = r.jsonObject["dest"]!!.jsonPrimitive.content
                            updated = Regex("\\b${Regex.escape(src)}\\b").replace(updated, dest)
                        }
                        components[component] = updated
                    }
                }
            }
        }

        val urlRegex = Regex("^https?://")
        return components.filterValues { !urlRegex.containsMatchIn(it) }.toMutableMap()
    }

    private fun getStateCode(state: String, countryCode: String): String? {
        val countryCodes = Templates.stateCodes[countryCode]?.jsonObject ?: return null
        for ((code, node) in countryCodes) {
            val name = when {
                node is JsonObject && node.containsKey("default") ->
                    node["default"]!!.jsonPrimitive.content
                node is JsonPrimitive -> node.content
                else -> continue
            }
            if (name.equals(state, ignoreCase = true)) return code
        }
        return null
    }

    private fun getCountyCode(county: String, countryCode: String): String? {
        val countryData = Templates.countyCodes[countryCode]?.jsonObject ?: return null
        for ((code, node) in countryData) {
            val name = when {
                node is JsonObject && node.containsKey("default") ->
                    node["default"]!!.jsonPrimitive.content
                node is JsonPrimitive -> node.content
                else -> continue
            }
            if (name.equals(county, ignoreCase = true)) return code
        }
        return null
    }

    private fun renderTemplate(
        template: JsonElement,
        components: MutableMap<String, String>
    ): String {
        val templateText = chooseTemplateText(template, components)
        var rendered = renderMustache(templateText, components)
        rendered = cleanupRender(rendered)

        val obj = template.jsonObject
        if (obj.containsKey("postformat_replace")) {
            val postformat = obj["postformat_replace"]!!.jsonArray
            for (entry in postformat) {
                val arr = entry.jsonArray
                val pattern = arr[0].jsonPrimitive.content
                val replacement = arr[1].jsonPrimitive.content
                rendered = Regex(pattern).replace(rendered, replacement)
            }
        }

        rendered = cleanupRender(rendered)
        return rendered.trim() + "\n"
    }

    companion object {
        private val cleanupReplacements = listOf(
            Regex("[},\\s]+$") to "",
            Regex("^[,\\s]+") to "",
            Regex("^- ") to "",
            Regex(",\\s*,") to ", ",
            Regex("[ \t]+,[ \t]+") to ", ",
            Regex("[ \t][ \t]+") to " ",
            Regex("[ \t]\n") to "\n",
            Regex("\n,") to "\n",
            Regex(",+") to ",",
            Regex(",\n") to "\n",
            Regex("\n[ \t]+") to "\n",
            Regex("\n+") to "\n",
        )

        private fun cleanupRender(rendered: String): String {
            var result = rendered
            for ((pattern, replacement) in cleanupReplacements) {
                result = pattern.replace(result, replacement)
                result = dedupe(result)
            }
            return result
        }

        private fun dedupe(rendered: String): String {
            return rendered.split("\n")
                .map { line ->
                    line.trim().split(", ")
                        .map { it.trim() }
                        .distinct()
                        .joinToString(", ")
                }
                .distinct()
                .joinToString("\n")
        }

        private fun renderMustache(template: String, data: Map<String, String>): String {
            val sb = StringBuilder()
            var i = 0
            while (i < template.length) {
                if (i + 2 < template.length && template[i] == '{' && template[i + 1] == '{') {
                    if (i + 3 < template.length && template[i + 2] == '{') {
                        // Triple brace: {{{field}}}
                        val end = template.indexOf("}}}", i + 3)
                        if (end != -1) {
                            val field = template.substring(i + 3, end).trim()
                            val value = data[field]
                            if (value != null) sb.append(value)
                            i = end + 3
                            continue
                        }
                    }
                    if (i + 8 < template.length && template.substring(i).startsWith("{{#first}}")) {
                        // {{#first}} ... {{/first}}
                        val end = template.indexOf("{{/first}}", i + 10)
                        if (end != -1) {
                            val inner = template.substring(i + 10, end)
                            val options = inner.split("||")
                            for (option in options) {
                                val rendered = renderMustache(option.trim(), data)
                                if (rendered.isNotBlank()) {
                                    sb.append(rendered)
                                    break
                                }
                            }
                            i = end + 10
                            continue
                        }
                    }
                    // Double brace: {{field}}
                    val end = template.indexOf("}}", i + 2)
                    if (end != -1) {
                        val field = template.substring(i + 2, end).trim()
                        val value = data[field]
                        if (value != null) sb.append(value)
                        i = end + 2
                        continue
                    }
                }
                sb.append(template[i])
                i++
            }
            return sb.toString()
        }

        private fun camelToSnake(s: String): String {
            val sb = StringBuilder()
            for (c in s) {
                if (c.isUpperCase()) {
                    if (sb.isNotEmpty()) sb.append('_')
                    sb.append(c.lowercaseChar())
                } else {
                    sb.append(c)
                }
            }
            return sb.toString()
        }
    }
}

private object Templates {
    private val json = Json { ignoreUnknownKeys = true }

    val worldwide: JsonObject by lazy {
        json.parseToJsonElement(readResourceText("address/worldwide.json")).jsonObject
    }
    val countryNames: JsonObject by lazy {
        json.parseToJsonElement(readResourceText("address/countrynames.json")).jsonObject
    }
    val aliases: JsonArray by lazy {
        json.parseToJsonElement(readResourceText("address/aliases.json")).jsonArray
    }
    val abbreviations: JsonObject by lazy {
        json.parseToJsonElement(readResourceText("address/abbreviations.json")).jsonObject
    }
    val country2lang: JsonObject by lazy {
        json.parseToJsonElement(readResourceText("address/country2lang.json")).jsonObject
    }
    val stateCodes: JsonObject by lazy {
        json.parseToJsonElement(readResourceText("address/statecodes.json")).jsonObject
    }
    val countyCodes: JsonObject by lazy {
        json.parseToJsonElement(readResourceText("address/countycodes.json")).jsonObject
    }
    val knownComponents: List<String> by lazy {
        aliases.map { it.jsonObject["alias"]!!.jsonPrimitive.content }
    }
}
