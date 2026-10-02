package org.scottishtecharmy.soundscape.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import org.scottishtecharmy.soundscape.ui.theme.currentAppButtonColors
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun NavigationButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    icon: ImageVector? = null,
    horizontalPadding: Dp = spacing.medium,
    // Shows a spinner in place of the chevron while the action the button started is running
    loading: Boolean = false,
    loadingTestTag: String = "",
) {
    Button(
        onClick = { onClick() },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        shape = RoundedCornerShape(spacing.none),
        colors = if (!LocalInspectionMode.current) currentAppButtonColors else ButtonDefaults.buttonColors(),
        // No vertical padding, so that the button is the same height as the search bar above it
        // (the 48dp minimum touch target) unless large text makes it taller.
        contentPadding = PaddingValues(horizontal = spacing.medium + spacing.small),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = spacing.targetSize)
                .padding(horizontal = spacing.small),
        ) {
            if (icon != null) {
                Icon(icon, null)
                Spacer(modifier = Modifier.width(spacing.small))
            }
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f),
            )
            if (loading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.defaultMinSize(spacing.targetSize, spacing.targetSize),
                ) {
                    SlowLoadingIndicator(
                        color = LocalContentColor.current,
                        modifier = Modifier.testTag(loadingTestTag),
                    )
                }
            } else {
                Icon(
                    Icons.Rounded.ChevronRight,
                    null,
                    modifier = Modifier.defaultMinSize(spacing.targetSize),
                )
            }
        }
    }
}
