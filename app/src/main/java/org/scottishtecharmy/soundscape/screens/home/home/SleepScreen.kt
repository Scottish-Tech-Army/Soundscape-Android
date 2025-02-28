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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.ui.theme.largePadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

private fun exitSleep(context: MainActivity, navController: NavHostController) {
    context.setServiceState(true)
    navController.popBackStack(HomeRoutes.Home.route, false)
}

@Composable
fun SleepScreenVM(
    navController: NavHostController,
    modifier: Modifier
) {
    val context = LocalActivity.current as MainActivity

    BackHandler(enabled = true) {
        // If the user presses the back button come out of sleep
        exitSleep(context, navController)
    }

    SleepScreen( { exitSleep(context, navController) } , modifier)
}

@Composable
fun SleepScreen(exitSleep: () -> Unit = {},
                modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row {
            Text(
                text = stringResource(R.string.sleep_sleeping),
                style = MaterialTheme.typography.titleLarge,
                modifier = modifier.largePadding(),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row {
            Text(
                text = stringResource(R.string.sleep_sleeping_message),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = modifier.largePadding()
            )
        }
        Row {
            Button(
                onClick = { exitSleep() },
                modifier = modifier.fillMaxWidth().height(spacing.targetSize * 4),
                shape = RoundedCornerShape(spacing.tiny),
                colors = ButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.38f)
                )
            ) {
                Text(
                    text = stringResource(R.string.sleep_wake_up_now),
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.displaySmall,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SleepScreenPreview() {
    SleepScreen(
        modifier = Modifier
    )
}
