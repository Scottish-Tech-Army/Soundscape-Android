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
}
