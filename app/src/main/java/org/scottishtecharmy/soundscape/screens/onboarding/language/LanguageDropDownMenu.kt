package org.scottishtecharmy.soundscape.screens.onboarding.language

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

@Composable
fun LanguageDropDownMenu(
    allLanguages: List<Language>,
    onLanguageSelected : (Language) -> Unit,
    selectedLanguageIndex: Int,
    modifier : Modifier = Modifier
){
    var expanded by rememberSaveable { mutableStateOf(false) }
    val languageSelected by remember(selectedLanguageIndex) {
        derivedStateOf {
            selectedLanguageIndex >= 0
        }
    }
    Box(modifier = modifier
        .fillMaxSize()
        .wrapContentSize(Alignment.CenterStart)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(4.dp)
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
                TextButton(
                    onClick = {
                        expanded = !expanded
                    },
                    modifier = Modifier.semantics {
                        if (languageSelected) {
                            selected = true
                        }
                    }
                ) {
                    Text(
                        text = if (languageSelected) {
                            allLanguages[selectedLanguageIndex].name
                        } else {
                            stringResource(R.string.no_language_selected) // TODO localize
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.Edit, contentDescription = null)

                }

            }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .selectableGroup(),
        ) {
            allLanguages.forEachIndexed { index, language ->
                val isSelected = remember { index == selectedLanguageIndex }
                DropdownMenuItem(
                    text = {
                        DropdownItemContent(language, isSelected)
                    },
                    modifier = Modifier.selectable(
                        selected = isSelected,
                        onClick = {
                            expanded = false
                            onLanguageSelected(language)
                        }
                    ),
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .testTag("LANGUAGE_SELECTION_${language.code}")
    ) {
        Text(
            text = language.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        if (isSelected) {
            Icon(
                Icons.Rounded.Done,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(20.dp))
        }
    }
}

@Preview
@Composable
fun LanguageDropDownMenuPreview(){
    SoundscapeTheme {
        LanguageDropDownMenu(
            allLanguages = MockLanguagePreviewData.languages,
            onLanguageSelected = {},
            selectedLanguageIndex = -1,

        )
    }
}