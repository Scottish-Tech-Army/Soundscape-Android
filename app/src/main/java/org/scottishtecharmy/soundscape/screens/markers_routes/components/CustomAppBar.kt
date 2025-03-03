package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun CustomAppBar(title : String,
                 onNavigateUp: () -> Unit,
                 navigationButtonTitle: String = stringResource(R.string.ui_back_button_title),
                 ) {
    Surface {
        Row(
            modifier  = Modifier.height(IntrinsicSize.Min).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            IconWithTextButton(
                text = navigationButtonTitle
            ) {
                onNavigateUp()
            }

            Box(
                modifier = Modifier.fillMaxHeight().weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = title,
                    modifier = Modifier.semantics { heading() }
                )
            }
        }
    }
}

@Preview(showBackground = true, fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun CustomAppBarPreview() {
    CustomAppBar(
        "Test app bar with long title",
        navigationButtonTitle = stringResource(R.string.ui_back_button_title),
        onNavigateUp = {},
    )
}

@Preview(showBackground = true, fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun CustomAppBarWithActionButtonPreview() {
    CustomAppBar(
        "Test app bar",
        navigationButtonTitle = stringResource(R.string.ui_back_button_title),
        onNavigateUp = {},
    )
}