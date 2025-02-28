package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.scottishtecharmy.soundscape.R

@Composable
fun MarkersAndRoutesAppBar(
   onNavigateUp: () -> Unit,
) {

    CustomAppBar(
        title = stringResource(R.string.search_view_markers),
        navigationButtonTitle = stringResource(R.string.ui_back_button_title),
        onNavigateUp = onNavigateUp,
    )
}

@Preview(showBackground = true)
@Composable
fun MarkersAndRoutesAppBarPreview() {
    MarkersAndRoutesAppBar(
        onNavigateUp = {},
    )
}