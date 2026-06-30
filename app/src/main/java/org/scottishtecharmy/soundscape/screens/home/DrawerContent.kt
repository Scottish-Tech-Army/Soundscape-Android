package org.scottishtecharmy.soundscape.screens.home

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Markunread
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.scottishtecharmy.soundscape.BuildConfig
import org.scottishtecharmy.soundscape.MainActivity.Companion.RECORD_TRAVEL_DEFAULT
import org.scottishtecharmy.soundscape.MainActivity.Companion.RECORD_TRAVEL_KEY
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.DrawerMenuItem
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun DrawerContent(
    onClose: () -> Unit,
    onNavigate: (String) -> Unit,
    rateSoundscape: () -> Unit,
    contactSupport: () -> Unit,
    shareRecording: () -> Unit,
    offlineMaps: () -> Unit,
    toggleTutorial: () -> Unit,
    tutorialRunning: Boolean,
    preferences: SharedPreferences?,
    newReleaseDialog: MutableState<Boolean>?,
    exitApp: () -> Unit
) {
    val recordingEnabled = preferences?.getBoolean(RECORD_TRAVEL_KEY, RECORD_TRAVEL_DEFAULT) == true
    val running = remember(tutorialRunning) { mutableStateOf(tutorialRunning) }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    ModalDrawerSheet(
        modifier = Modifier.requiredWidth(screenWidth),
        drawerContainerColor = MaterialTheme.colorScheme.background,
        drawerContentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Scaffold (
            topBar = {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("menuDrawerBack")
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        modifier =
                            Modifier
                                .size(spacing.targetSize)
                                .padding(start = spacing.extraSmall),
                        contentDescription = stringResource(R.string.ui_menu_close),
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
                            vertical = spacing.medium),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        color = MaterialTheme.colorScheme.onBackground
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
                    onClick = { exitApp() },
                    label = stringResource(R.string.menu_exit_app),
                    icon = Icons.AutoMirrored.Rounded.ExitToApp,
                    modifier = Modifier.testTag("menuExitApp")
                )

// Not implemented yet
//                DrawerMenuItem(
//                    onClick = { notAvailableToast() },
//                    label = stringResource(R.string.menu_devices),
//                    icon = Icons.Rounded.Headset,
//                )
                DrawerMenuItem(
                    onClick = { onNavigate(HomeRoutes.Settings.route) },
                    label = stringResource(R.string.settings_screen_title),
                    icon = Icons.Rounded.Settings,
                    modifier = Modifier.testTag("menuSettings")
                )
                DrawerMenuItem(
                    onClick = { onNavigate(HomeRoutes.Help.route + "/page${R.string.menu_help}") },
                    label = stringResource(R.string.menu_help),
                    Icons.AutoMirrored.Rounded.HelpOutline,
                    modifier = Modifier.testTag("menuHelpAndTutorials")
                )

                DrawerMenuItem(
                    onClick = {
                        onClose()
                        toggleTutorial()
                    },
                    label = if(running.value) stringResource(R.string.menu_audio_tutorial_cancel) else stringResource(R.string.menu_audio_tutorial),
                    Icons.Rounded.Headphones,
                    modifier = Modifier.testTag("menuAudioTutorial")
                )

                DrawerMenuItem(
                    onClick = { rateSoundscape() },
                    label = stringResource(R.string.menu_rate),
                    icon = Icons.Rounded.Star,
                    modifier = Modifier.testTag("menuRate")
                )

                DrawerMenuItem(
                    onClick = { contactSupport() },
                    label = stringResource(R.string.menu_contact_support),
                    icon = Icons.Rounded.Markunread,
                    modifier = Modifier.testTag("menuContactSupport")
                )

// This is supposed to share the app with someone else (not the location)
//                DrawerMenuItem(
//                    onClick = { shareLocation() },
//                    label = stringResource(R.string.share_title),
//                    icon = Icons.Rounded.IosShare,
//                )

                DrawerMenuItem(
                    onClick = { offlineMaps() },
                    label = stringResource(R.string.offline_maps_title),
                    Icons.Rounded.Download,
                    modifier = Modifier.testTag("menuOfflineMaps")
                )

                DrawerMenuItem(
                    onClick = { onNavigate(HomeRoutes.Help.route + "/page${R.string.settings_about_app}") },
                    label = stringResource(R.string.settings_about_app),
                    Icons.AutoMirrored.Rounded.HelpOutline,
                    modifier = Modifier.testTag("menuAboutSoundscape")
                )

                DrawerMenuItem(
                    onClick = {
                        newReleaseDialog?.value = true
                    },
                    label = stringResource(R.string.new_version_info_text),
                    Icons.AutoMirrored.Rounded.Comment,
                    modifier = Modifier.testTag("newReleaseInfo")
                )

                if (recordingEnabled) {
                    DrawerMenuItem(
                        onClick = { shareRecording() },
                        label = stringResource(R.string.menu_share_recorded_route),
                        icon = Icons.Rounded.Share,
                        modifier = Modifier.testTag("menuShareRecording")
                    )
                }
            }
        }
    }
}
@Preview
@Composable
fun PreviewDrawerContent() {
    DrawerContent(
        onClose = { },
        onNavigate = { },
        rateSoundscape = { },
        contactSupport = { },
        shareRecording = { },
        offlineMaps = { },
        toggleTutorial = { },
        tutorialRunning = false,
        preferences = null,
        newReleaseDialog = null,
        exitApp = { }
    )
}
