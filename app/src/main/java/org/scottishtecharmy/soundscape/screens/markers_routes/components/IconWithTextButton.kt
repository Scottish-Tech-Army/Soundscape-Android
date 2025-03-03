package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.scottishtecharmy.soundscape.screens.talkbackHint
import org.scottishtecharmy.soundscape.ui.theme.extraSmallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun IconWithTextButton(
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier.size(spacing.targetSize),
    textModifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.ChevronLeft,
    contentDescription: String? = null,
    text: String = "",
    talkbackHint: String = "",
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    fontWeight: FontWeight = FontWeight.Bold,
    fontSize: TextUnit = 22.sp,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clickable { onClick() }
            .defaultMinSize(minHeight = spacing.icon, minWidth = spacing.icon)
            .extraSmallPadding()
            .talkbackHint(talkbackHint)
        ,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = iconModifier, // Only modifies the set Icon
            imageVector = icon,
            contentDescription = contentDescription,
        )
        if (text.isNotEmpty()) {
            Text(
                modifier = textModifier,
                text = text,
                style = textStyle.copy(
                    fontWeight = fontWeight,
                    fontSize = fontSize
                )
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