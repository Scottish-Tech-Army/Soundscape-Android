package org.scottishtecharmy.soundscape.screens.onboarding.language

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.no_language_selected
import org.scottishtecharmy.soundscape.ui.theme.spacing

private const val LANGUAGE_DROPDOWN_MENU = "LANGUAGE_DROPDOWN_MENU"
private const val LANGUAGE_DROPDOWN_ITEM = "LANGUAGE_DROPDOWN_ITEM_"

@Composable
fun LanguageDropDownMenu(
    allLanguages: List<Language>,
    onLanguageSelected: (Language) -> Unit,
    selectedLanguageIndex: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val languageSelected by remember(selectedLanguageIndex) {
        derivedStateOf { selectedLanguageIndex >= 0 }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.CenterStart),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(spacing.extraSmall),
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier
                    .testTag("languageDropDownMenuToggleButton")
                    .semantics {
                        if (languageSelected) selected = true
                    },
            ) {
                Text(
                    text = if (languageSelected) {
                        allLanguages[selectedLanguageIndex].name
                    } else {
                        stringResource(Res.string.no_language_selected)
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .testTag(LANGUAGE_DROPDOWN_MENU)
                .selectableGroup()
                // Cap the popup height so the full list scrolls instead of
                // overflowing the screen. The DropdownMenu uses Window-anchored
                // positioning so this max plays nicely with anchor placement.
                .heightIn(max = 480.dp),
        ) {
            allLanguages.forEachIndexed { index, language ->
                val isSelected = remember(selectedLanguageIndex, index) {
                    index == selectedLanguageIndex
                }
                DropdownMenuItem(
                    text = { DropdownItemContent(language, isSelected) },
                    modifier = Modifier
                        .selectable(
                            selected = isSelected,
                            onClick = {
                                expanded = false
                                onLanguageSelected(language)
                            },
                        )
                        .testTag("$LANGUAGE_DROPDOWN_ITEM${language.code}-${language.region}"),
                    onClick = {
                        expanded = false
                        onLanguageSelected(language)
                    },
                )
            }
        }
    }
}

@Composable
private fun DropdownItemContent(
    language: Language,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        Text(
            text = language.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (isSelected) {
            Icon(
                Icons.Rounded.Done,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(spacing.icon),
            )
        } else {
            Spacer(modifier = Modifier.width(spacing.medium))
        }
    }
}
