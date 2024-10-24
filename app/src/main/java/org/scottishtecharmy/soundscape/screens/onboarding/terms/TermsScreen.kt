package org.scottishtecharmy.soundscape.screens.onboarding.terms

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.OnboardButton
import org.scottishtecharmy.soundscape.screens.onboarding.component.BoxWithGradientBackground
import org.scottishtecharmy.soundscape.ui.theme.Primary


@Composable
fun TermsScreen(
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val terms = getAllTerms()
    val checkedState = remember { mutableStateOf(false) }
    BoxWithGradientBackground(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 30.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = stringResource(R.string.terms_of_use_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .semantics {
                        heading()
                    },
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
                    .fillMaxWidth()
                    .toggleable( // This give the toggleable behaviour to the entire row, providing a better UX with alpha() Screen reader
                        value = checkedState.value,
                        role = Role.Checkbox,
                    ){
                        checkedState.value = !checkedState.value
                    },
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Checkbox(
                    checked = checkedState.value,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        uncheckedColor = MaterialTheme.colorScheme.onPrimary,
                    )
                )
                Text(
                    modifier = Modifier.clickable { checkedState.value = !checkedState.value },
                    text = stringResource(R.string.terms_of_use_accept_checkbox_acc_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp)
            ) {
                OnboardButton(
                    text = stringResource(R.string.ui_continue),
                    onClick = {
                        onNavigate()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = checkedState.value
                )
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

@Composable
fun getAllTerms(): List<Terms> {
    return listOf(
        Terms(
            name = stringResource(R.string.terms_of_use_message)
        ),
        Terms(
            name = stringResource(R.string.terms_of_use_medical_safety_disclaimer)
        )
    )
}


@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Preview
@Composable
fun TermsPreview() {
    TermsScreen(onNavigate = {})
}