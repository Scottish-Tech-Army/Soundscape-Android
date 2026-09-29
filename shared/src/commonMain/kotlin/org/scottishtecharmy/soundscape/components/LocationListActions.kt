package org.scottishtecharmy.soundscape.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.share_title
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.locationDetails.MapApp
import org.scottishtecharmy.soundscape.screens.home.locationDetails.label
import org.scottishtecharmy.soundscape.screens.home.locationDetails.rememberMapAppLauncher

/**
 * What a location list (Places Nearby, Markers) can do with an item besides opening it, offered
 * as screen reader actions so a TalkBack or VoiceOver user needn't go via Location Details.
 */
data class LocationListActions(
    val mapApps: List<MapApp> = emptyList(),
    val preferencesProvider: PreferencesProvider? = null,
    val onOpenInMapApp: ((MapApp, LocationDescription) -> Unit)? = null,
    val onShare: ((LocationDescription) -> Unit)? = null,
)

/** The "Open in …" and "Share" actions for [LocationItemDecoration.extraActions]. */
@Composable
fun rememberLocationItemActions(actions: LocationListActions): List<LocationItemAction> {
    val launcher = rememberMapAppLauncher(
        actions.mapApps,
        actions.preferencesProvider,
        actions.onOpenInMapApp,
    )
    val openLabel = launcher?.label()
    val shareLabel = stringResource(Res.string.share_title)
    return buildList {
        if (launcher != null && openLabel != null) {
            add(LocationItemAction(openLabel) { launcher.open(it) })
        }
        actions.onShare?.let { add(LocationItemAction(shareLabel, it)) }
    }
}
