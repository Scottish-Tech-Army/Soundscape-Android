package org.scottishtecharmy.soundscape.utils

import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers

/**
 * Serializes a route into the JSON format used for cross-platform sharing
 * (matches the iOS Soundscape route export shape).
 *
 * Built via kotlinx.serialization's JSON DSL rather than raw string
 * templates so free-text fields (route/marker name, description, address)
 * are properly escaped - a `"` or `\` in one of those used to produce
 * invalid, unparseable JSON.
 */
fun routeToShareJson(route: RouteWithMarkers): String {
    val json = buildJsonObject {
        put("name", route.route.name)
        put("id", route.route.routeId.toString())
        put("routeDescription", route.route.description)
        putJsonArray("waypoints") {
            route.markers.forEachIndexed { index, marker ->
                addJsonObject {
                    putJsonObject("marker") {
                        put("nickname", marker.name)
                        putJsonObject("location") {
                            put("name", marker.name)
                            putJsonObject("coordinate") {
                                put("latitude", marker.latitude)
                                put("longitude", marker.longitude)
                            }
                        }
                        put("estimatedAddress", marker.fullAddress)
                        put("id", marker.markerId.toString())
                    }
                    put("index", index)
                    put("markerId", marker.markerId.toString())
                }
            }
        }
    }
    return json.toString()
}
