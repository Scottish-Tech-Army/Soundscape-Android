package org.scottishtecharmy.soundscape.screens.home.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.resources.*
import org.scottishtecharmy.soundscape.screens.talkbackHint
import org.scottishtecharmy.soundscape.ui.theme.extraSmallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun ListPreferenceItem(
    description: String,
    value: Any,
    currentValue: Any,
    onClick: () -> Unit,
    index: Int,
    listSize: Int,
) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .extraSmallPadding()
            .defaultMinSize(minHeight = spacing.targetSize)
            .clickable(role = Role.RadioButton) { onClick() }
            .talkbackHint(
                if (value == currentValue) stringResource(Res.string.settings_keep_value)
                else stringResource(Res.string.settings_use_value)
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = description,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.CenterVertically).weight(1f),
        )
        Icon(
            modifier = Modifier.align(Alignment.CenterVertically).width(spacing.targetSize),
            imageVector = if (value == currentValue) Icons.Filled.CheckBox
            else Icons.Filled.CheckBoxOutlineBlank,
            tint = MaterialTheme.colorScheme.onSurface,
            contentDescription = "",
        )
    }
}

@Composable
fun ExpandableSectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    textColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClickLabel = if (expanded) "Collapse section" else "Expand section",
            ) { onToggle() }
            .extraSmallPadding()
            .defaultMinSize(minHeight = spacing.targetSize),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = textColor,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() }.weight(1f),
        )
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowDown
            else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = textColor,
        )
    }
}

@Composable
fun SettingDetails(title: StringResource, description: StringResource, textColor: Color) {
    Column {
        Text(
            text = stringResource(title),
            color = textColor,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(spacing.extraSmall),
        )
        Text(
            text = stringResource(description),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(spacing.extraSmall),
        )
    }
}

@Composable
fun ClickableOption(text: String, textColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(spacing.small),
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Default.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}
