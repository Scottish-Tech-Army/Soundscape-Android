package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import org.scottishtecharmy.soundscape.screens.markers_routes.navigation.ScreensForMarkersAndRoutes
import org.scottishtecharmy.soundscape.viewmodels.home.HomeViewModel

val items = listOf(
    ScreensForMarkersAndRoutes.Markers,
    ScreensForMarkersAndRoutes.Routes,
)

@Composable
fun MarkersAndRoutesTabsVM(viewModel: HomeViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MarkersAndRoutesTabs(
        state.routesTabSelected,
        setRoutesAndMarkersTab = { viewModel.setRoutesAndMarkersTab(it) })
}

@Composable
fun MarkersAndRoutesTabs(
    routesTabSelected: Boolean,
    setRoutesAndMarkersTab: (pickRoutes: Boolean) -> Unit) {

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        items.forEach { item ->
            val isSelected =
                routesTabSelected && (item == ScreensForMarkersAndRoutes.Routes) ||
                !routesTabSelected  && (item == ScreensForMarkersAndRoutes.Markers)

            NavigationBarItem(
                modifier = Modifier.padding(16.dp),
                selected = isSelected,
                onClick = { setRoutesAndMarkersTab(item == ScreensForMarkersAndRoutes.Routes) },
                label = {
                    Text(item.title, style = MaterialTheme.typography.bodyLarge)
                },
                icon = {
                    item.iconResId?.let {
                        Icon(painter = painterResource(it),
                            contentDescription = null, // No need of contentDescription as text is below,
                            modifier = Modifier.size(64.dp)
                        )
                    }

                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                )
            )
        }
    }
}

@Preview
@Composable
fun BottomNavigationBarPreview() {
    MaterialTheme {
        MarkersAndRoutesTabs(
            false,
            setRoutesAndMarkersTab = {}
        )
    }
}