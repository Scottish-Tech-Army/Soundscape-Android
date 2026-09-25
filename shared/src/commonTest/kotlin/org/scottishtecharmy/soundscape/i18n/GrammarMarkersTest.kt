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

    // Road names below are real ones from the Budapest extract.
    @Test
    fun hungarianRoadCaseInflectsTheNamesOwnStreetWord() {
        assertEquals("Gyaloglás észak felé az Andrássy úton", hu("Gyaloglás észak felé a(z) Andrássy út{úton}"))
        assertEquals("Gyaloglás észak felé a Váci utcán", hu("Gyaloglás észak felé a(z) Váci utca{úton}"))
        assertEquals("A Deák Ferenc téren", hu("A(z) Deák Ferenc tér{úton}"))
        assertEquals("A Hősök terén", hu("A(z) Hősök tere{úton}"))
        assertEquals("Az Erzsébet körúton", hu("A(z) Erzsébet körút{úton}"))
        assertEquals("Az Árpád fejedelem útján", hu("A(z) Árpád fejedelem útja{úton}"))
        assertEquals("A Budai alsó rakparton", hu("A(z) Budai alsó rakpart{úton}"))
        assertEquals("A Liszt Ferenc sétányon", hu("A(z) Liszt Ferenc sétány{úton}"))
        assertEquals("A Kazinczy közön", hu("A(z) Kazinczy köz{úton}"))
        assertEquals("A Margit hídon", hu("A(z) Margit híd{úton}"))
    }

    @Test
    fun hungarianRoadCaseMatchesCompoundsLongestFirst() {
        assertEquals("A Nagykörúton", hu("A(z) Nagykörút{úton}"))
        assertEquals("A Bajcsy-Zsilinszky úton", hu("A(z) Bajcsy-Zsilinszky út{úton}"))
        assertEquals("A Duna-korzón", hu("A(z) Duna-korzó{úton}"))
        assertEquals("A Pincesoron", hu("A(z) Pincesor{úton}"))
        assertEquals("A Dunakeszi alagútban", hu("A(z) Dunakeszi alagút{úton}")) // not «alagúton»
        assertEquals("A Városligeti fasoron", hu("A(z) Városligeti fasor{úton}"))
    }

    @Test
    fun hungarianRoadCaseCoversUnnamedWayClassNames() {
        assertEquals("Az Ösvényen", hu("A(z) Ösvény{úton}"))
        assertEquals("Az Úton", hu("A(z) Út{úton}"))
        assertEquals("A Gyalogúton", hu("A(z) Gyalogút{úton}"))
        assertEquals("Az Autópályán", hu("A(z) Autópálya{úton}"))
        assertEquals("A Főútvonalon", hu("A(z) Főútvonal{úton}"))
        assertEquals("A Lépcsőn", hu("A(z) Lépcső{úton}"))
    }

    @Test
    fun hungarianRoadCaseFallsBackToUtonForOtherNames() {
        assertEquals("Az M7 úton", hu("A(z) M7{úton}"))
        assertEquals("A Rudas fürdő úton", hu("A(z) Rudas fürdő{úton}"))
        assertEquals("Az M1 (Bécsi út) úton", hu("A(z) M1 (Bécsi út){úton}"))
    }

    @Test
    fun hungarianRoadCaseWorksInRealTemplates() {
        assertEquals(
            "A Kossuth Lajos utcán, az Astoria és a Ferenciek tere között",
            hu("A(z) Kossuth Lajos utca{úton}, a(z) Astoria és a(z) Ferenciek tere között"),
        )
        assertEquals("Nem mozogsz a Rákóczi úton", hu("Nem mozogsz a(z) Rákóczi út{úton}"))
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

    // --- Turkish -------------------------------------------------------------------------

    private fun tr(text: String) = resolveGrammarMarkers(text)

    @Test
    fun turkishLocativeFollowsHarmonyAndHardening() {
        assertEquals("İstanbul'da", tr("İstanbul'{DA}"))
        assertEquals("Kadıköy'de", tr("Kadıköy'{DA}"))
        assertEquals("Park'ta", tr("Park'{DA}"))
        assertEquals("Beşiktaş'ta", tr("Beşiktaş'{DA}"))
        assertEquals("Üsküdar'dan", tr("Üsküdar'{DAn}"))
        assertEquals("Bebek'ten", tr("Bebek'{DAn}"))
    }

    @Test
    fun turkishVowelFinalNamesTakeABufferConsonant() {
        assertEquals("Ankara'ya", tr("Ankara'{A}"))
        assertEquals("İzmir'e", tr("İzmir'{A}"))
        assertEquals("Bursa'yı", tr("Bursa'{I}"))
        assertEquals("Ankara'nın", tr("Ankara'{In}"))
        assertEquals("İzmir'in", tr("İzmir'{In}"))
        assertEquals("Konya'da", tr("Konya'{DA}"))
    }

    @Test
    fun turkishDottedAndDotlessIAreKeptApart() {
        assertEquals("Isparta'ya", tr("Isparta'{A}"))
        assertEquals("Iğdır'ın", tr("Iğdır'{In}"))
        assertEquals("İnebolu'nun", tr("İnebolu'{In}"))
    }

    @Test
    fun turkishPossessivePlaceNamesTakeTheExtraN() {
        assertEquals("Atatürk Caddesi'nde", tr("Atatürk Caddesi'{DA}"))
        assertEquals("Bağdat Caddesi'ne", tr("Bağdat Caddesi'{A}"))
        assertEquals("Moda Parkı'ndan", tr("Moda Parkı'{DAn}"))
        assertEquals("Kadıköy Mahallesi'nin", tr("Kadıköy Mahallesi'{In}"))
        assertEquals("İstiklal Sokağı'nı", tr("İstiklal Sokağı'{I}"))
        assertEquals("Galata Köprüsü'nde", tr("Galata Köprüsü'{DA}"))
        assertEquals("İstanbul Havalimanı'na", tr("İstanbul Havalimanı'{A}"))
        assertEquals("Taksim Meydanı'nda", tr("Taksim Meydanı'{DA}"))
    }

    @Test
    fun turkishStreetAbbreviationsAreReadInFull() {
        assertEquals("Bağdat Cd.'nde", tr("Bağdat Cd.'{DA}"))
        assertEquals("Moda Sk.'na", tr("Moda Sk.'{A}"))
    }

    @Test
    fun turkishNumbersGoByTheirLastSpokenWord() {
        assertEquals("saat 3'te", tr("saat 3'{DA}"))   // üç
        assertEquals("saat 4'te", tr("saat 4'{DA}"))   // dört
        assertEquals("saat 5'te", tr("saat 5'{DA}"))   // beş
        assertEquals("saat 6'da", tr("saat 6'{DA}"))   // altı
        assertEquals("saat 9'da", tr("saat 9'{DA}"))   // dokuz
        assertEquals("saat 10'da", tr("saat 10'{DA}")) // on
        assertEquals("saat 12'de", tr("saat 12'{DA}")) // on iki
        assertEquals("40'ta", tr("40'{DA}"))           // kırk
        assertEquals("60'ta", tr("60'{DA}"))           // altmış
        assertEquals("100'de", tr("100'{DA}"))         // yüz
        assertEquals("2000'de", tr("2000'{DA}"))       // iki bin
        assertEquals("0'da", tr("0'{DA}"))             // sıfır
        assertEquals("3'ün", tr("3'{In}"))
        assertEquals("5'in", tr("5'{In}"))
        assertEquals("2'nin", tr("2'{In}"))
        assertEquals("6'nın", tr("6'{In}"))
        assertEquals("10'un", tr("10'{In}"))
        assertEquals("D100'e", tr("D100'{A}"))
        assertEquals("E5'te", tr("E5'{DA}"))
    }

    @Test
    fun turkishAbbreviationsGoByLetterName() {
        assertEquals("TRT'ye", tr("TRT'{A}"))  // te
        assertEquals("ABD'de", tr("ABD'{DA}")) // de
        assertEquals("THY'nin", tr("THY'{In}")) // ye
        assertEquals("AVM'den", tr("AVM'{DAn}"))
    }

    @Test
    fun turkishRealTemplatesResolve() {
        assertEquals(
            "Moda Caddesi üzerinden Kadıköy İskelesi'nden Bağdat Caddesi'ne",
            tr("Moda Caddesi üzerinden Kadıköy İskelesi'{DAn} Bağdat Caddesi'{A}"),
        )
        assertEquals(
            "İş rotası: 5'in 2. ara noktasında",
            tr("İş rotası: 5'{In} 2. ara noktasında"),
        )
        assertEquals("Kadıköy'den uzaklaşıyorsunuz", tr("Kadıköy'{DAn} uzaklaşıyorsunuz"))
    }

    @Test
    fun turkishCurlyApostropheIsKept() {
        assertEquals("Ankara’ya", tr("Ankara’{A}"))
    }

    @Test
    fun turkishFallsBackToTheFrontForm() {
        assertEquals("★'de", tr("★'{DA}"))
        assertEquals("'e yakın", tr("'{A} yakın"))
    }

    // --- Finnish (names from the Helsinki extract) ------------------------------------------

    private fun fi(text: String) = resolveGrammarMarkers(text)

    @Test
    fun finnishRoadNameTakesTheAdessive() {
        assertEquals("Mannerheimintiellä", fi("{Tiellä Mannerheimintie}"))
        assertEquals("Aleksanterinkadulla, välillä A ja B", fi("{Kadulla Aleksanterinkatu}, välillä A ja B"))
        assertEquals("Matkalla pohjoiseen Hämeentiellä", fi("Matkalla pohjoiseen {tiellä Hämeentie}"))
        assertEquals("Kauppatorilla", fi("{Tiellä Kauppatori}"))
        assertEquals("Kalliomäellä", fi("{Tiellä Kalliomäki}"))
        assertEquals("Kirkkopolulla", fi("{Tiellä Kirkkopolku}"))
        assertEquals("Pitkänsillalla", fi("{Tiellä Pitkänsilta}"))
        assertEquals("Pohjoisesplanadilla", fi("{Kadulla Pohjoisesplanadi}"))
        assertEquals("Muurlan kuntoradalla", fi("{Tiellä Muurlan kuntorata}"))
    }

    @Test
    fun finnishLeadingAdjectiveDeclinesTooButGenitivesDoNot() {
        assertEquals("Vanhalla Vihdintiellä", fi("{Tiellä Vanha Vihdintie}"))
        assertEquals("Itäisellä Harjutiellä", fi("{Tiellä Itäinen Harjutie}"))
        assertEquals("Toisella linjalla", fi("{Kadulla Toinen linja}"))
        assertEquals("Ali-Seppälän tiellä", fi("{Tiellä Ali-Seppälän tie}"))
        assertEquals("Toivo Kuulan polulla", fi("{Tiellä Toivo Kuulan polku}"))
    }

    @Test
    fun finnishUnknownNamesKeepTheLabel() {
        assertEquals("Tiellä Almas väg", fi("{Tiellä Almas väg}"))
        assertEquals("Tiellä Etelä-Pohjoinen 2", fi("{Tiellä Etelä-Pohjoinen 2}"))
        assertEquals("matkalla tiellä E18", fi("matkalla {tiellä E18}"))
    }

    @Test
    fun finnishMarkerLeavesOtherBracesAlone() {
        assertEquals("{Jotain muuta}", fi("{Jotain muuta}"))
    }

    // --- Estonian (names from the Tallinn extract) ------------------------------------------

    private fun et(text: String) = resolveGrammarMarkers(text)

    @Test
    fun estonianStreetWordTakesTheAdessive() {
        assertEquals("Pärnu maanteel ja Kesklinna lähedal", et("{Teel Pärnu maantee} ja Kesklinna lähedal"))
        assertEquals("Järvevana teel", et("{Teel Järvevana tee}"))
        assertEquals("Kalda põigul, A ja B vahel", et("{Tänaval Kalda põik}, A ja B vahel"))
        assertEquals("Ei liigu Kadrioru kivisillal", et("Ei liigu {teel Kadrioru kivisild}"))
        assertEquals("Pirita kergliiklusteel", et("{Teel Pirita kergliiklustee}"))
        assertEquals("Nõmme terviserajal", et("{Teel Nõmme terviserada}"))
        assertEquals("Vabaduse väljakul", et("{Tänaval Vabaduse väljak}"))
    }

    @Test
    fun estonianAbbreviationsAreReadInFull() {
        assertEquals("Pärnu maanteel", et("{Teel Pärnu mnt}"))
        assertEquals("Kadrioru puiesteel", et("{Tänaval Kadrioru pst}"))
    }

    @Test
    fun estonianSingleWordNamesAreTanavStreets() {
        assertEquals("Metsa tänaval", et("{Tänaval Metsa}"))
        assertEquals("Ei liigu Tisleri tänaval", et("Ei liigu {teel Tisleri}"))
        assertEquals("Kahala-Hirvli tänaval", et("{Teel Kahala-Hirvli}"))
    }

    @Test
    fun estonianUnnamedWayClassKeepsItsCapital() {
        assertEquals("Rajal", et("{Teel Rada}"))
    }

    @Test
    fun estonianOtherNamesKeepTheLabel() {
        assertEquals("Teel Tallinn–Tartu 2", et("{Teel Tallinn–Tartu 2}"))
        assertEquals("Tänaval E20", et("{Tänaval E20}"))
    }
}
