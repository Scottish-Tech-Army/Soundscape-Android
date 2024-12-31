package org.scottishtecharmy.soundscape.utils

fun String.blankOrEmpty() = this.isBlank() || this.isEmpty()
fun String.nullIfEmpty(): String? = ifEmpty { null }
