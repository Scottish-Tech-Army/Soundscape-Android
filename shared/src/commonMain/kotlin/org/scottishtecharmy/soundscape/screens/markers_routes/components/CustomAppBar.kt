package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.scottishtecharmy.soundscape.ui.theme.extraSmallPadding
import org.scottishtecharmy.soundscape.resources.*

@Composable
fun CustomAppBar(title : String,
                 onNavigateUp: () -> Unit,
                 navigationButtonTitle: String = stringResource(Res.string.ui_back_button_title),
                 onRightButton: () -> Unit = {},
                 rightButtonTitle: String = ""
                 ) {
    FlexibleAppBar(
        title = title,
        leftSide = {
            IconWithTextButton(
                text = navigationButtonTitle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("appBarLeft")
            ) {
                onNavigateUp()
            }
        },
        rightSide = {
            if(rightButtonTitle.isNotEmpty()) {
                Text(
                    modifier = Modifier
                        .clickable(role = Role.Button) { onRightButton() }
                        .extraSmallPadding()
                        .testTag("appBarRight"),
                    text = rightButtonTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }
        },
    )
}
