package com.gaarx.tvplayer.di

import android.content.Context
import com.gaarx.tvplayer.data.services.SettingsService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    @Singleton
    @Provides
    fun provideSettingsService(@ApplicationContext context: Context) : SettingsService {
        return SettingsService(context)
    }
}