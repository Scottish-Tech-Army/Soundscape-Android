package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.resources.*

// CustomAppBar composable is now in shared module

@Preview(showBackground = true, fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun CustomAppBarPreview() {
    CustomAppBar(
        "Test app bar with long title",
        navigationButtonTitle = stringResource(Res.string.ui_back_button_title),
        onNavigateUp = {},
    )
}

@Preview(showBackground = true, fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun CustomAppBarWithActionButtonPreview() {
    CustomAppBar(
        "Test app bar",
        navigationButtonTitle = stringResource(Res.string.ui_back_button_title),
        onNavigateUp = {},
    )
}

@Preview(showBackground = true, fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun CustomAppBarWithRightButtonPreview() {
    CustomAppBar(
        "Test app bar",
        navigationButtonTitle = stringResource(Res.string.ui_back_button_title),
        onNavigateUp = {},
        rightButtonTitle = stringResource(Res.string.general_alert_done),
        onRightButton = {},
    )
}
