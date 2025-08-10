package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.sp
import org.scottishtecharmy.soundscape.screens.talkbackHint
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.ui.theme.tinyPadding

@Composable
fun IconWithTextButton(
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier.size(spacing.icon),
    textModifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.ChevronLeft,
    contentDescription: String? = null,
    text: String = "",
    talkbackHint: String = "",
    textStyle: TextStyle = MaterialTheme.typography.labelSmall,
    fontWeight: FontWeight = FontWeight.Bold,
    fontSize: TextUnit = 18.sp,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clickable { onClick() }
            .tinyPadding()
            .talkbackHint(talkbackHint),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = iconModifier, // Only modifies the set Icon
            imageVector = icon,
            contentDescription = contentDescription,
            tint = color
        )
        if (text.isNotEmpty()) {
            Text(
                modifier = textModifier,
                text = text,
                style = textStyle.copy(
                    fontWeight = fontWeight,
                    fontSize = fontSize
                ),
                color = color
            )
        }
    }
}

@Preview(fontScale = 1.5f, showBackground = true)
@Preview(showBackground = true)
@Composable
fun CustomIconButtonPreview(){
    IconWithTextButton(
        text = "Back",
        onClick = { /*TODO*/ }

    )
}