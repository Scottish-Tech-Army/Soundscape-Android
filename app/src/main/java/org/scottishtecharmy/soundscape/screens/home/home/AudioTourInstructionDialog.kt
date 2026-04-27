package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.audio.AudioTourInstruction
import org.scottishtecharmy.soundscape.resources.*

@Preview
@Composable
fun AudioTourDialogTest() {
    AudioTourInstructionDialog(
        instruction = AudioTourInstruction(stringResource(Res.string.tour_my_location)),
        onContinue = {},
    )
}
