package com.kersnazzle.soundscapealpha.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kersnazzle.soundscapealpha.ui.theme.IntroPrimary
import com.kersnazzle.soundscapealpha.ui.theme.IntroductionTheme
import com.kersnazzle.soundscapealpha.ui.theme.Primary

@Composable
fun OnboardButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    textColor: Color = Primary
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = IntroPrimary),
        shape = RoundedCornerShape(3.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = textColor,
        )
    }
}

@Preview
@Composable
fun PreviewButton() {
    IntroductionTheme {
        Column(modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        )
        {
            OnboardButton(text = "Hello", onClick = {}, Modifier.width(200.dp))
        }
    }
}