package org.scottishtecharmy.soundscape.utils

enum class RelativeDirections (val value: Int) {
    // Ahead, Right, Behind, and Left all get a 150 degree window centered in their respective
    // directions (e.g. right is 15 degrees to 165 degrees). In the areas where these windows
    // overlap, the relative directions get combined. For example, 0 degrees is "ahead", while
    // 16 degrees is "ahead to the right."
    COMBINED(0),
    // Ahead, Right, Behind, and Left all get a 90 degree window centered in their respective
    // directions (e.g. right is from 45 degrees to 135 degrees). These windows do not overlap,
    // so relative directions can only be "ahead", "to the right", "behind", or "to the left".
    INDIVIDUAL(1),
    // Ahead and Behind get a 150 degree window, while Left and Right get 30 degree windows in their
    // respective directions (e.g. right is 75 degrees to 105 degrees and behind is 105 degrees to
    // 255 degrees). These windows do not overlap, so relative directions can only be "ahead",
    // "to the right", "behind", or "to the left". This style of relative direction is biased towards
    // calling out things as either ahead or behind unless they are directly to the left or right.
    AHEAD_BEHIND(2),
    // Left and Right get a 120 degree window, while Ahead and Behind get 60 degree windows in their
    // respective directions (e.g. right is 30 degrees to 150 degrees and behind is 150 degrees to
    // 210 degrees). These windows do not overlap, so relative directions can only be "ahead",
    // "to the right", "behind", or "to the left". This style of relative direction is biased towards
    // calling out things as either left or right unless they are directly ahead or behind.
    LEFT_RIGHT(3)
}