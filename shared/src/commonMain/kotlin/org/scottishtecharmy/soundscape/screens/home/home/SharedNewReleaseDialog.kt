package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.platform.appVersionMinorTrimmed
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.new_version_info_completed
import org.scottishtecharmy.soundscape.resources.new_version_info_details
import org.scottishtecharmy.soundscape.resources.new_version_info_release_notes
import org.scottishtecharmy.soundscape.resources.new_version_info_text
import org.scottishtecharmy.soundscape.ui.theme.spacing

/**
 * Cross-platform new-release dialog. Splits the body string into sentences and renders
 * them as plain text. (The previous Android-only version pre-rendered each sentence as
 * HTML via commonmark; that path can be re-added behind a platform helper if richer
 * styling is needed.)
 *
 * The dialog only has room for the headlines, so a button opens the full release notes on the
 * website. It's a button rather than a link in the text so that a screen reader finds it as one of
 * the dialog's actions, and it leaves the dialog open, to be closed as before.
 */
@Composable
fun SharedNewReleaseDialog(
    innerPadding: PaddingValues,
    preferencesProvider: PreferencesProvider?,
    newReleaseDialog: MutableState<Boolean>,
) {
    val markdownText = stringResource(Res.string.new_version_info_details)
    val sentences = remember(markdownText) {
        markdownText.split(Regex("(?<=[.!?][ \n])\\s*")).filter { it.isNotBlank() }
    }
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        modifier = Modifier.padding(innerPadding),
        title = { Text(text = stringResource(Res.string.new_version_info_text)) },
        text = {
            LazyColumn(modifier = Modifier.padding(top = spacing.medium)) {
                itemsIndexed(sentences) { _, sentence ->
                    Text(text = sentence)
                }
            }
        },
        onDismissRequest = { },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag("newReleaseDialogReleaseNotes"),
                // Throws on a phone with no browser; there's nothing better to offer then.
                onClick = { runCatching { uriHandler.openUri(releaseNotesUrl()) } },
            ) {
                Text(text = stringResource(Res.string.new_version_info_release_notes))
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.testTag("newReleaseDialogDismiss"),
                onClick = {
                    preferencesProvider?.putString(
                        PreferenceKeys.LAST_NEW_RELEASE,
                        appVersionMinorTrimmed(),
                    )
                    newReleaseDialog.value = false
                },
            ) {
                Text(text = stringResource(Res.string.new_version_info_completed))
            }
        },
    )
}
