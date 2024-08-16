package org.scottishtecharmy.soundscape.components

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.screens.onboarding.LanguageViewModel
import org.scottishtecharmy.soundscape.ui.theme.Primary

data class Language(
    val name: String,
    val code: String
)

@Composable
fun LanguageSelectionBoxItem(language: Language){

    Row(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 17.dp)
            .fillMaxWidth()
            .clickable (
                onClick = {
                    // set app locale given the user's selected locale
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(
                            language.code
                        )
                    )
                }
            )
    ){
        if(sameLanguage(language.code)){
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

// An attempt was made here to mock the list of languages to preview LanguageSelectionBox
// But it didn't work :-(
class LanguageSelectionMock : PreviewParameterProvider< List<Language> > {
    override val values = listOf(LanguageViewModel(NativeAudioEngine()).getAllLanguages()).asSequence()
}

@Preview
@Composable
fun LanguageSelectionBox(@PreviewParameter(LanguageSelectionMock::class) allLanguages: List<Language>){
    LazyColumn(modifier = Modifier
        .clip(RoundedCornerShape(5.dp))
        .fillMaxWidth()
        .height(400.dp)
        .background(Color.White)
    )
    {
        items(allLanguages.size) { index ->
            LanguageSelectionBoxItem(language = allLanguages[index])
        }
    }
}

fun sameLanguage(code: String): Boolean {
    val currentLanguage = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    return currentLanguage == code
}
