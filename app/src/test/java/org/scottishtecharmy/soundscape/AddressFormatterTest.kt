package org.scottishtecharmy.soundscape

import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.utils.address.AddressFormatter

class AddressFormatterTest {

    @Test
    fun basicGBFormatting() {
        val formatter = AddressFormatter(abbreviate = false, appendCountry = false, appendUnknown = false)
        val json = """{"house_number":"48","road":"Station Road","city":"Glasgow","postcode":"G62 8AB","country_code":"GB"}"""
        val result = formatter.format(json)
        println("GB result: [$result]")
        assert(result.contains("Station Road")) { "Expected Station Road in: $result" }
        assert(result.contains("48")) { "Expected 48 in: $result" }
    }

    @Test
    fun basicUSFormatting() {
        val formatter = AddressFormatter(abbreviate = false, appendCountry = true, appendUnknown = false)
        val json = """{"house_number":"123","road":"Main St","city":"Springfield","state":"Illinois","postcode":"62701","country_code":"US"}"""
        val result = formatter.format(json)
        println("US result: [$result]")
        assert(result.contains("Main St")) { "Expected Main St in: $result" }
        assert(result.contains("Springfield")) { "Expected Springfield in: $result" }
    }

    @Test
    fun fallbackCountryCode() {
        val formatter = AddressFormatter(abbreviate = false, appendCountry = true, appendUnknown = false)
        val json = """{"house_number":"10","road":"Main Street","city":"Springfield"}"""
        val result = formatter.format(json, "GB")
        println("Fallback GB result: [$result]")
        assert(result.contains("Main Street")) { "Expected Main Street in: $result" }
    }

    @Test
    fun noRoadOrPostcode() {
        val formatter = AddressFormatter(abbreviate = false, appendCountry = false, appendUnknown = false)
        val json = """{"city":"Glasgow","neighbourhood":"Milngavie","country_code":"GB"}"""
        val result = formatter.format(json)
        println("Fallback result: [$result]")
        assert(result.contains("Glasgow") || result.contains("Milngavie")) { "Expected city/neighbourhood in: $result" }
    }

    @Test
    fun allSupportedCountries() {
        val countries = listOf(
            "AD", "AE", "AF", "AG", "AI", "AL", "AM", "AO", "AQ", "AR", "AS", "AT", "AU", "AW",
            "AX", "AZ", "BA", "BB", "BD", "BE", "BF", "BG", "BH", "BI", "BJ", "BL", "BM", "BN",
            "BO", "BQ", "BR", "BS", "BT", "BV", "BW", "BY", "BZ", "CA", "CC", "CD", "CF", "CG",
            "CH", "CI", "CK", "CL", "CM", "CN", "CO", "CR", "CU", "CV", "CW", "CX", "CY", "CZ",
            "DE", "DJ", "DK", "DM", "DO", "DZ", "EC", "EE", "EG", "EH", "ER", "ES", "ET", "FI",
            "FJ", "FK", "FM", "FO", "FR", "GA", "GB", "GD", "GE", "GF", "GG", "GH", "GI", "GL",
            "GM", "GN", "GP", "GQ", "GR", "GS", "GT", "GU", "GW", "GY", "HK", "HM", "HN", "HR",
            "HT", "HU", "ID", "IE", "IL", "IM", "IN", "IO", "IQ", "IR", "IS", "IT", "JE", "JM",
            "JO", "JP", "KE", "KG", "KH", "KI", "KM", "KN", "KP", "KR", "KW", "KY", "KZ", "LA",
            "LB", "LC", "LI", "LK", "LR", "LS", "LT", "LU", "LV", "LY", "MA", "MC", "MD", "ME",
            "MF", "MG", "MH", "MK", "ML", "MM", "MN", "MO", "MP", "MQ", "MR", "MS", "MT", "MU",
            "MV", "MW", "MX", "MY", "MZ", "NA", "NC", "NE", "NF", "NG", "NI", "NL", "NO", "NP",
            "NR", "NU", "NZ", "OM", "PA", "PE", "PF", "PG", "PH", "PK", "PL", "PM", "PN", "PR",
            "PS", "PT", "PW", "PY", "QA", "RE", "RO", "RS", "RU", "RW", "SA", "SB", "SC", "SD",
            "SE", "SG", "SH", "SI", "SJ", "SK", "SL", "SM", "SN", "SO", "SR", "SS", "ST", "SV",
            "SX", "SY", "SZ", "TC", "TD", "TF", "TG", "TH", "TJ", "TK", "TL", "TM", "TN", "TO",
            "TR", "TT", "TV", "TW", "TZ", "UA", "UG", "UM", "US", "UY", "UZ", "VA", "VC", "VE",
            "VG", "VI", "VN", "VU", "WF", "WS", "YE", "YT", "ZA", "ZM", "ZW"
        )
        val formatter = AddressFormatter(abbreviate = false, appendCountry = true, appendUnknown = false)
        var failures = 0
        for (cc in countries) {
            val json = """{"house_number":"10","road":"Main Street","city":"Springfield"}"""
            try {
                val result = formatter.format(json, cc)
                assert(result.isNotBlank()) { "Empty result for $cc" }
            } catch (e: Exception) {
                println("Failed for $cc: ${e.message}")
                failures++
            }
        }
        println("Failures: $failures / ${countries.size}")
        assert(failures == 0) { "$failures countries failed" }
    }
}
