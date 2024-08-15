package org.scottishtecharmy.soundscape.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.OnboardButton
import org.scottishtecharmy.soundscape.ui.theme.IntroTypography
import org.scottishtecharmy.soundscape.ui.theme.IntroductionTheme
import org.scottishtecharmy.soundscape.ui.theme.Primary

data class Terms(
    val name: String
)

fun getAllTerms(): List<Terms> {
    return listOf(
        Terms(
            name = "Your use of Soundscape is subject to the terms set out below (together, the \"Terms of Use\")"
        ),
        Terms(
            name="You acknowledge that Soundscape (1) is not designed, intended, or made available as a medical device, and (2) is not designed or intended to be a substitute for professional medical advice, diagnosis, treatment, or judgment and should not be used to replace or as a substitute for professional medical advice, diagnosis, treatment, or judgment.\n\nSafety Notice\n\nSoundscape is a navigation aid and should not be used in lieu of mobility skills, being aware of your surroundings, and good judgement. Use caution when navigating your environment as the mapping data incorporated into the Soundscape programme is captured from a third-party programme, and therefore, there may be limitations with the accuracy of the information presented."
        )
    )
}
@Composable
fun Terms(onNavigate: (String) -> Unit) {
    val terms = getAllTerms()
    val checkedState = remember { mutableStateOf(false) }
    IntroductionTheme {
        MaterialTheme(typography = IntroTypography) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 50.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = "Terms of Use",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .fillMaxWidth()
                        .height(400.dp)
                        .background(Color.White)
                ) {
                    items(terms) { term ->
                        TermsItem(
                            term.name
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .padding(top = 40.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Checkbox(
                        checked = checkedState.value,
                        onCheckedChange = { checkedState.value = it }
                    )
                    Text(
                        modifier = Modifier.clickable { checkedState.value = !checkedState.value },
                        text = "Accept Terms of Use",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp)
                ) {
                    if (checkedState.value){
                        OnboardButton(
                            text = stringResource(R.string.ui_continue),
                            onClick = {
                                onNavigate(Screens.Finish.route)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

            }
        }
    }
}

@Composable
fun TermsItem(text: String) {
    Row(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 17.dp)
            .fillMaxWidth()

    ) {
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = Primary)
    }

    HorizontalDivider(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 2.dp),
        thickness = 0.8.dp,
        color = Primary
    )
}



@Preview
@Composable
fun TermsPreview() {
    Terms(onNavigate = {})
}