package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkersAndRoutesAppBar(showAddIcon: Boolean,
                           onNavigateUp: () -> Unit,
                           onNavigateToDestination: () -> Unit
) {

    CustomAppBar(
        customTitle = stringResource(R.string.search_view_markers),
        onNavigateUp = onNavigateUp,
        showAddIcon = showAddIcon,
        onAddClicked = onNavigateToDestination,
    )
}

@Preview(showBackground = true)
@Composable
fun MarkersAndRoutesAppBarPreview() {
    SoundscapeTheme {
        MarkersAndRoutesAppBar(
            showAddIcon = true,
            onNavigateUp = {},
            onNavigateToDestination = {}

        )
    }
}