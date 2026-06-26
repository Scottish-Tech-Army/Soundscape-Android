package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.screens.talkbackDescription
import org.scottishtecharmy.soundscape.screens.talkbackLive
import org.scottishtecharmy.soundscape.ui.theme.extraSmallPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlexibleAppBar(title : String = "",
                   leftSide: @Composable () -> Unit = {},
                   rightSide: @Composable () -> Unit = {},
) {

    CenterAlignedTopAppBar(
        // We want to announce the name of the screen, but not include it in the swipe navigation
        title = {
            Text(
                modifier = Modifier
                    .semantics { heading() }
                    .talkbackLive()
                    .talkbackDescription(title),
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        navigationIcon = leftSide,
        actions = { rightSide() }
    )
}

@Preview(showBackground = true, fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun FlexibleAppBarPreview() {
    FlexibleAppBar(
        title = "Test app bar",
        leftSide = {
            Text(
                modifier = Modifier
                    .clickable { }
                    .extraSmallPadding(),
                text = stringResource(R.string.ui_back_button_title),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        rightSide = {
            Text(
                modifier = Modifier
                    .clickable { }
                    .extraSmallPadding(),
                text = stringResource(R.string.general_alert_done),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge
            )
        }
    )
}

@Preview(showBackground = true, fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun FlexibleAppBarEmptyPreview() {
    FlexibleAppBar(
        title = "Test app bar",
        leftSide = {
            Text(
                modifier = Modifier
                    .clickable { }
                    .extraSmallPadding(),
                text = stringResource(R.string.ui_back_button_title),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        rightSide = { }
    )
}