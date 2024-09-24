package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkersAndRoutesAppBar(showAddIcon: Boolean, navController: NavController) {

    CustomAppBar(stringResource(R.string.search_view_markers),
                 LocalContext.current.getString(R.string.search_view_markers),
                 navController,
                 showAddIcon,
                 HomeRoutes.AddRoute.route,
                 stringResource(R.string.general_alert_add)
    )
}

@Preview(showBackground = true)
@Composable
fun MarkersAndRoutesAppBarPreview() {
    SoundscapeTheme {
        MarkersAndRoutesAppBar(
            showAddIcon = true,
            navController = rememberNavController()
        )
    }
}