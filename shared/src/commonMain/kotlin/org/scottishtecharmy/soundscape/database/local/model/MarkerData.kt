package org.scottishtecharmy.soundscape.database.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.scottishtecharmy.soundscape.geoengine.utils.distance
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

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
        if (other !is MarkerEntity) return false
        val dist = distance(latitude, longitude, other.latitude, other.longitude)
        return (name == other.name) && (fullAddress == other.fullAddress) && (dist < 1.0)
    }
}
