package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.ui.theme.extraSmallPadding
import org.scottishtecharmy.soundscape.resources.*

// FlexibleAppBar composable is now in shared module

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
                text = stringResource(Res.string.ui_back_button_title),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        rightSide = {
            Text(
                modifier = Modifier
                    .clickable { }
                    .extraSmallPadding(),
                text = stringResource(Res.string.general_alert_done),
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
                text = stringResource(Res.string.ui_back_button_title),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        rightSide = { }
    )
}
