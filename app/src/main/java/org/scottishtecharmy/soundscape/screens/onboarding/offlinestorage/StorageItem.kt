package org.scottishtecharmy.soundscape.screens.onboarding.offlinestorage


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun StorageItem(
    index: Int,
    name: String,
    freeSpace: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    foregroundColor: Color,
    modifier: Modifier
) {

    if(index == 0) {
        HorizontalDivider(
            modifier = Modifier
                .padding(horizontal = spacing.small),
            thickness = spacing.tiny,
            color = foregroundColor
        )
    }

    Row(
        modifier = modifier
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
                tint = foregroundColor
            )
        }
        Spacer(modifier = Modifier.width(spacing.medium))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = foregroundColor,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = modifier
                .fillMaxWidth()
                .padding(end = spacing.small)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = freeSpace,
                style = MaterialTheme.typography.bodyMedium,
                color = foregroundColor,
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

@Preview
@Composable
fun StorageItemPreview() {
    Column(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        StorageItem(
            0,
            "Internal",
            "22 GB",
            true,
            {},
            MaterialTheme.colorScheme.onSurface,
            Modifier
        )
    }
}

@Preview
@Composable
fun StorageItemPreviewNotSelected() {
    Column(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        StorageItem(
            1,
            "Internal",
            "22 GB",
            false,
            {},
            MaterialTheme.colorScheme.onSurface,
            Modifier
        )
    }
}