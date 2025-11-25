package org.scottishtecharmy.soundscape.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.database.local.MarkersAndRoutesDatabase
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
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
        audioEngine.initialize(context)
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
object DataStoreModule {
    @Provides
    @Singleton
    fun provideDatabaseInstance(
        @ApplicationContext context: Context
    ): MarkersAndRoutesDatabase {
        // Fetches the database instance
        return MarkersAndRoutesDatabase.getMarkersInstance(context)
    }

    @Provides
    fun provideRoutesDao(database: MarkersAndRoutesDatabase): RouteDao {
        // Provides the DAO from the database
        return database.routeDao()
    }
}
