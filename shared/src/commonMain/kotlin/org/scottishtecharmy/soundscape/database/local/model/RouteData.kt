package org.scottishtecharmy.soundscape.database.local.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "routes"
)
class RouteEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "route_id")
    val routeId: Long = 0,

    var name: String,
    val description: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RouteEntity) return false
        return (name == other.name) && (description == other.description)
    }
}

@Entity(
    tableName = "route_marker_cross_ref",
    primaryKeys = ["route_id", "marker_id"],
    indices = [
        Index(value = ["route_id"]),
        Index(value = ["marker_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["route_id"],
            childColumns = ["route_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MarkerEntity::class,
            parentColumns = ["marker_id"],
            childColumns = ["marker_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RouteMarkerCrossRef(
    @ColumnInfo(name = "route_id")
    val routeId: Long,
    @ColumnInfo(name = "marker_id")
    val markerId: Long,
    @ColumnInfo(name = "marker_order")
    val markerOrder: Int? = null
)

data class RouteWithMarkers(
    @Embedded
    val route: RouteEntity,

    @Relation(
        parentColumn = "route_id",
        entityColumn = "marker_id",
        associateBy = Junction(
            value = RouteMarkerCrossRef::class,
            parentColumn = "route_id",
            entityColumn = "marker_id"
        )
    )
    var markers: List<MarkerEntity>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RouteWithMarkers) return false
        return (route == other.route) && (markers == other.markers)
    }
}
