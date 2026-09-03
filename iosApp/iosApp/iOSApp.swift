import AppIntents
import Shared
import SwiftUI

@main
struct iOSApp: App {
    @StateObject private var splashCoordinator = SplashCoordinator()
    @Environment(\.scenePhase) private var scenePhase

    init() {
        // Must be first: installs Kotlin/Native's setUnhandledExceptionHook so
        // any uncaught exception thrown during LegacyMigrator, FirebaseBootstrap,
        // or IosSoundscapeService construction (or any coroutine they spawn) is
        // written to stderr before the process dies, instead of vanishing.
        UnhandledExceptionLoggerKt.installUnhandledExceptionLogger()

        // Run the legacy → multiplatform data migration before the Compose
        // UI mounts. Synchronous so the new app's preferences and Room
        // database are populated before MainViewController reads them.
        // See LegacyMigrator.swift.
        LegacyMigrator.runIfNeeded()

        // Initialise Firebase and inject the analytics bridge into the
        // shared iOS singleton. Skipped on Debug/XCTest.
        // See FirebaseAnalyticsBridge.swift.
        FirebaseBootstrap.configureIfEnabled()
    }

    var body: some Scene {
        WindowGroup {
            ZStack {
                ComposeView()
                .ignoresSafeArea() // disables default platform insets so shared Compose UI can control its own padding and layout.
                    .onOpenURL { url in
                        IntentBridge.shared.handle(url: url)
                    }
                    .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { activity in
                        if let url = activity.webpageURL {
                            IntentBridge.shared.handle(url: url)
                        }
                    }

                if splashCoordinator.isVisible {
                    SplashView()
                        .transition(.opacity)
                }
            }
            .onAppear {
                splashCoordinator.start()
                // Installed here rather than in iOSApp.init(): editing markers and routes
                // needs the UI, and init() also runs for the background launches the
                // system makes to perform an intent, where reaching for the service would
                // construct the whole thing just to hang a callback on it.
                IosSoundscapeService.companion.getInstance().onMarkersOrRoutesChanged = {
                    refreshSiriParameters("markers or routes changed")
                }
            }
            .onChange(of: scenePhase) { phase in
                // Covers a fresh install, where no data has changed but the vocabulary has
                // never been built. scenePhase rather than didBecomeActiveNotification via
                // onReceive: that only delivers notifications posted after the view
                // subscribes, and at launch the notification can beat SwiftUI mounting it.
                if phase == .active { refreshSiriParameters("scene became active") }
            }
        }
    }
}

/// Rebuilds the vocabulary Siri matches spoken route and marker names against, from the
/// entities' suggestedEntities(). Without it a parameterised phrase still reaches the
/// right intent, but the system has nothing to map "Post Office" onto and falls back to
/// asking which one.
///
/// Called when a marker or route changes, via the service's onMarkersOrRoutesChanged
/// hook, and once when a scene becomes active. Data changes are the trigger Apple
/// documents, and the reason this stopped being a lifecycle-only affair: a marker saved
/// mid-session was unrecognisable by voice until something rebuilt the vocabulary, and
/// because an unmatched name means the phrase itself fails to match, the result was Siri
/// doing nothing at all rather than offering a list.
///
/// Never called from iOSApp.init(), which runs on every process launch including the
/// background ones the system makes to perform an intent — refreshing there would set it
/// re-querying entities while the intent it launched us for is still running.
private func refreshSiriParameters(_ reason: String) {
    // The markers-or-routes hook fires from a Kotlin coroutine on Dispatchers.Default, so
    // this can arrive on a background thread. AppIntents' registration APIs expect the
    // main thread, and off it the call is liable to be dropped — which looks exactly like
    // a refresh that ran but had no effect.
    guard Thread.isMainThread else {
        siriLog.notice("refresh off-main (\(reason, privacy: .public)); hopping to main")
        DispatchQueue.main.async { refreshSiriParameters(reason) }
        return
    }

    // Pair this with the suggestedEntities lines that should follow it. A refresh logged
    // with no query after it means the system ignored or deferred the request; a query
    // whose count is missing the marker just saved means it re-read stale data. The two
    // failures look identical from the outside and need different fixes.
    siriLog.notice("updateAppShortcutParameters (\(reason, privacy: .public))")
    SoundscapeAppShortcuts.updateAppShortcutParameters()
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
