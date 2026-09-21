package org.scottishtecharmy.soundscape.utils

/**
 * Minimal binary min-heap priority queue that mirrors the subset of
 * `java.util.PriorityQueue` we rely on, so callers can run on KMP targets.
 */
class PriorityQueue<E>(private val comparator: Comparator<in E>) {
    private val arr = ArrayList<E>()

    val size: Int get() = arr.size

    fun isEmpty(): Boolean = arr.isEmpty()
    fun isNotEmpty(): Boolean = arr.isNotEmpty()

    fun add(element: E) {
        arr.add(element)
        siftUp(arr.size - 1)
    }

    fun poll(): E? {
        if (arr.isEmpty()) return null
        val top = arr[0]
        val last = arr.removeAt(arr.size - 1)
        if (arr.isNotEmpty()) {
            arr[0] = last
            siftDown(0)
        }
        return top
    }

    private fun siftUp(index: Int) {
        var i = index
        while (i > 0) {
            val parent = (i - 1) / 2
            if (comparator.compare(arr[i], arr[parent]) < 0) {
                swap(i, parent)
                i = parent
            } else break
        }
    }

    private fun siftDown(index: Int) {
        var i = index
        while (true) {
            val l = i * 2 + 1
            val r = l + 1
            var smallest = i
            if (l < arr.size && comparator.compare(arr[l], arr[smallest]) < 0) smallest = l
            if (r < arr.size && comparator.compare(arr[r], arr[smallest]) < 0) smallest = r
            if (smallest == i) return
            swap(i, smallest)
            i = smallest
        }
    }

    private fun swap(a: Int, b: Int) {
        val t = arr[a]; arr[a] = arr[b]; arr[b] = t
    }
}
