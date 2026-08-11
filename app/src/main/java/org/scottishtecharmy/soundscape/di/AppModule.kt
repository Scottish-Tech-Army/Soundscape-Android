package org.scottishtecharmy.soundscape.di

import androidx.preference.PreferenceManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.scottishtecharmy.soundscape.SoundscapeIntents
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.audio.AudioTour
import org.scottishtecharmy.soundscape.audio.AudioTourHost
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.database.local.MarkersAndRoutesDatabaseProvider
import org.scottishtecharmy.soundscape.intents.IntentEventBus
import org.scottishtecharmy.soundscape.preferences.AndroidPreferencesProvider
import org.scottishtecharmy.soundscape.preferences.Preferences
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.screens.home.HomeViewModel
import org.scottishtecharmy.soundscape.screens.home.Navigator
import org.scottishtecharmy.soundscape.screens.onboarding.AudioOnboardingViewModel
import org.scottishtecharmy.soundscape.screens.onboarding.accessibility.AccessibilityOnboardingViewModel
import org.scottishtecharmy.soundscape.screens.onboarding.offlinestorage.OffscreenStorageOnboardingViewModel
import org.scottishtecharmy.soundscape.services.ServiceConnection
import org.scottishtecharmy.soundscape.utils.AndroidMarkersAndRoutesIo
import org.scottishtecharmy.soundscape.utils.AndroidOfflineMapsManager
import org.scottishtecharmy.soundscape.utils.MarkersAndRoutesIo
import org.scottishtecharmy.soundscape.viewmodels.SettingsViewModel

val appModule = module {

    single<Preferences> {
        Preferences(get())
    }

    single {
        val audioEngine = NativeAudioEngine(preferences = get())
        audioEngine.initialize(androidContext())
        audioEngine
    }

    single { Navigator() }

    single { MarkersAndRoutesDatabaseProvider.getInstance(androidContext()) }

    single { get<org.scottishtecharmy.soundscape.database.local.MarkersAndRoutesDatabase>().routeDao() }

    single<PreferencesProvider> {
        AndroidPreferencesProvider(PreferenceManager.getDefaultSharedPreferences(androidContext()))
    }

    single { SoundscapeServiceConnection() }
    // Expose the multiplatform ServiceConnection interface so commonMain
    // ViewModels can be constructed by Koin without going via the
    // platform-specific SoundscapeServiceConnection type.
    single<ServiceConnection> { get<SoundscapeServiceConnection>() }

    single {
        val connection = get<SoundscapeServiceConnection>()
        AudioTour(object : AudioTourHost {
            override fun isAudioEngineBusy(): Boolean =
                connection.soundscapeService?.isAudioEngineBusy() ?: false

            override fun clearTextToSpeechQueue() {
                connection.soundscapeService?.clearTextToSpeechQueue()
            }
        })
    }

    single { IntentEventBus() }
    single { SoundscapeIntents(get()) }

    // ViewModels — only the ones still injected via Koin in Android-only
    // composables. Shared per-screen ViewModels are constructed by SharedNavGraph
    // via the createXxxViewModel factories in AppCallbacks.
    viewModelOf(::HomeViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::AccessibilityOnboardingViewModel)
    viewModelOf(::AudioOnboardingViewModel)
    viewModelOf(::OffscreenStorageOnboardingViewModel)

    single { AndroidOfflineMapsManager(androidContext(), get()) }

    single { AndroidMarkersAndRoutesIo(androidContext()) }
    single<MarkersAndRoutesIo> { get<AndroidMarkersAndRoutesIo>() }
}
