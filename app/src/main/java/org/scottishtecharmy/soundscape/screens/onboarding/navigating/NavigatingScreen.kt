package org.scottishtecharmy.soundscape.screens.onboarding.navigating

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.components.OnboardButton
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.first_launch_permissions_granted
import org.scottishtecharmy.soundscape.resources.first_launch_permissions_location
import org.scottishtecharmy.soundscape.resources.first_launch_permissions_message
import org.scottishtecharmy.soundscape.resources.first_launch_permissions_not_granted
import org.scottishtecharmy.soundscape.resources.first_launch_permissions_notification
import org.scottishtecharmy.soundscape.resources.first_launch_permissions_required
import org.scottishtecharmy.soundscape.resources.first_launch_permissions_title
import org.scottishtecharmy.soundscape.resources.ui_continue
import org.scottishtecharmy.soundscape.screens.onboarding.component.BoxWithGradientBackground
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

enum class Permission(
    val manifestIdentifier: String,
    val icon: ImageVector,
    val mainText: StringResource,
    val subtitleText: StringResource,
) {
    ACCESS_FINE_LOCATION(
        Manifest.permission.ACCESS_FINE_LOCATION, Icons.Rounded.LocationOn,
        Res.string.first_launch_permissions_location,
        Res.string.first_launch_permissions_required,
    ),

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    POST_NOTIFICATIONS(
        Manifest.permission.POST_NOTIFICATIONS,
        Icons.Rounded.Notifications,
        Res.string.first_launch_permissions_notification,
        Res.string.first_launch_permissions_required,
    ),
}

@Composable
fun NavigatingScreen(
    // Called both when the user taps Continue and when the screen skips itself
    // because every permission is already granted. Either way there's nothing left
    // to review on this screen once it's satisfied, so callers should have this pop
    // the screen off the back stack - otherwise swiping back re-reveals it, its skip
    // check re-fires immediately, and it bounces straight forward again.
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    vm: NavigatingScreenViewModel = viewModel(),
) {
    val uiState = vm.state.collectAsStateWithLifecycle()

    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Permission.ACCESS_FINE_LOCATION,
                Permission.POST_NOTIFICATIONS
            )
        } else {
            listOf(
                Permission.ACCESS_FINE_LOCATION
            )
        }
    }

    val permissionsStatus = permissionsToRequest.associateWith {
        when (LocalContext.current.checkSelfPermission(it.manifestIdentifier)) {
            PackageManager.PERMISSION_GRANTED -> true
            else -> false
        }
    }

    LaunchedEffect(permissionsStatus) {
        vm.permissionsRequired(permissionsStatus)
    }

    // Skip this screen entirely if every required permission is already granted
    // (e.g. the user re-runs onboarding after already having granted them).
    LaunchedEffect(Unit) {
        if (requiredPermissionsGranted(permissionsStatus)) {
            onNavigate()
        }
    }

    val onPermissionResult = remember(vm) {
        { permission: Permission, granted: Boolean ->
            vm.onPermissionResult(permission, granted)
        }
    }

    Navigating(
        onContinue = onNavigate,
        permissionsStatus = uiState.value.permissionsStatus,
        onPermissionResult = onPermissionResult,
        continueEnabled = uiState.value.continueEnabled,
        modifier = modifier,
    )
}

data class PermissionRationaleUi(
    val permission: Permission,
    val icon: ImageVector,
    val mainText: StringResource,
    val subtitleText: StringResource,
    val onPermissionResult: (permission: Permission, granted: Boolean) -> Unit
) {

    @Composable
    fun permissionRequest(): ManagedActivityResultLauncher<String, Boolean> =
        singlePermissionResultLauncher(
            permissionToRequest = permission,
            onPermissionResult = onPermissionResult,
        )
}

@Composable
fun Navigating(
    onContinue: () -> Unit,
    permissionsStatus: Map<Permission, Boolean>,
    onPermissionResult: (permission: Permission, granted: Boolean) -> Unit,
    continueEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    BoxWithGradientBackground(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = spacing.large, vertical = spacing.large)
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        )
        {
            Text(
                text = stringResource(Res.string.first_launch_permissions_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .semantics {
                        heading()
                    }
                    .focusRequester(focusRequester) // Attach the requester
                    .focusable() // Make the text focusable,
            )

            Spacer(modifier = Modifier.height(spacing.large))
            Text(
                text = stringResource(Res.string.first_launch_permissions_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.focusable(),
            )
            Spacer(modifier = Modifier.height(spacing.large))

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(spacing.extraSmall))
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            )
            {
                permissionsStatus.forEach { (permission, granted) ->
                    key(permission) {
                        val rationale = PermissionRationaleUi(
                            permission = permission,
                            icon = permission.icon,
                            mainText = permission.mainText,
                            subtitleText = permission.subtitleText,
                            onPermissionResult = onPermissionResult
                        )
                        val launcher = rationale.permissionRequest()
                        PermissionRationale(
                            rationale.icon,
                            rationale.mainText,
                            rationale.subtitleText,
                            granted,
                            onClick = {
                                launcher.launch(permission.manifestIdentifier)
                            },
                            clickableTestTag = "navigatingScreenPermissionRow${permission.name}"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.large))

            Column(
                modifier = Modifier
                    .padding(horizontal = spacing.medium)
                    .focusable(false)
            ) {
                OnboardButton(
                    text = stringResource(Res.string.ui_continue),
                    onClick = { onContinue() },
                    enabled = continueEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusable()
                        .testTag("navigatingScreenContinueButton"),
                )
            }
        }
    }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        focusRequester.requestFocus()
    }
}

@Composable
fun singlePermissionResultLauncher(
    permissionToRequest: Permission,
    onPermissionResult: (permission: Permission, granted: Boolean) -> Unit,
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { granted ->
        onPermissionResult(permissionToRequest, granted)
    }
)

@Composable
fun PermissionRationale(
    icon: ImageVector,
    mainText: StringResource,
    subtitleText: StringResource,
    granted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    clickableTestTag: String = "",
) {
    Row(
        modifier = modifier
            .mediumPadding()
            .fillMaxWidth()
            .focusable()
            .semantics(mergeDescendants = true) {}
            .testTag(clickableTestTag)
            .then(
                // Once granted there's nothing left for a tap to do - permissions can't be
                // withdrawn from here - so drop the click action rather than leaving a
                // "double tap to activate" hint that does nothing.
                if (granted) {
                    Modifier
                } else {
                    Modifier.clickable { onClick() }
                }
            ),
    )
    {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(spacing.extraSmall))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(mainText),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(subtitleText),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                // The rationale text is only useful while the permission is still
                // outstanding - once granted, TalkBack should just say "<name>,
                // permission granted" rather than repeating the now-irrelevant
                // rationale.
                modifier = if (granted) Modifier.clearAndSetSemantics {} else Modifier,
            )
        }
        Spacer(modifier = Modifier.weight(0.05f))
        if (granted) {
            Icon(
                Icons.Default.Check,
                contentDescription = stringResource(Res.string.first_launch_permissions_granted),
                modifier = Modifier.align(Alignment.CenterVertically),
                tint = MaterialTheme.colorScheme.onSurface
            )
        } else {
            Icon(
                Icons.Default.Cancel,
                contentDescription = stringResource(Res.string.first_launch_permissions_not_granted),
                modifier = Modifier.align(Alignment.CenterVertically),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Preview
@Composable
fun NavigatingPreview() {
    Navigating(
        onContinue = {},
        permissionsStatus = emptyMap(),
        onPermissionResult = { _, _ -> },
        continueEnabled = true
    )
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Preview
@Composable
fun NavigatingPreviewContinueDisabled() {
    Navigating(
        onContinue = {},
        permissionsStatus = emptyMap(),
        onPermissionResult = { _, _ -> },
        continueEnabled = false
    )
}

@Preview(device = "spec:parent=pixel_5,orientation=portrait")
@Composable
fun PermissionRationalePreview() {
    SoundscapeTheme {
        PermissionRationale(
            Icons.Rounded.LocationOn,
            Res.string.first_launch_permissions_location,
            Res.string.first_launch_permissions_required,
            true,
            {}
        )
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=portrait")
@Composable
fun PermissionRationaleNotGrantedPreview() {
    SoundscapeTheme {
        PermissionRationale(
            Icons.Rounded.LocationOn,
            Res.string.first_launch_permissions_location,
            Res.string.first_launch_permissions_required,
            false,
            {}
        )
    }
}
