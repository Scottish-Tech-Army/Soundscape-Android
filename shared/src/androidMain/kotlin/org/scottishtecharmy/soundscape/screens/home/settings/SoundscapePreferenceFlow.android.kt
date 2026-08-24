package org.scottishtecharmy.soundscape.screens.home.settings

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import me.zhanghai.compose.preference.Preferences
import me.zhanghai.compose.preference.createDefaultPreferenceFlow

@Composable
internal actual fun rememberSoundscapePreferenceFlow(): MutableStateFlow<Preferences> =
    createDefaultPreferenceFlow()
