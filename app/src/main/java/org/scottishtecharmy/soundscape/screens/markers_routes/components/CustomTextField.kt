package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

// CustomTextField composable is now in shared module

@Preview(showBackground = true)
@Composable
fun CustomTextFieldPreview() {
    Column(
        modifier = Modifier
            .smallPadding()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        CustomTextField(
            fieldName = "Name",
            fieldHint = "Name of the marker",
            value = "The bench by the market",
            onValueChange = { },
            textStyle = TextStyle(fontSize = 18.sp),
            shape = RoundedCornerShape(spacing.extraSmall),
            isSingleLine = true
        )
        HorizontalDivider(thickness = 50.dp)
        CustomTextField(
            fieldName = "Description",
            fieldHint = "Description of marker",
            value = "",
            onValueChange = { },
            textStyle = TextStyle(fontSize = 18.sp),
            shape = RoundedCornerShape(spacing.extraSmall),
            isSingleLine = true
        )
    }
}
