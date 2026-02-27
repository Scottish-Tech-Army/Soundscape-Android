package org.scottishtecharmy.soundscape.services.mediacontrol

import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@OptIn(UnstableApi::class)
class SoundscapeDummyMediaPlayer(
    private val mediaControlTarget: MediaControlTarget
) : SimpleBasePlayer(Looper.getMainLooper()) {

    override fun getState(): State {

        val commands = Player.Commands.Builder()
            .addAll(
                COMMAND_PLAY_PAUSE,
                COMMAND_STOP,
                COMMAND_SEEK_TO_NEXT,
                COMMAND_SEEK_TO_PREVIOUS,
                COMMAND_SEEK_FORWARD,
                COMMAND_SEEK_BACK,
                COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .build()

        val playlist = listOf(
            MediaItemData.Builder("soundscape_item")
                .setMediaItem(
                    MediaItem.Builder()
                        .setMediaId("soundscape_item")
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle("Soundscape")
                                .setArtist("Scottish Tech Army")
                                .build()
                        )
                        .build()
                )
                .build()
        )

        return State.Builder()
            // Set which playback commands the player can handle
            .setAvailableCommands(commands)
            // Configure additional playback properties
            .setPlayWhenReady(true, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaylist(playlist)
            .setCurrentMediaItemIndex(0)
            .setContentPositionMs(0)
            .setDeviceInfo(DeviceInfo.Builder(DeviceInfo.PLAYBACK_TYPE_LOCAL).build())
            .setPlaybackState(STATE_READY)
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        mediaControlTarget.onPlayPause()
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> =
        Futures.immediateVoidFuture()
}