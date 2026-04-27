package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.home_screen_title
import org.scottishtecharmy.soundscape.resources.preview_title
import org.scottishtecharmy.soundscape.resources.sleep_sleep
import org.scottishtecharmy.soundscape.resources.sleep_sleep_acc_hint
import org.scottishtecharmy.soundscape.resources.street_preview_exit
import org.scottishtecharmy.soundscape.resources.ui_menu
import org.scottishtecharmy.soundscape.resources.ui_menu_hint
import org.scottishtecharmy.soundscape.screens.markers_routes.components.FlexibleAppBar
import org.scottishtecharmy.soundscape.screens.talkbackHint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedHomeTopAppBar(
    drawerState: DrawerState,
    coroutineScope: CoroutineScope,
    streetPreviewState: Boolean,
    streetPreviewFunctions: StreetPreviewFunctions,
    onSleep: () -> Unit,
) {
    FlexibleAppBar(
        title = if (streetPreviewState) {
            stringResource(Res.string.preview_title)
        } else {
            stringResource(Res.string.home_screen_title)
        },
        leftSide = {
            IconButton(
                onClick = { coroutineScope.launch { drawerState.open() } },
                modifier = Modifier
                    .talkbackHint(stringResource(Res.string.ui_menu_hint))
                    .testTag("topBarMenu"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = stringResource(Res.string.ui_menu),
                    modifier = Modifier.semantics { heading() },
                )
            }
        },
        rightSide = {
            if (streetPreviewState) {
                IconButton(
                    enabled = true,
                    onClick = { streetPreviewFunctions.exit() },
                    modifier = Modifier.talkbackHint(stringResource(Res.string.street_preview_exit)),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ExitToApp,
                        contentDescription = stringResource(Res.string.street_preview_exit),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else {
                IconButton(
                    enabled = true,
                    onClick = { onSleep() },
                    modifier = Modifier
                        .talkbackHint(stringResource(Res.string.sleep_sleep_acc_hint))
                        .testTag("topBarSleep"),
                ) {
                    Icon(
                        Icons.Rounded.Snooze,
                        contentDescription = stringResource(Res.string.sleep_sleep),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
    )
}
