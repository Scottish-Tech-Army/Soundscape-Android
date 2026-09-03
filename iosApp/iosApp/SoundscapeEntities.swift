import AppIntents
import Shared

// Saved routes and markers, exposed to Siri and the Shortcuts app as pickable values.
//
// These are what let "start my Commute route" work as a sentence rather than a guess:
// Siri gets the real list, so it can disambiguate between similar names and show a
// picker in Shortcuts, and the intent receives an id rather than a string. That is why
// the parameterised intents use SoundscapeAction.StartRouteById and
// .BeaconOnMarkerById — with a resolved entity in hand there is nothing left to match.
// The fuzzy StartRouteNamed / BeaconOnMarkerNamed remain for the soundscape:// URL
// path, where a bare name really is all the caller has.
//
// Named without the "Entity" suffix because the shared framework already exports Kotlin's
// RouteEntity and MarkerEntity under those names.

/// Opens the database directly rather than going through IosSoundscapeService.
///
/// The system queries these entities to build and refresh Siri's vocabulary, at times of
/// its own choosing and well outside any user action. Reaching for the service there
/// would construct it — starting the geo engine, location updates and the audio engine —
/// just to read a list of names.
///
/// Safe to call from the system's threads only because MarkersAndRoutesDatabaseProvider
/// now locks around construction; before that it was an unsynchronised
/// `INSTANCE ?: build()` that could produce two Room instances over one SQLite file.
private func sharedRouteDao() -> RouteDao {
    MarkersAndRoutesDatabaseProvider.shared.getInstance().routeDao()
}

// MARK: - Routes

struct SoundscapeRoute: AppEntity {
    let id: Int
    let name: String

    static var typeDisplayRepresentation: TypeDisplayRepresentation = "Route"
    static var defaultQuery = SoundscapeRouteQuery()

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(name)")
    }
}

struct SoundscapeRouteQuery: EntityStringQuery {

    /// Resolves ids back to routes — how a saved shortcut finds its route again.
    func entities(for identifiers: [Int]) async throws -> [SoundscapeRoute] {
        let dao = sharedRouteDao()
        var resolved: [SoundscapeRoute] = []
        for id in identifiers {
            if let route = try await dao.getRouteById(routeId: Int64(id)) {
                resolved.append(SoundscapeRoute(id: Int(route.routeId), name: route.name))
            }
        }
        return resolved
    }

    /// Narrows the list from what the user said or typed. Substring rather than the
    /// shared fuzzy matcher on purpose: this feeds a picker Siri can disambiguate from,
    /// so returning every plausible candidate beats picking a single best one.
    func entities(matching string: String) async throws -> [SoundscapeRoute] {
        let matches = try await allRoutes()
            .filter { $0.name.localizedCaseInsensitiveContains(string) }
        siriLog.notice("routes matching: \(matches.count, privacy: .public) hit(s)")
        return matches
    }

    func suggestedEntities() async throws -> [SoundscapeRoute] {
        let routes = try await allRoutes()
        // The decisive line when a name is not recognised: it says whether the system
        // re-queried at all, and whether what it saw included the route in question.
        siriLog.notice("suggestedEntities: \(routes.count, privacy: .public) routes")
        #if DEBUG
        siriLog.notice("  \(routes.map(\.name).joined(separator: ", "), privacy: .public)")
        #endif
        return routes
    }

    private func allRoutes() async throws -> [SoundscapeRoute] {
        try await sharedRouteDao().getAllRoutes().map {
            SoundscapeRoute(id: Int($0.routeId), name: $0.name)
        }
    }
}

// MARK: - Markers

struct SoundscapeMarker: AppEntity {
    let id: Int
    let name: String
    let address: String

    static var typeDisplayRepresentation: TypeDisplayRepresentation = "Marker"
    static var defaultQuery = SoundscapeMarkerQuery()

    /// The address rides along as a subtitle so two markers sharing a name are still
    /// tellable apart in a picker.
    var displayRepresentation: DisplayRepresentation {
        if address.isEmpty {
            return DisplayRepresentation(title: "\(name)")
        }
        return DisplayRepresentation(title: "\(name)", subtitle: "\(address)")
    }
}

struct SoundscapeMarkerQuery: EntityStringQuery {

    func entities(for identifiers: [Int]) async throws -> [SoundscapeMarker] {
        let dao = sharedRouteDao()
        var resolved: [SoundscapeMarker] = []
        for id in identifiers {
            if let marker = try await dao.getMarkerById(markerId: Int64(id)) {
                resolved.append(
                    SoundscapeMarker(
                        id: Int(marker.markerId),
                        name: marker.name,
                        address: marker.fullAddress
                    )
                )
            }
        }
        return resolved
    }

    func entities(matching string: String) async throws -> [SoundscapeMarker] {
        let matches = try await allMarkers()
            .filter { $0.name.localizedCaseInsensitiveContains(string) }
        siriLog.notice("markers matching: \(matches.count, privacy: .public) hit(s)")
        return matches
    }

    func suggestedEntities() async throws -> [SoundscapeMarker] {
        let markers = try await allMarkers()
        siriLog.notice("suggestedEntities: \(markers.count, privacy: .public) markers")
        #if DEBUG
        siriLog.notice("  \(markers.map(\.name).joined(separator: ", "), privacy: .public)")
        #endif
        return markers
    }

    private func allMarkers() async throws -> [SoundscapeMarker] {
        try await sharedRouteDao().getAllMarkers().map {
            SoundscapeMarker(id: Int($0.markerId), name: $0.name, address: $0.fullAddress)
        }
    }
}
