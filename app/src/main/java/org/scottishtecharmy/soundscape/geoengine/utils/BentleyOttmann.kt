package org.scottishtecharmy.soundscape.geoengine.utils

import java.util.NavigableSet
import java.util.PriorityQueue
import java.util.TreeSet

// The Bentley–Ottmann algorithm is a sweep line algorithm for listing all crossings in a set of
// line segments, i.e. it finds the intersection points of line segments
// https://en.wikipedia.org/wiki/Bentley%E2%80%93Ottmann_algorithm
// However there are some situations where this will fail so we'll need to catch those:
//  assumes that line segments are not vertical,
//  that line segment endpoints do not lie on other line segments,
//  that crossings are formed by only two line segments,
//  and that no two event points have the same x-coordinate
// https://en.wikipedia.org/wiki/Bentley%E2%80%93Ottmann_algorithm#Special_position
class BentleyOttmann(
    inputData: ArrayList<BoSegment>
) {
    private val Q: PriorityQueue<BoEvent>
    private val T: NavigableSet<BoSegment>
    private val X: ArrayList<BoPoint>

    init {
        this.Q = PriorityQueue(EventComparator())
        this.T = TreeSet(SegmentComparator())
        this.X = ArrayList()
        for (s in inputData) {
            this.Q.add(BoEvent(s.first(), s, 0))
            this.Q.add(BoEvent(s.second(), s, 1))
        }
    }

    fun findIntersections() {
        while (!this.Q.isEmpty()) {
            val e = this.Q.poll()
            val L = e?.get_Value()
            if (e != null) {
                when (e.getType()) {
                    0 -> {
                        for (s in e.getSegments()) {
                            if (L != null) {
                                this.recalculate(L)
                            }
                            this.T.add(s)
                            val r = this.T.lower(s)
                            if (r != null) {
                                if (L != null) {
                                    this.reportIntersection(r, s, L)
                                }
                            }
                            val t = this.T.higher(s)
                            if (t != null) {
                                if (L != null) {
                                    this.reportIntersection(t, s, L)
                                }
                            }
                            val r2 = this.T.lower(s)
                            val t2 = this.T.higher(s)
                            if (r2 != null && t2 != null) {
                                this.removeFuture(r2, t2)
                            }
                        }
                    }

                    1 -> {
                        for (s in e.getSegments()) {
                            val r = this.T.lower(s)
                            val t = this.T.higher(s)
                            if (r != null && t !=null) {
                                if (L != null) {
                                    this.reportIntersection(r, t, L)
                                }
                            }
                            this.T.remove(s)
                        }
                    }

                    2 -> {
                        val s1 = e.getSegments()[0]
                        val s2 = e.getSegments()[1]
                        this.swap(s1, s2)
                        if (s1.get_Value() < s2.get_Value()) {
                            val t = this.T.higher(s1)
                            if (t != null) {
                                if (L != null) {
                                    this.reportIntersection(t, s1, L)
                                }
                                this.removeFuture(t, s2)
                            }
                            val r = this.T.lower(s2)
                            if (r != null) {
                                if (L != null) {
                                    this.reportIntersection(r, s2, L)
                                }
                                this.removeFuture(r, s1)
                            }
                        } else {
                            val t = this.T.higher(s2)
                            if (t != null) {
                                if (L != null) {
                                    this.reportIntersection(t, s2, L)
                                }
                                this.removeFuture(t, s1)
                            }
                            val r = this.T.lower(s1)
                            if (r != null) {
                                if (L != null) {
                                    this.reportIntersection(r, s1, L)
                                }
                                this.removeFuture(r, s2)
                            }
                        }
                        this.X.add(e.getPoint())
                    }
                }
            }
        }
    }

    private fun reportIntersection(s1: BoSegment, s2: BoSegment, L: Double): Boolean {
        val x1 = s1.first().x_coord
        val y1 = s1.first().y_coord
        val x2 = s1.second().x_coord
        val y2 = s1.second().y_coord
        val x3 = s2.first().x_coord
        val y3 = s2.first().y_coord
        val x4 = s2.second().x_coord
        val y4 = s2.second().y_coord
        val r = (x2 - x1) * (y4 - y3) - (y2 - y1) * (x4 - x3)
        if (r != 0.0) {
            val t = ((x3 - x1) * (y4 - y3) - (y3 - y1) * (x4 - x3)) / r
            val u = ((x3 - x1) * (y2 - y1) - (y3 - y1) * (x2 - x1)) / r
            if (t >= 0 && t <= 1 && u >= 0 && u <= 1) {
                val xcoord = x1 + t * (x2 - x1)
                val ycoord = y1 + t * (y2 - y1)
                if (xcoord > L) {
                    this.Q.add(BoEvent(BoPoint(xcoord, ycoord), arrayListOf(s1, s2), 2))
                    return true
                }
            }
        }
        return false
    }

    private fun removeFuture(s1: BoSegment, s2: BoSegment): Boolean {
        for (e in this.Q) {
            if (e.getType() == 2) {
                if (e.getSegments()[0] == s1 && e.getSegments()[1] == s2 || e.getSegments()[0] == s2 && e.getSegments()[1] == s1) {
                    this.Q.remove(e)
                    return true
                }
            }
        }
        return false
    }

    private fun swap(s1: BoSegment, s2: BoSegment) {
        this.T.remove(s1)
        this.T.remove(s2)
        val value = s1.get_Value()
        s1.set_Value(s2.get_Value())
        s2.set_Value(value)
        this.T.add(s1)
        this.T.add(s2)
    }

    private fun recalculate(L: Double) {
        for (segment in T) {
            segment.calculateValue(L)
        }
    }

    fun printIntersections() {
        for (p in this.X) {
            println("(${p.x_coord}, ${p.y_coord})")
        }
    }

    fun getIntersections(): ArrayList<BoPoint> {
        return this.X
    }

    private inner class EventComparator : Comparator<BoEvent> {
        override fun compare(e1: BoEvent, e2: BoEvent): Int {
            return if (e1.get_Value() > e2.get_Value()) {
                1
            } else if (e1.get_Value() < e2.get_Value()) {
                -1
            } else {
                0
            }
        }
    }

    private inner class SegmentComparator : Comparator<BoSegment> {
        override fun compare(s1: BoSegment, s2: BoSegment): Int {
            return if (s1.get_Value() < s2.get_Value()) {
                1
            } else if (s1.get_Value() > s2.get_Value()) {
                -1
            } else {
                0
            }
        }
    }
}