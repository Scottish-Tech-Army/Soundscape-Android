package org.scottishtecharmy.soundscape.screens.markers_routes.components

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.ScreensForMarkersAndRoutes
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

@Composable
fun MarkersAndRoutesTabs(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val items = listOf(
        ScreensForMarkersAndRoutes.Markers,
        ScreensForMarkersAndRoutes.Routes,
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = selectedTabIndex == index
            NavigationBarItem(
                modifier = Modifier.padding(16.dp),
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        Log.d("MarkersAndRoutesTabs", "Navigating to ${item.route}, current index: $index")
                        onTabSelected(index)

                    } else {
                        Log.d("MarkersAndRoutesTabs", "Tab already selected: ${item.route}, index: $index")
                    }
                },
                label = {
                    Text(item.title, style = MaterialTheme.typography.bodyLarge)
                },
                icon = {
                    Image(
                        painter = painterResource(
                            id = if (isSelected) {
                                item.selectedIconResId!!
                            } else {
                                item.unselectedIconResId!!
                            }
                        ),
                        contentDescription = null, // No need for contentDescription as text is below
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.surfaceDim
                )
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MarkerTabsPreview() {
    SoundscapeTheme {
        // Preview with first tab selected
        MarkersAndRoutesTabs(
            selectedTabIndex = 0,
            onTabSelected = { /* Handle tab selection */ }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RoutesTabsPreview() {
    SoundscapeTheme {
        // Preview with first tab selected
        MarkersAndRoutesTabs(
            selectedTabIndex = 1,
            onTabSelected = { /* Handle tab selection */ }
        )
    }
}