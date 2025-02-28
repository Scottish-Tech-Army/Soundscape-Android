package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.scottishtecharmy.soundscape.screens.markers_routes.navigation.ScreensForMarkersAndRoutes
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing
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
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        items.forEach { item ->
            val isSelected =
                routesTabSelected && (item == ScreensForMarkersAndRoutes.Routes) ||
                !routesTabSelected  && (item == ScreensForMarkersAndRoutes.Markers)

            NavigationBarItem(
                modifier = Modifier.mediumPadding(),
                selected = isSelected,
                onClick = { setRoutesAndMarkersTab(item == ScreensForMarkersAndRoutes.Routes) },
                label = {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                icon = {
                    item.iconResId?.let {
                        Icon(painter = painterResource(it),
                            contentDescription = null, // No need of contentDescription as text is below,
                            modifier = Modifier.size(spacing.extraLarge)
                        )
                    }

                },
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