package org.scottishtecharmy.soundscape

import org.scottishtecharmy.soundscape.geoengine.utils.address.AddressFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddressFormatterTest {

    @Test
    fun basicGBFormatting() {
        val formatter =
            AddressFormatter(abbreviate = false, appendCountry = false, appendUnknown = false)
        val json =
            """{"house_number":"48","road":"Station Road","city":"Glasgow","postcode":"G62 8AB","country_code":"GB"}"""
        val result = formatter.format(json)
        println("GB result: [$result]")
        assertTrue(result.contains("Station Road"), "Expected Station Road in: $result")
        assertTrue(result.contains("48"), "Expected 48 in: $result")
    }

    @Test
    fun basicUSFormatting() {
        val formatter =
            AddressFormatter(abbreviate = false, appendCountry = true, appendUnknown = false)
        val json =
            """{"house_number":"123","road":"Main St","city":"Springfield","state":"Illinois","postcode":"62701","country_code":"US"}"""
        val result = formatter.format(json)
        println("US result: [$result]")
        assertTrue(result.contains("Main St"), "Expected Main St in: $result")
        assertTrue(result.contains("Springfield"), "Expected Springfield in: $result")
    }

    @Test
    fun fallbackCountryCode() {
        val formatter =
            AddressFormatter(abbreviate = false, appendCountry = true, appendUnknown = false)
        val json = """{"house_number":"10","road":"Main Street","city":"Springfield"}"""
        val result = formatter.format(json, "GB")
        println("Fallback GB result: [$result]")
        assertTrue(result.contains("Main Street"), "Expected Main Street in: $result")
    }

    @Test
    fun noRoadOrPostcode() {
        val formatter =
            AddressFormatter(abbreviate = false, appendCountry = false, appendUnknown = false)
        val json = """{"city":"Glasgow","neighbourhood":"Milngavie","country_code":"GB"}"""
        val result = formatter.format(json)
        println("Fallback result: [$result]")
        assertTrue(result.contains("Glasgow") || result.contains("Milngavie"), "Expected city/neighbourhood in: $result")
    }

    @Test
    fun lineOfJustASeparatorIsRemoved() {
        // El Salvador's template has a "{{postcode}} - {{city}}" line, which leaves a lone "-"
        // for an address with neither
        val formatter =
            AddressFormatter(abbreviate = false, appendCountry = false, appendUnknown = false)
        val json = """{"house_number":"315","road":"Calle Presbítero Vicente Aguilar","country_code":"SV"}"""
        assertEquals("Calle Presbítero Vicente Aguilar 315\n", formatter.format(json))
    }

    @Test
    fun japaneseAddressInJapaneseIsWrittenLargestFirst() {
        val formatter =
            AddressFormatter(abbreviate = false, appendCountry = false, appendUnknown = false)
        val json = """{"house_number":"20","road":"創造のみち","city":"北区","country_code":"JP"}"""
        assertEquals("北区\n創造のみち\n20\n", formatter.format(json))
    }

    @Test
    fun japaneseAddressInLatinScriptIsWrittenSmallestFirst() {
        val formatter =
            AddressFormatter(abbreviate = false, appendCountry = false, appendUnknown = false)
        val json = """{"house_number":"20","road":"Sozo-no-michi","city":"Kita","country_code":"JP"}"""
        assertEquals("20 Sozo-no-michi\nKita\n", formatter.format(json))
    }

    @Test
    fun iranianAddressInPersianUsesThePersianTemplate() {
        // The Persian template starts from the province, where the English one ends with it
        val formatter =
            AddressFormatter(abbreviate = false, appendCountry = false, appendUnknown = false)
        val json =
            """{"house_number":"۱۴","road":"رشتچی","city":"تهران","state":"استان تهران","country_code":"IR"}"""
        assertEquals("استان تهران\nتهران\nرشتچی\n۱۴\n", formatter.format(json))
    }

    // The other countries with a template in their own language as well as one in a Latin-script
    // language, each with an address written both ways

    private fun format(json: String) =
        AddressFormatter(abbreviate = false, appendCountry = false, appendUnknown = false).format(json)

    @Test
    fun chineseAddressInChineseIsWrittenLargestFirst() {
        assertEquals(
            "北京市\n长安街\n1\n",
            format("""{"house_number":"1","road":"长安街","city":"北京市","country_code":"CN"}""")
        )
    }

    @Test
    fun chineseAddressInLatinScriptIsWrittenSmallestFirst() {
        assertEquals(
            "1 Chang'an Avenue\nBeijing\n",
            format("""{"house_number":"1","road":"Chang'an Avenue","city":"Beijing","country_code":"CN"}""")
        )
    }

    @Test
    fun koreanAddressInKoreanIsWrittenLargestFirst() {
        assertEquals(
            "서울특별시\n세종대로\n175\n",
            format("""{"house_number":"175","road":"세종대로","city":"서울특별시","country_code":"KR"}""")
        )
    }

    @Test
    fun koreanAddressInLatinScriptIsWrittenSmallestFirst() {
        assertEquals(
            "175 Sejong-daero\nSeoul\n",
            format("""{"house_number":"175","road":"Sejong-daero","city":"Seoul","country_code":"KR"}""")
        )
    }

    @Test
    fun taiwaneseAddressInChineseIsWrittenLargestFirst() {
        assertEquals(
            "臺北市\n信義路五段\n7\n",
            format("""{"house_number":"7","road":"信義路五段","city":"臺北市","country_code":"TW"}""")
        )
    }

    @Test
    fun taiwaneseAddressInLatinScriptIsWrittenSmallestFirst() {
        assertEquals(
            "7 Xinyi Road Section 5\nTaipei\n",
            format("""{"house_number":"7","road":"Xinyi Road Section 5","city":"Taipei","country_code":"TW"}""")
        )
    }

    @Test
    fun hongKongAddressInChineseIsWrittenLargestFirst() {
        assertEquals(
            "中西區\n皇后大道中\n1\n",
            format("""{"house_number":"1","road":"皇后大道中","state_district":"中西區","country_code":"HK"}""")
        )
    }

    @Test
    fun hongKongAddressInLatinScriptIsWrittenSmallestFirst() {
        assertEquals(
            "1 Queen's Road Central\nCentral and Western\n",
            format("""{"house_number":"1","road":"Queen's Road Central","state_district":"Central and Western","country_code":"HK"}""")
        )
    }

    @Test
    fun macauAddressInChineseIsWrittenLargestFirst() {
        assertEquals(
            "大堂區\n新馬路\n1\n",
            format("""{"house_number":"1","road":"新馬路","suburb":"大堂區","country_code":"MO"}""")
        )
    }

    @Test
    fun macauAddressInPortugueseIsWrittenStreetFirst() {
        assertEquals(
            "Avenida de Almeida Ribeiro 1\nSé\n",
            format("""{"house_number":"1","road":"Avenida de Almeida Ribeiro","suburb":"Sé","country_code":"MO"}""")
        )
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
        val formatter =
            AddressFormatter(abbreviate = false, appendCountry = true, appendUnknown = false)
        var failures = 0
        for (cc in countries) {
            val json = """{"house_number":"10","road":"Main Street","city":"Springfield"}"""
            try {
                val result = formatter.format(json, cc)
                assertTrue(result.isNotBlank(), "Empty result for $cc")
            } catch (e: Exception) {
                println("Failed for $cc: ${e.message}")
                failures++
            }
        }
        println("Failures: $failures / ${countries.size}")
        assertTrue(failures == 0, "$failures countries failed")
    }
}
