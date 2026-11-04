package org.scottishtecharmy.soundscape.geoengine.utils.address

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JapaneseAddressTest {

    @Test
    fun chomeMappedEachWay() {
        // The ways the district and chōme are mapped around Osaka, Sakai and Kyoto
        assertEquals("梅田三丁目", JapaneseAddress.chome(null, "梅田三丁目"))
        assertEquals("豊崎3丁目", JapaneseAddress.chome("豊崎", "3"))
        assertEquals("出島町2丁", JapaneseAddress.chome("出島町", "2丁"))
        assertEquals("中崎西四丁目", JapaneseAddress.chome("中崎西", "中崎西四丁目"))
        assertEquals("豊崎3", JapaneseAddress.chome(null, "豊崎3"))
        assertEquals("祇園町北側", JapaneseAddress.chome("祇園町北側", null))
        assertNull(JapaneseAddress.chome(null, null))
    }

    @Test
    fun wholeAddressInNeighbourhoodIsIgnored() {
        assertEquals("梅田", JapaneseAddress.chome("梅田", "Umeda, Kita-ku, Osaka-shi, Osaka 530-0001, Japan"))
    }

    @Test
    fun blockAndBuildingNumbersAreJoined() {
        assertEquals("梅田三丁目1-1", JapaneseAddress.address(null, "梅田三丁目", "1", "1"))
        assertEquals("梅田三丁目1", JapaneseAddress.address(null, "梅田三丁目", "1", null))
        assertEquals("祇園町北側267", JapaneseAddress.address("祇園町北側", null, null, "267"))
        // The floor of a building isn't part of its address
        assertEquals("中島町103", JapaneseAddress.address("中島町", null, "103", "フジタビル1BF"))
        // Nor is there an address without its district
        assertNull(JapaneseAddress.address(null, null, "6", "8"))
    }

    @Test
    fun addressIsFoundHoweverItsNumbersAreWritten() {
        val key = JapaneseAddress.searchKeys(null, "梅田三丁目", "1", "1")!!.first
        for (typed in listOf(
            "梅田3-1-1",
            "梅田三丁目1番1号",
            "梅田３丁目１−１",
            "梅田 3-1-1",
            "北区梅田3丁目1-1",
            "大阪市北区梅田三丁目1番1号",
        )) {
            assertTrue(JapaneseAddress.isMatch(JapaneseAddress.searchKey(typed)!!, key), typed)
        }
        // However its chōme was mapped
        assertEquals(key, JapaneseAddress.searchKeys("梅田", "3", "1", "1")!!.first)
    }

    @Test
    fun blockIsFoundWithoutItsBuilding() {
        val (building, block) = JapaneseAddress.searchKeys(null, "梅田三丁目", "1", "1")!!
        val typed = JapaneseAddress.searchKey("梅田三丁目1番")!!
        assertFalse(JapaneseAddress.isMatch(typed, building))
        assertTrue(JapaneseAddress.isMatch(typed, block))
    }

    @Test
    fun blockOfADistrictWithNoChomeIsFoundWithBan() {
        // 大深町 has no chōme, so its block is the only number in the address and 番 is what says
        // it's a block - without it "大深町4" could be the end of a name
        val block = JapaneseAddress.searchKeys(null, "大深町", "4", "20")!!.second
        assertTrue(JapaneseAddress.isMatch(JapaneseAddress.searchKey("大深町4番")!!, block))
        assertTrue(JapaneseAddress.isMatch(JapaneseAddress.searchKey("北区大深町4番地")!!, block))
        assertNull(JapaneseAddress.searchKey("大深町4"))
        // A name that ends in a number still isn't an address
        assertNull(JapaneseAddress.searchKey("ローソン梅田3"))
    }

    @Test
    fun otherAddressesDontMatch() {
        val key = JapaneseAddress.searchKeys(null, "梅田三丁目", "1", "1")!!.first
        for (typed in listOf("梅田3-1-11", "梅田3-11-1", "梅田2-1-1", "茶屋町1-1")) {
            assertFalse(JapaneseAddress.isMatch(JapaneseAddress.searchKey(typed)!!, key), typed)
        }
        // A name, a number with no name, or an address that isn't Japanese isn't looked for as one
        assertNull(JapaneseAddress.searchKey("大阪駅"))
        assertNull(JapaneseAddress.searchKey("3-1-1"))
        assertNull(JapaneseAddress.searchKey("Calle 4 25"))
    }

    @Test
    fun blockAddressTaggedAsAStreetAddress() {
        // The district in addr:street, behind its city and ward, and the chōme, block and building
        // in addr:housenumber - from shops in Kobe and Osaka
        assertEquals(
            JapaneseAddress.Parts("中央区", "北長狭通", "1", "31", "35"),
            JapaneseAddress.fromStreet("神戸市中央区北長狭通", "1-31-35")
        )
        assertEquals(
            JapaneseAddress.Parts("中央区", "北長狭通", "1", "30", "19"),
            JapaneseAddress.fromStreet("神戸市中央区北長狭通", "１丁目３０−１９")
        )
        assertEquals(
            JapaneseAddress.Parts(null, "道頓堀", "1", "4", "17"),
            JapaneseAddress.fromStreet("道頓堀", "1-4-17")
        )
        assertEquals(
            JapaneseAddress.Parts("中央区", "博労町", "2", "4", "11"),
            JapaneseAddress.fromStreet("大阪府大阪市中央区博労町", "2丁目4番11号")
        )
        assertEquals(
            JapaneseAddress.Parts("浪速区", "敷津西", "2", "5", "10"),
            JapaneseAddress.fromStreet("浪速区敷津西", "2-5-10")
        )
        // The chōme in addr:street with the district
        assertEquals(
            JapaneseAddress.Parts("中央区", null, "南船場3丁目", "11", "24"),
            JapaneseAddress.fromStreet("中央区南船場３丁目", "１１−２４")
        )
        assertEquals(
            JapaneseAddress.Parts(null, null, "南桜塚一丁目", "16", "7"),
            JapaneseAddress.fromStreet("南桜塚一丁目", "16-7")
        )
        // Which is then the address it would have been tagged as
        val parts = JapaneseAddress.fromStreet("神戸市中央区北長狭通", "1-31-35")!!
        assertEquals(
            "北長狭通1丁目31-35",
            JapaneseAddress.address(parts.quarter, parts.neighbourhood, parts.blockNumber, parts.houseNumber)
        )
        val key = JapaneseAddress.searchKeys(parts.quarter, parts.neighbourhood, parts.blockNumber, parts.houseNumber)!!.first
        assertTrue(JapaneseAddress.isMatch(JapaneseAddress.searchKey("北長狭通1-31-35")!!, key))
        assertTrue(JapaneseAddress.isMatch(JapaneseAddress.searchKey("神戸市中央区北長狭通一丁目31番35号")!!, key))
    }

    @Test
    fun streetAddressesArentTakenForBlockAddresses() {
        // Plots of land, and a number with no chōme
        assertNull(JapaneseAddress.fromStreet("平岡町新在家", "2214-57"))
        assertNull(JapaneseAddress.fromStreet("桐ケ丘3", "333"))
        assertNull(JapaneseAddress.fromStreet("南武庫之荘５丁目", "１０"))
        // Phone numbers
        assertNull(JapaneseAddress.fromStreet("河原町三条下ル大黒町71京劇会館ビル１F", "075-212-9026"))
        assertNull(JapaneseAddress.fromStreet("奈良県生駒郡三郷町立野南1-29-1", "0745-73-1138"))
        // Roads, and a floor in the housenumber
        assertNull(JapaneseAddress.fromStreet("国道176号線", "1-2-3"))
        assertNull(JapaneseAddress.fromStreet("堀川通 (Horikawa-dori);京都府道38号京都広河原美山線", "1-2-3"))
        assertNull(JapaneseAddress.fromStreet("日本橋町東1丁目", "10-7 1F"))
        assertNull(JapaneseAddress.fromStreet("周防町通り", "1-15-15"))
        assertNull(JapaneseAddress.fromStreet("南堀江立花通（オレンジストリート）", "1-24-7"))
        // But a district can be named after one, as 北長狭通 and 心斎橋筋 are
        assertEquals(
            JapaneseAddress.Parts(null, "心斎橋筋", "2", "8", "20"),
            JapaneseAddress.fromStreet("心斎橋筋", "2丁目8番20号")
        )
        // Streets elsewhere
        assertNull(JapaneseAddress.fromStreet("Kersland Drive", "1-2-3"))
        assertNull(JapaneseAddress.fromStreet("중앙로", "1-2-3"))
        assertNull(JapaneseAddress.fromStreet("中山路", "1-2-3"))
    }
}
