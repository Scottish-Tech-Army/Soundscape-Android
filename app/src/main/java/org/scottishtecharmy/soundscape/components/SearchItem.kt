package org.scottishtecharmy.soundscape.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.ui.theme.Foreground2
import org.scottishtecharmy.soundscape.ui.theme.IntroductionTheme


data class SearchItem(
    val text: String,
    val label: String,
)

@Composable
fun SearchItemButton(item: SearchItem, onClick: () -> Unit, modifier: Modifier,) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(0),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.LocationOn,
                contentDescription = "Choose destination",
                tint = Color.White
            )
            Column(modifier = Modifier.padding(start = 18.dp)) {
                Text(
                    item.text,
                    fontWeight = FontWeight(400),
                    fontSize = 18.sp,
                    color = Color.White,
                )
                Text(
                    item.label,
                    color = Foreground2,
                    fontWeight = FontWeight(350),
                )
            }
        }
    }
}

@Preview(name = "Light Mode")
@Composable
fun PreviewSearchItemButton() {
    IntroductionTheme {
        Column(modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        )
        {
            val test = SearchItem("Bristol", "1")
            SearchItemButton(test, onClick = {}, Modifier.width(200.dp))
        }
    }
}