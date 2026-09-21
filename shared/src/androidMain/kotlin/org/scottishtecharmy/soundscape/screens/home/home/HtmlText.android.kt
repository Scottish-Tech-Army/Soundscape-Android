package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration

actual fun parseHtmlToAnnotatedString(html: String): AnnotatedString =
    AnnotatedString.fromHtml(
        htmlString = html,
        linkStyles = TextLinkStyles(
            style = SpanStyle(textDecoration = TextDecoration.Underline)
        )
    )
