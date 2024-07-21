package com.kersnazzle.soundscapealpha.screens.onboarding

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getString
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.kersnazzle.soundscapealpha.R
import com.kersnazzle.soundscapealpha.components.OnboardButton
import com.kersnazzle.soundscapealpha.screens.navigation.Screens
import com.kersnazzle.soundscapealpha.ui.theme.IntroTypography
import com.kersnazzle.soundscapealpha.ui.theme.IntroductionTheme

@Composable
fun Listening(navController: NavHostController) {
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
                    painter = painterResource(R.drawable.ic_listening),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2.1f)
                )

                Spacer(modifier = Modifier.height(50.dp))

                Column(modifier = Modifier.padding(horizontal = 50.dp)) {
                    Text(
                        text = stringResource(R.string.first_launch_headphones_title),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(50.dp))
                    Text(
                        text = stringResource(R.string.first_launch_headphones_message_1),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.first_launch_headphones_message_2),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(50.dp))

                    OnboardButton(
                        text = stringResource(R.string.ui_continue),
                        onClick = { navController.navigate(Screens.Hearing.route) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun ListeningPreview() {
    Listening(navController = rememberNavController())
}