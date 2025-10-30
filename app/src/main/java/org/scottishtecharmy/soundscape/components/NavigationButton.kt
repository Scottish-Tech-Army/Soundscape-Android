package org.scottishtecharmy.soundscape.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import org.scottishtecharmy.soundscape.ui.theme.currentAppButtonColors
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun NavigationButton(
    onClick: () -> Unit = {},
    text: String,
    icon: ImageVector? = null,
    horizontalPadding: Dp = spacing.medium,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { onClick() },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        shape = RoundedCornerShape(spacing.none),
        colors = if (!LocalInspectionMode.current) currentAppButtonColors else ButtonDefaults.buttonColors()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = spacing.small,
                    vertical = spacing.extraSmall
                ),
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    null
                )
                Spacer(modifier = Modifier.width(spacing.small))
            }
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                modifier = Modifier
// TODO: this was a bit too assertive
//                    .semantics {
//                        liveRegion = LiveRegionMode.Assertive
//                        this.contentDescription = text
//                    }
                    .weight(1f)

            )
            Icon(
                Icons.Rounded.ChevronRight,
                null,
                modifier = Modifier.defaultMinSize(spacing.targetSize)
            )
        }
    }
}

@Preview
@Composable
fun PreviewNavigationButton() {
    NavigationButton(text = "Long text to show what happens on a wrap", onClick = {})
}