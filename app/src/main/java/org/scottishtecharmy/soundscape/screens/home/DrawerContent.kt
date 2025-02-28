package org.scottishtecharmy.soundscape.screens.home

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.DrawerMenuItem
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun DrawerContent(
    drawerState: DrawerState,
    onNavigate: (String) -> Unit,
    rateSoundscape: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.background,
        drawerContentColor = MaterialTheme.colorScheme.onBackground,
    ) {
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

//  Not implemented yet
//        DrawerMenuItem(
//            onClick = { notAvailableToast() },
//            label = stringResource(R.string.menu_devices),
//            icon = Icons.Rounded.Headset,
//        )
        DrawerMenuItem(
            onClick = { onNavigate(HomeRoutes.Settings.route) },
            label = stringResource(R.string.general_alert_settings),
            icon = Icons.Rounded.Settings,
        )
        DrawerMenuItem(
            onClick = { onNavigate(HomeRoutes.Help.route + "/page${R.string.menu_help_and_tutorials}") },
            label = stringResource(R.string.menu_help_and_tutorials),
            Icons.AutoMirrored.Rounded.HelpOutline,
        )

// Not implemented yet
//        DrawerMenuItem(
//            onClick = { notAvailableToast() },
//            label = stringResource(R.string.menu_send_feedback),
//            icon = Icons.Rounded.MailOutline,
//        )
        DrawerMenuItem(
            onClick = { rateSoundscape() },
            label = stringResource(R.string.menu_rate),
            icon = Icons.Rounded.Star,
        )

// This is supposed to share the app with someone else (not the location)
//        DrawerMenuItem(
//            onClick = { shareLocation() },
//            label = stringResource(R.string.share_title),
//            icon = Icons.Rounded.IosShare,
//        )
        DrawerMenuItem(
            onClick = { onNavigate(HomeRoutes.Help.route + "/page${R.string.settings_about_app}") },
            label = stringResource(R.string.settings_about_app),
            Icons.AutoMirrored.Rounded.HelpOutline,
        )
    }
}
@Preview
@Composable
fun PreviewDrawerContent() {
    DrawerContent(
        DrawerState(DrawerValue.Open) { true },
        { },
        { },
    )
}
