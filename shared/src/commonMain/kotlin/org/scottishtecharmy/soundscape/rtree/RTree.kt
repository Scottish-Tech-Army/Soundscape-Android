package org.scottishtecharmy.soundscape.rtree

import kotlin.math.ceil
import kotlin.math.sqrt

data class Entry<T, out G : Geometry>(val value: T, val geometry: G)

private sealed class QItem<T, G : Geometry>(val dist: Double) : Comparable<QItem<T, G>> {
    class NodeItem<T, G : Geometry>(dist: Double, val node: Node<T, G>) : QItem<T, G>(dist)
    class EntryItem<T, G : Geometry>(dist: Double, val entry: Entry<T, G>) : QItem<T, G>(dist)
    override fun compareTo(other: QItem<T, G>): Int = dist.compareTo(other.dist)
}

private class MinHeap<E : Comparable<E>> {
    private val arr = ArrayList<E>()
    fun isEmpty() = arr.isEmpty()
    fun push(e: E) { arr.add(e); siftUp(arr.size - 1) }
    fun pop(): E {
        val top = arr[0]
        val last = arr.removeAt(arr.size - 1)
        if (arr.isNotEmpty()) { arr[0] = last; siftDown(0) }
        return top
    }
    private fun siftUp(i: Int) {
        var idx = i
        while (idx > 0) {
            val p = (idx - 1) / 2
            if (arr[idx] < arr[p]) { val t = arr[idx]; arr[idx] = arr[p]; arr[p] = t; idx = p } else break
        }
    }
    private fun siftDown(i: Int) {
        var idx = i
        while (true) {
            val l = idx * 2 + 1; val r = l + 1
            var s = idx
            if (l < arr.size && arr[l] < arr[s]) s = l
            if (r < arr.size && arr[r] < arr[s]) s = r
            if (s == idx) return
            val t = arr[idx]; arr[idx] = arr[s]; arr[s] = t; idx = s
        }
    }
}

private sealed interface Node<T, G : Geometry> {
    val mbr: Rectangle
}

private class Leaf<T, G : Geometry>(
    val entries: List<Entry<T, G>>,
    override val mbr: Rectangle
) : Node<T, G>

private class Branch<T, G : Geometry>(
    val children: List<Node<T, G>>,
    override val mbr: Rectangle
) : Node<T, G>

class RTree<T, G : Geometry> private constructor(
    private val root: Node<T, G>?,
    val size: Int
) {
    fun isEmpty(): Boolean = root == null

    fun search(box: Rectangle): Sequence<Entry<T, G>> {
        if (root == null) return emptySequence()
        val out = mutableListOf<Entry<T, G>>()
        searchNode(root, box, out)
        return out.asSequence()
    }

    private fun searchNode(node: Node<T, G>, box: Rectangle, out: MutableList<Entry<T, G>>) {
        if (!node.mbr.intersects(box)) return
        when (node) {
            is Leaf -> for (e in node.entries) if (e.geometry.intersects(box)) out.add(e)
            is Branch -> for (c in node.children) searchNode(c, box, out)
        }
    }

    fun nearest(
        px: Double,
        py: Double,
        maxDistance: Double = Double.POSITIVE_INFINITY,
        maxCount: Int = Int.MAX_VALUE
    ): List<Entry<T, G>> {
        if (root == null || maxCount <= 0) return emptyList()
        val pq = MinHeap<QItem<T, G>>()
        pq.push(QItem.NodeItem(root.mbr.distanceToPoint(px, py), root))
        val results = ArrayList<Entry<T, G>>(minOf(maxCount, 16))
        while (!pq.isEmpty() && results.size < maxCount) {
            val item = pq.pop()
            if (item.dist > maxDistance) break
            when (item) {
                is QItem.NodeItem<T, G> -> when (val n = item.node) {
                    is Leaf -> for (e in n.entries) {
                        val d = e.geometry.distanceToPoint(px, py)
                        if (d <= maxDistance) pq.push(QItem.EntryItem(d, e))
                    }
                    is Branch -> for (c in n.children) {
                        val d = c.mbr.distanceToPoint(px, py)
                        if (d <= maxDistance) pq.push(QItem.NodeItem(d, c))
                    }
                }
                is QItem.EntryItem -> results.add(item.entry)
            }
        }
        return results
    }

    fun entries(): Sequence<Entry<T, G>> {
        if (root == null) return emptySequence()
        val out = mutableListOf<Entry<T, G>>()
        collectAll(root, out)
        return out.asSequence()
    }

    private fun collectAll(node: Node<T, G>, out: MutableList<Entry<T, G>>) {
        when (node) {
            is Leaf -> out.addAll(node.entries)
            is Branch -> for (c in node.children) collectAll(c, out)
        }
    }

    companion object {
        const val DEFAULT_NODE_CAPACITY = 16

        fun <T, G : Geometry> create(
            entries: List<Entry<T, G>>,
            nodeCapacity: Int = DEFAULT_NODE_CAPACITY
        ): RTree<T, G> {
            require(nodeCapacity >= 2)
            if (entries.isEmpty()) return RTree(null, 0)
            val root = buildStr(entries, nodeCapacity)
            return RTree(root, entries.size)
        }

        private fun <T, G : Geometry> buildStr(
            entries: List<Entry<T, G>>,
            capacity: Int
        ): Node<T, G> {
            val leaves = packLeaves(entries, capacity)
            var level: List<Node<T, G>> = leaves
            while (level.size > 1) {
                level = packBranches(level, capacity)
            }
            return level[0]
        }

        private fun <T, G : Geometry> packLeaves(
            entries: List<Entry<T, G>>,
            capacity: Int
        ): List<Leaf<T, G>> {
            val leafCount = ceil(entries.size.toDouble() / capacity).toInt()
            val sliceCount = ceil(sqrt(leafCount.toDouble())).toInt().coerceAtLeast(1)
            val perSlice = sliceCount * capacity

            val sortedByX = entries.sortedBy { it.geometry.mbr().centerX }
            val leaves = mutableListOf<Leaf<T, G>>()
            var i = 0
            while (i < sortedByX.size) {
                val sliceEnd = minOf(i + perSlice, sortedByX.size)
                val slice = sortedByX.subList(i, sliceEnd).sortedBy { it.geometry.mbr().centerY }
                var j = 0
                while (j < slice.size) {
                    val end = minOf(j + capacity, slice.size)
                    val group = slice.subList(j, end)
                    val mbr = Rectangle.of(group.map { it.geometry })
                    leaves.add(Leaf(group.toList(), mbr))
                    j = end
                }
                i = sliceEnd
            }
            return leaves
        }

        private fun <T, G : Geometry> packBranches(
            nodes: List<Node<T, G>>,
            capacity: Int
        ): List<Node<T, G>> {
            val parentCount = ceil(nodes.size.toDouble() / capacity).toInt()
            val sliceCount = ceil(sqrt(parentCount.toDouble())).toInt().coerceAtLeast(1)
            val perSlice = sliceCount * capacity

            val sortedByX = nodes.sortedBy { it.mbr.centerX }
            val parents = mutableListOf<Node<T, G>>()
            var i = 0
            while (i < sortedByX.size) {
                val sliceEnd = minOf(i + perSlice, sortedByX.size)
                val slice = sortedByX.subList(i, sliceEnd).sortedBy { it.mbr.centerY }
                var j = 0
                while (j < slice.size) {
                    val end = minOf(j + capacity, slice.size)
                    val group = slice.subList(j, end)
                    val mbrList = group.map { it.mbr as Geometry }
                    val mbr = Rectangle.of(mbrList)
                    parents.add(Branch(group.toList(), mbr))
                    j = end
                }
                i = sliceEnd
            }
            return parents
        }
    }
}
