package org.scottishtecharmy.soundscape.screens.markers_routes.marker_route_screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme


@Composable
fun RoutesScreen(
    navController: NavController,
    onNavigateToAddRoute: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.semantics(mergeDescendants = true) {}
            ) {
            Box(modifier = Modifier.padding(top = 40.dp)) {
                Image(
                    painter = painterResource(
                        id = R.drawable.routes_selected
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp)
                )
            }
            Box(modifier = Modifier.padding(top = 10.dp)) {
                Text(
                    stringResource(R.string.routes_no_routes_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
            }
            Box(modifier = Modifier.padding(top = 10.dp)) {
                Text(
                    stringResource(R.string.routes_no_routes_hint_1),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
            Box(modifier = Modifier.padding(top = 10.dp, bottom = 40.dp)) {
                Text(
                    stringResource(R.string.routes_no_routes_hint_2),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
        IconButton(
            onClick = {
                onNavigateToAddRoute.invoke()
            },
            modifier = Modifier.size(90.dp)
        ){
            Icon(
                Icons.Rounded.AddCircleOutline,
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = stringResource(R.string.general_alert_add),
                modifier = Modifier.size(80.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RoutesScreenPreview() {
    SoundscapeTheme {
        RoutesScreen(
            navController = rememberNavController(),
            onNavigateToAddRoute = {}
        )
    }
}