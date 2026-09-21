package org.scottishtecharmy.soundscape.screens.onboarding.language

import android.app.LocaleManager
import android.content.Context
import android.content.res.Resources
import android.os.Build
import java.util.Locale

actual fun getAppLocale(): LocaleSnapshot? {
    val locale = appLocaleProvider?.invoke() ?: return null
    return LocaleSnapshot(locale.language, locale.country.takeIf { it.isNotEmpty() })
}

actual fun getSystemLocale(): LocaleSnapshot {
    // On API 33+ LocaleManager.systemLocales is the real device locale,
    // unaffected by per-app locale settings. Older APIs fall back to
    // Resources.getSystem() which is the best signal we have without
    // AppCompat in this module.
    val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val ctx = androidAppContext ?: return systemLocaleFromResources()
        val mgr = ctx.getSystemService(LocaleManager::class.java)
        mgr.systemLocales[0]
    } else {
        Resources.getSystem().configuration.locales[0]
    }
    return LocaleSnapshot(locale.language, locale.country.takeIf { it.isNotEmpty() })
}

private fun systemLocaleFromResources(): LocaleSnapshot {
    val locale = Resources.getSystem().configuration.locales[0]
    return LocaleSnapshot(locale.language, locale.country.takeIf { it.isNotEmpty() })
}

/**
 * Optional Application context. The Android app sets this once at startup so
 * [getSystemLocale] can reach `LocaleManager` on API 33+ without each call
 * site threading a context through.
 */
@Volatile
internal var androidAppContext: Context? = null

/**
 * App-side hook for reading the per-app locale override. Returns the [Locale]
 * the user has explicitly chosen for the app, or `null` if the app is
 * following the system default. The Android app installs an
 * `AppCompatDelegate.getApplicationLocales()` reader; the shared module can't
 * depend on AppCompat directly.
 */
@Volatile
internal var appLocaleProvider: (() -> Locale?)? = null

/** Wires up the application context and app-locale reader. Call from Application.onCreate. */
fun installLocaleContext(context: Context, appLocale: () -> Locale? = { null }) {
    androidAppContext = context.applicationContext
    appLocaleProvider = appLocale
}
