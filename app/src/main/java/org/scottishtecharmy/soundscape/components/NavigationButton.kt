package org.scottishtecharmy.soundscape.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NavigationButton(
    onClick: () -> Unit = {},
    text: String, icon:
    ImageVector? = null,
    horizontalPadding: Dp = 16.dp
) {
    Button(
        onClick = { onClick() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        shape = RoundedCornerShape(0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (icon != null) {
                Box {
                    Icon(
                        icon,
                        null,
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Box(
                Modifier.weight(6f)
            ) {
                Text(
                    text, fontSize = 16.sp, fontWeight = FontWeight(400),
                    textAlign = TextAlign.Start
                )
            }
            Box(
                Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Rounded.ChevronRight,
                    null,
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}