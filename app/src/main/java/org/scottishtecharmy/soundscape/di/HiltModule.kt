package org.scottishtecharmy.soundscape.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.datastore.DataStoreManager
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class AppDataStoreManager {
    @Provides
    @Singleton
    fun provideDataStoreManager(@ApplicationContext context: Context): DataStoreManager {
        return DataStoreManager(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
class AppNativeAudioEngine {
    @Provides
    @Singleton
    fun provideNativeAudioEngine(@ApplicationContext context: Context): NativeAudioEngine {
        val audioEngine = NativeAudioEngine()
        audioEngine.initialize(context)
        return audioEngine
    }
}

@Module
@InstallIn(SingletonComponent::class)
class AppSoundscapeServiceConnection {
    @Provides
    @Singleton
    fun provideSoundscapeServiceConnection(@ApplicationContext context: Context): SoundscapeServiceConnection {
        val serviceConnection = SoundscapeServiceConnection(context)
        return serviceConnection
    }
}
