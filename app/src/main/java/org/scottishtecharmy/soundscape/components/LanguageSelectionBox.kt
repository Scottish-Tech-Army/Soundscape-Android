package org.scottishtecharmy.soundscape.components

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.OnboardButton
import org.scottishtecharmy.soundscape.ui.theme.IntroTypography
import org.scottishtecharmy.soundscape.ui.theme.IntroductionTheme
import org.scottishtecharmy.soundscape.ui.theme.Primary

data class Language(
    val name: String,
    val code: String
)
// TODO setup locale translation files, etc.
// https://android-developers.googleblog.com/2022/11/per-app-language-preferences-part-1.html
@Composable
fun LanguageSelectionBoxItem(language: Language){

    Row(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 10.dp)
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

@Composable
fun LanguageSelectionBox(){
    LazyColumn(modifier = Modifier
        .clip(RoundedCornerShape(5.dp))
        .fillMaxWidth()
        .height(400.dp)
        .background(Color.White)
    )
    {
        items(getAllLanguages().size) { index ->
            LanguageSelectionBoxItem(language = getAllLanguages()[index])
        }
    }
}

fun sameLanguage(code: String): Boolean {
    val currentLanguage = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    return currentLanguage == code
}

@Preview
@Composable
fun LanguageSelectionBoxItemPreview() {
    IntroductionTheme {
        MaterialTheme(typography = IntroTypography){
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 50.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = stringResource(R.string.first_launch_soundscape_language),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.first_launch_soundscape_language_text),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                LanguageSelectionBox()

                Spacer(modifier = Modifier.height(40.dp))

                Column(modifier = Modifier.padding(horizontal = 50.dp)) {
                    OnboardButton(
                        text = stringResource(R.string.ui_continue),
                        onClick = {  },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

            }
        }
    }
}

fun getAllLanguages(): List<Language> {
    return listOf(
        Language(
            name = "Dansk",
            code = "da"
        ),
        Language(
            name = "Deutsch",
            code = "de"
        ),
        Language(
            name = "Ελληνικά",
            code = "el"
        ),
        Language(
            name = "English",
            code = "en"
        ),
        Language(
            name = "Español",
            code = "es"
        ),
        Language(
            name = "Suomi",
            code = "fi"
        ),
        Language(
            name = "Français",
            code = "fr"
        ),
        Language(
            name = "Italiano",
            code = "it"
        ),
        Language(
            name = "日本語",
            code = "ja"

        ),
        Language(
            name = "Norsk",
            code = "nb"
        ),
        Language(
            name = "Nederlands",
            code = "nl"
        ),
        Language(
            name = "Português (Brasil)",
            code = "pt"
        ),
        Language(
            name = "Svenska",
            code = "sv"
        )
    )
}