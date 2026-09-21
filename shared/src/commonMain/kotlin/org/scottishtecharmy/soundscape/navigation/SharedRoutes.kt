package org.scottishtecharmy.soundscape.navigation

/**
 * Route definitions for the shared navigation graph used by both Android and iOS.
 * Per-entry data (e.g. selected location, offline-maps target) travels through
 * NavigationStateHolder keyed by NavBackStackEntry.id rather than via path args.
 */
object SharedRoutes {
    const val WELCOME = "welcome"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val PLACES_NEARBY = "places_nearby_screen"
    const val MARKERS_AND_ROUTES = "markers_and_routes_screen"
    const val LOCATION_DETAILS = "location_details"
    const val EDIT_MARKER = "edit_marker"
    const val ROUTE_DETAILS = "route_details_screen"
    const val ADD_ROUTE = "add_route"
    const val EDIT_ROUTE = "edit_route"
    const val OFFLINE_MAPS = "offline_maps"
    const val ONBOARDING = "onboarding"

    /**
     * One-shot screen shown on the first run after upgrading from the legacy iOS app, while
     * its markers and routes are imported. Not part of onboarding: an upgrading user has
     * already been through that and never sees it again.
     */
    const val LEGACY_MIGRATION = "legacy_migration"
    const val SLEEP = "sleep_screen"
    const val HELP = "help_screen"
    const val OPEN_SOURCE_LICENSES = "open_source_licenses"
    const val ADVANCED_MARKERS_AND_ROUTES_SETTINGS = "advanced_markers_and_routes_settings"
}
