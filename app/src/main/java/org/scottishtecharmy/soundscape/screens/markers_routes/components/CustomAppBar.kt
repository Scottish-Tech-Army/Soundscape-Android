package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.ui.theme.extraSmallPadding

@Composable
fun CustomAppBar(title : String,
                 onNavigateUp: () -> Unit,
                 navigationButtonTitle: String = stringResource(R.string.ui_back_button_title),
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
                        .clickable { onRightButton() }
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

@Preview(showBackground = true, fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun CustomAppBarWithRightButtonPreview() {
    CustomAppBar(
        "Test app bar",
        navigationButtonTitle = stringResource(R.string.ui_back_button_title),
        onNavigateUp = {},
        rightButtonTitle = stringResource(R.string.general_alert_done),
        onRightButton = {},
    )
}