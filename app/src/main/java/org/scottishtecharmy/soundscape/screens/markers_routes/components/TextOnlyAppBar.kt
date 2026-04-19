package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.resources.*

// TextOnlyAppBar composable is now in shared module

@Preview(showBackground = true, fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun TextOnlyBarPreview() {
    TextOnlyAppBar(
        "Test app bar with long title",
        navigationButtonTitle = stringResource(Res.string.ui_back_button_title),
        onNavigateUp = {},
        rightButtonTitle = stringResource(Res.string.general_alert_done),
        onRightButton = {}
    )
}

@Preview(showBackground = true, fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun TextOnlyBarPreviewWithActionButtonPreview() {
    TextOnlyAppBar(
        "Test app bar",
        navigationButtonTitle = stringResource(Res.string.ui_back_button_title),
        onNavigateUp = {},
        rightButtonTitle = stringResource(Res.string.general_alert_done),
        onRightButton = {}
    )
}
