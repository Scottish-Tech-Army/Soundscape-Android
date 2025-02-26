package org.scottishtecharmy.soundscape.screens.home.home

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.screens.home.BottomButtonFunctions
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

@Composable
fun HomeBottomAppBar(
    bottomButtonFunctions: BottomButtonFunctions,
    modifier : Modifier = Modifier
) {
    val myLocationHint = stringResource(R.string.ui_action_button_my_location_acc_hint)
    val nearbyMarkersHint = stringResource(R.string.ui_action_button_nearby_markers_acc_hint)
    val aroundMeHint = stringResource(R.string.ui_action_button_around_me_acc_hint)
    val aheadOfMeHint = stringResource(R.string.ui_action_button_ahead_of_me_acc_hint)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp, 4.dp, 0.dp, 0.dp)),
        color = MaterialTheme.colorScheme.primary
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                textAlign = TextAlign.Start,
                text = stringResource(R.string.callouts_panel_title).uppercase(),
                modifier = Modifier
                    .padding(start = 16.dp, end = 8.dp)
                    .semantics {
                        heading()
                    }
                ,
                style = MaterialTheme.typography.labelSmall,
            )
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
            ) {
                HomeBottomAppBarButton(
                    icon = painterResource(R.drawable.my_location_24px),
                    text = stringResource(R.string.ui_action_button_my_location),
                    onClick = { bottomButtonFunctions.myLocation() },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .semantics { onClick(label = myLocationHint, action = { false }) }
                )

                HomeBottomAppBarButton(
                    icon = painterResource(R.drawable.around_me_24px),
                    text = stringResource(R.string.ui_action_button_around_me),
                    onClick = { bottomButtonFunctions.aroundMe() },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .semantics { onClick(label = aroundMeHint, action = { false }) }
                )

                HomeBottomAppBarButton(
                    icon = painterResource(R.drawable.ahead_of_me_24px),
                    text = stringResource(R.string.ui_action_button_ahead_of_me),
                    onClick = { bottomButtonFunctions.aheadOfMe() },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .semantics { onClick(label = aheadOfMeHint, action = { false }) }
                )

                HomeBottomAppBarButton(
                    icon = painterResource(R.drawable.nearby_markers_24px),
                    text = stringResource(R.string.ui_action_button_nearby_markers),
                    onClick = { bottomButtonFunctions.nearbyMarkers() },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .semantics { onClick(label = nearbyMarkersHint, action = { false }) }
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
    modifier: Modifier = Modifier
) {
    Button(
        onClick = {
            onClick()
        },
        shape = RectangleShape,
        modifier = modifier,
        contentPadding = PaddingValues(4.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxHeight().fillMaxWidth()
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier =
                Modifier
                    .size(24.dp)
                    .align(Alignment.CenterHorizontally),
            )
            Spacer(
                modifier = Modifier.height(8.dp)
            )
            Text(
                text = text,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
            )

        }
    }
}

@Preview
@Composable
fun PreviewHomeBottomAppBar(){
    SoundscapeTheme {
        HomeBottomAppBar(
            BottomButtonFunctions(null)
        )
    }
}

@Preview(fontScale = 2.0f)
@Composable
fun PreviewHomeBottomAppBarLarge(){
    SoundscapeTheme {
        HomeBottomAppBar(
            BottomButtonFunctions(null)
        )
    }
}