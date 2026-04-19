package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import org.scottishtecharmy.soundscape.ui.theme.extraSmallPadding

@Composable
fun TextOnlyAppBar(title : String = "",
                   onNavigateUp: () -> Unit = {},
                   navigationButtonTitle: String = "    ",
                   onRightButton: () -> Unit = {},
                   rightButtonTitle: String = "    "
) {
    FlexibleAppBar(
        title = title,
        leftSide = {
            Text(
                modifier = Modifier
                    .clickable(role = Role.Button) { onNavigateUp() }
                    .extraSmallPadding()
                    .testTag("flexibleAppBarLeft"),
                text = navigationButtonTitle,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        rightSide = {
            Text(
                modifier = Modifier
                    .clickable(role = Role.Button) { onRightButton() }
                    .extraSmallPadding()
                    .testTag("flexibleAppBarRight"),
                text = rightButtonTitle,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    )
}
