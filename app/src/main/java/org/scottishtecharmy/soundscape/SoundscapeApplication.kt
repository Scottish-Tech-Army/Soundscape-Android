package org.scottishtecharmy.soundscape

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.scottishtecharmy.soundscape.di.appModule

class SoundscapeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SoundscapeApplication)
            modules(appModule)
        }
    }
}
