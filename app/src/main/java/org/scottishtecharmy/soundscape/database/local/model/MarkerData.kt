package org.scottishtecharmy.soundscape.database.local.model

import io.realm.kotlin.ext.backlinks
import io.realm.kotlin.query.RealmResults
import io.realm.kotlin.types.RealmObject
import kotlin.Double.Companion.NaN

class MarkerData(
    var addressName: String,
    var location: Location?,
    var fullAddress: String = ""
) : RealmObject {
    constructor() : this("", Location(NaN, NaN))

    val route: RealmResults<RouteData> by backlinks(RouteData::waypoints)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MarkerData
        return (addressName == other.addressName) &&
                (location?.latitude == other.location?.latitude) &&
                (location?.longitude == other.location?.longitude)
    }}
