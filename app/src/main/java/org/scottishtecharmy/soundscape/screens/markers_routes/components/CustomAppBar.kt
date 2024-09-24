package org.scottishtecharmy.soundscape.screens.markers_routes.components

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
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAppBar(customTitle : String,
                 customContentDescription : String,
                 navController: NavController,
                 showAddIcon: Boolean = false,
                 addIconLink : String = "",
                 addIconDescription : String = "") {

    CenterAlignedTopAppBar(
        navigationIcon = {
            CustomIconButton(
                modifier = Modifier.width(80.dp),
                iconModifier = Modifier.size(40.dp),
                onClick = {
//                    navController.popBackStack(MainScreens.Home.route, false)
                    navController.navigate(HomeRoutes.Home.route) {
                        popUpTo(HomeRoutes.Home.route) {
                            inclusive = false  // Ensures Home screen is not popped from the stack
                        }
                        launchSingleTop = true  // Prevents multiple instances of Home
                    }
                },
                iconText = stringResource(R.string.ui_back_button_title),
                contentDescription = stringResource(R.string.ui_back_button_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        },
        actions = {
            // Only show add route icon when on the routes tab
            AnimatedVisibility(
                visible = showAddIcon,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                CustomIconButton(
                    modifier = Modifier.defaultMinSize(48.dp),
                    iconModifier = Modifier.size(30.dp),
                    onClick = {navController.navigate(addIconLink)},
                    icon = Icons.Default.Add,
                    contentDescription = addIconDescription,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        title = {
            CustomAppBarTitle(
                modifier = Modifier.semantics { contentDescription = customContentDescription },
                title = customTitle,
                contentAlignment = Alignment.Center
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
fun CustomAppBarPreview() {
    SoundscapeTheme {
        CustomAppBar(
            "Test app bar",
            "An app bar test screen",
            showAddIcon = false,
            navController = rememberNavController()
        )
    }

}