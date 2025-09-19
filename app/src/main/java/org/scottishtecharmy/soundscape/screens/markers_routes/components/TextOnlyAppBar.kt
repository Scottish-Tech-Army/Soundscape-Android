package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.ui.theme.extraSmallPadding

@Composable
fun TextOnlyAppBar(title : String = "",
                   onNavigateUp: () -> Unit = {},
                   navigationButtonTitle: String = "    ",
                   onRightButton: () -> Unit = {},
                   rightButtonTitle: String = "    "
) {
    FlexibleAppBar(
        title = title,
        leftSide = {
            Text(
                modifier = Modifier
                    .clickable { onNavigateUp() }
                    .extraSmallPadding()
                    .testTag("flexibleAppBarLeft"),
                text = navigationButtonTitle,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        rightSide = {
            Text(
                modifier = Modifier
                    .clickable { onRightButton() }
                    .extraSmallPadding()
                    .testTag("flexibleAppBarRight"),
                text = rightButtonTitle,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    )
}

@Preview(showBackground = true, fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun TextOnlyBarPreview() {
    TextOnlyAppBar(
        "Test app bar with long title",
        navigationButtonTitle = stringResource(R.string.ui_back_button_title),
        onNavigateUp = {},
        rightButtonTitle = stringResource(R.string.general_alert_done),
        onRightButton = {}
    )
}

@Preview(showBackground = true, fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun TextOnlyBarPreviewWithActionButtonPreview() {
    TextOnlyAppBar(
        "Test app bar",
        navigationButtonTitle = stringResource(R.string.ui_back_button_title),
        onNavigateUp = {},
        rightButtonTitle = stringResource(R.string.general_alert_done),
        onRightButton = {}
    )
}