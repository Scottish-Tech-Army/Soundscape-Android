package org.scottishtecharmy.soundscape.i18n

import kotlin.test.Test
import kotlin.test.assertEquals

class GrammarMarkersTest {

    private fun hu(text: String) = resolveGrammarMarkers(text)
    private fun ko(text: String) = resolveGrammarMarkers(text)

    @Test
    fun textWithoutMarkersIsReturnedUnchanged() {
        val text = "Walking north on Main Street (A82)"
        assertEquals(text, resolveGrammarMarkers(text))
        assertEquals("Haladás észak felé az úton", hu("Haladás észak felé az úton"))
    }

    // --- Hungarian -----------------------------------------------------------------------

    @Test
    fun hungarianWordsChooseByFirstLetter() {
        assertEquals("Haladás észak felé a Rákóczi úton", hu("Haladás észak felé a(z) Rákóczi úton"))
        assertEquals("Haladás észak felé az Andrássy úton", hu("Haladás észak felé a(z) Andrássy úton"))
        assertEquals("Járda az Üllői mellett", hu("Járda a(z) Üllői mellett"))
        assertEquals("Járda az őrház mellett", hu("Járda a(z) őrház mellett"))
        assertEquals("Járda a Szent István mellett", hu("Járda a(z) Szent István mellett"))
    }

    @Test
    fun hungarianCapitalisedArticleKeepsItsCase() {
        assertEquals("Az Erzsébet körúton", hu("A(z) Erzsébet körúton"))
        assertEquals("A Váci úton", hu("A(z) Váci úton"))
    }

    @Test
    fun hungarianAbbreviationsAndRoadNumbersGoByLetterName() {
        assertEquals("az M7 úton", hu("a(z) M7 úton"))       // em
        assertEquals("az M0 úton", hu("a(z) M0 úton"))
        assertEquals("az SZTE mellett", hu("a(z) SZTE mellett")) // esz
        assertEquals("az ELTE mellett", hu("a(z) ELTE mellett"))
        assertEquals("a BKV mellett", hu("a(z) BKV mellett"))    // bé
        assertEquals("a K mellett", hu("a(z) K mellett"))        // ká
        assertEquals("az X mellett", hu("a(z) X mellett"))       // iksz
    }

    @Test
    fun hungarianNumbersGoByTheirSpokenForm() {
        assertEquals("az 1-es úton", hu("a(z) 1-es úton"))       // egy
        assertEquals("az 5 közül", hu("a(z) 5 közül"))           // öt
        assertEquals("az 52 közül", hu("a(z) 52 közül"))         // ötvenkettő
        assertEquals("az 500 közül", hu("a(z) 500 közül"))       // ötszáz
        assertEquals("az 1000 közül", hu("a(z) 1000 közül"))     // ezer
        assertEquals("az 1500 közül", hu("a(z) 1500 közül"))     // ezerötszáz
        assertEquals("az 5000 közül", hu("a(z) 5000 közül"))     // ötezer
        assertEquals("az 1000000 közül", hu("a(z) 1000000 közül")) // egymillió
        assertEquals("a 2 közül", hu("a(z) 2 közül"))            // kettő
        assertEquals("a 12 közül", hu("a(z) 12 közül"))          // tizenkettő
        assertEquals("a 15 közül", hu("a(z) 15 közül"))          // tizenöt
        assertEquals("a 100 közül", hu("a(z) 100 közül"))        // száz
        assertEquals("a 10000 közül", hu("a(z) 10000 közül"))    // tízezer
        assertEquals("a 0 közül", hu("a(z) 0 közül"))            // nulla
    }

    @Test
    fun hungarianResolvesEveryMarkerInAString() {
        assertEquals(
            "A Kossuth úton, az Arany János és a Petőfi között",
            hu("A(z) Kossuth úton, a(z) Arany János és a(z) Petőfi között"),
        )
    }

    @Test
    fun hungarianLooksPastOpeningQuotes() {
        assertEquals("az „Oktogon” felé", hu("a(z) „Oktogon” felé"))
        assertEquals("a \"Blaha\" felé", hu("a(z) \"Blaha\" felé"))
    }

    @Test
    fun hungarianWithNothingToFollowFallsBackToA() {
        assertEquals("Járda a , majd", hu("Járda a(z) , majd"))
        assertEquals("Járda a", hu("Járda a(z)"))
    }

    @Test
    fun hungarianMarkerInsideAWordIsLeftAlone() {
        assertEquals("Pizza(z) Bar", hu("Pizza(z) Bar"))
    }

    // --- Korean --------------------------------------------------------------------------

    @Test
    fun koreanParticlesFollowTheFinalConsonant() {
        // 강남역 ends in ㄱ, 서초 in a vowel.
        assertEquals("강남역을 지나는 길", ko("강남역을(를) 지나는 길"))
        assertEquals("서초를 지나는 길", ko("서초을(를) 지나는 길"))
        assertEquals("강남역과 서초를 연결하는 길", ko("강남역과(와) 서초을(를) 연결하는 길"))
        assertEquals("서초와 강남역을 연결하는 길", ko("서초과(와) 강남역을(를) 연결하는 길"))
        assertEquals("강남역이 있습니다", ko("강남역이(가) 있습니다"))
        assertEquals("서초가 있습니다", ko("서초이(가) 있습니다"))
        assertEquals("강남역은", ko("강남역은(는)"))
        assertEquals("서초는", ko("서초은(는)"))
    }

    @Test
    fun koreanReversedMarkerOrderIsResolvedTheSameWay() {
        assertEquals("강남역을", ko("강남역를(을)"))
        assertEquals("서초를", ko("서초를(을)"))
        assertEquals("서초와", ko("서초와(과)"))
    }

    @Test
    fun koreanRoTakesRoAfterAVowelOrRieul() {
        assertEquals("강남역으로 이어지는 길", ko("강남역(으)로 이어지는 길"))
        assertEquals("서초로 이어지는 길", ko("서초(으)로 이어지는 길"))
        assertEquals("서울로 이어지는 길", ko("서울(으)로 이어지는 길"))
        // ㄹ is only special for (으)로.
        assertEquals("서울을 지나는 길", ko("서울을(를) 지나는 길"))
    }

    @Test
    fun koreanCopulaIsDroppedAfterAVowel() {
        assertEquals("강남역이라는 경로를 찾을 수 없습니다", ko("강남역(이)라는 경로를 찾을 수 없습니다"))
        assertEquals("출근길이라는 경로", ko("출근길(이)라는 경로"))
        assertEquals("학교라는 경로를 찾을 수 없습니다", ko("학교(이)라는 경로를 찾을 수 없습니다"))
    }

    @Test
    fun koreanNumbersAreReadInSinoKorean() {
        assertEquals("1번 출구로", ko("1번 출구(으)로"))
        assertEquals("7로", ko("7(으)로"))        // 칠
        assertEquals("3으로", ko("3(으)로"))      // 삼
        assertEquals("2로", ko("2(으)로"))        // 이
        assertEquals("10을", ko("10을(를)"))      // 십
        assertEquals("100을", ko("100을(를)"))    // 백
        assertEquals("5를", ko("5을(를)"))        // 오
        assertEquals("M7을", ko("M7을(를)"))      // 칠
    }

    @Test
    fun koreanLatinTextGoesByLetterNameOrSpelling() {
        assertEquals("KTX를", ko("KTX을(를)"))              // 엑스
        assertEquals("BTL을", ko("BTL을(를)"))              // 엘
        assertEquals("A로", ko("A(으)로"))                 // 에이
        assertEquals("Main Street를", ko("Main Street을(를)")) // 스트리트
        assertEquals("City Hall을", ko("City Hall을(를)"))     // 홀
        assertEquals("City Hall로", ko("City Hall(으)로"))
        assertEquals("Gangnam을", ko("Gangnam을(를)"))         // 강남
        assertEquals("Sinchon을", ko("Sinchon을(를)"))         // 신촌
        assertEquals("Wangsimni를", ko("Wangsimni을(를)"))     // 왕십리
    }

    @Test
    fun koreanLooksPastClosingQuotes() {
        assertEquals("“강남역”을", ko("“강남역”을(를)"))
        assertEquals("'서초'를", ko("'서초'을(를)"))
    }

    @Test
    fun koreanFallsBackToTheUnbracketedForm() {
        assertEquals("江南을", ko("江南을(를)"))
        assertEquals("를 지나는 길", ko("를(을) 지나는 길"))
        assertEquals("로 이어지는 길", ko("(으)로 이어지는 길"))
        assertEquals("라는 경로", ko("(이)라는 경로"))
    }

    @Test
    fun koreanRealTemplatesResolve() {
        assertEquals(
            "서초역을 지나 강남대로로 이어지는 테헤란로",
            ko("서초역을(를) 지나 강남대로(으)로 이어지는 테헤란로"),
        )
        assertEquals(
            "논현로와 강남대로를 연결하는 테헤란로",
            ko("논현로과(와) 강남대로을(를) 연결하는 테헤란로"),
        )
    }
}
