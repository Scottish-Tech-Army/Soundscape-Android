package org.scottishtecharmy.soundscape.screens.onboarding

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.scottishtecharmy.soundscape.OnboardingActivity
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.OnboardButton
import org.scottishtecharmy.soundscape.ui.theme.IntroTypography
import org.scottishtecharmy.soundscape.ui.theme.IntroductionTheme

fun Context.getActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

@Composable
private fun LocalImage() {
    Image(
        painter = painterResource(R.drawable.ic_finish),
        contentDescription = null,
        modifier = Modifier
            .padding(24.dp)
            .clip(RoundedCornerShape(90.dp))
    )
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Preview
@Composable
fun Finish() {
    val context = LocalContext.current

    val landscape = (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE)
    val alignment = if(landscape)
        Alignment.CenterVertically
    else
        Alignment.Top

    IntroductionTheme {
        MaterialTheme(typography = IntroTypography) {
            Row(
                verticalAlignment = alignment,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
            ) {
                if (landscape) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight(),
                    ) {
                        LocalImage()
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxHeight(),
                ) {
                    if (!landscape) {
                        LocalImage()
                        Spacer(modifier = Modifier.height(50.dp))
                    }

                    Column(
                        modifier = Modifier.padding(horizontal = 50.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.first_launch_prompt_title),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(30.dp))

                        Text(
                            text = stringResource(R.string.first_launch_prompt_message),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(50.dp))

                        OnboardButton(
                            text = stringResource(R.string.first_launch_prompt_button),
                            onClick = {
                                // Tell our OnboardingActivity that we have completed onboarding
                                (context.getActivity() as OnboardingActivity).onboardingComplete()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
