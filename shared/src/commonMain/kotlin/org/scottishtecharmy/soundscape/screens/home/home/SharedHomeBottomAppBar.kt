package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.audio.TourButton
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.ahead_of_me_24px
import org.scottishtecharmy.soundscape.resources.around_me_24px
import org.scottishtecharmy.soundscape.resources.callouts_panel_title
import org.scottishtecharmy.soundscape.resources.my_location_24px
import org.scottishtecharmy.soundscape.resources.nearby_markers_24px
import org.scottishtecharmy.soundscape.resources.ui_action_button_ahead_of_me
import org.scottishtecharmy.soundscape.resources.ui_action_button_ahead_of_me_acc_hint
import org.scottishtecharmy.soundscape.resources.ui_action_button_around_me
import org.scottishtecharmy.soundscape.resources.ui_action_button_around_me_acc_hint
import org.scottishtecharmy.soundscape.resources.ui_action_button_my_location
import org.scottishtecharmy.soundscape.resources.ui_action_button_my_location_acc_hint
import org.scottishtecharmy.soundscape.resources.ui_action_button_nearby_markers
import org.scottishtecharmy.soundscape.resources.ui_action_button_nearby_markers_acc_hint
import org.scottishtecharmy.soundscape.ui.theme.currentAppButtonColors
import org.scottishtecharmy.soundscape.ui.theme.spacing

data class BottomButtonFunctions(
    val myLocation: () -> Unit = {},
    val aroundMe: () -> Unit = {},
    val aheadOfMe: () -> Unit = {},
    val nearbyMarkers: () -> Unit = {},
)

data class RouteFunctions(
    val skipPrevious: () -> Unit = {},
    val skipNext: () -> Unit = {},
    val mute: () -> Unit = {},
    val stop: () -> Unit = {},
)

data class SearchFunctions(
    val onTriggerSearch: (String) -> Unit = {},
)

data class StreetPreviewFunctions(
    val go: () -> Unit = {},
    val exit: () -> Unit = {},
)

@Composable
fun SharedHomeBottomAppBar(
    bottomButtonFunctions: BottomButtonFunctions,
    modifier: Modifier = Modifier,
    activeCallout: TourButton? = null,
) {
    val myLocationHint = stringResource(Res.string.ui_action_button_my_location_acc_hint)
    val nearbyMarkersHint = stringResource(Res.string.ui_action_button_nearby_markers_acc_hint)
    val aroundMeHint = stringResource(Res.string.ui_action_button_around_me_acc_hint)
    val aheadOfMeHint = stringResource(Res.string.ui_action_button_ahead_of_me_acc_hint)

    Surface(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    spacing.extraSmall,
                    spacing.extraSmall,
                    spacing.none,
                    spacing.extraSmall,
                ),
            ),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.small),
            modifier = Modifier
                .padding(bottom = spacing.small)
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Text(
                textAlign = TextAlign.Start,
                text = stringResource(Res.string.callouts_panel_title).uppercase(),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(start = spacing.medium, top = spacing.medium)
                    .semantics { heading() },
                style = MaterialTheme.typography.labelSmall,
            )
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
            ) {
                HomeBottomAppBarButton(
                    icon = painterResource(Res.drawable.my_location_24px),
                    text = stringResource(Res.string.ui_action_button_my_location),
                    onClick = { bottomButtonFunctions.myLocation() },
                    isActive = activeCallout == TourButton.MY_LOCATION,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics { onClick(label = myLocationHint, action = { false }) }
                        .testTag("homeMyLocation"),
                )

                HomeBottomAppBarButton(
                    icon = painterResource(Res.drawable.around_me_24px),
                    text = stringResource(Res.string.ui_action_button_around_me),
                    onClick = { bottomButtonFunctions.aroundMe() },
                    isActive = activeCallout == TourButton.AROUND_ME,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics { onClick(label = aroundMeHint, action = { false }) }
                        .testTag("homeAroundMe"),
                )

                HomeBottomAppBarButton(
                    icon = painterResource(Res.drawable.ahead_of_me_24px),
                    text = stringResource(Res.string.ui_action_button_ahead_of_me),
                    onClick = { bottomButtonFunctions.aheadOfMe() },
                    isActive = activeCallout == TourButton.AHEAD_OF_ME,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics { onClick(label = aheadOfMeHint, action = { false }) }
                        .testTag("homeAheadOfMe"),
                )

                HomeBottomAppBarButton(
                    icon = painterResource(Res.drawable.nearby_markers_24px),
                    text = stringResource(Res.string.ui_action_button_nearby_markers),
                    onClick = { bottomButtonFunctions.nearbyMarkers() },
                    isActive = activeCallout == TourButton.NEARBY_MARKERS,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics { onClick(label = nearbyMarkersHint, action = { false }) }
                        .testTag("homeNearbyMarkers"),
                )
            }
        }
    }
}

@Composable
private fun HomeBottomAppBarButton(
    icon: Painter,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
) {
    // Legacy iOS pulsed a `LineScaleParty` NVActivityIndicator inside the
    // button while the callout audio played. Equivalent here: the button
    // flips to a solid high-contrast highlight (theme primary) for the
    // duration of the callout, and the icon pulses in scale for a motion
    // cue. Stops the moment the callout finishes or the user cancels.
    val infiniteTransition = rememberInfiniteTransition(label = "calloutPulse")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "iconScale",
    )

    val colorScheme = MaterialTheme.colorScheme
    val restingColors = currentAppButtonColors
    val activeColors = if (isActive) {
        ButtonDefaults.buttonColors(
            containerColor = colorScheme.primary,
            contentColor = colorScheme.onPrimary,
            disabledContainerColor = restingColors.disabledContainerColor,
            disabledContentColor = restingColors.disabledContentColor,
        )
    } else {
        restingColors
    }

    Button(
        onClick = onClick,
        shape = RectangleShape,
        modifier = modifier,
        contentPadding = PaddingValues(spacing.extraSmall),
        colors = activeColors,
    ) {
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxHeight().fillMaxWidth(),
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(spacing.icon)
                    .align(Alignment.CenterHorizontally)
                    .graphicsLayer {
                        val s = if (isActive) iconScale else 1f
                        scaleX = s
                        scaleY = s
                    },
            )
            Spacer(modifier = Modifier.height(spacing.small))
            Text(
                text = text,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
