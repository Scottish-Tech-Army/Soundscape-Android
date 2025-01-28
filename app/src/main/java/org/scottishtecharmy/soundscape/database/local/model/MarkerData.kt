package org.scottishtecharmy.soundscape.database.local.model

import io.realm.kotlin.types.RealmObject
import kotlin.Double.Companion.NaN

class MarkerData(
    var addressName: String,
    var fullAddress: String,
    var location: Location?,
) : RealmObject {
    constructor() : this("", "", Location(NaN, NaN))
}
