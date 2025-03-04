package org.scottishtecharmy.soundscape.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics

/**
 * talkbackHint adds a string which describes what the element does on a double click
 * @param hint String that is read out by talkback
 */
@Composable
fun Modifier.talkbackHint(hint: String) =
// I did add code here to disable the hints, but that just results in the default Android "Double
// tap to activate" message which is less helpful than our own.
//    if(LocalHintsEnabled.current)
        semantics {
            onClick(label = hint, action = { false })
        }
//    else {
//        semantics {}
//    }
