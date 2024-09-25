package org.scottishtecharmy.soundscape.screens.home

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.DrawerMenuItem

@Composable
fun DrawerContent(
    onNavigate: (String) -> Unit,
    drawerState: DrawerState,
    shareLocation: () -> Unit,
    rateSoundscape: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notAvailableText = "This is not implemented yet."
    val notAvailableToast = {
        Toast.makeText(context, notAvailableText, Toast.LENGTH_SHORT).show()
    }
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.background,
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
                        .size(32.dp)
                        .padding(start = 4.dp),
                contentDescription = stringResource(R.string.ui_menu_close),
                tint = Color.White,
            )
        }
        DrawerMenuItem(
            onClick = { notAvailableToast() },
            label = stringResource(R.string.menu_devices),
            icon = Icons.Rounded.Headset,
        )
        DrawerMenuItem(
            onClick = { onNavigate(HomeRoutes.Settings.route) },
            // Weirdly, original iOS Soundscape doesn't seem to have translation strings for "Settings"
            label = stringResource(R.string.general_alert_settings),
            icon = Icons.Rounded.Settings,
        )
        DrawerMenuItem(
            onClick = { notAvailableToast() },
            label = stringResource(R.string.menu_help_and_tutorials),
            Icons.AutoMirrored.Rounded.HelpOutline,
        )
        DrawerMenuItem(
            onClick = { notAvailableToast() },
            label = stringResource(R.string.menu_send_feedback),
            icon = Icons.Rounded.MailOutline,
        )
        DrawerMenuItem(
            onClick = { rateSoundscape() },
            label = stringResource(R.string.menu_rate),
            icon = Icons.Rounded.Star,
        )
        DrawerMenuItem(
            onClick = { shareLocation() },
            label = stringResource(R.string.share_title),
            icon = Icons.Rounded.IosShare,
        )
    }
}
