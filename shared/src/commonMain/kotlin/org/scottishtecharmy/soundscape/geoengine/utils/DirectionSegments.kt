package org.scottishtecharmy.soundscape.geoengine.utils

fun getCombinedDirectionSegments(
    heading: Double
): Array<Segment> {
    return arrayOf(
        Segment(heading + 180.0, 60.0),
        Segment(heading + 225.0, 30.0),
        Segment(heading + 270.0, 60.0),
        Segment(heading + 315.0, 30.0),
        Segment(heading, 60.0),
        Segment(heading + 45, 30.0),
        Segment(heading + 90, 60.0),
        Segment(heading + 135, 30.0)
    )
}

fun getIndividualDirectionSegments(
    heading: Double
): Array<Segment> {
    return arrayOf(
        Quadrant(heading + 180.0),
        Quadrant(heading + 270.0),
        Quadrant(heading + 0.0),
        Quadrant(heading + 90.0),
    )
}

fun getAheadBehindDirectionSegments(
    heading: Double
): Array<Segment> {
    return arrayOf(
        Segment(heading + 180.0, 150.0),
        Segment(heading + 270.0, 30.0),
        Segment(heading + 0.0, 150.0),
        Segment(heading + 90.0, 30.0),
    )
}

fun getLeftRightDirectionSegments(
    heading: Double
): Array<Segment> {
    return arrayOf(
        Segment(heading + 180.0, 60.0),
        Segment(heading + 270.0, 120.0),
        Segment(heading + 0.0, 60.0),
        Segment(heading + 90.0, 120.0),
    )
}
