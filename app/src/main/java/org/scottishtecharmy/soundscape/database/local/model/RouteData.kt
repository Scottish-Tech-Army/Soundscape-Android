package org.scottishtecharmy.soundscape.database.local.model

import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlin.Double.Companion.NaN

class RoutePoint(var name: String, var latitude: Double, var longitude: Double) : RealmObject
{
    constructor() : this("", NaN, NaN)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoutePoint

        return (name == other.name) && (latitude == other.latitude) && (longitude == other.longitude)
    }
}

class RouteData : RealmObject {
    @PrimaryKey
    var name : String = ""
    var description : String = ""
    var waypoints : RealmList<RoutePoint> = realmListOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteData
        if ((name != other.name) ||
            (description != other.description) ||
            (waypoints != other.waypoints))
        {
            return false
        }
        return true
    }
}