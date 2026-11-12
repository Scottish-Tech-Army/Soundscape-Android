package org.scottishtecharmy.soundscape.rtree

/**
 * Counts of the geometry searches done by the geo engine, for comparing what a change costs. The
 * GPX replay (MvtTileTest.testCallouts) writes them to build/test-output/perf-counters.txt; replay
 * the same GPX before and after a change and compare the two files. Counts rather than times, so
 * that they are the same on every run and every machine.
 *
 * Split by whether a grid is being built ([gridBuild]), which happens each time the user moves far
 * enough to need new tiles, or the work is being done for each location update.
 *
 * Plain increments, not atomics: in the app a grid build and a location update can overlap and lose
 * the odd count, which doesn't matter for what these are for, and an increment is all they cost.
 */
object PerfCounters {
    /** Set while a grid is being built, so that its searches are counted separately. */
    var gridBuild = false

    class Counts {
        var boxSearches = 0L
        var boxSearchNodesVisited = 0L
        var nearestSearches = 0L
        var nearestHeapPops = 0L
        var dijkstraRuns = 0L
        var dijkstraNodesPopped = 0L

        fun lines(prefix: String) = listOf(
            "$prefix box searches $boxSearches",
            "$prefix box search nodes visited $boxSearchNodesVisited",
            "$prefix nearest searches $nearestSearches",
            "$prefix nearest heap pops $nearestHeapPops",
            "$prefix dijkstra runs $dijkstraRuns",
            "$prefix dijkstra nodes popped $dijkstraNodesPopped",
        )
    }

    var gridBuilds = 0L
        private set
    var perFix = Counts()
        private set
    var grid = Counts()
        private set

    private val current get() = if (gridBuild) grid else perFix

    fun startGridBuild() {
        gridBuilds++
        gridBuild = true
    }

    fun endGridBuild() {
        gridBuild = false
    }

    fun searchCall() { current.boxSearches++ }
    fun searchNode() { current.boxSearchNodesVisited++ }
    fun nearestCall() { current.nearestSearches++ }
    fun nearestPop() { current.nearestHeapPops++ }
    fun dijkstraCall() { current.dijkstraRuns++ }
    fun dijkstraPop() { current.dijkstraNodesPopped++ }

    fun reset() {
        gridBuild = false
        gridBuilds = 0
        perFix = Counts()
        grid = Counts()
    }

    fun report(): String =
        (listOf("grid builds $gridBuilds") + perFix.lines("per-fix") + grid.lines("grid-build"))
            .joinToString("\n", postfix = "\n")
}
