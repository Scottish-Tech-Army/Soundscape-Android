package org.scottishtecharmy.soundscape.screens.onboarding.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.components.OnboardButton
import org.scottishtecharmy.soundscape.platform.isLocationPermissionDenied
import org.scottishtecharmy.soundscape.platform.isLocationPermissionGranted
import org.scottishtecharmy.soundscape.platform.isMotionPermissionAvailable
import org.scottishtecharmy.soundscape.platform.isMotionPermissionDenied
import org.scottishtecharmy.soundscape.platform.isMotionPermissionGranted
import org.scottishtecharmy.soundscape.platform.openPermissionSettings
import org.scottishtecharmy.soundscape.platform.requestLocationPermission
import org.scottishtecharmy.soundscape.platform.requestMotionPermission
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.first_launch_permissions_denied
import org.scottishtecharmy.soundscape.resources.first_launch_permissions_granted
import org.scottishtecharmy.soundscape.resources.first_launch_permissions_location
import org.scottishtecharmy.soundscape.resources.first_launch_permissions_message
import org.scottishtecharmy.soundscape.resources.first_launch_permissions_motion
import org.scottishtecharmy.soundscape.resources.first_launch_permissions_not_granted
import org.scottishtecharmy.soundscape.resources.first_launch_permissions_required
import org.scottishtecharmy.soundscape.resources.first_launch_permissions_title
import org.scottishtecharmy.soundscape.resources.ui_continue
import org.scottishtecharmy.soundscape.screens.onboarding.component.BoxWithGradientBackground
import org.scottishtecharmy.soundscape.ui.theme.spacing

/** How often to re-check iOS's permission status while this screen is on screen. */
private const val PERMISSION_POLL_INTERVAL_MS = 300L

/**
 * The two permissions iOS can ask for up front, matching Android's NavigatingScreen - each
 * is a row the user taps to trigger its system prompt, rather than one shared Continue
 * button firing every request at once.
 *
 * iOS has no single API that reports back when a permission prompt is answered (unlike
 * Android's ActivityResultContracts.RequestPermission), so [isGranted]/[isDenied] are
 * polled rather than delivered as a result.
 */
private enum class OnboardingPermission(
    val icon: ImageVector,
    val titleRes: StringResource,
    val request: () -> Unit,
    val isGranted: () -> Boolean,
    val isDenied: () -> Boolean,
) {
    LOCATION(
        Icons.Rounded.LocationOn,
        Res.string.first_launch_permissions_location,
        ::requestLocationPermission,
        ::isLocationPermissionGranted,
        ::isLocationPermissionDenied,
    ),
    MOTION(
        Icons.AutoMirrored.Rounded.DirectionsWalk,
        Res.string.first_launch_permissions_motion,
        ::requestMotionPermission,
        ::isMotionPermissionGranted,
        ::isMotionPermissionDenied,
    ),
}

private data class PermissionStatus(val granted: Boolean, val denied: Boolean)

@Composable
fun PermissionsScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Only listed if this device actually has step-counting hardware - e.g. not the
    // simulator - mirroring how NavigatingScreen only lists ACTIVITY_RECOGNITION when
    // AndroidStepProvider.hasStepSensor is true.
    val permissions = remember {
        buildList {
            add(OnboardingPermission.LOCATION)
            if (isMotionPermissionAvailable()) add(OnboardingPermission.MOTION)
        }
    }

    fun currentStatuses() = permissions.associateWith {
        PermissionStatus(granted = it.isGranted(), denied = it.isDenied())
    }

    var statuses by remember { mutableStateOf(currentStatuses()) }

    LaunchedEffect(permissions) {
        while (statuses.values.any { !it.granted }) {
            delay(PERMISSION_POLL_INTERVAL_MS)
            statuses = currentStatuses()
        }
    }

    val allGranted = statuses.values.all { it.granted }

    BoxWithGradientBackground(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = spacing.large, vertical = spacing.large)
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = stringResource(Res.string.first_launch_permissions_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(modifier = Modifier.height(spacing.large))
            Text(
                text = stringResource(Res.string.first_launch_permissions_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(spacing.large))

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(spacing.extraSmall))
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                permissions.forEach { permission ->
                    key(permission) {
                        val status = statuses[permission] ?: PermissionStatus(false, false)
                        PermissionRow(
                            icon = permission.icon,
                            mainText = permission.titleRes,
                            granted = status.granted,
                            denied = status.denied,
                            // Once denied, iOS won't show the system prompt again - send
                            // the user to Settings instead of tapping into a no-op.
                            onClick = { if (status.denied) openPermissionSettings() else permission.request() },
                            clickableTestTag = "permissionsScreenPermissionRow${permission.name}",
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.extraLarge))

            Column(modifier = Modifier.padding(horizontal = spacing.medium)) {
                OnboardButton(
                    text = stringResource(Res.string.ui_continue),
                    onClick = { onContinue() },
                    enabled = allGranted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("permissionsScreenContinueButton"),
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    mainText: StringResource,
    granted: Boolean,
    denied: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    clickableTestTag: String = "",
) {
    Row(
        modifier = modifier
            .padding(spacing.medium)
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .testTag(clickableTestTag)
            .then(
                // Once granted there's nothing left for a tap to do. Otherwise the tap
                // either shows the system prompt, or - once denied, since iOS won't show
                // that prompt again - opens Settings instead.
                if (granted) Modifier else Modifier.clickable { onClick() },
            ),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.width(spacing.extraSmall))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(mainText),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    when {
                        denied -> Res.string.first_launch_permissions_denied
                        else -> Res.string.first_launch_permissions_required
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = if (granted) Modifier.clearAndSetSemantics {} else Modifier,
            )
        }
        Spacer(modifier = Modifier.width(spacing.extraSmall))
        if (granted) {
            Icon(
                Icons.Default.Check,
                contentDescription = stringResource(Res.string.first_launch_permissions_granted),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            Icon(
                Icons.Default.Cancel,
                contentDescription = stringResource(Res.string.first_launch_permissions_not_granted),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
