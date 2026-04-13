package org.scottishtecharmy.soundscape.screens.onboarding.navigating

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
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
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.OnboardButton
import org.scottishtecharmy.soundscape.screens.onboarding.component.BoxWithGradientBackground
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun NavigatingScreen(
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    vm: NavigatingScreenViewModel = viewModel(),
) {
    val uiState = vm.state.collectAsStateWithLifecycle()

    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.RECORD_AUDIO
            )
        } else {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    val permissionsStatus = permissionsToRequest.associateWith {
        when (LocalContext.current.checkSelfPermission(it)) {
            PackageManager.PERMISSION_GRANTED -> true
            else -> false
        }
    }

    LaunchedEffect(permissionsStatus) {
        vm.permissionsRequired(permissionsStatus)
    }

    val onPermissionResult = remember(vm) {
        { permission: String, granted: Boolean ->
            vm.onPermissionResult(permission, granted)
        }
    }

    Navigating(
        onContinue = onNavigate,
        permissionsStatus = uiState.value.permissionsStatus,
        onPermissionResult = onPermissionResult,
        modifier = modifier,
    )
}

data class PermissionRationaleUi(
    val permission: String,
    val icon: ImageVector,
    @StringRes val mainText: Int,
    @StringRes val subtitleText: Int,
    val onPermissionResult: (permission: String, granted: Boolean) -> Unit
) {

    @Composable
    fun permissionRequest(): ManagedActivityResultLauncher<String, Boolean> =
        singlePermissionResultLauncher(
            permissionToRequest = permission,
            onPermissionResult = onPermissionResult,
        )
}

fun manifestPermissionToPermissionRationaleUi(
    permission: String,
    onPermissionResult: (permission: String, granted: Boolean) -> Unit
): PermissionRationaleUi {
    return when (permission) {
        Manifest.permission.ACCESS_FINE_LOCATION -> {
            PermissionRationaleUi(
                permission,
                Icons.Rounded.LocationOn,
                R.string.first_launch_permissions_location,
                R.string.first_launch_permissions_required,
                onPermissionResult,
            )
        }

        Manifest.permission.ACCESS_BACKGROUND_LOCATION -> {
            PermissionRationaleUi(
                permission,
                Icons.Rounded.LocationOn,
                R.string.first_launch_permissions_location,
                R.string.first_launch_permissions_required,
                onPermissionResult,
            )
        }

        Manifest.permission.POST_NOTIFICATIONS -> {
            PermissionRationaleUi(
                permission,
                Icons.Rounded.Notifications,
                R.string.first_launch_permissions_notification,
                R.string.first_launch_permissions_required,
                onPermissionResult,
            )
        }

        Manifest.permission.RECORD_AUDIO -> {
            PermissionRationaleUi(
                permission,
                Icons.Rounded.Mic,
                R.string.first_launch_permissions_record_audio,
                R.string.first_launch_permissions_required_for_voice_control,
                onPermissionResult,
            )
        }

        else -> throw UnsupportedOperationException("Unknown permission: $permission")
    }
}

@Composable
fun Navigating(
    onContinue: () -> Unit,
    permissionsStatus: Map<String, Boolean>,
    onPermissionResult: (permission: String, granted: Boolean) -> Unit,
    modifier: Modifier = Modifier
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
                text = stringResource(id = R.string.first_launch_permissions_title),
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
                text = stringResource(id = R.string.first_launch_permissions_message),
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
                        val rationale = manifestPermissionToPermissionRationaleUi(
                            permission,
                            onPermissionResult
                        )
                        val launcher = rationale.permissionRequest()
                        PermissionRationale(
                            rationale.icon,
                            rationale.mainText,
                            rationale.subtitleText,
                            granted,
                            onClick = {
                                launcher.launch(permission)
                            }
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
                    text = stringResource(R.string.ui_continue),
                    // just bodging this at the moment as having problems with rationales
                    // and handling denied permissions
                    onClick = { onContinue() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusable()
                        .testTag("navigatingScreenContinueButton"),
                )
            }
        }
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun singlePermissionResultLauncher(
    permissionToRequest: String,
    onPermissionResult: (permission: String, granted: Boolean) -> Unit,
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { granted ->
        onPermissionResult(permissionToRequest, granted)
    }
)

@Composable
fun PermissionRationale(
    icon: ImageVector,
    @StringRes mainText: Int,
    @StringRes subtitleText: Int,
    granted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .mediumPadding()
            .fillMaxWidth(),
    )
    {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(spacing.extraSmall))
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {}
                .clickable {
                    onClick()
                }
        ) {
            Text(
                text = stringResource(mainText),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.focusable(),
            )
            Text(
                text = stringResource(subtitleText),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.focusable(),
            )
        }
        Spacer(modifier = Modifier.weight(0.05f))
        if (granted) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterVertically),
                tint = MaterialTheme.colorScheme.onSurface
            )
        } else {
            Icon(
                Icons.Default.Cancel,
                contentDescription = null,
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
    Navigating(onContinue = {}, permissionsStatus = emptyMap(), onPermissionResult = { _, _ -> })
}

@Preview(device = "spec:parent=pixel_5,orientation=portrait")
@Composable
fun PermissionRationalePreview() {
    SoundscapeTheme {
        PermissionRationale(
            Icons.Rounded.LocationOn,
            R.string.first_launch_permissions_location,
            R.string.first_launch_permissions_required,
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
            R.string.first_launch_permissions_location,
            R.string.first_launch_permissions_required,
            false,
            {}
        )
    }
}