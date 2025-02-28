package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import org.scottishtecharmy.soundscape.R

@Composable
fun CustomTextButton(
    modifier: Modifier = Modifier, // Modifier for button
    onClick: () -> Unit,
    text: String, // Button text
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    fontWeight: FontWeight
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = textStyle,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(fontScale = 1.5f, showBackground = true)
@Preview(showBackground = true)
@Composable
fun CustomTextButtonPreview() {
    CustomTextButton(
        onClick = { /*TODO*/ },
        text = stringResource(R.string.general_alert_done),
        textStyle = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}