package com.gaarx.tvplayer.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gaarx.tvplayer.data.dao.ApiCallDao
import com.gaarx.tvplayer.data.dao.ApiResponseKeyDao
import com.gaarx.tvplayer.data.dao.CategoryDao
import com.gaarx.tvplayer.data.dao.ChannelDao
import com.gaarx.tvplayer.data.dao.ApiCallHeaderDao
import com.gaarx.tvplayer.data.dao.DrmHeaderDao
import com.gaarx.tvplayer.data.dao.EPGDao
import com.gaarx.tvplayer.data.dao.ProxyDao
import com.gaarx.tvplayer.data.dao.StreamSourceHeaderDao
import com.gaarx.tvplayer.data.dao.StreamSourceDao
import com.gaarx.tvplayer.data.database.converters.Converters
import com.gaarx.tvplayer.data.database.entities.ApiCallEntity
import com.gaarx.tvplayer.data.database.entities.ApiCallHeaderEntity
import com.gaarx.tvplayer.data.database.entities.ApiResponseKeyEntity
import com.gaarx.tvplayer.data.database.entities.CategoryEntity
import com.gaarx.tvplayer.data.database.entities.ChannelEntity
import com.gaarx.tvplayer.data.database.entities.ChannelShortnameEntity
import com.gaarx.tvplayer.data.database.entities.EPGProgramEntity
import com.gaarx.tvplayer.data.database.entities.StreamSourceEntity
import com.gaarx.tvplayer.data.database.entities.StreamSourceHeaderEntity
import com.gaarx.tvplayer.data.database.entities.ProxyEntity
import com.gaarx.tvplayer.data.database.entities.DrmHeaderEntity

@Database(
    entities = [
        ChannelEntity::class,
        CategoryEntity::class,
        ChannelShortnameEntity::class,
        ApiCallEntity::class,
        ApiResponseKeyEntity::class,
        ApiCallHeaderEntity::class,
        StreamSourceEntity::class,
        StreamSourceHeaderEntity::class,
        EPGProgramEntity::class,
        ProxyEntity::class,
        DrmHeaderEntity::class],
    version = 1
)
@TypeConverters(Converters::class)
abstract class ChannelDatabase : RoomDatabase() {
    abstract fun getChannelDao(): ChannelDao
    abstract fun getCategoryDao(): CategoryDao
    abstract fun getApiCallDao(): ApiCallDao
    //abstract fun getChannelShortnameDao(): ChannelShortnameDao
    abstract fun getApiResponseKeyDao(): ApiResponseKeyDao
    abstract fun getStreamSourceDao(): StreamSourceDao
    abstract fun getHeaderApiCallDao(): ApiCallHeaderDao
    abstract fun getHeaderStreamSourceDao(): StreamSourceHeaderDao
    abstract fun getEPGDao(): EPGDao
    abstract fun getProxyDao(): ProxyDao
    abstract fun getHeaderDrmDao(): DrmHeaderDao
}