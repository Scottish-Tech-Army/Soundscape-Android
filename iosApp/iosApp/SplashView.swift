import AVFoundation
import SwiftUI

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

@MainActor
final class SplashCoordinator: ObservableObject {
    @Published var isVisible: Bool = true

    private var player: AVAudioPlayer?
    private var audioDelegate: SplashAudioDelegate?
    private var didFinishAudio = false
    private var minDelayPassed = false
    private var hasStarted = false

    func start() {
        guard !hasStarted else { return }
        hasStarted = true

        let currentMinor = currentMinorVersion()
        let storedMinor = UserDefaults.standard.string(forKey: lastNewReleaseKey)

        if storedMinor == currentMinor {
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

        playSplashAudio()
    }

    private func playSplashAudio() {
        guard let url = Bundle.main.url(forResource: splashSoundResource, withExtension: splashSoundExtension) else {
            didFinishAudio = true
            dismissIfReady()
            return
        }
        do {
            // .playback so the splash audio is heard regardless of the silent
            // switch (MediaPlayer on Android plays through the media stream).
            // .mixWithOthers so we don't hijack any music the user is already
            // playing. AVAudioSession is iOS-only, hence the os guard.
            #if os(iOS)
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, options: [.mixWithOthers])
            try AVAudioSession.sharedInstance().setActive(true)
            #endif
            let p = try AVAudioPlayer(contentsOf: url)
            p.volume = 0.7
            let delegate = SplashAudioDelegate { [weak self] in
                Task { @MainActor [weak self] in
                    self?.didFinishAudio = true
                    self?.dismissIfReady()
                }
            }
            p.delegate = delegate
            audioDelegate = delegate
            p.prepareToPlay()
            if !p.play() {
                didFinishAudio = true
                dismissIfReady()
                return
            }
            player = p
        } catch {
            didFinishAudio = true
            dismissIfReady()
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
        #if os(iOS)
        try? AVAudioSession.sharedInstance().setActive(false, options: [.notifyOthersOnDeactivation])
        #endif
    }

    private func currentMinorVersion() -> String {
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
