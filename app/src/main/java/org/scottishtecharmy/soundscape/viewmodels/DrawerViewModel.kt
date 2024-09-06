package org.scottishtecharmy.soundscape.viewmodels

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.model.ReviewErrorCode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import java.net.URLEncoder
import javax.inject.Inject

@HiltViewModel
class DrawerViewModel @Inject constructor(@ApplicationContext val context: Context, soundscapeServiceConnection : SoundscapeServiceConnection): ViewModel() {

    private var serviceConnection : SoundscapeServiceConnection? = null
    private val reviewManager = ReviewManagerFactory.create(context)

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

    fun rateSoundscape() {
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // We got the ReviewInfo object
                val reviewInfo = task.result

                val flow = reviewManager.launchReviewFlow(context as MainActivity, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    // The flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even whether the review dialog was shown. Thus, no
                    // matter the result, we continue our app flow.
                }
            } else {
                // There was some problem, log or handle the error code.
                @ReviewErrorCode val reviewErrorCode = (task.exception as ReviewException).errorCode
                Log.e("DrawerViewModel", "Error requesting review: $reviewErrorCode")
            }
        }

    }

    init {
        serviceConnection = soundscapeServiceConnection
    }
}