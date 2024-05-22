package com.kersnazzle.soundscapealpha.utils

enum class Direction(val value: Int) {
    // This is tracking around clockwise from 6 o'clock
    BEHIND(0),
    BEHIND_LEFT(1),
    LEFT(2),
    AHEAD_LEFT(3),
    AHEAD(4),
    AHEAD_RIGHT(5),
    RIGHT(6),
    BEHIND_RIGHT(7),
    UNKNOWN(8);

}