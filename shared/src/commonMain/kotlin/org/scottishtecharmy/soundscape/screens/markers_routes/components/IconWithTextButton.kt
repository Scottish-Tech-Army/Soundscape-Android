package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.scottishtecharmy.soundscape.screens.StartsSpeechControl
import org.scottishtecharmy.soundscape.screens.talkbackHint
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.ui.theme.tinyPadding

/**
 * @param buttonTestTag when set, tags the button and - for a [startsSpeech] button, whose iOS
 * accessibility element is a native proxy rather than this Compose node - carries the tag over
 * as the proxy's accessibilityIdentifier. Prefer it to tagging via [modifier] on such buttons,
 * which VoiceOver would no longer see.
 * @param startsSpeech true when activating this button immediately starts app speech - a beacon
 * announcement, a route's first waypoint, street preview's callouts. Tells VoiceOver to stay
 * quiet for it instead of speaking the label and its activation click over the top. See
 * [StartsSpeechControl].
 */
@Composable
fun IconWithTextButton(
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier.size(spacing.icon),
    textModifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.ChevronLeft,
    contentDescription: String? = null,
    text: String = "",
    talkbackHint: String = "",
    textStyle: TextStyle = MaterialTheme.typography.labelSmall,
    fontWeight: FontWeight = FontWeight.Bold,
    fontSize: TextUnit = 18.sp,
    color: Color = MaterialTheme.colorScheme.onSurface,
    buttonTestTag: String? = null,
    startsSpeech: Boolean = false,
    onClick: () -> Unit
) {
    val button: @Composable (Modifier) -> Unit = { buttonModifier ->
        Row(
            modifier = buttonModifier
                .clickable(role = Role.Button) { onClick() }
                .tinyPadding()
                .talkbackHint(talkbackHint)
                .then(if (buttonTestTag != null) Modifier.testTag(buttonTestTag) else Modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = iconModifier, // Only modifies the set Icon
                imageVector = icon,
                contentDescription = contentDescription,
                tint = color
            )
            if (text.isNotEmpty()) {
                Text(
                    modifier = textModifier,
                    text = text,
                    style = textStyle.copy(
                        fontWeight = fontWeight,
                        fontSize = fontSize
                    ),
                    color = color
                )
            }
        }
    }

    if (startsSpeech) {
        StartsSpeechControl(
            // What Compose would otherwise have merged for the label, in the same order: the
            // icon's description first, then the visible text.
            label = listOfNotNull(contentDescription, text.ifEmpty { null }).joinToString(", "),
            hint = talkbackHint,
            identifier = buttonTestTag,
            onActivate = onClick,
            modifier = modifier,
            content = button,
        )
    } else {
        button(modifier)
    }
}
