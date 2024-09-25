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

    init {
        serviceConnection = soundscapeServiceConnection
    }
}