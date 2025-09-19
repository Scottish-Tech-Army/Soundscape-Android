package org.scottishtecharmy.soundscape.screens.home

import android.content.SharedPreferences
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
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Markunread
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.BuildConfig
import org.scottishtecharmy.soundscape.MainActivity.Companion.RECORD_TRAVEL_DEFAULT
import org.scottishtecharmy.soundscape.MainActivity.Companion.RECORD_TRAVEL_KEY
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.DrawerMenuItem
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun DrawerContent(
    drawerState: DrawerState,
    onNavigate: (String) -> Unit,
    rateSoundscape: () -> Unit,
    contactSupport: () -> Unit,
    shareRecording: () -> Unit,
    preferences: SharedPreferences?
) {
    val scope = rememberCoroutineScope()
    val recordingEnabled = preferences?.getBoolean(RECORD_TRAVEL_KEY, RECORD_TRAVEL_DEFAULT) == true

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.background,
        drawerContentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Scaffold (
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
                    onClick = { onNavigate(HomeRoutes.Help.route + "/page${R.string.menu_help_and_tutorials}") },
                    label = stringResource(R.string.menu_help_and_tutorials),
                    Icons.AutoMirrored.Rounded.HelpOutline,
                    modifier = Modifier.testTag("menuHelpAndTutorials")
                )

// Not implemented yet
//                DrawerMenuItem(
//                    onClick = { notAvailableToast() },
//                    label = stringResource(R.string.menu_send_feedback),
//                    icon = Icons.Rounded.MailOutline,
//                )
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
                    onClick = { onNavigate(HomeRoutes.Help.route + "/page${R.string.settings_about_app}") },
                    label = stringResource(R.string.settings_about_app),
                    Icons.AutoMirrored.Rounded.HelpOutline,
                    modifier = Modifier.testTag("menuAboutSoundscape")
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
        DrawerState(DrawerValue.Open) { true },
        { },
        { },
        { },
        { },
        null
    )
}
