package org.scottishtecharmy.soundscape.viewmodels

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import java.net.URLEncoder
import javax.inject.Inject

@HiltViewModel
class DrawerViewModel @Inject constructor(soundscapeServiceConnection : SoundscapeServiceConnection): ViewModel() {

    private var serviceConnection : SoundscapeServiceConnection? = null

    fun shareLocation(context: Context) {
        // Share the current location using standard Android sharing mechanism. It's shared as a
        //
        //  soundscape://latitude,longitude
        //
        // URI, with the , encoded. This shows up in Slack as a clickable link which is the main
        // usefulness for now
        val location = serviceConnection?.getLocationFlow()?.value
        if(location != null) {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TITLE, "Problem location")
                val latitude = location.latitude
                val longitude = location.longitude
                val uriData: String = URLEncoder.encode("$latitude,$longitude", Charsets.UTF_8.name())
                putExtra(Intent.EXTRA_TEXT, "soundscape://$uriData")
                type = "text/plain"

            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            context.startActivity(shareIntent)
        }
    }

    init {
        serviceConnection = soundscapeServiceConnection
    }
}