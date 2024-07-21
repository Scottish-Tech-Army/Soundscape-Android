package com.kersnazzle.soundscapealpha.screens.onboarding

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.kersnazzle.soundscapealpha.R
import com.kersnazzle.soundscapealpha.components.OnboardButton
import com.kersnazzle.soundscapealpha.screens.navigation.Screens
import com.kersnazzle.soundscapealpha.ui.theme.IntroTypography
import com.kersnazzle.soundscapealpha.ui.theme.IntroductionTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun Hearing(navController: NavHostController) {

    IntroductionTheme {
        MaterialTheme(typography = IntroTypography) {
            Column(
                modifier = Modifier
                    .padding(top = 50.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                Image(
                    painter = painterResource(R.drawable.ic_surroundings),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2.1f)
                )

                Spacer(modifier = Modifier.height(50.dp))

                Column(
                    modifier = Modifier.padding(horizontal = 50.dp)
                ) {
                    Text(
                        text = stringResource(R.string.first_launch_callouts_title),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(50.dp))
                    Text(
                        text = stringResource(R.string.first_launch_callouts_message),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    Text(
                        text = stringResource(R.string.first_launch_callouts_listen),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val context = LocalContext.current
                    val notAvailableText = "This is not implemented yet."
                    val notAvailableToast = {
                        Toast.makeText(context, notAvailableText, Toast.LENGTH_SHORT).show()
                    }
                    // TODO Strings to send to text-to-speech
                    // <string name="first_launch_callouts_example_1">Cafe</string>
                    // <string name="first_launch_callouts_example_3">Main Street goes left</string>
                    // <string name="first_launch_callouts_example_4">Main Street goes right</string>

                    Button(
                        onClick = { notAvailableToast() },
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(3.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.2f))
                    )
                    {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = stringResource(R.string.first_launch_callouts_listen_accessibility_label))
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    OnboardButton(
                        text = stringResource(R.string.ui_continue),
                        onClick = {
                            navController.navigate(Screens.Navigating.route)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

            }
        }
    }

}

@Preview
@Composable
fun HearingPreview() {
    Hearing(navController = rememberNavController())
}