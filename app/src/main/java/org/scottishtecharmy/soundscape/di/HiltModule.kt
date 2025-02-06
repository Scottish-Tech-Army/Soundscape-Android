package org.scottishtecharmy.soundscape.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.realm.kotlin.Realm
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.database.local.RealmConfiguration
import org.scottishtecharmy.soundscape.database.local.dao.RoutesDao
import org.scottishtecharmy.soundscape.database.repository.RoutesRepository
import org.scottishtecharmy.soundscape.screens.home.Navigator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppNativeAudioEngine {
    @Provides
    @Singleton
    fun provideNativeAudioEngine(
        @ApplicationContext context: Context,
    ): NativeAudioEngine {
        val audioEngine = NativeAudioEngine()
        audioEngine.initialize(context, false)
        return audioEngine
    }
}

@Module
@InstallIn(SingletonComponent::class)
class AppSoundscapeNavigator {
    @Provides
    @Singleton
    fun provideNavigator(): Navigator = Navigator()
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    fun provideRoutesRepository(routesDao: RoutesDao): RoutesRepository = RoutesRepository(routesDao)
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideMarkersRealmInstance(): Realm {
        // Fetches a Realm instance using the RealmConfiguration methods
        return RealmConfiguration.getMarkersInstance()
    }

    @Provides
    fun provideRoutesDao(realm: Realm): RoutesDao {
        // Provides RoutesDao, which depends on Realm
        return RoutesDao(realm)
    }
}
