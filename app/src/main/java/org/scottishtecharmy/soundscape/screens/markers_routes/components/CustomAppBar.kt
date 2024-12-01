package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

@Composable
fun CustomAppBar(title : String,
                 onNavigateUp: () -> Unit,
                 onAddClicked: (() -> Unit)? = null,
                 onDoneClicked: (() -> Unit)? = null,
                 showAddIcon: Boolean = false,
                 showDoneButton: Boolean = false,
                 navigationButtonTitle: String = stringResource(R.string.ui_back_button_title),
) {
    Surface(
        color = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier  = Modifier
                .height(IntrinsicSize.Min)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconWithTextButton(
                iconModifier = Modifier.size(40.dp),
                onClick = {
                    onNavigateUp()
                },
                iconText = navigationButtonTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = title,
                )
            }


            AnimatedVisibility(
                visible = showAddIcon,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
                modifier = Modifier.fillMaxHeight()
            ) {
                IconButton(
                    onClick = {
                        onAddClicked?.invoke()
                    }
                ){
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.general_alert_add),
                    )
                }
            }
            if (showDoneButton) {
                TextButton(
                    onClick = {
                        onDoneClicked?.invoke()
                    }
                ) {
                    Text(
                        //text = stringResource(R.string.general_alert_done),
                        text = "Done",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun CustomAppBarPreview() {
    SoundscapeTheme {
        CustomAppBar(
            "Test app bar with long title",
            navigationButtonTitle = "Back",
            showAddIcon = false,
            onNavigateUp = {},
            onAddClicked = {}
        )
    }
}

@Preview(showBackground = true, fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun CustomAppBarWithActionButtonPreview() {
    SoundscapeTheme {
        CustomAppBar(
            "Test app bar",
            navigationButtonTitle = "Back",
            showAddIcon = true,
            onNavigateUp = {},
            onAddClicked = {}
        )
    }
}

@Preview(showBackground = true, fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun CustomAppBarWithDoneActionButtonPreview() {
    SoundscapeTheme {
        CustomAppBar(
            "Test app bar",
            navigationButtonTitle = "Back",
            showDoneButton = true,
            onNavigateUp = {},
            onAddClicked = {}
        )
    }
}