package com.gaarj.iptvplayer.di

import android.content.Context
import com.gaarj.iptvplayer.data.services.EPGService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EPGModule {

    @Singleton
    @Provides
    fun provideEPGReader(@ApplicationContext context: Context) : EPGService {
        return EPGService(context)
    }
}