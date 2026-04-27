package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.language_mismatch_keep
import org.scottishtecharmy.soundscape.resources.language_mismatch_message
import org.scottishtecharmy.soundscape.resources.language_mismatch_switch
import org.scottishtecharmy.soundscape.resources.language_mismatch_title
import org.scottishtecharmy.soundscape.screens.onboarding.language.Language

/**
 * Dialog shown when the phone's language differs from the app's configured language
 * and the phone language is supported by the app.
 */
@Composable
fun SharedLanguageMismatchDialog(
    innerPadding: PaddingValues,
    preferencesProvider: PreferencesProvider?,
    showDialog: MutableState<Boolean>,
    phoneLanguage: Language,
    onSetApplicationLocale: (String?) -> Unit,
) {
    AlertDialog(
        modifier = Modifier.padding(innerPadding),
        title = { Text(text = stringResource(Res.string.language_mismatch_title)) },
        text = {
            Text(text = stringResource(Res.string.language_mismatch_message, phoneLanguage.name))
        },
        onDismissRequest = { },
        confirmButton = {
            TextButton(
                onClick = {
                    preferencesProvider?.putBoolean(PreferenceKeys.LANGUAGE_SUPPORTED_PROMPTED, true)
                    showDialog.value = false
                    onSetApplicationLocale("${phoneLanguage.code}-${phoneLanguage.region}")
                },
            ) {
                Text(text = stringResource(Res.string.language_mismatch_switch, phoneLanguage.name))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    preferencesProvider?.putBoolean(PreferenceKeys.LANGUAGE_SUPPORTED_PROMPTED, true)
                    showDialog.value = false
                },
            ) {
                Text(text = stringResource(Res.string.language_mismatch_keep))
            }
        },
    )
}
