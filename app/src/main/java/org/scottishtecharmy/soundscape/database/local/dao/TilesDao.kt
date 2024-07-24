package org.scottishtecharmy.soundscape.database.local.dao

import org.scottishtecharmy.soundscape.database.local.model.TileData
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import io.realm.kotlin.notifications.ResultsChange
import io.realm.kotlin.query.RealmResults
import io.realm.kotlin.types.RealmInstant
import kotlinx.coroutines.flow.Flow

class TilesDao(val realm: Realm) {

    suspend fun insertTile(tile: TileData) = realm.write {
        copyToRealm(tile, updatePolicy = UpdatePolicy.ALL)
    }

    // fetch all objects of a type as a flow, asynchronously
    fun getAllTiles(): Flow<ResultsChange<TileData>> = realm.query<TileData>().asFlow()

    fun getTile(quadkey: String): RealmResults<TileData> {
        return realm.query<TileData>("quadKey == $0", quadkey).find()
    }

    suspend fun deleteTile(quadkey: String) = realm.write {

        val findTile = query<TileData>("quadKey == $0", quadkey).find()
        delete(findTile)
    }

    suspend fun updateTile(tile: TileData?) = realm.write {
        // find the first tile as quad key is unique and used for primary key
        val findTile = query<TileData>("quadKey == $0", tile?.quadKey ?: "0").first().find()

        // this isn't optimal as I'm storing the whole tile string and the assorted Feature Collections
        // that make up the string - roads, paths, blah
        findTile?.apply {
            lastUpdated = RealmInstant.now()
            tileString = tile?.tileString ?: "-"
            roads = tile?.roads ?: "-"
            paths = tile?.paths ?: "-"
            intersections = tile?.intersections ?: "-"
            entrances = tile?.entrances ?: "-"
            pois = tile?.pois ?: "-"
            busStops = tile?.busStops ?: "-"
            crossings = tile?.crossings ?: "-"
        }
    }

}