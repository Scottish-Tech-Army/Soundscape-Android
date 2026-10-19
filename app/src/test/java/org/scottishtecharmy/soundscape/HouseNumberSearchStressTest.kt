package org.scottishtecharmy.soundscape

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.experimental.categories.Category
import org.scottishtecharmy.soundscape.geoengine.GRID_SIZE
import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.OfflineGeocoder
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.TileSearch
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.utils.process
import java.lang.management.ManagementFactory
import javax.management.ObjectName
import kotlin.test.assertTrue
import kotlin.time.measureTime

// Minutes long, so only run nightly
@Category(NightlyOnlyTest::class)
class HouseNumberSearchStressTest {

    private class Searcher(val location: LngLatAlt) {
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, GRID_SIZE)
        val settlementState = getGridStateForLocation(location, 12, 3)
        val tileSearch = TileSearch(offlineExtractPath, gridState, settlementState)
        val geocoder = OfflineGeocoder(
            gridState,
            settlementState,
            tileSearch,
            processor = { it.process() }
        )

        /** How many tiles, and strings in all, TileSearch's string cache holds. */
        fun cacheSize() = Pair(tileSearch.stringCache.size, tileSearch.stringCache.values.sumOf { it.size })

        fun search(text: String) = runBlocking {
            geocoder.getAddressFromLocationName(text, location, null)
        }
    }

    // Craigdhu Road is outside this grid, so house numbers on it go through locateHouseNumber
    private val milngavie = LngLatAlt(-4.3060165, 55.9475021)

    private fun histogram(): Map<String, Pair<Long, Long>> {
        val server = ManagementFactory.getPlatformMBeanServer()
        val text = server.invoke(
            ObjectName("com.sun.management:type=DiagnosticCommand"),
            "gcClassHistogram",
            arrayOf<Any>(arrayOf<String>()),
            arrayOf(Array<String>::class.java.name)
        ) as String
        val result = mutableMapOf<String, Pair<Long, Long>>()
        val regex = Regex("""^\s*\d+:\s+(\d+)\s+(\d+)\s+(\S+)""")
        for (line in text.lines()) {
            val match = regex.find(line) ?: continue
            val (instances, bytes, name) = match.destructured
            result[name] = Pair(instances.toLong(), bytes.toLong())
        }
        return result
    }

    private val watched = listOf(
        "org.scottishtecharmy.soundscape.geoengine.GridState",
        "org.scottishtecharmy.soundscape.FileGridState",
        "org.scottishtecharmy.soundscape.geoengine.utils.geocoders.StreetDescription",
        "org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree",
        "org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way",
        "org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection",
        "org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature",
        "vector_tile.Tile",
        "vector_tile.Tile\$Feature",
    )

    private fun snapshot(label: String): Map<String, Long> {
        // gcClassHistogram does a full GC first, so only live objects are counted
        val histogram = histogram()
        val heap = ManagementFactory.getMemoryMXBean().heapMemoryUsage.used
        val threads = Thread.getAllStackTraces().keys
        val treeThreads = threads.count { it.name.startsWith("TreeContext") }
        println("=== $label: heap ${heap / 1_000_000}MB, threads ${threads.size}, TreeContext threads $treeThreads")
        val counts = mutableMapOf<String, Long>()
        for (name in watched) {
            val (instances, bytes) = histogram[name] ?: Pair(0L, 0L)
            counts[name] = instances
            println("    ${name.substringAfterLast('.')}: $instances (${bytes / 1000}kB)")
        }
        counts["heap"] = heap
        counts["threads"] = threads.size.toLong()
        counts["treeThreads"] = treeThreads.toLong()
        return counts
    }

    @Test
    fun hostileQueriesDontThrow() {
        val glasgow = Searcher(milngavie)
        val osaka = Searcher(osakaTestLocation)
        val streets = listOf("craigdhu road", "craigdhu road milngavie", "dirleton gate")
        val numbers = listOf(
            "0", "1", "2", "56", "112", "113", "999", "99999", "2147483647", "2147483648",
            "99999999999999999999", "112a", "112A", "112-114", "112/114", "-", "--", "-112", "112-",
            "1-", "１１２", "５６", "56ー", "ー56", "1.5", "12½", "١١٢", "۱۱۲", "0000056", "56 58",
        )
        val queries = mutableListOf<String>()
        for (street in streets) for (number in numbers) {
            queries.add("$number $street")
            queries.add("$street $number")
        }
        queries += listOf(
            "", " ", "　", "1", "112", "-", "ー", "112 112", "112　craigdhu　road",
            "craigdhu", "road 112", "a".repeat(500) + " 112", "112 " + "craigdhu road ".repeat(50),
            "😀 112", "112 😀", "craigdhu road 112 😀",
            "近鉄今里駅前通 4-11-12", "4-11-12 近鉄今里駅前通", "近鉄今里駅前通4-11-12",
            "4-11-12近鉄今里駅前通", "近鉄今里駅前通４－１１－１２", "近鉄今里駅前通4ー11ー12",
            "近鉄今里駅前通 4-11-13", "近鉄今里駅前通 5", "近鉄今里駅前通 1", "近鉄今里駅前通 -",
            "七松線 29", "七松線29", "七松線 30", "29七松線", "道頓堀 1-4-17", "道頓堀1-4-17",
            "道頓堀 99999999999", "西難波町6-4-29", "4-", "-4", "4--11", "ー", "－", "１",
            "서울 12", "ถนน 12", "ถนน12", "12ถนน", "12-", "-12-",
        )

        var worst = 0L
        var worstQuery = ""
        val failures = mutableListOf<String>()
        for (searcher in listOf(glasgow, osaka)) {
            for (query in queries) {
                val time = measureTime {
                    try {
                        val results = searcher.search(query)
                        println("'$query' -> ${results?.size} ${results?.firstOrNull()?.name}")
                    } catch (e: Throwable) {
                        failures.add("'$query' at ${searcher.location}: $e")
                        e.printStackTrace()
                    }
                }
                if (time.inWholeMilliseconds > worst) {
                    worst = time.inWholeMilliseconds
                    worstQuery = query
                }
            }
        }
        println("Slowest: '$worstQuery' ${worst}ms")
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun repeatedSearchesBeyondTheGridDontLeak() {
        val searcher = Searcher(milngavie)
        val osaka = Searcher(osakaTestLocation)
        val queries = listOf(
            // Placed between two houses: builds a StreetDescription on a GridState of its own
            "56 craigdhu road milngavie",
            // A house that's in the housenumber layer
            "112 craigdhu road milngavie",
            // No bracket
            "999 craigdhu road milngavie",
            "20 dirleton gate",
        )
        val osakaQueries = listOf("近鉄今里駅前通 4-11-12", "七松線 29", "道頓堀 1-4-17")

        fun round() {
            for (q in queries) searcher.search(q)
            for (q in osakaQueries) osaka.search(q)
        }

        // Warm up so caches (stringCache etc) are full before measuring
        repeat(3) { round() }
        val baseline = snapshot("baseline")
        val allocation = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        val threadId = Thread.currentThread().id

        val rounds = 60
        var after: Map<String, Long> = baseline
        for (i in 1..rounds) {
            val before = allocation.getThreadAllocatedBytes(threadId)
            val time = measureTime { round() }
            val allocated = allocation.getThreadAllocatedBytes(threadId) - before
            if (i % 20 == 0) {
                println("round $i: $time, ${allocated / 1_000_000}MB allocated on this thread")
                after = snapshot("after $i rounds")
            }
        }

        // Nothing should keep growing with the number of searches
        val problems = mutableListOf<String>()
        for (name in watched + listOf("threads", "treeThreads")) {
            val b = baseline[name] ?: 0
            val a = after[name] ?: 0
            if (a > b + maxOf(2, b / 100)) problems.add("$name grew from $b to $a")
        }
        val heapGrowth = (after["heap"] ?: 0) - (baseline["heap"] ?: 0)
        println("Heap growth over $rounds rounds: ${heapGrowth / 1_000_000}MB")
        if (heapGrowth > 20_000_000) problems.add("heap grew by ${heapGrowth / 1_000_000}MB")
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    @Test
    fun uniqueSearchesOneAfterAnotherDontPileUp() {
        val glasgow = Searcher(milngavie)
        val osaka = Searcher(osakaTestLocation)
        glasgow.search("craigdhu road")
        osaka.search("道頓堀")

        // Real names from the tiles searched, each searched once with a number of its own, so
        // that every search is new and many reach locateHouseNumber with a different street
        fun names(searcher: Searcher) = searcher.tileSearch.stringCache.values.flatten()
            .distinct().filter { it.length > 3 }.shuffled(kotlin.random.Random(1))
        val glasgowNames = names(glasgow)
        val osakaNames = names(osaka)
        println("${glasgowNames.size} Glasgow names, ${osakaNames.size} Osaka names")
        val random = kotlin.random.Random(2)
        var serial = 0
        fun nextQuery(i: Int): Pair<Searcher, String> {
            serial++
            return when (i % 6) {
                0 -> Pair(glasgow, "$serial craigdhu road milngavie")
                1 -> Pair(glasgow, "${random.nextInt(1, 400)} ${glasgowNames[serial % glasgowNames.size]} ")
                2 -> Pair(glasgow, (1..12).map { 'a' + random.nextInt(26) }.joinToString("") + " $serial")
                3 -> Pair(osaka, "${osakaNames[serial % osakaNames.size]} ${random.nextInt(1, 30)}-${random.nextInt(1, 30)}")
                // A Japanese block address, which is all that fills TileSearch's addressCache
                4 -> Pair(osaka, "西難波町${random.nextInt(1, 9)}-${random.nextInt(1, 20)}-$serial")
                else -> Pair(osaka, "${osakaNames[serial % osakaNames.size]}$serial")
            }
        }

        val total = 600
        val checkpoints = mutableListOf<Map<String, Long>>()
        for (i in 1..total) {
            val (searcher, query) = nextQuery(i)
            searcher.search(query)
            if (i % 150 == 0) {
                val counts = snapshot("after $i unique searches").toMutableMap()
                val (gTiles, gStrings) = glasgow.cacheSize()
                val (oTiles, oStrings) = osaka.cacheSize()
                println("    stringCache: Glasgow $gTiles tiles/$gStrings strings, Osaka $oTiles tiles/$oStrings strings")
                counts["cacheStrings"] = (gStrings + oStrings).toLong()
                checkpoints.add(counts)
            }
        }

        // Compared from the first checkpoint, once every kind of search has run many times
        val first = checkpoints.first()
        val last = checkpoints.last()
        val problems = mutableListOf<String>()
        for (name in watched + listOf("threads", "treeThreads", "cacheStrings")) {
            val b = first[name] ?: 0
            val a = last[name] ?: 0
            if (a > b + maxOf(2, b / 100)) problems.add("$name grew from $b to $a")
        }
        val heapGrowth = (last["heap"] ?: 0) - (first["heap"] ?: 0)
        println("Heap growth over ${total - 150} unique searches: ${heapGrowth / 1_000_000}MB")
        if (heapGrowth > 20_000_000) problems.add("heap grew by ${heapGrowth / 1_000_000}MB")
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }
}
