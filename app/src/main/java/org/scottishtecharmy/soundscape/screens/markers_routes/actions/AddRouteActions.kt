package org.scottishtecharmy.soundscape.screens.markers_routes.actions

import android.content.Context
import android.widget.Toast
import androidx.navigation.NavController
import org.scottishtecharmy.soundscape.screens.home.MainScreens
import org.scottishtecharmy.soundscape.screens.markers_routes.validators.validateFields

// Function to handle "Add Waypoints" button click
fun onAddWaypointsClicked(
    context: Context,
    navController: NavController,
    name: String,
    description: String,
    setNameError: (Boolean) -> Unit,
    setDescriptionError: (Boolean) -> Unit

) {
    val notAvailableText = "This is not implemented yet."
    val notAvailableToast = {
        Toast.makeText(context, notAvailableText, Toast.LENGTH_SHORT).show()}
    val (isValid, errorStates) = validateFields(name, description)
    setNameError(!errorStates.first)
    setDescriptionError(!errorStates.second)

    if (!errorStates.first || !errorStates.second) {
//             Navigate to the MarkersAndRoutesScreen, showing the Routes tab
        navController.navigate("${MainScreens.MarkersAndRoutes.route}/routes")
        {
            popUpTo(MainScreens.MarkersAndRoutes.route) {
                inclusive = true
            }
            launchSingleTop = true
        }
    } else {
//            navController.navigate("target_screen_route")
        notAvailableToast()

    }
}


