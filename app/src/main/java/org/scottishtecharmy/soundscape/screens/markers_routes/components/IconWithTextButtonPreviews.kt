package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

// IconWithTextButton composable is now in shared module

@Preview(fontScale = 1.5f, showBackground = true)
@Preview(showBackground = true)
@Composable
fun CustomIconButtonPreview(){
    IconWithTextButton(
        text = "Back",
        onClick = { /*TODO*/ }

    )
}
