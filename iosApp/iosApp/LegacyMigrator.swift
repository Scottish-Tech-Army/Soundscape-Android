//
//  LegacyMigrator.swift
//
//  One-shot importer that runs on first launch of the new (multiplatform)
//  build for users upgrading from the legacy Soundscape iOS app. Both apps
//  ship with the same bundle identifier (org.scottishtecharmy.soundscape), so
//  the new build inherits the legacy app's container — including
//  Documents/database.realm and the GDA* NSUserDefaults keys.
//
//  This file:
//    1. Reads markers and routes from the legacy Realm database.
//    2. Stages them as JSON for the Kotlin side (LegacyMigrationKt) to write
//       into the new Room database. The write itself deliberately does NOT
//       happen here: markers the legacy app never stored a name for have to
//       be looked up in map tiles, and blocking app launch on the network is
//       how an app gets killed by the watchdog. It runs from the migration
//       screen instead - see LegacyMigration.kt and LegacyMigrationScreen.kt.
//    3. Migrates a curated subset of the GDA* preferences into their new
//       PreferenceKeys equivalents.
//    4. Sets a `LegacyMigrationDone` flag so subsequent launches no-op.
//
//  Nothing belonging to the legacy app is ever deleted. The Realm files
//  and the GDA* preferences are left exactly as they were found, so a user
//  who rolls back to the legacy build still has their data, and so support
//  can recover anything a future release's import turns out to have
//  missed. The import even reads from a throwaway copy of the database so
//  that opening it can't upgrade the on-disk format underneath the legacy
//  app.
//
//  Migration aborts (without setting the done flag) on any error while
//  reading or staging the legacy database, so a failed attempt is retried on
//  the next launch.
//  Settings translation is best-effort — individual key failures are
//  tolerated.
//

import Foundation
import RealmSwift
import Shared

// MARK: - Legacy Realm models (mirror of legacy app schema)
//
// We declare every @Persisted field that exists on disk so that opening the
// realm doesn't trip a schema-mismatch error. The migrator only reads a
// subset, but Realm needs the model schema to cover every column.

final class LegacyReferenceEntity: Object {
    @Persisted(primaryKey: true) var id: String = ""
    @Persisted var entityKey: String?
    @Persisted var lastUpdatedDate: Date?
    @Persisted var lastSelectedDate: Date?
    @Persisted var isNew: Bool = true
    @Persisted var isTemp: Bool = true
    @Persisted var latitude: Double = 0.0
    @Persisted var longitude: Double = 0.0
    @Persisted var nickname: String?
    @Persisted var estimatedAddress: String?
    @Persisted var annotation: String?

    override class func _realmObjectName() -> String? { "ReferenceEntity" }
}

final class LegacyRouteWaypoint: EmbeddedObject {
    @Persisted var index: Int = -1
    @Persisted var markerId: String = ""

    override class func _realmObjectName() -> String? { "RouteWaypoint" }
}

final class LegacyRoute: Object {
    @Persisted(primaryKey: true) var id: String = ""
    @Persisted var name: String = ""
    @Persisted var routeDescription: String?
    @Persisted var waypoints: List<LegacyRouteWaypoint>
    @Persisted var firstWaypointLatitude: Double?
    @Persisted var firstWaypointLongitude: Double?
    @Persisted var isNew: Bool = true
    @Persisted var createdDate: Date = Date()
    @Persisted var lastUpdatedDate: Date = Date()
    @Persisted var lastSelectedDate: Date = Date()

    override class func _realmObjectName() -> String? { "Route" }
}

// MARK: - Migrator

enum LegacyMigrator {
    private static let doneKey = "LegacyMigrationDone"


    /// One-shot legacy read + always-on UserDefaults hygiene. The read half
    /// (legacy database → staged payload, plus settings translation) is gated
    /// by `LegacyMigrationDone` and runs at most once per install.
    /// The hygiene sweep runs on every launch — it's a fast no-op for
    /// already-clean defaults and lets us ship later fixes without needing
    /// to reset the done flag. Synchronous so the caller can be sure
    /// preferences are settled before the Compose UI reads them.
    ///
    /// `defaults`, `documentsPath` and `stageDatabase` default to the real
    /// production values/behaviour; tests override them to drive each
    /// branch deterministically without touching real Realm/Room state.
    static func runIfNeeded(
        defaults: UserDefaults = .standard,
        documentsPath: String = NSHomeDirectory() + "/Documents",
        stageDatabase: (String) -> Int32 = { stageLegacyDatabase(at: $0) }
    ) {
        if !defaults.bool(forKey: doneKey) {
            let legacyRealmPath = documentsPath + "/database.realm"

            if !FileManager.default.fileExists(atPath: legacyRealmPath) {
                // Fresh install: no legacy data to migrate. Mark done so we
                // never probe again.
                defaults.set(true, forKey: doneKey)
            } else {
                let stagedCount = stageDatabase(legacyRealmPath)
                if stagedCount < 0 {
                    // Couldn't read or stage the legacy database. Retry on the
                    // next launch (or once we can ship a fix). Do NOT set the
                    // done flag.
                    print("[LegacyMigrator] staging failed; will retry on next launch")
                } else {
                    migrateSettings(defaults: defaults)
                    defaults.set(true, forKey: doneKey)
                    print("[LegacyMigrator] staged \(stagedCount) markers + routes for import")
                }
            }
        }

        sweepIncompatibleDefaults(defaults: defaults)
    }

    // MARK: Database import

    /// Reads markers and routes out of an already-open legacy `realm` and
    /// encodes them as the JSON payload the Kotlin importer expects.
    ///
    /// `name` carries the legacy nickname and is empty for the many markers
    /// that never had one; naming those from `entityKey` is the Kotlin
    /// side's job, since that's where the tile data lives. Pure —
    /// no file I/O, no Kotlin/Room call — so tests can exercise it against
    /// an in-memory fixture Realm without touching the production database.
    /// Returns nil only if the payload can't be JSON-encoded.
    static func buildLegacyPayload(realm: Realm) -> String? {
        let savedReferences = realm.objects(LegacyReferenceEntity.self).filter("isTemp == false")
        var markersJson: [[String: Any]] = []
        markersJson.reserveCapacity(savedReferences.count)

        for ref in savedReferences {
            // Only the nickname is a name the legacy app actually stored. Everything else it
            // showed was looked up from `entityKey` against tile data each time it drew the
            // marker list, so we pass the key through and let the Kotlin importer do the same
            // lookup against our own tiles — see LegacyMarkerNamer.kt.
            markersJson.append([
                "legacyId": ref.id,
                "name": ref.nickname?.nonEmpty ?? "",
                "latitude": ref.latitude,
                "longitude": ref.longitude,
                "fullAddress": ref.estimatedAddress ?? "",
                "entityKey": ref.entityKey ?? "",
            ])
        }

        let routes = realm.objects(LegacyRoute.self)
        var routesJson: [[String: Any]] = []
        routesJson.reserveCapacity(routes.count)

        for route in routes {
            let waypointIds = route.waypoints
                .sorted(byKeyPath: "index", ascending: true)
                .map { $0.markerId }
                .filter { !$0.isEmpty }
            guard !waypointIds.isEmpty else { continue }

            routesJson.append([
                "name": route.name,
                "description": route.routeDescription ?? "",
                "waypointLegacyIds": Array(waypointIds),
            ])
        }

        let payload: [String: Any] = ["markers": markersJson, "routes": routesJson]

        guard let data = try? JSONSerialization.data(withJSONObject: payload, options: []) else {
            print("[LegacyMigrator] could not encode payload")
            return nil
        }
        return String(data: data, encoding: .utf8)
    }

    /// Reads the legacy Realm at `realmPath` and stages the resulting JSON
    /// payload for the migration screen to import.
    ///
    /// Returns the number of markers and routes staged, or -1 if the legacy
    /// database couldn't be read or the payload couldn't be written. Nothing
    /// is staged for a legacy install that has no markers or routes at all —
    /// there would be nothing for the migration screen to show.
    private static func stageLegacyDatabase(at realmPath: String) -> Int32 {
        guard let json = buildPayloadPreservingOriginal(at: realmPath) else { return -1 }

        let count = stagedItemCount(in: json)
        guard count > 0 else { return 0 }

        guard LegacyMigrationKt.stageLegacyMigrationPayload(payloadJson: json) else { return -1 }
        return count
    }

    /// How many markers and routes a payload holds, or 0 if it can't be read
    /// back — in which case there's nothing worth showing a screen for.
    private static func stagedItemCount(in json: String) -> Int32 {
        guard let data = json.data(using: .utf8),
              let payload = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else { return 0 }

        let markers = (payload["markers"] as? [Any])?.count ?? 0
        let routes = (payload["routes"] as? [Any])?.count ?? 0
        return Int32(markers + routes)
    }

    /// Builds the JSON payload from the legacy database at `realmPath`
    /// without modifying it in any way.
    ///
    /// Reads a throwaway copy rather than the file itself: opening a realm
    /// read-write lets RealmSwift 20.x upgrade an older on-disk format
    /// (e.g. v22) in place, and read-only mode refuses to open such a file
    /// at all. Since we leave the legacy database behind for the user, the
    /// original has to come out of this byte-for-byte unchanged, so we let
    /// Realm upgrade the copy instead.
    ///
    /// Returns nil if the copy, the open or the JSON encode fails.
    static func buildPayloadPreservingOriginal(at realmPath: String) -> String? {
        let fm = FileManager.default
        let scratchDirectory = NSTemporaryDirectory() + "LegacyMigration-" + UUID().uuidString
        let scratchPath = scratchDirectory + "/database.realm"
        do {
            try fm.createDirectory(atPath: scratchDirectory, withIntermediateDirectories: true)
            try fm.copyItem(atPath: realmPath, toPath: scratchPath)
        } catch {
            print("[LegacyMigrator] could not copy legacy realm: \(error)")
            try? fm.removeItem(atPath: scratchDirectory)
            return nil
        }

        // Realm's auxiliary files (.lock/.management) are created alongside
        // the copy, so removing the whole directory cleans up after us. The
        // payload is built in a nested scope so the Realm instance is
        // released before we delete the files it was reading.
        let json = readLegacyPayload(fromCopyAt: scratchPath)
        try? fm.removeItem(atPath: scratchDirectory)
        return json
    }

    /// Opens the copied legacy realm at `path` and returns the JSON payload,
    /// or nil if it can't be opened or encoded.
    private static func readLegacyPayload(fromCopyAt path: String) -> String? {
        var config = Realm.Configuration(
            fileURL: URL(fileURLWithPath: path),
            objectTypes: [LegacyReferenceEntity.self, LegacyRoute.self, LegacyRouteWaypoint.self],
        )
        // Schema migration block. The legacy app shipped at schemaVersion 0
        // and our model is shape-compatible (we declare every @Persisted
        // column the legacy file has). Bumping schemaVersion to 1 with a
        // no-op block lets Realm treat any minor mismatch as a migration
        // it can handle without prompting.
        config.schemaVersion = 1
        config.migrationBlock = { _, _ in }
        config.deleteRealmIfMigrationNeeded = false

        let realm: Realm
        do {
            realm = try Realm(configuration: config)
        } catch {
            print("[LegacyMigrator] could not open legacy realm: \(error)")
            return nil
        }

        return buildLegacyPayload(realm: realm)
    }

    // MARK: Settings translation

    static func migrateSettings(defaults: UserDefaults) {
        // Units
        if defaults.object(forKey: "GDASettingsMetric") != nil {
            let metric = defaults.bool(forKey: "GDASettingsMetric")
            defaults.set(metric ? "Metric" : "Imperial", forKey: "MeasurementUnits")
        }

        // App language. The legacy app stored a locale identifier; the new
        // app uses both `SearchLanguage` (for OSM/Photon search) and the
        // standard `AppleLanguages` array (which iOS reads at process
        // launch to pick the per-app localisation).
        if let locale = defaults.string(forKey: "GDASettingsLocaleIdentifier"), !locale.isEmpty {
            defaults.set(locale, forKey: "SearchLanguage")
            defaults.set([locale], forKey: "AppleLanguages")
        }

        // Speech rate. Legacy slider is 0.0–1.0 with 0.55 as the comfortable
        // default; new slider is 0.5–2.0 with 1.0 as default. Preserve the
        // user's distance from the default by rescaling proportionally and
        // clamping into the new range.
        if defaults.object(forKey: "GDASettingsSpeakingRate") != nil {
            let legacyRate = defaults.float(forKey: "GDASettingsSpeakingRate")
            let scaled = legacyRate / 0.55
            let clamped = min(max(scaled, 0.5), 2.0)
            defaults.set(clamped, forKey: "SpeechRate")
        }

        // Beacon style. Legacy values include "V2Beacon", "Classic" and
        // some experimental names; new catalogue is the BEACON_TYPES map in
        // shared/audio/BeaconTypes.kt. Map to the closest equivalent.
        if let legacyBeacon = defaults.string(forKey: "GDASelectedBeaconName") {
            let mapped: String
            switch legacyBeacon {
            case "V2Beacon", "Current": mapped = "Current"
            case "Classic", "Original": mapped = "Original"
            case "Tactile": mapped = "Tactile"
            case "Flare": mapped = "Flare"
            case "Shimmer": mapped = "Shimmer"
            case "Ping": mapped = "Ping"
            case "Drop": mapped = "Drop"
            case "Signal": mapped = "Signal"
            case "Mallet": mapped = "Mallet"
            default: mapped = "Current"
            }
            defaults.set(mapped, forKey: "BeaconType")
        }

        // Master automatic-callouts toggle.
        if defaults.object(forKey: "GDASettingsAutomaticCalloutsEnabled") != nil {
            defaults.set(
                defaults.bool(forKey: "GDASettingsAutomaticCalloutsEnabled"),
                forKey: "AllowCallouts",
            )
        }

        // Per-category callouts are reduced from seven toggles to a smaller
        // set. Place + Landmark collapse into the new "PlaceAndLandmarks"
        // key (true if either was enabled). Mobility maps directly. The
        // remaining four legacy categories (information, safety,
        // intersections, destination) have no equivalent and are dropped —
        // documented in docs/ios-upgrade-from-legacy.md.
        let placeOn = defaults.object(forKey: "GDASettingsPlaceSenseEnabled") == nil
            ? true : defaults.bool(forKey: "GDASettingsPlaceSenseEnabled")
        let landmarkOn = defaults.object(forKey: "GDASettingsLandmarkSenseEnabled") == nil
            ? true : defaults.bool(forKey: "GDASettingsLandmarkSenseEnabled")
        defaults.set(placeOn || landmarkOn, forKey: "PlaceAndLandmarks")

        if defaults.object(forKey: "GDASettingsMobilitySenseEnabled") != nil {
            defaults.set(
                defaults.bool(forKey: "GDASettingsMobilitySenseEnabled"),
                forKey: "Mobility",
            )
        }

        // Mix-with-other-audio.
        if defaults.object(forKey: "GDAAudioSessionMixesWithOthers") != nil {
            defaults.set(
                defaults.bool(forKey: "GDAAudioSessionMixesWithOthers"),
                forKey: "MixAudio",
            )
        }

        // Marker sort preference.
        if let style = defaults.string(forKey: "GDAMarkerSortStyle") {
            defaults.set(style == "alphanumeric", forKey: "MarkersSortByName")
        }

        // First-launch flag — if the legacy user finished onboarding, skip
        // the new app's onboarding flow.
        if defaults.bool(forKey: "GDAFirstLaunchDidComplete") {
            defaults.set(false, forKey: "FirstLaunch")
        }

        // The GDA* keys are deliberately left in place. They cost a few
        // hundred bytes, the new app never reads them again, and keeping
        // them means a user who rolls back to the legacy build still has
        // their settings — and that support can see what the legacy app
        // held if a translation above turns out to be wrong.
    }

    // MARK: Defaults hygiene

    /// Removes UserDefaults entries that would crash the Compose
    /// preference library when it iterates `dictionaryRepresentation()`:
    /// non-primitive values (Data, Date, Dictionary, mixed Array) that
    /// aren't owned by Apple/system frameworks. The new app stores only
    /// Bool/Number/String through `IosPreferencesProvider`, so anything
    /// else under our control is detritus.
    ///
    /// Legacy `GDA*` keys are exempt — whatever type they hold, they are
    /// the legacy app's settings and we leave them alone.
    ///
    /// Idempotent — runs on every launch so a future fix can land
    /// without resetting `LegacyMigrationDone`.
    static func sweepIncompatibleDefaults(defaults: UserDefaults) {
        let systemPrefixes = ["Apple", "NS", "kCFP", "com.apple.", "WebKit", "MK"]
        let preserved: Set<String> = [doneKey]

        for (key, value) in defaults.dictionaryRepresentation() {
            if preserved.contains(key) { continue }
            if key.hasPrefix("GDA") { continue }

            let isSystem = systemPrefixes.contains { key.hasPrefix($0) }
            if isSystem { continue }

            // ComposePreference accepts Bool/Number/String and arrays of
            // strings. NSNumber covers Bool/Int/Float/Double for free.
            if value is NSString || value is NSNumber { continue }
            if let array = value as? [String] { _ = array; continue }

            defaults.removeObject(forKey: key)
        }
    }
}

private extension String {
    /// Returns nil if the string is empty, otherwise returns self. Lets the
    /// migrator chain optional fallbacks without manually checking each.
    var nonEmpty: String? { isEmpty ? nil : self }
}
