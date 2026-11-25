package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.platform.testTagsAsResourceId
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.contact_support_recording_exclude
import org.scottishtecharmy.soundscape.resources.contact_support_recording_include
import org.scottishtecharmy.soundscape.resources.contact_support_recording_message
import org.scottishtecharmy.soundscape.resources.contact_support_recording_title

/**
 * Asked before the support email opens when travel recording is on: the recording is up to an
 * hour of the user's precise locations, so it is only attached if they say so. Dismissing the
 * dialog (back, or a tap outside) sends nothing.
 */
@Composable
fun ContactSupportRecordingDialog(
    onAnswer: (includeRecording: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val title = stringResource(Res.string.contact_support_recording_title)
    AlertDialog(
        onDismissRequest = onDismiss,
        // A dialog is its own window, outside the theme's testTagsAsResourceId.
        modifier = Modifier.testTagsAsResourceId().semantics { paneTitle = title },
        title = { Text(text = title) },
        text = { Text(text = stringResource(Res.string.contact_support_recording_message)) },
        confirmButton = {
            TextButton(
                onClick = { onAnswer(true) },
                modifier = Modifier.testTag("contactSupportIncludeRecording"),
            ) {
                Text(text = stringResource(Res.string.contact_support_recording_include))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onAnswer(false) },
                modifier = Modifier.testTag("contactSupportExcludeRecording"),
            ) {
                Text(text = stringResource(Res.string.contact_support_recording_exclude))
            }
        },
    )
}
