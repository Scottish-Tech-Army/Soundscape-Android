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
        val countryCode = components["country_code"] ?: (fallbackCountryCode ?: "GB").uppercase()

        if (appendCountry) {
            val countryName = (Templates.countryNames[countryCode] as? JsonPrimitive)?.content
            if (countryName != null && components["country"] == null) {
                components["country"] = countryName
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
        val useCountry = (country?.get("use_country") as? JsonPrimitive)?.content
        if (country != null && useCountry != null) {
            val oldCountryCode = countryCode
            countryCode = useCountry.uppercase()

            val changeCountry = (country["change_country"] as? JsonPrimitive)?.content
            if (changeCountry != null) {
                var newCountry = changeCountry
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
            val addComponentText = (oldCountry?.get("add_component") as? JsonPrimitive)?.content
            if (addComponentText != null && "=" in addComponentText) {
                val (k, v) = addComponentText.split("=", limit = 2)
                if (k == "state") components["state"] = v
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
                val aliasName = (obj["alias"] as? JsonPrimitive)?.content ?: continue
                val targetName = (obj["name"] as? JsonPrimitive)?.content ?: continue
                if (aliasName == key && !components.containsKey(targetName)) {
                    newKey = targetName
                    break
                }
            }
            aliased[key] = value
            aliased[newKey] = value
        }
        return aliased
    }

    private fun findTemplate(components: MutableMap<String, String>): JsonElement {
        val cc = components["country_code"] ?: "default"
        return Templates.worldwide[cc]
            ?: Templates.worldwide["default"]
            ?: JsonObject(emptyMap())
    }

    private fun resolveTemplateRef(ref: String): String {
        val resolved = Templates.worldwide[ref]
        return if (resolved is JsonPrimitive) resolved.content else ref
    }

    private fun chooseTemplateText(
        template: JsonElement,
        components: Map<String, String>
    ): String {
        val obj = template.jsonObject
        val defaultTemplate = Templates.worldwide["default"]?.jsonObject

        var selected: String? = (obj["address_template"] as? JsonPrimitive)?.content
            ?.let { resolveTemplateRef(it) }
        if (selected == null) {
            selected = (defaultTemplate?.get("address_template") as? JsonPrimitive)?.content
                ?.let { resolveTemplateRef(it) }
                ?: ""
        }

        val required = listOf("road", "postcode")
        val missingCount = required.count { !components.containsKey(it) }
        if (missingCount == 2) {
            val fallbackRef = (obj["fallback_template"] as? JsonPrimitive)?.content
                ?: (defaultTemplate?.get("fallback_template") as? JsonPrimitive)?.content
            if (fallbackRef != null) {
                selected = resolveTemplateRef(fallbackRef)
            }
        }
        return selected
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
            val stateVal = components["state"] ?: ""
            val countryCodeVal = components["country_code"] ?: ""
            val stateCode = getStateCode(stateVal, countryCodeVal)
            if (stateCode != null) components["state_code"] = stateCode

            if (Regex(
                    "^washington,? d\\.?c\\.?",
                    RegexOption.IGNORE_CASE
                ).containsMatchIn(stateVal)
            ) {
                components["state_code"] = "DC"
                components["state"] = "District of Columbia"
                components["city"] = "Washington"
            }
        }

        if (!components.containsKey("county_code") && components.containsKey("county")) {
            val countyVal = components["county"] ?: ""
            val countryCodeVal = components["country_code"] ?: ""
            val countyCode = getCountyCode(countyVal, countryCodeVal)
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

        if (abbreviate) {
            val cc = components["country_code"]
            val languages = cc?.let { Templates.country2lang[it]?.jsonArray }
            if (languages != null) {
                for (lang in languages) {
                    val langKey = (lang as? JsonPrimitive)?.content ?: continue
                    val langAbbrevs = Templates.abbreviations[langKey]?.jsonArray ?: continue
                    for (abbrevEntry in langAbbrevs) {
                        val obj = abbrevEntry.jsonObject
                        val component = (obj["component"] as? JsonPrimitive)?.content ?: continue
                        val currentVal = components[component] ?: continue
                        val abbrevsArray = obj["replacements"]?.jsonArray ?: continue
                        var updated = currentVal
                        for (r in abbrevsArray) {
                            val entry = r.jsonObject
                            val src = (entry["src"] as? JsonPrimitive)?.content ?: continue
                            val dest = (entry["dest"] as? JsonPrimitive)?.content ?: continue
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
            val name = when (node) {
                is JsonObject -> (node["default"] as? JsonPrimitive)?.content ?: continue
                is JsonPrimitive -> node.content
                else -> continue
            }
            if (name.equals(state, ignoreCase = true)) return code
        }
        return null
    }

    private fun getCountyCode(county: String, countryCode: String): String? {
        val countryData = Templates.countyCodes[countryCode]?.jsonObject ?: return null
        for ((code, node) in countryData) {
            val name = when (node) {
                is JsonObject -> (node["default"] as? JsonPrimitive)?.content ?: continue
                is JsonPrimitive -> node.content
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
        val postformat = obj["postformat_replace"] as? JsonArray
        if (postformat != null) {
            for (entry in postformat) {
                val arr = entry as? JsonArray ?: continue
                val pattern = (arr.getOrNull(0) as? JsonPrimitive)?.content ?: continue
                val replacement = (arr.getOrNull(1) as? JsonPrimitive)?.content ?: continue
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
        aliases.mapNotNull { (it.jsonObject["alias"] as? JsonPrimitive)?.content }
    }
}
