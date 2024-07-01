package com.kersnazzle.soundscapealpha.database.local.model

import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class TileData() : RealmObject {
    @PrimaryKey
    var quadKey : String = ""
    var lastUpdated : RealmInstant? = RealmInstant.now() // this timestamps it
    var tileString : String = ""
    var roads : String = "" // this is a test just to store the roads Feature Collection as string will look at how to store as embedded realm object
    var paths : String = "" // same as above
    var intersections : String = "" // same as above
    var entrances : String = "" // same as above
    var pois : String = "" // same as above
    //var pois : RealmList<GDASpatialDataResultEntity> = realmListOf()
    //var roads : RealmList<GDASpatialDataResultEntity> = realmListOf()
    //var paths : RealmList<GDASpatialDataResultEntity> = realmListOf()
    //var intersections : RealmList<Intersection> = realmListOf()
    //var entrances : RealmList<Entrances> = realmListOf()
    // Need to store the ttl for the tile which is created + 7 * 24 * 60 * 60
    //var ttl: RealmInstant? = ...

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TileData

        return quadKey == other.quadKey
    }

    override fun hashCode(): Int {
        var result = quadKey.hashCode()
        result = 31 * result + lastUpdated.hashCode()
        return result
    }
}