package org.scottishtecharmy.soundscape

import com.github.davidmoten.rtree2.RTree as Rtree2
import com.github.davidmoten.rtree2.geometry.Geometries
import com.github.davidmoten.rtree2.internal.EntryDefault
import org.junit.Test
import org.scottishtecharmy.soundscape.rtree.Entry
import org.scottishtecharmy.soundscape.rtree.Geometry
import org.scottishtecharmy.soundscape.rtree.Line
import org.scottishtecharmy.soundscape.rtree.Point
import org.scottishtecharmy.soundscape.rtree.RTree
import org.scottishtecharmy.soundscape.rtree.Rectangle
import kotlin.random.Random
import kotlin.test.assertEquals

class RTreeParityTest {

    private data class Item(val id: Int)

    @Test
    fun boxSearchMatchesRtree2() {
        val rng = Random(42)
        val ours = mutableListOf<Entry<Item, Geometry>>()
        val theirs = mutableListOf<com.github.davidmoten.rtree2.Entry<Item, com.github.davidmoten.rtree2.geometry.Geometry>>()

        repeat(2000) { i ->
            val item = Item(i)
            when (i % 3) {
                0 -> {
                    val x = rng.nextDouble(-10.0, 10.0)
                    val y = rng.nextDouble(-10.0, 10.0)
                    ours.add(Entry(item, Point(x, y)))
                    theirs.add(EntryDefault(item, Geometries.point(x, y)))
                }
                1 -> {
                    val x1 = rng.nextDouble(-10.0, 10.0); val y1 = rng.nextDouble(-10.0, 10.0)
                    val x2 = x1 + rng.nextDouble(-0.5, 0.5); val y2 = y1 + rng.nextDouble(-0.5, 0.5)
                    ours.add(Entry(item, Line(x1, y1, x2, y2)))
                    theirs.add(EntryDefault(item, Geometries.line(x1, y1, x2, y2)))
                }
                else -> {
                    val x = rng.nextDouble(-10.0, 10.0); val y = rng.nextDouble(-10.0, 10.0)
                    val w = rng.nextDouble(0.01, 0.5); val h = rng.nextDouble(0.01, 0.5)
                    ours.add(Entry(item, Rectangle(x, y, x + w, y + h)))
                    theirs.add(EntryDefault(item, Geometries.rectangle(x, y, x + w, y + h)))
                }
            }
        }

        val ourTree = RTree.create(ours)
        val theirTree: Rtree2<Item, com.github.davidmoten.rtree2.geometry.Geometry> =
            Rtree2.create(theirs)

        repeat(50) {
            val qx1 = rng.nextDouble(-10.0, 10.0); val qy1 = rng.nextDouble(-10.0, 10.0)
            val qx2 = qx1 + rng.nextDouble(0.1, 4.0); val qy2 = qy1 + rng.nextDouble(0.1, 4.0)
            val box = Rectangle(qx1, qy1, qx2, qy2)

            val ourIds = ourTree.search(box).map { it.value.id }.toSortedSet()
            val theirIds = theirTree.search(Geometries.rectangle(qx1, qy1, qx2, qy2))
                .map { it.value().id }.toSortedSet()

            assertEquals(theirIds, ourIds, "mismatch for box $box")
        }
    }

    @Test
    fun nearestMatchesRtree2() {
        val rng = Random(7)
        val ours = mutableListOf<Entry<Item, Geometry>>()
        val theirs = mutableListOf<com.github.davidmoten.rtree2.Entry<Item, com.github.davidmoten.rtree2.geometry.Geometry>>()

        repeat(1500) { i ->
            val item = Item(i)
            when (i % 3) {
                0 -> {
                    val x = rng.nextDouble(-10.0, 10.0); val y = rng.nextDouble(-10.0, 10.0)
                    ours.add(Entry(item, Point(x, y)))
                    theirs.add(EntryDefault(item, Geometries.point(x, y)))
                }
                1 -> {
                    val x1 = rng.nextDouble(-10.0, 10.0); val y1 = rng.nextDouble(-10.0, 10.0)
                    val x2 = x1 + rng.nextDouble(-0.5, 0.5); val y2 = y1 + rng.nextDouble(-0.5, 0.5)
                    ours.add(Entry(item, Line(x1, y1, x2, y2)))
                    theirs.add(EntryDefault(item, Geometries.line(x1, y1, x2, y2)))
                }
                else -> {
                    val x = rng.nextDouble(-10.0, 10.0); val y = rng.nextDouble(-10.0, 10.0)
                    val w = rng.nextDouble(0.01, 0.5); val h = rng.nextDouble(0.01, 0.5)
                    ours.add(Entry(item, Rectangle(x, y, x + w, y + h)))
                    theirs.add(EntryDefault(item, Geometries.rectangle(x, y, x + w, y + h)))
                }
            }
        }

        val ourTree = RTree.create(ours)
        val theirTree: Rtree2<Item, com.github.davidmoten.rtree2.geometry.Geometry> =
            Rtree2.create(theirs)

        repeat(30) {
            val qx = rng.nextDouble(-10.0, 10.0); val qy = rng.nextDouble(-10.0, 10.0)
            val dist = rng.nextDouble(0.5, 4.0)
            val k = rng.nextInt(1, 20)

            val ourIds = ourTree.nearest(qx, qy, dist, k).map { it.value.id }.toSortedSet()
            val theirIds = theirTree.nearest(Geometries.point(qx, qy), dist, k)
                .map { it.value().id }.toSortedSet()

            assertEquals(theirIds, ourIds, "nearest mismatch q=($qx,$qy) dist=$dist k=$k")
        }
    }

    @Test
    fun emptyTree() {
        val tree = RTree.create<Item, Geometry>(emptyList())
        assertEquals(0, tree.search(Rectangle(0.0, 0.0, 1.0, 1.0)).count())
    }

    @Test
    fun allEntriesReturned() {
        val entries = (0 until 137).map { Entry(Item(it), Point(it.toDouble(), it.toDouble())) }
        val tree = RTree.create(entries)
        assertEquals(137, tree.entries().count())
    }
}
