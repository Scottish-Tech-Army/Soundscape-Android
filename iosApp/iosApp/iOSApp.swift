import SwiftUI
import Shared

@main
struct iOSApp: App {
    @StateObject private var splashCoordinator = SplashCoordinator()

    init() {
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
            .onAppear { splashCoordinator.start() }
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
