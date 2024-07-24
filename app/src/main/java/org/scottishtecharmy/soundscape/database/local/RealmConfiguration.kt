package org.scottishtecharmy.soundscape.database.local

import org.scottishtecharmy.soundscape.database.local.model.TileData
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration

object RealmConfiguration {
    private var realm: Realm? = null

    fun getInstance(): Realm {
        // has this object been created or opened yet?
        if (realm == null || realm!!.isClosed()) {
            // create the realm db based on the TileData model/schema
            var config = RealmConfiguration.create(setOf(TileData::class))
            realm = Realm.open(config)
        }
        return realm!!
    }
}