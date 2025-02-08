package org.scottishtecharmy.soundscape.database.local.model

import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class RouteData(
    var name: String,
    var description: String,
) : RealmObject {
    constructor() : this("", "")

    @PrimaryKey
    var objectId: ObjectId = ObjectId()

    var waypoints: RealmList<MarkerData> = realmListOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteData
        return !(
            (name != other.name) ||
            (description != other.description) ||
            (waypoints != other.waypoints)
        )
    }
}
