package org.scottishtecharmy.soundscape

import android.app.Application
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SoundscapeApplication : Application(){
    override fun onCreate() {
        super.onCreate()
    }
    companion object {
        init {
            if(!Build.FINGERPRINT.contains("robolectric")) {
                System.loadLibrary(BuildConfig.FMOD_LIB)
            }
        }
    }
}