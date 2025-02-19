package org.scottishtecharmy.soundscape.screens.home.home

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.ui.theme.Foreground2
import org.scottishtecharmy.soundscape.ui.theme.OnSurface
import org.scottishtecharmy.soundscape.ui.theme.PurpleGradientDark
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

private fun exitSleep(context: MainActivity, navController: NavHostController) {
    context.setServiceState(true)
    navController.popBackStack(HomeRoutes.Home.route, false)
}

@Composable
fun SleepScreen(navController: NavHostController,
                modifier: Modifier) {
    val context = LocalActivity.current as MainActivity

    BackHandler(enabled = true) {
        // If the user presses the back button come out of sleep
        exitSleep(context, navController)
    }

    Column(
        modifier = modifier.background(PurpleGradientDark).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row {
            Text(
                text = stringResource(R.string.sleep_sleeping),
                style = MaterialTheme.typography.titleLarge,
                color = Foreground2,
                modifier = modifier.padding(40.dp)
            )
        }
        Row {
            Text(
                text = stringResource(R.string.sleep_sleeping_message),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurface,
                modifier = modifier.padding(40.dp)
            )
        }
        Row {
            Button(
                onClick = {
                    exitSleep(context, navController)
                },
                modifier = modifier.fillMaxWidth().height(200.dp),
                shape = RoundedCornerShape(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.sleep_wake_up_now),
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SleepScreenPreview() {
    SoundscapeTheme {
        SleepScreen(
            navController = rememberNavController(),
            modifier = Modifier
        )
    }
}
