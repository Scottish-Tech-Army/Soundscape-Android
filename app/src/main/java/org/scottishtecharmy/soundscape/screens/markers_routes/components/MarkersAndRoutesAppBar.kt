package org.scottishtecharmy.soundscape.screens.markers_routes.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.screens.home.MainScreens
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkersAndRoutesAppBar(showAddIcon: Boolean, navController: NavController) {

    CustomAppBar(stringResource(R.string.markers_routes_app_bar_title),
                 LocalContext.current.getString(R.string.marker_and_routes_screen_title_description),
                 navController,
                 showAddIcon,
                 MainScreens.AddRoute.route,
                 stringResource(R.string.markers_and_routes_add_route_button_description)
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