package org.scottishtecharmy.soundscape.screens.onboarding.language

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LanguageSelectionBox(
    allLanguages: List<Language>,
    onLanguageSelected : (Language) -> Unit,
    selectedLanguageIndex: Int,
){
    LazyColumn(modifier = Modifier
        .clip(RoundedCornerShape(5.dp))
        .fillMaxWidth()
        .height(400.dp)
        .background(Color.White)
    )
    {
        items(allLanguages.size) { index ->
            LanguageSelectionBoxItem(
                language = allLanguages[index],
                onSelected = {
                    onLanguageSelected(allLanguages[index])
                },
                isSelected = selectedLanguageIndex == index
            )
        }
    }
}
