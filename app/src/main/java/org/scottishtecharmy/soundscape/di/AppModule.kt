package org.scottishtecharmy.soundscape.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.SoundscapeIntents
import org.scottishtecharmy.soundscape.audio.AudioTour
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.database.local.MarkersAndRoutesDatabase
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.screens.home.Navigator
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyViewModel
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.AddAndEditRouteViewModel
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.MarkersViewModel
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen.RouteDetailsViewModel
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen.RoutesViewModel
import org.scottishtecharmy.soundscape.screens.onboarding.AudioOnboardingViewModel
import org.scottishtecharmy.soundscape.screens.onboarding.accessibility.AccessibilityOnboardingViewModel
import org.scottishtecharmy.soundscape.screens.onboarding.language.LanguageViewModel
import org.scottishtecharmy.soundscape.screens.onboarding.offlinestorage.OffscreenStorageOnboardingViewModel
import org.scottishtecharmy.soundscape.viewmodels.AdvancedMarkersAndRoutesSettingsViewModel
import org.scottishtecharmy.soundscape.viewmodels.LocationDetailsViewModel
import org.scottishtecharmy.soundscape.viewmodels.OfflineMapsViewModel
import org.scottishtecharmy.soundscape.viewmodels.OpenSourceLicensesViewModel
import org.scottishtecharmy.soundscape.viewmodels.SettingsViewModel
import org.scottishtecharmy.soundscape.viewmodels.home.HomeViewModel
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

val appModule = module {

    single {
        val audioEngine = NativeAudioEngine()
        audioEngine.initialize(androidContext())
        audioEngine
    }

    single { Navigator() }

    single { MarkersAndRoutesDatabase.getMarkersInstance(androidContext()) }

    single { get<MarkersAndRoutesDatabase>().routeDao() }

    single { SoundscapeServiceConnection() }

    single { AudioTour(androidContext(), get()) }

    single { SoundscapeIntents(get()) }

    // ViewModels
    viewModelOf(::HomeViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::LocationDetailsViewModel)
    viewModelOf(::OpenSourceLicensesViewModel)
    viewModelOf(::AdvancedMarkersAndRoutesSettingsViewModel)
    viewModelOf(::LanguageViewModel)
    viewModelOf(::AccessibilityOnboardingViewModel)
    viewModelOf(::AudioOnboardingViewModel)
    viewModelOf(::PlacesNearbyViewModel)
    viewModelOf(::MarkersViewModel)
    viewModelOf(::RoutesViewModel)
    viewModelOf(::RouteDetailsViewModel)
    viewModelOf(::AddAndEditRouteViewModel)
    viewModelOf(::OffscreenStorageOnboardingViewModel)

    viewModel { (locationDescription: LocationDescription) ->
        OfflineMapsViewModel(androidContext(), locationDescription)
    }
}
