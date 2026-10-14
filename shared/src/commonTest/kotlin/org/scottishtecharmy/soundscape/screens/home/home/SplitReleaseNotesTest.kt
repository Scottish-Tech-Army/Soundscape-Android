package org.scottishtecharmy.soundscape.screens.home.home

import kotlin.test.Test
import kotlin.test.assertEquals

class SplitReleaseNotesTest {

    @Test
    fun latinSentences_splitAfterStopAndSpace() {
        assertEquals(
            listOf("This is a major update.", "Travel mode is much improved."),
            splitReleaseNotes("\nThis is a major update.\n\nTravel mode is much improved.\n"),
        )
    }

    @Test
    fun decimalAndVersionNumbers_stayWhole() {
        assertEquals(
            listOf("Version 1.2 is out.", "It needs iOS 16.4."),
            splitReleaseNotes("Version 1.2 is out. It needs iOS 16.4."),
        )
    }

    @Test
    fun ideographicStops_splitWithoutASpace() {
        assertEquals(
            listOf("これは大型アップデートです。", "移動モードが改善されました。"),
            splitReleaseNotes("これは大型アップデートです。移動モードが改善されました。"),
        )
        assertEquals(
            listOf("这是一次重大更新。", "出行模式大幅改进！"),
            splitReleaseNotes("这是一次重大更新。出行模式大幅改进！"),
        )
    }

    @Test
    fun dandaAndUrduStops_split() {
        assertEquals(
            listOf("यह एक बड़ा अपडेट है।", "यात्रा मोड बेहतर है।"),
            splitReleaseNotes("यह एक बड़ा अपडेट है। यात्रा मोड बेहतर है।"),
        )
        assertEquals(
            listOf("یہ ایک بڑی اپ ڈیٹ ہے۔", "سفر موڈ بہتر ہے۔"),
            splitReleaseNotes("یہ ایک بڑی اپ ڈیٹ ہے۔ سفر موڈ بہتر ہے۔"),
        )
    }

    @Test
    fun unpunctuatedThai_splitsAtParagraphBreaks() {
        assertEquals(
            listOf("นี่คือการอัปเดตครั้งใหญ่ของ Soundscape", "โหมดสลีปจะปลุกตัวเอง"),
            splitReleaseNotes("\nนี่คือการอัปเดตครั้งใหญ่ของ Soundscape\n\nโหมดสลีปจะปลุกตัวเอง\n"),
        )
    }
}
