package org.scottishtecharmy.soundscape.database.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.jvm.javaClass

@Entity(
    tableName = "markers",
    indices = [Index("latitude", "longitude", name = "location_index")]
)
class MarkerEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "marker_id")
    val markerId: Long = 0L,

    val name: String,
    val longitude: Double,
    val latitude: Double,
    val fullAddress: String = "")
{
    fun getLngLatAlt(): LngLatAlt {
        return LngLatAlt(longitude, latitude)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MarkerEntity
        return (name == other.name) &&
                (latitude == other.latitude) &&
                (longitude == other.longitude)
    }
}

