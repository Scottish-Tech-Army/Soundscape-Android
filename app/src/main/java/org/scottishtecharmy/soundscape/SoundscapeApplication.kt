package org.scottishtecharmy.soundscape

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SoundscapeApplication : Application(){
    override fun onCreate() {
        super.onCreate()
    }
    companion object {
        init {
            System.loadLibrary(BuildConfig.FMOD_LIB)
        }
    }
}