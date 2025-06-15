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
        if (javaClass != other?.javaClass) return false

        other as RouteEntity
        return !(
                    (name != other.name) ||
                    (description != other.description)
                )
    }
}

@Entity(
    tableName = "route_marker_cross_ref",
    primaryKeys = ["route_id", "marker_id"], // Composite primary key
    indices = [
        Index(value = ["route_id"]),
        Index(value = ["marker_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["route_id"],
            childColumns = ["route_id"],
            onDelete = ForeignKey.CASCADE // If a route is deleted, remove its entries from this table
        ),
        ForeignKey(
            entity = MarkerEntity::class,
            parentColumns = ["marker_id"],
            childColumns = ["marker_id"],
            onDelete = ForeignKey.CASCADE // If a marker is deleted, remove its entries from this table
        )
    ]
)
data class RouteMarkerCrossRef(
    @ColumnInfo(name = "route_id")
    val routeId: Long,
    @ColumnInfo(name = "marker_id") // Indexing marker_id can be useful for querying markers of a route
    val markerId: Long,
    @ColumnInfo(name = "marker_order") // Optional: To maintain order of markers within a route
    val markerOrder: Int? = null
)

data class RouteWithMarkers(
    @Embedded
    val route: RouteEntity,

    @Relation(
        parentColumn = "route_id", // From RouteEntity
        entityColumn = "marker_id", // From MarkerEntity
        associateBy = Junction(
            value = RouteMarkerCrossRef::class,
            parentColumn = "route_id", // Column in RouteMarkerCrossRef that points to RouteEntity
            entityColumn = "marker_id"  // Column in RouteMarkerCrossRef that points to MarkerEntity
        )
    )
    var markers: List<MarkerEntity>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteWithMarkers

        if (route != other.route) return false
        if (markers != other.markers) return false

        return true
    }
}
