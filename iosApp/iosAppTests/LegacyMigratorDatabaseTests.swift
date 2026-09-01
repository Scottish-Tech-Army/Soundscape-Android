//
//  LegacyMigratorDatabaseTests.swift
//
//  Unit tests for the parts of LegacyMigrator that LegacyMigratorTests.swift
//  doesn't cover: the Realm→JSON payload builder, the copy-and-read step
//  that keeps the legacy database intact, and the runIfNeeded orchestration
//  across its four branches (fresh install, successful import, failed
//  import, already done). All of these run against in-memory or temporary
//  Realm fixtures, temp directories and isolated UserDefaults suites —
//  never the production Realm/Room/UserDefaults.standard.
//

import XCTest
import RealmSwift
@testable import Soundscape

final class LegacyMigratorDatabaseTests: XCTestCase {

    private var suiteName: String!
    private var defaults: UserDefaults!
    private var documentsPath: String!

    override func setUp() {
        super.setUp()
        suiteName = "LegacyMigratorDatabaseTests-" + UUID().uuidString
        defaults = UserDefaults(suiteName: suiteName)!
        documentsPath = NSTemporaryDirectory() + "LegacyMigratorDatabaseTests-" + UUID().uuidString
        try! FileManager.default.createDirectory(atPath: documentsPath, withIntermediateDirectories: true)
    }

    override func tearDown() {
        defaults.removePersistentDomain(forName: suiteName)
        defaults = nil
        suiteName = nil
        try? FileManager.default.removeItem(atPath: documentsPath)
        documentsPath = nil
        super.tearDown()
    }

    // MARK: - Helpers

    private func makeInMemoryRealm() -> Realm {
        let config = Realm.Configuration(
            inMemoryIdentifier: UUID().uuidString,
            objectTypes: [LegacyReferenceEntity.self, LegacyRoute.self, LegacyRouteWaypoint.self],
        )
        return try! Realm(configuration: config)
    }

    private func addMarker(
        to realm: Realm,
        id: String,
        isTemp: Bool = false,
        nickname: String? = nil,
        estimatedAddress: String? = nil,
        entityKey: String? = nil,
        latitude: Double = 0,
        longitude: Double = 0
    ) {
        try! realm.write {
            let marker = LegacyReferenceEntity()
            marker.id = id
            marker.isTemp = isTemp
            marker.nickname = nickname
            marker.estimatedAddress = estimatedAddress
            marker.entityKey = entityKey
            marker.latitude = latitude
            marker.longitude = longitude
            realm.add(marker)
        }
    }

    private func addRoute(to realm: Realm, id: String, name: String, waypointIds: [String]) {
        try! realm.write {
            let route = LegacyRoute()
            route.id = id
            route.name = name
            for (index, markerId) in waypointIds.enumerated() {
                let waypoint = LegacyRouteWaypoint()
                waypoint.index = index
                waypoint.markerId = markerId
                route.waypoints.append(waypoint)
            }
            realm.add(route)
        }
    }

    private func decodePayload(_ json: String?, file: StaticString = #filePath, line: UInt = #line) -> [String: Any] {
        guard let json, let data = json.data(using: .utf8) else {
            XCTFail("Expected a non-nil JSON payload", file: file, line: line)
            return [:]
        }
        guard let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            XCTFail("Payload wasn't a JSON object: \(json)", file: file, line: line)
            return [:]
        }
        return object
    }

    private func createFile(at path: String, contents: Data = Data()) {
        FileManager.default.createFile(atPath: path, contents: contents)
    }

    // MARK: - buildLegacyPayload

    func testBuildPayloadIncludesSavedMarkerWithAllFields() {
        let realm = makeInMemoryRealm()
        addMarker(
            to: realm,
            id: "leg-1",
            nickname: "Pub",
            estimatedAddress: "1 Royal Mile",
            latitude: 55.95,
            longitude: -3.19,
        )

        let payload = decodePayload(LegacyMigrator.buildLegacyPayload(realm: realm))
        let markers = payload["markers"] as? [[String: Any]]

        XCTAssertEqual(markers?.count, 1)
        let marker = markers?.first
        XCTAssertEqual(marker?["legacyId"] as? String, "leg-1")
        XCTAssertEqual(marker?["name"] as? String, "Pub")
        XCTAssertEqual(marker?["latitude"] as? Double, 55.95)
        XCTAssertEqual(marker?["longitude"] as? Double, -3.19)
        XCTAssertEqual(marker?["fullAddress"] as? String, "1 Royal Mile")
    }

    func testBuildPayloadExcludesTempMarkers() {
        let realm = makeInMemoryRealm()
        addMarker(to: realm, id: "temp-1", isTemp: true, nickname: "En route beacon")
        addMarker(to: realm, id: "saved-1", isTemp: false, nickname: "Saved")

        let payload = decodePayload(LegacyMigrator.buildLegacyPayload(realm: realm))
        let markers = payload["markers"] as? [[String: Any]]

        XCTAssertEqual(markers?.count, 1)
        XCTAssertEqual(markers?.first?["legacyId"] as? String, "saved-1")
    }

    func testBuildPayloadNameFallsBackToNicknameWhenPresent() {
        let realm = makeInMemoryRealm()
        addMarker(to: realm, id: "m1", nickname: "Nickname", estimatedAddress: "Address", entityKey: "EntityKey")

        let payload = decodePayload(LegacyMigrator.buildLegacyPayload(realm: realm))
        let marker = (payload["markers"] as? [[String: Any]])?.first
        XCTAssertEqual(marker?["name"] as? String, "Nickname")
    }

    func testBuildPayloadNameFallsBackToAddressWhenNoNickname() {
        let realm = makeInMemoryRealm()
        addMarker(to: realm, id: "m1", nickname: nil, estimatedAddress: "Address", entityKey: "EntityKey")

        let payload = decodePayload(LegacyMigrator.buildLegacyPayload(realm: realm))
        let marker = (payload["markers"] as? [[String: Any]])?.first
        XCTAssertEqual(marker?["name"] as? String, "Address")
    }

    func testBuildPayloadNameFallsBackToEntityKeyWhenNoNicknameOrAddress() {
        let realm = makeInMemoryRealm()
        addMarker(to: realm, id: "m1", nickname: nil, estimatedAddress: nil, entityKey: "EntityKey")

        let payload = decodePayload(LegacyMigrator.buildLegacyPayload(realm: realm))
        let marker = (payload["markers"] as? [[String: Any]])?.first
        XCTAssertEqual(marker?["name"] as? String, "EntityKey")
    }

    func testBuildPayloadNameFallsBackToUnnamedWhenAllFallbacksMissing() {
        let realm = makeInMemoryRealm()
        addMarker(to: realm, id: "m1", nickname: nil, estimatedAddress: nil, entityKey: nil)

        let payload = decodePayload(LegacyMigrator.buildLegacyPayload(realm: realm))
        let marker = (payload["markers"] as? [[String: Any]])?.first
        XCTAssertEqual(marker?["name"] as? String, "Unnamed")
    }

    func testBuildPayloadOrdersRouteWaypointsByIndexRegardlessOfInsertionOrder() {
        let realm = makeInMemoryRealm()
        addMarker(to: realm, id: "m1", nickname: "M1")
        addMarker(to: realm, id: "m2", nickname: "M2")
        addMarker(to: realm, id: "m3", nickname: "M3")

        try! realm.write {
            let route = LegacyRoute()
            route.id = "r1"
            route.name = "Loop"
            // Append out of index order to prove sorting isn't insertion order.
            let w2 = LegacyRouteWaypoint(); w2.index = 2; w2.markerId = "m3"
            let w0 = LegacyRouteWaypoint(); w0.index = 0; w0.markerId = "m1"
            let w1 = LegacyRouteWaypoint(); w1.index = 1; w1.markerId = "m2"
            route.waypoints.append(w2)
            route.waypoints.append(w0)
            route.waypoints.append(w1)
            realm.add(route)
        }

        let payload = decodePayload(LegacyMigrator.buildLegacyPayload(realm: realm))
        let routes = payload["routes"] as? [[String: Any]]
        XCTAssertEqual(routes?.count, 1)
        XCTAssertEqual(routes?.first?["waypointLegacyIds"] as? [String], ["m1", "m2", "m3"])
    }

    func testBuildPayloadExcludesRouteWithNoResolvableWaypoints() {
        let realm = makeInMemoryRealm()
        try! realm.write {
            let route = LegacyRoute()
            route.id = "r1"
            route.name = "Broken"
            let empty = LegacyRouteWaypoint(); empty.index = 0; empty.markerId = ""
            route.waypoints.append(empty)
            realm.add(route)
        }

        let payload = decodePayload(LegacyMigrator.buildLegacyPayload(realm: realm))
        XCTAssertEqual((payload["routes"] as? [[String: Any]])?.count, 0)
    }

    func testBuildPayloadIncludesMultipleRoutesSharingMarkers() {
        let realm = makeInMemoryRealm()
        addMarker(to: realm, id: "m1", nickname: "M1")
        addMarker(to: realm, id: "m2", nickname: "M2")
        addRoute(to: realm, id: "r1", name: "A", waypointIds: ["m1", "m2"])
        addRoute(to: realm, id: "r2", name: "B", waypointIds: ["m2", "m1"])

        let payload = decodePayload(LegacyMigrator.buildLegacyPayload(realm: realm))
        let routes = (payload["routes"] as? [[String: Any]])?.sorted {
            ($0["name"] as? String ?? "") < ($1["name"] as? String ?? "")
        }

        XCTAssertEqual(routes?.count, 2)
        XCTAssertEqual(routes?[0]["waypointLegacyIds"] as? [String], ["m1", "m2"])
        XCTAssertEqual(routes?[1]["waypointLegacyIds"] as? [String], ["m2", "m1"])
    }

    func testBuildPayloadEmptyRealmProducesEmptyArrays() {
        let realm = makeInMemoryRealm()

        let payload = decodePayload(LegacyMigrator.buildLegacyPayload(realm: realm))

        XCTAssertEqual((payload["markers"] as? [[String: Any]])?.count, 0)
        XCTAssertEqual((payload["routes"] as? [[String: Any]])?.count, 0)
    }

    // MARK: - buildPayloadPreservingOriginal

    /// Writes a legacy-shaped realm to `path` and returns once it's closed,
    /// so the caller can treat the file as a static on-disk fixture.
    private func writeOnDiskLegacyRealm(at path: String, markerName: String) {
        autoreleasepool {
            let config = Realm.Configuration(
                fileURL: URL(fileURLWithPath: path),
                objectTypes: [LegacyReferenceEntity.self, LegacyRoute.self, LegacyRouteWaypoint.self],
            )
            let realm = try! Realm(configuration: config)
            addMarker(to: realm, id: "leg-1", nickname: markerName, latitude: 1, longitude: 2)
        }
    }

    func testBuildPayloadPreservingOriginalReadsMarkersFromDisk() {
        let realmPath = documentsPath + "/database.realm"
        writeOnDiskLegacyRealm(at: realmPath, markerName: "On disk")

        let payload = decodePayload(LegacyMigrator.buildPayloadPreservingOriginal(at: realmPath))
        let markers = payload["markers"] as? [[String: Any]]

        XCTAssertEqual(markers?.count, 1)
        XCTAssertEqual(markers?.first?["name"] as? String, "On disk")
    }

    func testBuildPayloadPreservingOriginalLeavesTheLegacyFileByteIdentical() {
        // The whole point of reading a copy: Realm would otherwise upgrade
        // the on-disk format in place, and the legacy database has to stay
        // usable by the legacy build.
        let realmPath = documentsPath + "/database.realm"
        writeOnDiskLegacyRealm(at: realmPath, markerName: "On disk")
        let before = try! Data(contentsOf: URL(fileURLWithPath: realmPath))

        XCTAssertNotNil(LegacyMigrator.buildPayloadPreservingOriginal(at: realmPath))

        let after = try! Data(contentsOf: URL(fileURLWithPath: realmPath))
        XCTAssertEqual(before, after)
    }

    func testBuildPayloadPreservingOriginalReturnsNilForAnUnreadableFile() {
        // Bytes that are not a realm at all: Realm refuses to open the copy,
        // so the import reports failure and gets retried on a later launch.
        let realmPath = documentsPath + "/database.realm"
        let garbage = Data("this is not a realm file".utf8)
        createFile(at: realmPath, contents: garbage)

        XCTAssertNil(LegacyMigrator.buildPayloadPreservingOriginal(at: realmPath))
        // Even a failed read leaves the file exactly as it was.
        XCTAssertEqual(try? Data(contentsOf: URL(fileURLWithPath: realmPath)), garbage)
    }

    func testBuildPayloadPreservingOriginalTreatsAnEmptyFileAsAnEmptyDatabase() {
        // Realm initialises a zero-length file into a fresh empty realm
        // rather than failing. That happens to the throwaway copy, so the
        // import sees no markers or routes and the original stays empty.
        let realmPath = documentsPath + "/database.realm"
        createFile(at: realmPath)

        let payload = decodePayload(LegacyMigrator.buildPayloadPreservingOriginal(at: realmPath))

        XCTAssertEqual((payload["markers"] as? [[String: Any]])?.count, 0)
        XCTAssertEqual((payload["routes"] as? [[String: Any]])?.count, 0)
        XCTAssertEqual(try? Data(contentsOf: URL(fileURLWithPath: realmPath)), Data())
    }

    func testBuildPayloadPreservingOriginalCleansUpItsScratchCopy() {
        let realmPath = documentsPath + "/database.realm"
        writeOnDiskLegacyRealm(at: realmPath, markerName: "On disk")

        let tempDirectory = NSTemporaryDirectory()
        let before = Set((try? FileManager.default.contentsOfDirectory(atPath: tempDirectory)) ?? [])
        XCTAssertNotNil(LegacyMigrator.buildPayloadPreservingOriginal(at: realmPath))
        let after = Set((try? FileManager.default.contentsOfDirectory(atPath: tempDirectory)) ?? [])

        XCTAssertTrue(
            after.subtracting(before).filter { $0.hasPrefix("LegacyMigration-") }.isEmpty,
            "Scratch copy left behind in \(tempDirectory)",
        )
    }

    // MARK: - runIfNeeded orchestration

    func testRunIfNeededFreshInstallNoRealmFileSetsDoneFlagWithoutImporting() {
        var importCalled = false

        LegacyMigrator.runIfNeeded(
            defaults: defaults,
            documentsPath: documentsPath,
            importDatabase: { _ in
                importCalled = true
                return 0
            },
        )

        XCTAssertFalse(importCalled)
        XCTAssertTrue(defaults.bool(forKey: "LegacyMigrationDone"))
    }

    func testRunIfNeededSuccessfulImportRunsSettingsKeepsLegacyDataAndSetsDone() {
        let realmPath = documentsPath + "/database.realm"
        createFile(at: realmPath)
        defaults.set(true, forKey: "GDASettingsMetric")

        var callCount = 0
        var capturedPath: String?

        LegacyMigrator.runIfNeeded(
            defaults: defaults,
            documentsPath: documentsPath,
            importDatabase: { path in
                callCount += 1
                capturedPath = path
                return 2
            },
        )

        XCTAssertEqual(callCount, 1)
        XCTAssertEqual(capturedPath, realmPath)
        // migrateSettings ran.
        XCTAssertEqual(defaults.string(forKey: "MeasurementUnits"), "Metric")
        // ...and left the legacy database and settings behind.
        XCTAssertTrue(FileManager.default.fileExists(atPath: realmPath))
        XCTAssertTrue(defaults.bool(forKey: "GDASettingsMetric"))
        XCTAssertTrue(defaults.bool(forKey: "LegacyMigrationDone"))
    }

    func testRunIfNeededFailedImportLeavesArtefactsAndDoesNotSetDone() {
        let realmPath = documentsPath + "/database.realm"
        createFile(at: realmPath)

        LegacyMigrator.runIfNeeded(
            defaults: defaults,
            documentsPath: documentsPath,
            importDatabase: { _ in -1 },
        )

        XCTAssertTrue(FileManager.default.fileExists(atPath: realmPath))
        XCTAssertFalse(defaults.bool(forKey: "LegacyMigrationDone"))
    }

    func testRunIfNeededAlreadyDoneIsNoOp() {
        defaults.set(true, forKey: "LegacyMigrationDone")
        createFile(at: documentsPath + "/database.realm")
        var importCalled = false

        LegacyMigrator.runIfNeeded(
            defaults: defaults,
            documentsPath: documentsPath,
            importDatabase: { _ in
                importCalled = true
                return 0
            },
        )

        XCTAssertFalse(importCalled)
    }

    func testRunIfNeededAlwaysRunsHygieneSweepRegardlessOfBranch() {
        // A Date is one of the types the sweep still drops — GDA* keys are
        // exempt now, so they can't be used as the marker any more.
        // Already-done branch: hygiene sweep still fires even though import is skipped.
        defaults.set(true, forKey: "LegacyMigrationDone")
        defaults.set(Date(), forKey: "StaleTimestamp")
        LegacyMigrator.runIfNeeded(
            defaults: defaults,
            documentsPath: documentsPath,
            importDatabase: { _ in 0 },
        )
        XCTAssertNil(defaults.object(forKey: "StaleTimestamp"))

        // Fresh-install branch (no database.realm at documentsPath): hygiene sweep still fires.
        let freshSuiteName = "LegacyMigratorDatabaseTests-fresh-" + UUID().uuidString
        let freshDefaults = UserDefaults(suiteName: freshSuiteName)!
        defer { freshDefaults.removePersistentDomain(forName: freshSuiteName) }
        freshDefaults.set(Date(), forKey: "StaleTimestamp")

        LegacyMigrator.runIfNeeded(
            defaults: freshDefaults,
            documentsPath: documentsPath,
            importDatabase: { _ in 0 },
        )
        XCTAssertNil(freshDefaults.object(forKey: "StaleTimestamp"))
    }
}
