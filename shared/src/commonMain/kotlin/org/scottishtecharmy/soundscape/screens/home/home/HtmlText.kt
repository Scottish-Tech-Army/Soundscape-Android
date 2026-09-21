package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.ui.text.AnnotatedString

expect fun parseHtmlToAnnotatedString(html: String): AnnotatedString
