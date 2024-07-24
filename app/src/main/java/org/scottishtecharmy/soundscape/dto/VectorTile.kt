package org.scottishtecharmy.soundscape.dto

data class VectorTile(
    var quadkey: String = "",
    var tileX: Int = 0,
    var tileY: Int = 0,
    var zoom: Int = 0
)
