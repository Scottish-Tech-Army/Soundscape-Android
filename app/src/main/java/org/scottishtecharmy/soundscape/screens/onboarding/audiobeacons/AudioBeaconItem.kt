package org.scottishtecharmy.soundscape.screens.onboarding.audiobeacons


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun AudioBeaconItem(
    text: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    foregroundColor: Color,
    modifier: Modifier
) {
    Row(
        modifier = modifier
            // Accessibility recommendation for the size of a clickable thing
            .padding(horizontal = spacing.small, vertical = spacing.medium)
            .fillMaxWidth()
            .selectable(
                selected = isSelected
            ) {
                onSelect()
            }
    ) {
        if (isSelected) {
            Icon(
                Icons.Rounded.Done,
                contentDescription = null,
                tint = foregroundColor,
                modifier = modifier
                    .size(spacing.icon)
            )
        }
        Spacer(modifier = Modifier.width(spacing.medium))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = foregroundColor
        )
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = modifier
                .fillMaxWidth()
                .padding(end = spacing.small)
        ) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = foregroundColor,
                modifier = modifier
                    .size(spacing.icon)
            )
        }
    }

    HorizontalDivider(
        modifier = Modifier
            .padding(horizontal = spacing.small, vertical = spacing.tiny),
        thickness = spacing.tiny,
        color = foregroundColor
    )
}
