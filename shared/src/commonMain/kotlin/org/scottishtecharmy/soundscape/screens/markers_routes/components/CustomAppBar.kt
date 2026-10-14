package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.scottishtecharmy.soundscape.screens.talkbackHint
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.ui_back_button_title
import org.scottishtecharmy.soundscape.ui.theme.extraSmallPadding

@Composable
fun CustomAppBar(
    title: String,
    onNavigateUp: () -> Unit,
    navigationButtonTitle: String = stringResource(Res.string.ui_back_button_title),
    onRightButton: () -> Unit = {},
    rightButtonTitle: String = "",
    // What a screen reader calls the right button, when its title needs the screen's context, such as "New"
    rightButtonDescription: String? = null,
    // Screen reader hint for the right button, read after "Double tap to"
    rightButtonHint: String? = null,
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
            if (rightButtonTitle.isNotEmpty()) {
                Text(
                    modifier = Modifier
                        .clickable(role = Role.Button) { onRightButton() }
                        .extraSmallPadding()
                        .then(
                            if (rightButtonDescription != null) Modifier.semantics {
                                contentDescription = rightButtonDescription
                            } else Modifier
                        )
                        .then(if (rightButtonHint != null) Modifier.talkbackHint(rightButtonHint) else Modifier)
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
