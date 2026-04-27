package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.audio.AudioTourInstruction
import org.scottishtecharmy.soundscape.resources.*
import org.scottishtecharmy.soundscape.screens.talkbackHint

@Composable
fun AudioTourInstructionDialog(
    instruction: AudioTourInstruction,
    onContinue: () -> Unit,
) {
    val dialogTitle = stringResource(Res.string.menu_audio_tutorial)
    AlertDialog(
        onDismissRequest = { /* Don't dismiss on outside click for accessibility */ },
        modifier = Modifier.semantics { paneTitle = dialogTitle },
        text = {
            Text(text = instruction.text)
        },
        confirmButton = {
            TextButton(
                onClick = onContinue,
                modifier = Modifier.talkbackHint(stringResource(Res.string.tour_continue_hint))
            ) {
                Text(text = stringResource(Res.string.tour_continue_button))
            }
        },
    )
}
