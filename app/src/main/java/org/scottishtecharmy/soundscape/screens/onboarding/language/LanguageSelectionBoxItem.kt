package org.scottishtecharmy.soundscape.screens.onboarding.language

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.scottishtecharmy.soundscape.ui.theme.Primary


@Composable
fun LanguageSelectionBoxItem(
    language: Language,
    isSelected: Boolean,
    onSelected: () -> Unit,
){

    Row(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 17.dp)
            .fillMaxWidth()
            .selectable(selected = isSelected){
                onSelected()
            }
            .testTag("LANGUAGE_SELECTION_${language.code}")
    ){
        if(isSelected){
            Icon(
                Icons.Rounded.Done,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier
                    .size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = language.name,
            style = MaterialTheme.typography.bodyMedium,
            color = Primary)
    }

    HorizontalDivider(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 2.dp),
        thickness = 0.8.dp,
        color = Primary
    )
}