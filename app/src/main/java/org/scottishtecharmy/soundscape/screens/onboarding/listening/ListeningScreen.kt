package org.scottishtecharmy.soundscape.screens.onboarding.listening

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.OnboardButton
import org.scottishtecharmy.soundscape.screens.onboarding.BoxWithGradientBackground

@Composable
fun ListeningScreen(
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Listening(
        modifier = modifier,
        onNavigate = onNavigate
    )
}

@Composable
fun Listening(
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val landscape = (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE)
    val alignment = if(landscape)
        Alignment.CenterVertically
    else
        Alignment.Top

    BoxWithGradientBackground(modifier = modifier) {
        Row(
            verticalAlignment = alignment,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
        ) {
            if (landscape) {
                Column(modifier = Modifier.fillMaxHeight()) {
                    LocalImage()
                }
            }
            Column(
                modifier = Modifier
                    .padding(top = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                if (!landscape) {
                    LocalImage()
                    Spacer(modifier = Modifier.height(50.dp))
                }

                Column(modifier = Modifier.padding(horizontal = 30.dp)) {
                    Text(
                        text = stringResource(R.string.first_launch_headphones_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics {
                            heading()
                        }
                    )
                    Spacer(modifier = Modifier.height(50.dp))
                    Text(
                        text = stringResource(R.string.first_launch_headphones_message_1),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.first_launch_headphones_message_2),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(50.dp))

                    OnboardButton(
                        text = stringResource(R.string.ui_continue),
                        onClick = { onNavigate() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalImage() {
    Image(
        painter = painterResource(R.drawable.ic_listening),
        contentDescription = null,
    )
}


@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Preview
@Composable
fun ListeningPreview() {
    Listening(onNavigate = {})
}