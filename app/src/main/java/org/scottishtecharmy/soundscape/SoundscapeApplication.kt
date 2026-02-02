package org.scottishtecharmy.soundscape

import android.app.Application
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SoundscapeApplication : Application(){
    companion object {
        init {
            // Skip loading native/external code in unit tests which run on Windows, rather than Android.
            if (Build.FINGERPRINT != "robolectric") {
                System.loadLibrary(BuildConfig.FMOD_LIB)
            }
        }
    }
}
