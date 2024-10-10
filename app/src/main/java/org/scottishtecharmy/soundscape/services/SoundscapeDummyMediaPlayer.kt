package org.scottishtecharmy.soundscape.services

import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.DeviceInfo
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi

@OptIn(UnstableApi::class)
class SoundscapeDummyMediaPlayer : SimpleBasePlayer(Looper.getMainLooper()) {

    override fun getState(): State {

        val commands : Player.Commands = Player.Commands.EMPTY
        commands.buildUpon().addAll(
            COMMAND_PLAY_PAUSE,
            COMMAND_STOP,
            COMMAND_SEEK_TO_NEXT,
            COMMAND_SEEK_TO_PREVIOUS,
            COMMAND_SEEK_FORWARD,
            COMMAND_SEEK_BACK,
            COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)

        return State.Builder()
            // Set which playback commands the player can handle
            .setAvailableCommands(commands)
            // Configure additional playback properties
            .setPlayWhenReady(true, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setCurrentMediaItemIndex(0)
            .setContentPositionMs(0)
            .setDeviceInfo(DeviceInfo.Builder(DeviceInfo.PLAYBACK_TYPE_LOCAL).build())
            .build()
    }
}