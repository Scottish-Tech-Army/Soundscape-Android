package com.kersnazzle.soundscapealpha.screens.onboarding

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.kersnazzle.soundscapealpha.R
import com.kersnazzle.soundscapealpha.components.LanguageSelectionBox
import com.kersnazzle.soundscapealpha.components.OnboardButton
import com.kersnazzle.soundscapealpha.screens.navigation.Screens
import com.kersnazzle.soundscapealpha.ui.theme.IntroTypography
import com.kersnazzle.soundscapealpha.ui.theme.IntroductionTheme

// TODO setup locale translation files, etc.
// https://android-developers.googleblog.com/2022/11/per-app-language-preferences-part-1.html
@Composable
fun Language(navController: NavHostController){
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

                //val selectedLocale = AppCompatDelegate.getApplicationLocales()[0]
                //Log.d("Locales", "Locale is set to: $selectedLocale")

                Column(modifier = Modifier.padding(horizontal = 50.dp)) {
                    OnboardButton(
                        text = stringResource(R.string.ui_continue),
                        onClick = { navController.navigate(Screens.Listening.route) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

            }
        }
    }
}

@Preview
@Composable
fun LanguagePreview(){
    Language(navController = rememberNavController())
}