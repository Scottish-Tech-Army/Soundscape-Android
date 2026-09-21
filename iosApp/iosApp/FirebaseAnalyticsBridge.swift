import Foundation
import FirebaseCore
import FirebaseAnalytics
import FirebaseCrashlytics
import Shared

// Firebase wiring for iOS. Mirrors the Android FirebaseAnalyticsImpl.
//
// Gating (matches app/src/main/.../MainActivity.kt):
//   - Debug builds: never initialise Firebase.
//   - XCTest runs (local unit tests or Firebase Test Lab): never initialise.
//   - Release, non-test: initialise Firebase and inject the bridge into
//     IosSoundscapeService before the shared code fetches the singleton.

enum FirebaseBootstrap {
    static func configureIfEnabled() {
        guard shouldEnableFirebase() else { return }
        FirebaseApp.configure()
        IosSoundscapeService.companion.setAnalyticsFactory(factory: {
            FirebaseAnalyticsBridge()
        })
    }
}

private func shouldEnableFirebase() -> Bool {
    #if DEBUG
    return false
    #else
    let env = ProcessInfo.processInfo.environment
    if env["XCTestConfigurationFilePath"] != nil { return false }
    if NSClassFromString("XCTestCase") != nil { return false }
    return true
    #endif
}

final class FirebaseAnalyticsBridge: NSObject, SoundscapeAnalytics {

    // Kotlin `Map<String, Any?>?` reaches Swift as `[String: Any]?` via NSDictionary.
    // Nullable Kotlin values arrive as NSNull, which Firebase rejects, so strip them.
    private func sanitize(_ params: [String: Any]?) -> [String: Any]? {
        guard let params else { return nil }
        var out: [String: Any] = [:]
        for (k, v) in params where !(v is NSNull) { out[k] = v }
        return out.isEmpty ? nil : out
    }

    func logEvent(name: String, params: [String: Any]?) {
        FirebaseAnalytics.Analytics.logEvent(name, parameters: sanitize(params))
    }

    func logCostlyEvent(name: String, params: [String: Any]?) {
        // Mirror Android: costly events are dropped to control monthly event count.
    }

    func crashSetCustomKey(key: String, value: String) {
        Crashlytics.crashlytics().setCustomValue(value, forKey: key)
    }

    func crashLogNotes(name: String) {
        Crashlytics.crashlytics().log(name)
    }
}
