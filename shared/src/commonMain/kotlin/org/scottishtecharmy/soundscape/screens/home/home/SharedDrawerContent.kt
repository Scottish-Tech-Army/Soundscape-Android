package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Markunread
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.components.DrawerMenuItem
import org.scottishtecharmy.soundscape.navigation.SharedRoutes
import org.scottishtecharmy.soundscape.platform.appVersionName
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.menu_audio_tutorial
import org.scottishtecharmy.soundscape.resources.menu_audio_tutorial_cancel
import org.scottishtecharmy.soundscape.resources.menu_contact_support
import org.scottishtecharmy.soundscape.resources.menu_help
import org.scottishtecharmy.soundscape.resources.menu_rate
import org.scottishtecharmy.soundscape.resources.menu_share_recorded_route
import org.scottishtecharmy.soundscape.resources.new_version_info_text
import org.scottishtecharmy.soundscape.resources.offline_maps_title
import org.scottishtecharmy.soundscape.resources.settings_about_app
import org.scottishtecharmy.soundscape.resources.settings_screen_title
import org.scottishtecharmy.soundscape.resources.ui_menu_close
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun SharedDrawerContent(
    drawerState: DrawerState,
    onNavigate: (String) -> Unit,
    rateSoundscape: () -> Unit,
    contactSupport: () -> Unit,
    shareRecording: () -> Unit,
    offlineMaps: () -> Unit,
    toggleTutorial: () -> Unit,
    tutorialRunning: Boolean,
    recordingEnabled: Boolean,
    newReleaseDialog: MutableState<Boolean>?,
) {
    val scope = rememberCoroutineScope()
    val running = remember(tutorialRunning) { mutableStateOf(tutorialRunning) }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.background,
        drawerContentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Scaffold(
            topBar = {
                IconButton(
                    onClick = {
                        scope.launch {
                            if (drawerState.isClosed) {
                                drawerState.open()
                            } else {
                                drawerState.close()
                            }
                        }
                    },
                    modifier = Modifier.testTag("menuDrawerBack"),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        modifier = Modifier
                            .size(spacing.targetSize)
                            .padding(start = spacing.extraSmall),
                        contentDescription = stringResource(Res.string.ui_menu_close),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            },
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = spacing.medium,
                            vertical = spacing.medium,
                        ),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = "v${appVersionName()}",
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
            ) {
                DrawerMenuItem(
                    onClick = { onNavigate(SharedRoutes.SETTINGS) },
                    label = stringResource(Res.string.settings_screen_title),
                    icon = Icons.Rounded.Settings,
                    modifier = Modifier.testTag("menuSettings"),
                )
                DrawerMenuItem(
                    onClick = { onNavigate(SharedRoutes.HELP + "/page${Res.string.menu_help.key}") },
                    label = stringResource(Res.string.menu_help),
                    icon = Icons.AutoMirrored.Rounded.HelpOutline,
                    modifier = Modifier.testTag("menuHelpAndTutorials"),
                )
                DrawerMenuItem(
                    onClick = {
                        scope.launch { drawerState.close() }
                        toggleTutorial()
                    },
                    label = if (running.value) {
                        stringResource(Res.string.menu_audio_tutorial_cancel)
                    } else {
                        stringResource(Res.string.menu_audio_tutorial)
                    },
                    icon = Icons.Rounded.Headphones,
                    modifier = Modifier.testTag("menuAudioTutorial"),
                )
                DrawerMenuItem(
                    onClick = { rateSoundscape() },
                    label = stringResource(Res.string.menu_rate),
                    icon = Icons.Rounded.Star,
                    modifier = Modifier.testTag("menuRate"),
                )
                DrawerMenuItem(
                    onClick = { contactSupport() },
                    label = stringResource(Res.string.menu_contact_support),
                    icon = Icons.Rounded.Markunread,
                    modifier = Modifier.testTag("menuContactSupport"),
                )
                DrawerMenuItem(
                    onClick = { offlineMaps() },
                    label = stringResource(Res.string.offline_maps_title),
                    icon = Icons.Rounded.Download,
                    modifier = Modifier.testTag("menuOfflineMaps"),
                )
                DrawerMenuItem(
                    onClick = { onNavigate(SharedRoutes.HELP + "/page${Res.string.settings_about_app.key}") },
                    label = stringResource(Res.string.settings_about_app),
                    icon = Icons.AutoMirrored.Rounded.HelpOutline,
                    modifier = Modifier.testTag("menuAboutSoundscape"),
                )
                DrawerMenuItem(
                    onClick = { newReleaseDialog?.value = true },
                    label = stringResource(Res.string.new_version_info_text),
                    icon = Icons.AutoMirrored.Rounded.Comment,
                    modifier = Modifier.testTag("newReleaseInfo"),
                )

                if (recordingEnabled) {
                    DrawerMenuItem(
                        onClick = { shareRecording() },
                        label = stringResource(Res.string.menu_share_recorded_route),
                        icon = Icons.Rounded.Share,
                        modifier = Modifier.testTag("menuShareRecording"),
                    )
                }
            }
        }
    }
}
