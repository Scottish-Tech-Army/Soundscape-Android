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
//    2. Hands them to the Kotlin side (LegacyMigrationKt.runLegacyMigrationImport)
//       which writes them into the new Room database.
//    3. Migrates a curated subset of the GDA* preferences into their new
//       PreferenceKeys equivalents.
//    4. Deletes the legacy Realm files and the cache realms in Library/.
//    5. Sets a `LegacyMigrationDone` flag so subsequent launches no-op.
//
//  Migration aborts (without setting the done flag) on any error during
//  database import, so legacy data is never deleted before a successful
//  import. Settings translation is best-effort — individual key failures
//  are tolerated.
//

import Foundation
import RealmSwift
import Shared

// MARK: - Legacy Realm models (mirror of legacy app schema)
//
// We declare every @Persisted field that exists on disk so that opening the
// realm read-only doesn't trip a schema-mismatch error. The migrator only
// reads a subset, but Realm needs the model schema to cover every column.

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

    /// One-shot legacy import + always-on UserDefaults hygiene. The import
    /// half (database + settings translation + legacy file deletion) is
    /// gated by `LegacyMigrationDone` and runs at most once per install.
    /// The hygiene sweep runs on every launch — it's a fast no-op for
    /// already-clean defaults and lets us ship later fixes without needing
    /// to reset the done flag. Synchronous so the caller can be sure
    /// preferences are settled before the Compose UI reads them.
    ///
    /// `defaults`, `documentsPath` and `importDatabase` default to the real
    /// production values/behaviour; tests override them to drive each
    /// branch deterministically without touching real Realm/Room state.
    static func runIfNeeded(
        defaults: UserDefaults = .standard,
        documentsPath: String = NSHomeDirectory() + "/Documents",
        importDatabase: (String) -> Int32 = { importLegacyDatabase(at: $0) }
    ) {
        if !defaults.bool(forKey: doneKey) {
            let legacyRealmPath = documentsPath + "/database.realm"

            if !FileManager.default.fileExists(atPath: legacyRealmPath) {
                // Fresh install: no legacy data to migrate. Mark done so we
                // never probe again.
                defaults.set(true, forKey: doneKey)
            } else {
                let importedCount = importDatabase(legacyRealmPath)
                if importedCount < 0 {
                    // Database import failed. Leave the legacy files in
                    // place so the user can retry on next launch (or we
                    // can ship a fix). Do NOT set the done flag.
                    print("[LegacyMigrator] database import failed; will retry on next launch")
                } else {
                    migrateSettings(defaults: defaults)
                    deleteLegacyArtefacts(documentsPath: documentsPath)
                    defaults.set(true, forKey: doneKey)
                    print("[LegacyMigrator] migrated \(importedCount) markers + routes")
                }
            }
        }

        sweepIncompatibleDefaults(defaults: defaults)
    }

    // MARK: Database import

    /// Reads markers and routes out of an already-open legacy `realm` and
    /// encodes them as the JSON payload the Kotlin importer expects. Pure —
    /// no file I/O, no Kotlin/Room call — so tests can exercise it against
    /// an in-memory fixture Realm without touching the production database.
    /// Returns nil only if the payload can't be JSON-encoded.
    static func buildLegacyPayload(realm: Realm) -> String? {
        let savedReferences = realm.objects(LegacyReferenceEntity.self).filter("isTemp == false")
        var markersJson: [[String: Any]] = []
        markersJson.reserveCapacity(savedReferences.count)

        for ref in savedReferences {
            let name = ref.nickname?.nonEmpty
                ?? ref.estimatedAddress?.nonEmpty
                ?? ref.entityKey?.nonEmpty
                ?? "Unnamed"
            markersJson.append([
                "legacyId": ref.id,
                "name": name,
                "latitude": ref.latitude,
                "longitude": ref.longitude,
                "fullAddress": ref.estimatedAddress ?? "",
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

    /// Opens the legacy Realm at `realmPath`, builds the JSON payload via
    /// [buildLegacyPayload], and hands it to the Kotlin side to write into
    /// the production Room database.
    private static func importLegacyDatabase(at realmPath: String) -> Int32 {
        // Open the legacy realm read-write. Read-only mode refuses to touch
        // the file, but RealmSwift 20.x will reject any legacy file at an
        // older on-disk format (e.g. v22) unless it can upgrade in place.
        // Letting Realm upgrade is harmless because we delete the file
        // after a successful import.
        var config = Realm.Configuration(
            fileURL: URL(fileURLWithPath: realmPath),
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
            return -1
        }

        guard let json = buildLegacyPayload(realm: realm) else { return -1 }

        return LegacyMigrationKt.runLegacyMigrationImport(payloadJson: json)
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

        // Remove the now-translated legacy keys to keep NSUserDefaults tidy
        // and avoid confusion if a later release inspects them.
        let legacyKeys = [
            "GDASettingsMetric",
            "GDASettingsLocaleIdentifier",
            "GDASettingsSpeakingRate",
            "GDASelectedBeaconName",
            "GDASettingsAutomaticCalloutsEnabled",
            "GDASettingsPlaceSenseEnabled",
            "GDASettingsLandmarkSenseEnabled",
            "GDASettingsMobilitySenseEnabled",
            "GDASettingsInformationSenseEnabled",
            "GDASettingsSafetySenseEnabled",
            "GDASettingsIntersectionsSenseEnabled",
            "GDASettingsDestinationSenseEnabled",
            "GDAAudioSessionMixesWithOthers",
            "GDAMarkerSortStyle",
            "GDAFirstLaunchDidComplete",
            "GDABeaconTutorialDidComplete",
            "GDAMarkerTutorialDidComplete",
            "GDAPreviewTutorialDidComplete",
            "GDARouteTutorialDidComplete",
            "GDAUserDefaultClientIdentifier",
            "GDAAppUseCount",
            "GDANewFeaturesLastDisplayedVersion",
            "GDAAppleSynthVoice",
            "GDABeaconVolume",
            "GDATTSVolume",
            "GDAOtherVolume",
            "GDATTSAudioGain",
            "GDABeaconAudioGain",
            "GDAAFXAudioGain",
            "GDASettingsTelemetryOptout",
            "GDASettingsUseOldBeacon",
            "GDAPlayBeaconStartEndMelody",
            "GDASettingsAPNsDeviceToken",
            "GDASettingsPushNotificationTags",
            "GDASettingsPreviewIntersectionsIncludeUnnamedRoads",
            "GDASettingsInitialCloudSyncCompleted",
            "GDASettingsPreviewInitialRoadFinderComplete",
            "GDASettingsPreviewInitialRoadFinderError",
            "GDAFirstUseExperienceShare",
            "GDAFirstUseExperienceDonateSiriShortcuts",
        ]
        for key in legacyKeys {
            defaults.removeObject(forKey: key)
        }
    }

    // MARK: Defaults hygiene

    /// Removes UserDefaults entries that would crash the Compose
    /// preference library when it iterates `dictionaryRepresentation()`.
    /// Drops anything still under the legacy `GDA*` prefix as well as any
    /// non-primitive value (Data, Date, Dictionary, mixed Array) that
    /// isn't owned by Apple/system frameworks. The new app stores only
    /// Bool/Number/String through `IosPreferencesProvider`, so anything
    /// else under our control is detritus.
    ///
    /// Idempotent — runs on every launch so a future fix can land
    /// without resetting `LegacyMigrationDone`.
    static func sweepIncompatibleDefaults(defaults: UserDefaults) {
        let systemPrefixes = ["Apple", "NS", "kCFP", "com.apple.", "WebKit", "MK"]
        let preserved: Set<String> = [doneKey]

        for (key, value) in defaults.dictionaryRepresentation() {
            if preserved.contains(key) { continue }

            if key.hasPrefix("GDA") {
                defaults.removeObject(forKey: key)
                continue
            }

            let isSystem = systemPrefixes.contains { key.hasPrefix($0) }
            if isSystem { continue }

            // ComposePreference accepts Bool/Number/String and arrays of
            // strings. NSNumber covers Bool/Int/Float/Double for free.
            if value is NSString || value is NSNumber { continue }
            if let array = value as? [String] { _ = array; continue }

            defaults.removeObject(forKey: key)
        }
    }

    // MARK: Cleanup

    static func deleteLegacyArtefacts(documentsPath: String) {
        let fm = FileManager.default
        let realmFiles = [
            documentsPath + "/database.realm",
            documentsPath + "/database.realm.lock",
            documentsPath + "/database.realm.note",
            documentsPath + "/database.realm.management",
        ]
        for path in realmFiles {
            try? fm.removeItem(atPath: path)
        }

        // Cache realms live in Library/cache.<n>.realm — POI cache, fully
        // regenerable. Sweep them out so we don't waste space.
        let libraryPath = NSHomeDirectory() + "/Library"
        if let entries = try? fm.contentsOfDirectory(atPath: libraryPath) {
            for entry in entries where entry.hasPrefix("cache.") && entry.contains(".realm") {
                try? fm.removeItem(atPath: libraryPath + "/" + entry)
            }
        }
    }
}

private extension String {
    /// Returns nil if the string is empty, otherwise returns self. Lets the
    /// migrator chain optional fallbacks without manually checking each.
    var nonEmpty: String? { isEmpty ? nil : self }
}
