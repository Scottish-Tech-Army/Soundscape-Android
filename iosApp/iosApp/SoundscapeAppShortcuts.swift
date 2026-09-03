import AppIntents

/// Makes the intents usable by voice with no setup: these phrases work as soon as the app
/// is installed, without the user having to build a shortcut first.
///
/// One shortcut per AudioMenu group, not one per action. The system allows ten, and eleven
/// separately-worded sentences competing for them is more than anyone will remember —
/// especially without a screen to browse. Every phrase is the same shape,
/// "Soundscape <group> <choice>", so remembering the four group words is enough to reach
/// everything; the choice within a group is a parameter Siri will prompt for if it is left
/// off, which makes "Soundscape surroundings" a usable command in its own right.
///
/// Every phrase must contain \(.applicationName) — the system requires the app name so it
/// can tell which app a phrase belongs to — and putting it first turns that requirement
/// into the mnemonic rather than a suffix tacked on.
///
/// Six of ten slots used. The three unparameterised leaf actions that lost their own
/// phrases still exist as intents, reachable from the Shortcuts app and any phrase the
/// user builds there.
///
/// Phrases are English-only. They resolve against the app bundle rather than the shared
/// Compose resources, so localizing them is separate work from the app's translations.
struct SoundscapeAppShortcuts: AppShortcutsProvider {

    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: SurroundingsIntent(),
            phrases: [
                "\(.applicationName) surroundings \(\.$kind)",
                "\(.applicationName) surroundings",
            ],
            shortTitle: "Surroundings",
            systemImageName: "circle.dotted"
        )

        AppShortcut(
            intent: RouteControlIntent(),
            phrases: [
                "\(.applicationName) route \(\.$command)",
                "\(.applicationName) route",
            ],
            shortTitle: "Route",
            systemImageName: "figure.walk.circle"
        )

        AppShortcut(
            intent: StartRouteIntent(),
            phrases: [
                "\(.applicationName) start route \(\.$route)",
                "\(.applicationName) start route",
            ],
            shortTitle: "Start Route",
            systemImageName: "play.circle"
        )

        AppShortcut(
            intent: StartBeaconIntent(),
            phrases: [
                "\(.applicationName) beacon \(\.$marker)",
                "\(.applicationName) beacon",
            ],
            shortTitle: "Beacon",
            systemImageName: "dot.radiowaves.left.and.right"
        )

        AppShortcut(
            intent: ListIntent(),
            phrases: [
                "\(.applicationName) list \(\.$kind)",
                "\(.applicationName) list",
            ],
            shortTitle: "List",
            systemImageName: "list.bullet.circle"
        )

        AppShortcut(
            intent: StopBeaconIntent(),
            phrases: [
                "\(.applicationName) stop beacon",
            ],
            shortTitle: "Stop Beacon",
            systemImageName: "xmark.circle"
        )
    }
}
