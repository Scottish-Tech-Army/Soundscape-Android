package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import org.scottishtecharmy.soundscape.ui.theme.TopBarTypography

@Composable
fun CustomIconButton(
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: ImageVector = Icons.Default.ChevronLeft,
    contentDescription: String = "",
    iconText: String = "",
    textColor: Color = MaterialTheme.colorScheme.onPrimary, // Icon text colour White set as with default
    iconTint: Color = MaterialTheme.colorScheme.onPrimary, // Icon colour with White set as default
    textStyle: TextStyle = TopBarTypography.titleLarge, // TextStyle with set default value
    fontWeight: FontWeight = FontWeight.Normal, // FontWeight with set default value
    fontSize: TextUnit = 18.sp // FontSize with set default value
) {
    IconButton(
        modifier = modifier, // IconButton's Modifier
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = iconModifier, // Only modifies the set Icon
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
            )
            if (iconText.isNotEmpty()) {
                Text(
                    text = iconText,
                    color = textColor,
                    style = textStyle.copy(
                        fontWeight = fontWeight,
                        fontSize = fontSize
                    )
                )
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun CustomIconButtonPreview(){
    SoundscapeTheme {
        CustomIconButton(
            modifier = Modifier.width(80.dp),
            iconText = "Back",
            onClick = { /*TODO*/ }

        )

    }
}