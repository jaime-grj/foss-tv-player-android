package com.gaarj.iptvplayer.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gaarj.iptvplayer.data.database.ChannelDatabase
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import dagger.Provides
import java.util.concurrent.Executors

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {

    private const val DATABASE_NAME = "iptv.db"

    @Singleton
    @Provides
    fun provideRoom(@ApplicationContext context: Context) =
        Room.databaseBuilder(context, ChannelDatabase::class.java, DATABASE_NAME)
            /*.setQueryCallback({ sqlQuery, _ ->
                Log.d("RoomQuery", "SQL Query: $sqlQuery")
            }, Executors.newSingleThreadExecutor())*/
            .build()

    @Singleton
    @Provides
    fun provideChannelDao(db: ChannelDatabase) = db.getChannelDao()

    @Singleton
    @Provides
    fun provideCategoryDao(db: ChannelDatabase) = db.getCategoryDao()

    @Singleton
    @Provides
    fun provideApiResponseKeyDao(db: ChannelDatabase) = db.getApiResponseKeyDao()

    @Singleton
    @Provides
    fun provideApiCallDao(db: ChannelDatabase) = db.getApiCallDao()

    /*@Singleton
    @Provides
    fun provideChannelShortnameDao(db: ChannelDatabase) = db.getChannelShortnameDao()*/

    @Singleton
    @Provides
    fun provideStreamSourceDao(db: ChannelDatabase) = db.getStreamSourceDao()

    @Singleton
    @Provides
    fun provideHeaderStreamSourceDao(db: ChannelDatabase) = db.getHeaderStreamSourceDao()

    @Singleton
    @Provides
    fun provideHeaderApiCallDao(db: ChannelDatabase) = db.getHeaderApiCallDao()

    @Singleton
    @Provides
    fun provideEPGDao(db: ChannelDatabase) = db.getEPGDao()

    @Singleton
    @Provides
    fun provideProxyDao(db: ChannelDatabase) = db.getProxyDao()
}