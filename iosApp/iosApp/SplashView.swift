import AVFoundation
import Shared
import SwiftUI
import UIKit

// Mirrors Android's MainActivity splash flow (app/src/main/.../MainActivity.kt):
// the dt_soundscape audio plays once per minor version, gated on the
// "LastNewRelease" preference key (also written by SharedNewReleaseDialog),
// and the splash UI stays up for at least 1.5s so the Double Tap attribution
// is readable.
private let lastNewReleaseKey = "LastNewRelease"
private let splashSoundResource = "dt_soundscape"
private let splashSoundExtension = "mp3"
private let attributionMinDelay: TimeInterval = 1.5
private let alreadyPlayedDelay: TimeInterval = 0.3
// Safety-net ceiling on how long we wait for the clip. Must stay comfortably above the
// ~3.0s dt_soundscape.mp3 runs to, or the gate opens while it is still playing and the
// overlap this gate exists to prevent comes back. A little longer than Android's 3500ms
// (MainActivity.kt) because the timer starts before AVAudioPlayer has been built.
private let maxSplashDelay: TimeInterval = 5.0

@MainActor
final class SplashCoordinator: ObservableObject {
    @Published var isVisible: Bool = true

    private var player: AVAudioPlayer?
    private var audioDelegate: SplashAudioDelegate?
    private var didFinishAudio = false
    private var minDelayPassed = false
    private var hasStarted = false
    private var didReleaseGeoEngine = false

    func start() {
        guard !hasStarted else { return }
        hasStarted = true

        let currentMinor = Self.currentMinorVersion()
        let storedMinor = UserDefaults.standard.string(forKey: lastNewReleaseKey)

        if storedMinor == currentMinor {
            // No hold was taken in this case (holdGeoEngineForSplashIfNeeded makes the
            // same check), but releasing is a no-op when there is nothing held and keeps
            // the two checks from having to stay in step forever.
            releaseGeoEngine()
            DispatchQueue.main.asyncAfter(deadline: .now() + alreadyPlayedDelay) { [weak self] in
                self?.dismiss()
            }
            return
        }

        // Write immediately so a crash mid-audio doesn't replay the splash on
        // the next launch — matches MainActivity.kt:319-321.
        UserDefaults.standard.set(currentMinor, forKey: lastNewReleaseKey)

        DispatchQueue.main.asyncAfter(deadline: .now() + attributionMinDelay) { [weak self] in
            self?.minDelayPassed = true
            self?.dismissIfReady()
        }

        // Safety net, mirroring MainActivity.kt's maxSplashDelay: if the player never
        // reports completion — it was reclaimed, or the file decodes to silence — stop
        // waiting on it, so the splash lifts and the geo engine starts anyway. Any audio
        // still playing carries on; we only stop gating on it.
        DispatchQueue.main.asyncAfter(deadline: .now() + maxSplashDelay) { [weak self] in
            self?.finishAudio()
        }

        playSplashAudio()
    }

    /// The Double Tap clip is done (or we have stopped waiting on it): lift the splash's
    /// audio gate and let the geo engine start. Idempotent — the player's delegate, the
    /// failure paths and the safety-net timer can all arrive here.
    private func finishAudio() {
        didFinishAudio = true
        releaseGeoEngine()
        dismissIfReady()
    }

    /// Releases the hold taken in `holdGeoEngineForSplashIfNeeded()`, so the shared
    /// service starts the geo engine now rather than putting its first TTS callout on
    /// top of the Double Tap clip.
    private func releaseGeoEngine() {
        guard !didReleaseGeoEngine else { return }
        didReleaseGeoEngine = true
        IosSoundscapeService.companion.releaseGeoEngineStartAfterSplash()
    }

    private func playSplashAudio() {
        guard let url = Bundle.main.url(forResource: splashSoundResource, withExtension: splashSoundExtension) else {
            finishAudio()
            return
        }
        // Delegate AVAudioSession setup to IosAudioEngine so the splash player
        // and the shared engine don't race each other on setCategory/setActive.
        // The engine owns the session lifecycle from here on — we never call
        // setActive(false) below, since the engine is about to start using it.
        IosSoundscapeService.companion.getInstance().audioEngine.configureAudioSession()
        do {
            let p = try AVAudioPlayer(contentsOf: url)
            p.volume = 0.7
            let delegate = SplashAudioDelegate { [weak self] in
                Task { @MainActor [weak self] in
                    self?.finishAudio()
                }
            }
            p.delegate = delegate
            audioDelegate = delegate
            p.prepareToPlay()
            if !p.play() {
                finishAudio()
                return
            }
            player = p
        } catch {
            finishAudio()
        }
    }

    private func dismissIfReady() {
        guard didFinishAudio && minDelayPassed else { return }
        dismiss()
    }

    private func dismiss() {
        guard isVisible else { return }
        withAnimation(.easeOut(duration: 0.25)) {
            isVisible = false
        }
        player = nil
        audioDelegate = nil
    }

    fileprivate static func currentMinorVersion() -> String {
        // Must mirror PlatformInfo.appVersionName()/appVersionMinorTrimmed() exactly:
        // CI pre-trims MARKETING_VERSION (CFBundleShortVersionString) to major.minor for
        // TestFlight grouping, while AppVersionName keeps the full major.minor.patch. Reading
        // CFBundleShortVersionString here would trim an already-trimmed value on those builds,
        // producing a different string than the Kotlin side writes to the same "LastNewRelease"
        // key and causing this splash and the release-notes dialog to fight over it forever.
        let raw = (Bundle.main.infoDictionary?["AppVersionName"] as? String)
            ?? (Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String)
            ?? "0.0.0"
        if let lastDot = raw.range(of: ".", options: .backwards) {
            return String(raw[..<lastDot.lowerBound])
        }
        return raw
    }
}

extension SplashCoordinator {
    /// Whether this launch is going to play the Double Tap clip — the same check `start()`
    /// makes, without any of its side effects.
    static func splashAudioPending() -> Bool {
        UserDefaults.standard.string(forKey: lastNewReleaseKey) != currentMinorVersion()
    }

    /// Asks the shared service to hold its geo engine start until the Double Tap clip has
    /// finished, so the clip and the engine's first TTS callout don't talk over each other.
    ///
    /// Called from `iOSApp.init()` rather than from `start()`: the hold only has an effect
    /// if it is in place before the first `IosSoundscapeService.getInstance()`, and by the
    /// time the splash view appears ComposeView may already have built the service.
    ///
    /// Skipped on a background launch — that is the system running an App Intent, where no
    /// splash appears and so nothing would release the hold. (The service's own timeout
    /// would eventually, but Siri shouldn't be made to wait it out.)
    static func holdGeoEngineForSplashIfNeeded() {
        guard UIApplication.shared.applicationState != .background else { return }
        guard splashAudioPending() else { return }
        IosSoundscapeService.companion.holdGeoEngineStartForSplash()
    }
}

private final class SplashAudioDelegate: NSObject, AVAudioPlayerDelegate {
    private let onFinish: () -> Void
    init(onFinish: @escaping () -> Void) { self.onFinish = onFinish }
    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) { onFinish() }
    func audioPlayerDecodeErrorDidOccur(_ player: AVAudioPlayer, error: Error?) { onFinish() }
}

struct SplashView: View {
    var body: some View {
        ZStack {
            Color("SplashBackground").ignoresSafeArea()
            VStack(spacing: 0) {
                Spacer()
                Image("SoundscapeLogo")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 200, height: 200)
                    .accessibilityHidden(true)
                Spacer()
                Image("DoubleTapBranding")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(maxWidth: 240)
                    .padding(.bottom, 48)
                    .accessibilityLabel("Presented by Double Tap")
            }
        }
    }
}
