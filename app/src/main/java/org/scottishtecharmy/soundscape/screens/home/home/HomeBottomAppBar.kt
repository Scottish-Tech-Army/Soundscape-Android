package org.scottishtecharmy.soundscape.screens.home.home

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

@Composable
fun HomeBottomAppBar(
    getMyLocation: () -> Unit,
    getWhatsAroundMe: () -> Unit,
    getWhatsAheadOfMe: () -> Unit,
    modifier : Modifier = Modifier
) {
    val context = LocalContext.current
    val notAvailableText = "This is not implemented yet."
    val notAvailableToast = {
        Toast.makeText(context, notAvailableText, Toast.LENGTH_SHORT).show()
    }


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
                    onClick = getMyLocation,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )

                HomeBottomAppBarButton(
                    icon = painterResource(R.drawable.around_me_24px),
                    text = stringResource(R.string.ui_action_button_around_me),
                    onClick = getWhatsAroundMe,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )

                HomeBottomAppBarButton(
                    icon = painterResource(R.drawable.ahead_of_me_24px),
                    text = stringResource(R.string.ui_action_button_ahead_of_me),
                    onClick = notAvailableToast,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )

                HomeBottomAppBarButton(
                    icon = painterResource(R.drawable.nearby_markers_24px),
                    text = stringResource(R.string.ui_action_button_nearby_markers),
                    onClick = getWhatsAheadOfMe,
                    modifier = Modifier.weight(1f).fillMaxHeight()
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
        contentPadding = PaddingValues(0.dp)
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
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Preview
@Composable
fun PreviewHomeBottomAppBar(){
    SoundscapeTheme {
        HomeBottomAppBar(
            getMyLocation = {},
            getWhatsAheadOfMe = {},
            getWhatsAroundMe = {}
        )
    }
}

@Preview(fontScale = 2.0f)
@Composable
fun PreviewHomeBottomAppBarLarge(){
    SoundscapeTheme {
        HomeBottomAppBar(
            getMyLocation = {},
            getWhatsAheadOfMe = {},
            getWhatsAroundMe = {}
        )
    }
}