package org.scottishtecharmy.soundscape.utils

import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers

/**
 * Serializes a route into the JSON format used for cross-platform sharing
 * (matches the iOS Soundscape route export shape).
 */
fun routeToShareJson(route: RouteWithMarkers): String {
    val sb = StringBuilder()
    sb.append("{\n")
    sb.append("\t\"name\": \"${route.route.name}\",\n")
    sb.append("\t\"id\": \"${route.route.routeId}\",\n")
    sb.append("\t\"routeDescription\": \"${route.route.description}\",\n")
    sb.append("\t\"waypoints\": [\n")

    for ((index, marker) in route.markers.withIndex()) {
        if (index > 0) sb.append(",\n")
        sb.append("\t\t{\n")
        sb.append("\t\t\t\"marker\": {\n")
        sb.append("\t\t\t\t\"nickname\": \"${marker.name}\",\n")
        sb.append("\t\t\t\t\"location\": {\n")
        sb.append("\t\t\t\t\t\"name\": \"${marker.name}\",\n")
        sb.append("\t\t\t\t\t\"coordinate\": {\n")
        sb.append("\t\t\t\t\t\t\"latitude\": ${marker.latitude},\n")
        sb.append("\t\t\t\t\t\t\"longitude\": ${marker.longitude}\n")
        sb.append("\t\t\t\t\t}\n")
        sb.append("\t\t\t\t},\n")
        sb.append("\t\t\t\t\"estimatedAddress\": \"${marker.fullAddress}\",\n")
        sb.append("\t\t\t\t\"id\": \"${marker.markerId}\"\n")
        sb.append("\t\t\t},\n")
        sb.append("\t\t\t\"index\": $index,\n")
        sb.append("\t\t\t\"markerId\": \"${marker.markerId}\"\n")
        sb.append("\t\t}")
    }
    sb.append("\n\t]\n")
    sb.append("}\n")
    return sb.toString()
}
