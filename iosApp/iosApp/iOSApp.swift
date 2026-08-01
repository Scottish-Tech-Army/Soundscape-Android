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
    }

    var body: some Scene {
        WindowGroup {
            ZStack {
                ComposeView()
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
