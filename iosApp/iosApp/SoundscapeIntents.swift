import AppIntents
import OSLog
import Shared

/// Shared log for the Siri integration. Unified logging rather than print so it can be
/// filtered on a device in Console.app — subsystem org.scottishtecharmy.soundscape,
/// category Siri — without a debugger attached.
///
/// Counts are logged .public because they are what tell you whether a refresh actually
/// saw a newly-saved marker. Names are user data, so they are only made readable in
/// DEBUG builds; in Release they redact to <private>.
let siriLog = Logger(subsystem: "org.scottishtecharmy.soundscape", category: "Siri")

/// A failed [SoundscapeAction], carrying the wording the shared executor produced so
/// Siri speaks Soundscape's own phrasing rather than a generic system error.
///
/// Throwing is how these intents report failure at all: success is deliberately
/// silent — the spatial callout is the answer, and it carries direction information
/// that a spoken confirmation could not — so there is no dialog to attach a message
/// to on the way out.
struct SoundscapeIntentError: Error, CustomLocalizedStringResourceConvertible {
    let message: String

    var localizedStringResource: LocalizedStringResource { "\(message)" }
}

enum SoundscapeIntentRunner {

    /// How long a callout may wait for a position fix and loaded map tiles.
    ///
    /// The intent can be what launches the app: IosSoundscapeService starts the geo
    /// engine from its init, but a fix and the first tile grid land a second or two
    /// after that. Without the wait, every cold "what's around me?" would report
    /// NotReady. Five seconds leaves headroom inside the window the system allows an
    /// intent, while still failing early enough to say something useful.
    private static let readyTimeoutMs: Int64 = 5_000

    /// Runs an action against the shared executor and translates its ActionResult
    /// into intent terms. Constructing the service also starts it, which is what
    /// makes a background launch work.
    ///
    /// Returns the confirmation the executor produced, or nil when the action's own
    /// audio is the answer — a callout speaks for itself, and so does skipping to the
    /// next waypoint, which the route player announces. Callers that have nothing to
    /// say discard it; the ones that do wrap it with `dialog(for:)`.
    @discardableResult
    static func run(_ action: SoundscapeAction) async throws -> String? {
        siriLog.notice("perform \(String(describing: action), privacy: .public)")
        let service = IosSoundscapeService.companion.getInstance()
        let result = try await service.actions.execute(
            action: action,
            readyTimeoutMs: readyTimeoutMs
        )
        siriLog.notice("  -> \(String(describing: result), privacy: .public)")

        switch result {
        case let ok as ActionResult.Ok:
            return ok.speech
        case let notReady as ActionResult.NotReady:
            throw SoundscapeIntentError(message: notReady.speech)
        case let notFound as ActionResult.NotFound:
            throw SoundscapeIntentError(message: notFound.speech)
        case let needsUi as ActionResult.NeedsUi:
            // No action in this set produces NeedsUi; handled so that adding one
            // which does can't fail silently.
            throw SoundscapeIntentError(
                message: needsUi.speech ?? "Open Soundscape to finish that."
            )
        default:
            throw SoundscapeIntentError(message: "Soundscape couldn't do that.")
        }
    }

    /// Wraps a confirmation for Siri to speak.
    ///
    /// Unlike the thrown errors above, a dialog on a *successful* result is spoken
    /// when the intent was invoked by voice, which is what these confirmations need
    /// to be worth anything to someone not looking at the screen. The fallback only
    /// guards against an empty bubble if an action ever returns Ok with no wording.
    static func dialog(for speech: String?) -> IntentDialog {
        guard let speech, !speech.isEmpty else { return IntentDialog("Done") }
        return IntentDialog("\(speech)")
    }
}


// MARK: - Menu groups
//
// The four AudioMenu groups become four intents, each taking the choice within the group
// as a parameter, rather than one intent per leaf action. That keeps the spoken commands
// to a single memorable shape — "Soundscape surroundings around me" — instead of eleven
// separately-remembered sentences competing for ten App Shortcut slots.
//
// The two fixed groups are AppEnum rather than AppEntity, which matters for more than
// tidiness: enum cases are known at compile time, so the system never has to be told they
// changed. Only routes and markers are genuinely dynamic and still need
// updateAppShortcutParameters().

enum CalloutKind: String, AppEnum, CaseIterable {
    case myLocation
    case aroundMe
    case aheadOfMe
    case nearbyMarkers

    static var typeDisplayRepresentation = TypeDisplayRepresentation(name: "Callout")

    /// Wording matches the app's own buttons: directions_my_location,
    /// help_orient_page_title, help_explore_page_title, callouts_nearby_markers.
    static var caseDisplayRepresentations: [CalloutKind: DisplayRepresentation] = [
        .myLocation: "My Location",
        .aroundMe: "Around Me",
        .aheadOfMe: "Ahead of Me",
        .nearbyMarkers: "Nearby Markers",
    ]

    var action: SoundscapeAction {
        switch self {
        case .myLocation: return SoundscapeAction.MyLocation.shared
        case .aroundMe: return SoundscapeAction.AroundMe.shared
        case .aheadOfMe: return SoundscapeAction.AheadOfMe.shared
        case .nearbyMarkers: return SoundscapeAction.NearbyMarkers.shared
        }
    }
}

enum RouteCommand: String, AppEnum, CaseIterable {
    case nextWaypoint
    case previousWaypoint
    case muteBeacon
    case stop

    static var typeDisplayRepresentation = TypeDisplayRepresentation(name: "Route Command")

    /// Mirrors AudioMenu's Route submenu: menu_route_next_waypoint,
    /// menu_route_previous_waypoint, beacon_action_mute_beacon,
    /// route_detail_action_stop_route.
    static var caseDisplayRepresentations: [RouteCommand: DisplayRepresentation] = [
        .nextWaypoint: "Next Waypoint",
        .previousWaypoint: "Previous Waypoint",
        .muteBeacon: "Mute Beacon",
        .stop: "Stop",
    ]

    var action: SoundscapeAction {
        switch self {
        case .nextWaypoint: return SoundscapeAction.NextWaypoint.shared
        case .previousWaypoint: return SoundscapeAction.PreviousWaypoint.shared
        case .muteBeacon: return SoundscapeAction.ToggleBeaconMute.shared
        case .stop: return SoundscapeAction.StopRoute.shared
        }
    }
}

enum ListKind: String, AppEnum, CaseIterable {
    case routes
    case markers
    case commands

    static var typeDisplayRepresentation = TypeDisplayRepresentation(name: "List")

    /// Markers, not beacons: a beacon is set *on* a marker, so the saved things being
    /// listed are markers. Keeps the wording in step with the rest of the app and with the
    /// reply itself, which reads "Available markers are,".
    static var caseDisplayRepresentations: [ListKind: DisplayRepresentation] = [
        .routes: "Routes",
        .markers: "Markers",
        .commands: "Commands",
    ]

    /// nil for .commands, which describes the Siri surface itself rather than anything in
    /// the database, and so has no shared action behind it.
    var action: SoundscapeAction? {
        switch self {
        case .routes: return SoundscapeAction.ListRoutes.shared
        case .markers: return SoundscapeAction.ListMarkers.shared
        case .commands: return nil
        }
    }
}

// MARK: - Intents
//
// All set openAppWhenRun = false: callouts, beacons and route control all play through
// Soundscape's own audio engine in the background, and pulling the user into the app
// would interrupt whatever they were doing for no benefit.
//
// Whether an intent speaks follows one rule: does the app already answer? Starting a
// route or beacon announces itself, spatialised at the destination; skipping a waypoint
// is announced by the route player; muting and stopping change or end the beacon tone,
// which is audible in itself. All of those stay silent. Only the list actions speak,
// being the only ones with no audio of their own. The executor supplies wording in every case and the
// platform decides — which is the point of it returning text rather than speaking.

struct SurroundingsIntent: AppIntent {
    static var title: LocalizedStringResource = "Hear My Surroundings"
    static var description = IntentDescription("Describes where you are and what is around you.")
    static var openAppWhenRun = false

    @Parameter(title: "Callout")
    var kind: CalloutKind

    static var parameterSummary: some ParameterSummary {
        Summary("Hear \(\.$kind)")
    }

    /// Silent on success: the spatial callout is the answer, and its positioning carries
    /// information a spoken confirmation could not.
    func perform() async throws -> some IntentResult {
        try await SoundscapeIntentRunner.run(kind.action)
        return .result()
    }
}

struct RouteControlIntent: AppIntent {
    static var title: LocalizedStringResource = "Control Route"
    static var description = IntentDescription("Skips waypoints, mutes the beacon, or stops the route.")
    static var openAppWhenRun = false

    @Parameter(title: "Command")
    var command: RouteCommand

    static var parameterSummary: some ParameterSummary {
        Summary("Route \(\.$command)")
    }

    /// Silent for the whole group. Skipping a waypoint is announced by the route player,
    /// and muting or stopping is audible by definition — the beacon tone changes or
    /// ceases, which is the feedback. Sharing one return type meant the alternative was a
    /// bare "Done" on the three that already answer for themselves, to keep a spoken
    /// "Route stopped" on the one that doesn't; the beacon falling silent covers that case
    /// well enough not to be worth the other three.
    func perform() async throws -> some IntentResult {
        try await SoundscapeIntentRunner.run(command.action)
        return .result()
    }
}

struct StartRouteIntent: AppIntent {
    static var title: LocalizedStringResource = "Start Route"
    static var description = IntentDescription("Starts one of your saved routes.")
    static var openAppWhenRun = false

    @Parameter(title: "Route")
    var route: SoundscapeRoute

    static var parameterSummary: some ParameterSummary {
        Summary("Start \(\.$route)")
    }

    /// Silent: RoutePlayer.play() already announces the first waypoint through
    /// createBeaconAtWaypoint, and it does so with AudioType.LOCALIZED — spatialised at
    /// the waypoint, so the announcement itself tells you which way to walk. Having Siri
    /// read a flat confirmation over the top would both duplicate it and drop the only
    /// part that carries direction.
    func perform() async throws -> some IntentResult {
        try await SoundscapeIntentRunner.run(
            SoundscapeAction.StartRouteById(routeId: Int64(route.id), reverse: false)
        )
        return .result()
    }
}

struct StartBeaconIntent: AppIntent {
    static var title: LocalizedStringResource = "Start Beacon"
    static var description = IntentDescription("Sets an audio beacon on one of your saved markers.")
    static var openAppWhenRun = false

    @Parameter(title: "Marker")
    var marker: SoundscapeMarker

    static var parameterSummary: some ParameterSummary {
        Summary("Set a beacon on \(\.$marker)")
    }

    /// Silent, for the same reason as StartRouteIntent: the beacon announces itself
    /// spatially as it starts.
    func perform() async throws -> some IntentResult {
        try await SoundscapeIntentRunner.run(
            SoundscapeAction.BeaconOnMarkerById(markerId: Int64(marker.id))
        )
        return .result()
    }
}

/// Kept out of the route group: stopping the beacon is not a route command, and folding it
/// in would have made "Soundscape route stop beacon" the way to reach it.
struct StopBeaconIntent: AppIntent {
    static var title: LocalizedStringResource = "Stop Beacon"
    static var description = IntentDescription("Switches the audio beacon off.")
    static var openAppWhenRun = false

    /// Silent: the beacon tone ceasing is the feedback, exactly as it is for the route
    /// group's stop.
    func perform() async throws -> some IntentResult {
        try await SoundscapeIntentRunner.run(SoundscapeAction.StopBeacon.shared)
        return .result()
    }
}

struct ListIntent: AppIntent {
    static var title: LocalizedStringResource = "List"
    static var description = IntentDescription("Reads back your saved routes, your saved markers, or the commands you can say.")
    static var openAppWhenRun = false

    @Parameter(title: "List")
    var kind: ListKind

    static var parameterSummary: some ParameterSummary {
        Summary("List \(\.$kind)")
    }

    func perform() async throws -> some IntentResult & ProvidesDialog {
        guard let action = kind.action else {
            return .result(dialog: IntentDialog(Self.commandSummary))
        }
        let speech = try await SoundscapeIntentRunner.run(action)
        return .result(dialog: SoundscapeIntentRunner.dialog(for: speech))
    }

    /// A LocalizedStringResource rather than a String so it can be translated at all — but
    /// deliberately not translated from English. It recites the spoken phrases, so a
    /// language only gets a version of this once someone has authored that language's
    /// phrases in AppShortcuts.xcstrings; a literal translation would tell the user to say
    /// commands that do not exist.
    private static let commandSummary: LocalizedStringResource =
        "You can say: Soundscape surroundings, Soundscape route, Soundscape start route, Soundscape beacon, Soundscape stop beacon, or Soundscape list."
}
