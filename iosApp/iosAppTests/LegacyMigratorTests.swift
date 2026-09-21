//
//  LegacyMigratorTests.swift
//
//  Unit tests for LegacyMigrator's settings translation and the
//  defaults-hygiene sweep that protects ComposePreference from values it
//  can't classify. Each test gets its own UserDefaults suite so they
//  don't leak across or pollute the real app domain.
//

import XCTest
@testable import Soundscape

final class LegacyMigratorTests: XCTestCase {

    private var suiteName: String!
    private var defaults: UserDefaults!

    override func setUp() {
        super.setUp()
        suiteName = "LegacyMigratorTests-" + UUID().uuidString
        defaults = UserDefaults(suiteName: suiteName)!
    }

    override func tearDown() {
        defaults.removePersistentDomain(forName: suiteName)
        defaults = nil
        suiteName = nil
        super.tearDown()
    }

    // MARK: - migrateSettings

    func testMetricFlagBecomesMeasurementUnitsString() {
        defaults.set(true, forKey: "GDASettingsMetric")
        LegacyMigrator.migrateSettings(defaults: defaults)
        XCTAssertEqual(defaults.string(forKey: "MeasurementUnits"), "Metric")

        defaults.set(false, forKey: "GDASettingsMetric")
        LegacyMigrator.migrateSettings(defaults: defaults)
        XCTAssertEqual(defaults.string(forKey: "MeasurementUnits"), "Imperial")
    }

    func testLocaleIdentifierWritesBothSearchLanguageAndAppleLanguages() {
        defaults.set("fr-FR", forKey: "GDASettingsLocaleIdentifier")
        LegacyMigrator.migrateSettings(defaults: defaults)

        XCTAssertEqual(defaults.string(forKey: "SearchLanguage"), "fr-FR")
        XCTAssertEqual(defaults.array(forKey: "AppleLanguages") as? [String], ["fr-FR"])
    }

    func testEmptyLocaleIdentifierIsIgnored() {
        // AppleLanguages is inherited from NSGlobalDomain so we can't
        // assert nil here — instead, check the migrator doesn't write
        // anything new on top of it. Capture before/after.
        let appleLangsBefore = defaults.array(forKey: "AppleLanguages") as? [String]
        defaults.set("", forKey: "GDASettingsLocaleIdentifier")

        LegacyMigrator.migrateSettings(defaults: defaults)

        XCTAssertNil(defaults.string(forKey: "SearchLanguage"))
        XCTAssertEqual(defaults.array(forKey: "AppleLanguages") as? [String], appleLangsBefore)
    }

    func testSpeakingRateRescalesProportionally() {
        // Legacy default 0.55 maps to the new default 1.0.
        defaults.set(Float(0.55), forKey: "GDASettingsSpeakingRate")
        LegacyMigrator.migrateSettings(defaults: defaults)
        XCTAssertEqual(defaults.float(forKey: "SpeechRate"), 1.0, accuracy: 0.001)

        // Half of legacy default → half the new default.
        defaults.set(Float(0.275), forKey: "GDASettingsSpeakingRate")
        LegacyMigrator.migrateSettings(defaults: defaults)
        XCTAssertEqual(defaults.float(forKey: "SpeechRate"), 0.5, accuracy: 0.001)
    }

    func testSpeakingRateClampsToNewSliderRange() {
        // The legacy slider's nominal range is 0.0–1.0, but UserDefaults
        // holds whatever float ends up there. The clamp protects the new
        // app from out-of-range or corrupt values; exercise both ends.

        // Legacy 2.0 (corrupt / out-of-range) rescales to ~3.6, clamps to 2.0.
        defaults.set(Float(2.0), forKey: "GDASettingsSpeakingRate")
        LegacyMigrator.migrateSettings(defaults: defaults)
        XCTAssertEqual(defaults.float(forKey: "SpeechRate"), 2.0, accuracy: 0.001)

        // Legacy 0.1 rescales to ~0.18, clamps to 0.5.
        defaults.set(Float(0.1), forKey: "GDASettingsSpeakingRate")
        LegacyMigrator.migrateSettings(defaults: defaults)
        XCTAssertEqual(defaults.float(forKey: "SpeechRate"), 0.5, accuracy: 0.001)
    }

    func testV2BeaconNameMapsToCurrent() {
        defaults.set("V2Beacon", forKey: "GDASelectedBeaconName")
        LegacyMigrator.migrateSettings(defaults: defaults)
        XCTAssertEqual(defaults.string(forKey: "BeaconType"), "Current")
    }

    func testClassicBeaconNameMapsToOriginal() {
        defaults.set("Classic", forKey: "GDASelectedBeaconName")
        LegacyMigrator.migrateSettings(defaults: defaults)
        XCTAssertEqual(defaults.string(forKey: "BeaconType"), "Original")
    }

    func testKnownNewCatalogueBeaconNamePassesThrough() {
        defaults.set("Tactile", forKey: "GDASelectedBeaconName")
        LegacyMigrator.migrateSettings(defaults: defaults)
        XCTAssertEqual(defaults.string(forKey: "BeaconType"), "Tactile")
    }

    func testUnknownBeaconNameFallsBackToCurrent() {
        defaults.set("ExperimentalCowBell", forKey: "GDASelectedBeaconName")
        LegacyMigrator.migrateSettings(defaults: defaults)
        XCTAssertEqual(defaults.string(forKey: "BeaconType"), "Current")
    }

    func testMasterCalloutToggleCopiesAcross() {
        defaults.set(false, forKey: "GDASettingsAutomaticCalloutsEnabled")
        LegacyMigrator.migrateSettings(defaults: defaults)
        XCTAssertFalse(defaults.bool(forKey: "AllowCallouts"))
    }

    func testPlacesAndLandmarksOrMergesEitherEnabled() {
        defaults.set(false, forKey: "GDASettingsPlaceSenseEnabled")
        defaults.set(true, forKey: "GDASettingsLandmarkSenseEnabled")
        LegacyMigrator.migrateSettings(defaults: defaults)
        XCTAssertTrue(defaults.bool(forKey: "PlaceAndLandmarks"))
    }

    func testPlacesAndLandmarksFalseOnlyWhenBothOff() {
        defaults.set(false, forKey: "GDASettingsPlaceSenseEnabled")
        defaults.set(false, forKey: "GDASettingsLandmarkSenseEnabled")
        LegacyMigrator.migrateSettings(defaults: defaults)
        XCTAssertFalse(defaults.bool(forKey: "PlaceAndLandmarks"))
    }

    func testPlacesAndLandmarksDefaultsOnWhenLegacyKeysAbsent() {
        // No GDA keys set — legacy default for both was true, so the
        // OR-merge should yield true.
        LegacyMigrator.migrateSettings(defaults: defaults)
        XCTAssertTrue(defaults.bool(forKey: "PlaceAndLandmarks"))
    }

    func testMobilityCopiesAcross() {
        defaults.set(false, forKey: "GDASettingsMobilitySenseEnabled")
        LegacyMigrator.migrateSettings(defaults: defaults)
        XCTAssertFalse(defaults.bool(forKey: "Mobility"))
    }

    func testMixAudioCopiesAcross() {
        defaults.set(true, forKey: "GDAAudioSessionMixesWithOthers")
        LegacyMigrator.migrateSettings(defaults: defaults)
        XCTAssertTrue(defaults.bool(forKey: "MixAudio"))
    }

    func testMarkerSortAlphanumericMapsToTrue() {
        defaults.set("alphanumeric", forKey: "GDAMarkerSortStyle")
        LegacyMigrator.migrateSettings(defaults: defaults)
        XCTAssertTrue(defaults.bool(forKey: "MarkersSortByName"))
    }

    func testMarkerSortDistanceMapsToFalse() {
        defaults.set("distance", forKey: "GDAMarkerSortStyle")
        LegacyMigrator.migrateSettings(defaults: defaults)
        XCTAssertFalse(defaults.bool(forKey: "MarkersSortByName"))
    }

    func testFirstLaunchFlipsToFalseWhenLegacyOnboardingComplete() {
        defaults.set(true, forKey: "GDAFirstLaunchDidComplete")
        LegacyMigrator.migrateSettings(defaults: defaults)
        XCTAssertFalse(defaults.bool(forKey: "FirstLaunch"))
    }

    func testFirstLaunchUntouchedWhenLegacyOnboardingIncomplete() {
        // Don't set GDAFirstLaunchDidComplete. The migrator must not write
        // FirstLaunch — the new app's default-true should win on first
        // read.
        LegacyMigrator.migrateSettings(defaults: defaults)
        XCTAssertNil(defaults.object(forKey: "FirstLaunch"))
    }

    func testTranslatedLegacyKeysAreLeftInPlace() {
        // Translation reads the legacy settings but never removes them —
        // they stay behind for rollback to the legacy build and for support.
        defaults.set(true, forKey: "GDASettingsMetric")
        defaults.set(true, forKey: "GDAAudioSessionMixesWithOthers")
        defaults.set("V2Beacon", forKey: "GDASelectedBeaconName")
        defaults.set(true, forKey: "GDASettingsAPNsDeviceToken") // a non-translated noisy one

        LegacyMigrator.migrateSettings(defaults: defaults)

        XCTAssertTrue(defaults.bool(forKey: "GDASettingsMetric"))
        XCTAssertTrue(defaults.bool(forKey: "GDAAudioSessionMixesWithOthers"))
        XCTAssertEqual(defaults.string(forKey: "GDASelectedBeaconName"), "V2Beacon")
        XCTAssertTrue(defaults.bool(forKey: "GDASettingsAPNsDeviceToken"))
    }

    // MARK: - sweepIncompatibleDefaults

    func testSweepKeepsEveryGDAKeyWhateverItsType() {
        // Legacy settings are left in place, including the ones holding
        // types the sweep would otherwise drop.
        defaults.set(true, forKey: "GDADidAddDevice_BoseAR")
        defaults.set("ignored", forKey: "GDABannerDidDismissForKey_DeviceReachability_Foo")
        defaults.set(Data([0x01, 0x02]), forKey: "GDASettingsAPNsDeviceToken")
        defaults.set(Date(), forKey: "GDASomeTimestamp")

        LegacyMigrator.sweepIncompatibleDefaults(defaults: defaults)

        XCTAssertTrue(defaults.bool(forKey: "GDADidAddDevice_BoseAR"))
        XCTAssertEqual(
            defaults.string(forKey: "GDABannerDidDismissForKey_DeviceReachability_Foo"),
            "ignored",
        )
        XCTAssertNotNil(defaults.object(forKey: "GDASettingsAPNsDeviceToken"))
        XCTAssertNotNil(defaults.object(forKey: "GDASomeTimestamp"))
    }

    func testSweepRemovesNonPrimitiveValues() {
        // ComposePreference can't classify Data, Date, or Dictionary — these
        // crash the iOS preference flow on read. The sweep removes anything
        // that isn't NSNumber/NSString/[String].
        defaults.set(Data([0x62, 0x70, 0x6c, 0x69]), forKey: "SomeBplistBlob")
        defaults.set(Date(), forKey: "SomeDate")
        defaults.set(["one": 1, "two": 2], forKey: "SomeDict")

        LegacyMigrator.sweepIncompatibleDefaults(defaults: defaults)

        XCTAssertNil(defaults.object(forKey: "SomeBplistBlob"))
        XCTAssertNil(defaults.object(forKey: "SomeDate"))
        XCTAssertNil(defaults.object(forKey: "SomeDict"))
    }

    func testSweepKeepsPrimitiveValues() {
        defaults.set(true, forKey: "AllowCallouts")
        defaults.set(Float(1.5), forKey: "SpeechRate")
        defaults.set("Current", forKey: "BeaconType")
        defaults.set(["en-GB"], forKey: "AppleLanguages") // also Apple-prefixed

        LegacyMigrator.sweepIncompatibleDefaults(defaults: defaults)

        XCTAssertTrue(defaults.bool(forKey: "AllowCallouts"))
        XCTAssertEqual(defaults.float(forKey: "SpeechRate"), 1.5, accuracy: 0.001)
        XCTAssertEqual(defaults.string(forKey: "BeaconType"), "Current")
        XCTAssertEqual(defaults.array(forKey: "AppleLanguages") as? [String], ["en-GB"])
    }

    func testSweepLeavesAppleAndSystemDataAlone() {
        // Even unsupported types under system prefixes are left alone —
        // the sweep is for app-controlled keys, not system keys.
        defaults.set(Data([0x00, 0x01]), forKey: "AppleSomeOpaqueState")
        defaults.set(Date(), forKey: "NSSomeSystemTimestamp")

        LegacyMigrator.sweepIncompatibleDefaults(defaults: defaults)

        XCTAssertNotNil(defaults.object(forKey: "AppleSomeOpaqueState"))
        XCTAssertNotNil(defaults.object(forKey: "NSSomeSystemTimestamp"))
    }

    func testSweepIsIdempotent() {
        defaults.set(Data([0x62]), forKey: "BogusBlob")
        defaults.set("ok", forKey: "Keeper")

        LegacyMigrator.sweepIncompatibleDefaults(defaults: defaults)
        LegacyMigrator.sweepIncompatibleDefaults(defaults: defaults)

        XCTAssertNil(defaults.object(forKey: "BogusBlob"))
        XCTAssertEqual(defaults.string(forKey: "Keeper"), "ok")
    }
}
