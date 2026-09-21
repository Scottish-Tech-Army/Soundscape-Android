package org.scottishtecharmy.soundscape.clipper

/**
 * Binary min-heap of pending sweep events, ordered by [compareEvents].
 *
 * Deliberately concrete rather than generic: this sits in the innermost loop of the sweep and
 * runs on Kotlin/Native as well as the JVM, where a generic heap over a boxed Comparable costs
 * an allocation per comparison. RTree.kt has its own private MinHeap for the same reason - the
 * duplication is intentional, since that one is generic, private to another package, and
 * ordered by natural ordering rather than an external comparator.
 */
internal class EventHeap {
    private val items = ArrayList<SweepEvent>()

    val size: Int get() = items.size

    fun isEmpty(): Boolean = items.isEmpty()

    fun push(event: SweepEvent) {
        items.add(event)
        var child = items.size - 1
        while (child > 0) {
            val parent = (child - 1) / 2
            if (compareEvents(items[child], items[parent]) >= 0) break
            swap(child, parent)
            child = parent
        }
    }

    fun pop(): SweepEvent {
        val top = items[0]
        val last = items.removeAt(items.size - 1)
        if (items.isNotEmpty()) {
            items[0] = last
            var parent = 0
            while (true) {
                val left = 2 * parent + 1
                if (left >= items.size) break
                val right = left + 1
                var smallest = left
                if (right < items.size && compareEvents(items[right], items[left]) < 0) {
                    smallest = right
                }
                if (compareEvents(items[smallest], items[parent]) >= 0) break
                swap(smallest, parent)
                parent = smallest
            }
        }
        return top
    }

    private fun swap(a: Int, b: Int) {
        val tmp = items[a]
        items[a] = items[b]
        items[b] = tmp
    }
}

/**
 * The sweep status: the edges currently straddling the sweep line, ordered bottom to top by
 * [compareSegments].
 *
 * A sorted array rather than the balanced tree the reference implementations use. That is
 * sound because edges in the status never cross - every crossing has already been split out
 * by the time both edges are present - so [compareSegments] is a consistent total order over
 * the contents and binary search finds the right slot. It costs an O(n) array move per
 * insertion, but n here is the number of edges crossing one vertical line through a single POI
 * or building polygon: tens, occasionally a few hundred for a large park. Forty lines that can
 * be read and checked beat two hundred and fifty that can't.
 *
 * If a pathological polygon ever makes this the bottleneck, replacing the backing list with a
 * balanced structure is a change local to this class.
 */
internal class SweepStatus {
    private val segments = ArrayList<SweepEvent>()

    val size: Int get() = segments.size

    /** Insert [event] in order and return the index it landed at. */
    fun insert(event: SweepEvent): Int {
        var low = 0
        var high = segments.size
        while (low < high) {
            val mid = (low + high) / 2
            if (compareSegments(segments[mid], event) < 0) low = mid + 1 else high = mid
        }
        segments.add(low, event)
        return low
    }

    /**
     * Index of [event] by identity, or -1. A linear scan rather than a binary search: dividing
     * a segment rewrites the `otherEvent` of edges already in the status, which can leave the
     * ordering momentarily stale, and a binary search over a stale order can miss an element
     * that is definitely present. Insertion already costs O(n) for the array move, so scanning
     * costs nothing asymptotically and cannot fail.
     */
    fun indexOf(event: SweepEvent): Int {
        for (i in segments.indices) {
            if (segments[i] === event) return i
        }
        return -1
    }

    fun removeAt(index: Int) {
        segments.removeAt(index)
    }

    /** The edge at [index], or null when the index falls outside the status. */
    fun at(index: Int): SweepEvent? = segments.getOrNull(index)
}
