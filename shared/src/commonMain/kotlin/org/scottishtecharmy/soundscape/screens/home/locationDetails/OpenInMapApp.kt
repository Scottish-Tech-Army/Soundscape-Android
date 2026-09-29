package org.scottishtecharmy.soundscape.screens.home.locationDetails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.platform.isIos
import org.scottishtecharmy.soundscape.preferences.PreferenceDefaults
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.preferences.rememberStringPreference
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.general_alert_cancel
import org.scottishtecharmy.soundscape.resources.location_detail_action_open_in_app
import org.scottishtecharmy.soundscape.resources.location_detail_action_open_in_app_hint
import org.scottishtecharmy.soundscape.resources.location_detail_action_open_in_named_app
import org.scottishtecharmy.soundscape.resources.location_detail_action_open_in_named_app_hint
import org.scottishtecharmy.soundscape.resources.open_in_app_change
import org.scottishtecharmy.soundscape.resources.open_in_app_dialog_title
import org.scottishtecharmy.soundscape.resources.open_in_app_remember
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.markers_routes.components.IconWithTextButton
import org.scottishtecharmy.soundscape.ui.theme.spacing

/**
 * An installed app which can show a location on a map.
 *
 * @param id stable identifier stored in [PreferenceKeys.PREFERRED_MAP_APP]: the package name on
 * Android, a key from IosMapApps on iOS.
 * @param name the app's user-visible name.
 */
data class MapApp(val id: String, val name: String)

/**
 * Opens locations in the user's map app: straight into the remembered one if there is one,
 * otherwise via a chooser which can remember the pick. Shared by Location Details' button and
 * the "Open in" accessibility action on Places Nearby and Markers list items.
 */
class MapAppLauncher internal constructor(
    /** The remembered app, if it is still installed. */
    val preferred: MapApp?,
    private val mapApps: List<MapApp>,
    private val onOpenInMapApp: (MapApp, LocationDescription) -> Unit,
    private val showChooser: (LocationDescription) -> Unit,
) {
    /** Opens [desc] in the remembered app, or asks which app when there's a choice to make. */
    fun open(desc: LocationDescription) {
        when {
            preferred != null -> onOpenInMapApp(preferred, desc)
            // Nothing to choose between, so don't make the user dismiss a one-item list.
            mapApps.size == 1 -> onOpenInMapApp(mapApps.first(), desc)
            else -> showChooser(desc)
        }
    }

    /** Always asks, so the remembered app can be changed. */
    fun choose(desc: LocationDescription) = showChooser(desc)
}

/**
 * Remembers a [MapAppLauncher], or null when there are no map apps to open in. The chooser
 * dialog is part of the caller's composition, so it stays up only while the caller does.
 */
@Composable
fun rememberMapAppLauncher(
    mapApps: List<MapApp>,
    preferencesProvider: PreferencesProvider?,
    onOpenInMapApp: ((MapApp, LocationDescription) -> Unit)?,
): MapAppLauncher? {
    if (mapApps.isEmpty() || onOpenInMapApp == null) return null

    val preferredId by rememberStringPreference(
        preferencesProvider,
        PreferenceKeys.PREFERRED_MAP_APP,
        PreferenceDefaults.PREFERRED_MAP_APP,
    )
    // A remembered app which has since been uninstalled is ignored until the user picks again.
    val preferred = mapApps.firstOrNull { it.id == preferredId }
    var choosingFor by remember { mutableStateOf<LocationDescription?>(null) }

    choosingFor?.let { desc ->
        MapAppChooserDialog(
            mapApps = mapApps,
            initiallyRemember = preferred != null,
            onDismiss = { choosingFor = null },
            onChosen = { app, remember ->
                choosingFor = null
                preferencesProvider?.putString(
                    PreferenceKeys.PREFERRED_MAP_APP,
                    if (remember) app.id else PreferenceDefaults.PREFERRED_MAP_APP,
                )
                onOpenInMapApp(app, desc)
            },
        )
    }
    return remember(preferred, mapApps, onOpenInMapApp) {
        MapAppLauncher(preferred, mapApps, onOpenInMapApp) { choosingFor = it }
    }
}

/** "Open in Waze" once an app is remembered, "Open in Maps App" until then. */
@Composable
fun MapAppLauncher.label(): String =
    preferred?.let { stringResource(Res.string.location_detail_action_open_in_named_app, it.name) }
        ?: stringResource(Res.string.location_detail_action_open_in_app)

/**
 * "Open in Maps App" button for Location Details. Once an app is remembered the button opens it
 * directly and its label names it. A long press brings the chooser back; TalkBack offers that as
 * "double tap and hold", and on iOS it is a VoiceOver custom action instead.
 */
@Composable
internal fun OpenInMapAppButton(
    locationDescription: LocationDescription,
    launcher: MapAppLauncher,
    modifier: Modifier = Modifier,
) {
    val preferred = launcher.preferred
    val changeLabel = stringResource(Res.string.open_in_app_change)
    IconWithTextButton(
        icon = Icons.AutoMirrored.Filled.OpenInNew,
        text = launcher.label(),
        talkbackHint = if (preferred != null) {
            stringResource(Res.string.location_detail_action_open_in_named_app_hint, preferred.name)
        } else {
            stringResource(Res.string.location_detail_action_open_in_app_hint)
        },
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .defaultMinSize(minHeight = spacing.targetSize)
            .fillMaxWidth()
            .then(
                if (preferred != null && isIos) {
                    Modifier.semantics {
                        customActions = listOf(
                            CustomAccessibilityAction(changeLabel) {
                                launcher.choose(locationDescription)
                                true
                            },
                        )
                    }
                } else Modifier,
            ),
        buttonTestTag = "locationDetailsOpenInMapApp",
        onLongClick = if (preferred != null) ({ launcher.choose(locationDescription) }) else null,
        onLongClickLabel = changeLabel,
    ) {
        launcher.open(locationDescription)
    }
}

@Composable
private fun MapAppChooserDialog(
    mapApps: List<MapApp>,
    initiallyRemember: Boolean,
    onDismiss: () -> Unit,
    onChosen: (MapApp, remember: Boolean) -> Unit,
) {
    var rememberChoice by remember { mutableStateOf(initiallyRemember) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.open_in_app_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.none)) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .semantics {
                            collectionInfo = CollectionInfo(rowCount = mapApps.size, columnCount = 1)
                        },
                ) {
                    mapApps.forEachIndexed { index, app ->
                        Text(
                            text = app.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = spacing.targetSize)
                                .clickable(role = Role.Button) { onChosen(app, rememberChoice) }
                                .semantics {
                                    collectionItemInfo = CollectionItemInfo(
                                        rowIndex = index,
                                        rowSpan = 1,
                                        columnIndex = 0,
                                        columnSpan = 1,
                                    )
                                }
                                .padding(vertical = spacing.small)
                                .testTag("mapAppChoice_${app.id}"),
                        )
                    }
                }
                HorizontalDivider()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = spacing.targetSize)
                        .toggleable(
                            value = rememberChoice,
                            role = Role.Checkbox,
                            onValueChange = { rememberChoice = it },
                        )
                        .testTag("mapAppRememberChoice"),
                ) {
                    Checkbox(checked = rememberChoice, onCheckedChange = null)
                    Text(
                        text = stringResource(Res.string.open_in_app_remember),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = spacing.small),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.general_alert_cancel))
            }
        },
    )
}
