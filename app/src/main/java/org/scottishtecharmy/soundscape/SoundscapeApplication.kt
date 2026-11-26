package org.scottishtecharmy.soundscape

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.scottishtecharmy.soundscape.di.appModule
import org.scottishtecharmy.soundscape.logging.AppLog
import org.scottishtecharmy.soundscape.platform.isDebugBuild
import org.scottishtecharmy.soundscape.screens.onboarding.language.installLocaleContext
import org.scottishtecharmy.soundscape.utils.ProcessExitLogger

class SoundscapeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLog.initialize(isDebugBuild)
        // The support log is lost with the process, so say how the last ones ended
        ProcessExitLogger.logPreviousExits(this)
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
