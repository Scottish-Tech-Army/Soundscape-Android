package org.scottishtecharmy.soundscape

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.scottishtecharmy.soundscape.di.appModule
import org.scottishtecharmy.soundscape.screens.onboarding.language.installLocaleContext

class SoundscapeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // The shared module can't depend on AppCompat, so the app provides the
        // app-locale reader. Returning null means "follow system default".
        installLocaleContext(this) {
            AppCompatDelegate.getApplicationLocales()[0]
        }
        startKoin {
            androidContext(this@SoundscapeApplication)
            modules(appModule)
        }
    }
}
