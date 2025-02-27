package org.scottishtecharmy.soundscape.screens.onboarding.terms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.OnboardButton
import org.scottishtecharmy.soundscape.screens.onboarding.component.BoxWithGradientBackground
import org.scottishtecharmy.soundscape.ui.theme.Primary
import org.scottishtecharmy.soundscape.ui.theme.spacing


@Composable
fun TermsScreen(
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val checkedState = remember { mutableStateOf(false) }
    BoxWithGradientBackground(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = spacing.medium, vertical = spacing.large)
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
                    .padding(bottom = spacing.medium)
                    .semantics {
                        heading()
                    },
            )

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(spacing.extraSmall))
                    .fillMaxWidth()
                    .background(Color.White)
                    .verticalScroll(rememberScrollState())
            ) {
                TermsItem(stringResource(R.string.terms_of_use_message))
                TermsItem(stringResource(R.string.terms_of_use_medical_safety_disclaimer))
            }

            Row(
                modifier = Modifier
                    .padding(top = spacing.large)
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
                    text = stringResource(R.string.terms_of_use_accept_checkbox_acc_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.large)
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
            .padding(horizontal = spacing.small, vertical = spacing.medium)
            .fillMaxWidth()

    ) {
        Spacer(modifier = Modifier.width(spacing.medium))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = Primary)
    }

    HorizontalDivider(
        modifier = Modifier
            .padding(horizontal = spacing.small, vertical = spacing.tiny),
        thickness = spacing.tiny,
        color = Primary
    )
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Preview
@Composable
fun TermsPreview() {
    TermsScreen(onNavigate = {})
}