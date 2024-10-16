package com.gaarj.iptvplayer.data

import androidx.room.Transaction
import com.gaarj.iptvplayer.data.dao.ApiCallDao
import com.gaarj.iptvplayer.data.dao.ApiCallHeaderDao
import com.gaarj.iptvplayer.data.dao.ApiResponseKeyDao
import com.gaarj.iptvplayer.data.dao.ChannelDao
import com.gaarj.iptvplayer.data.dao.StreamSourceDao
import com.gaarj.iptvplayer.data.dao.StreamSourceHeaderDao
import com.gaarj.iptvplayer.data.database.entities.ChannelEntity
import com.gaarj.iptvplayer.data.database.entities.toDatabase
import com.gaarj.iptvplayer.data.services.ChannelService
import com.gaarj.iptvplayer.domain.model.ApiCallHeaderItem
import com.gaarj.iptvplayer.domain.model.ApiCallItem
import com.gaarj.iptvplayer.domain.model.ApiResponseKeyItem
import com.gaarj.iptvplayer.domain.model.ChannelItem
import com.gaarj.iptvplayer.domain.model.ChannelShortnameItem
import com.gaarj.iptvplayer.domain.model.StreamSourceHeaderItem
import com.gaarj.iptvplayer.domain.model.StreamSourceItem
import com.gaarj.iptvplayer.domain.model.toDomain
import javax.inject.Inject

class ChannelRepository @Inject constructor(
    private val channelService: ChannelService,
    private val channelDao: ChannelDao,
    private val streamSourceDao: StreamSourceDao,
    private val apiCallDao: ApiCallDao,
    private val apiResponseKeyDao: ApiResponseKeyDao,
    private val streamSourceHeaderDao: StreamSourceHeaderDao,
    private val apiCallHeaderDao: ApiCallHeaderDao
) {

    suspend fun getFavouriteChannels(): List<ChannelItem> {
        val channelEntities = channelDao.getFavouriteChannels()

        return channelEntities.map { channelEntity ->
            // Fetch streamSources, relatedChannels, channelShortnames as needed
            val streamSources = fetchStreamSourcesForChannel(channelEntity.id)
            val relatedChannels = fetchRelatedChannels(channelEntity.id)
            val channelShortnames = fetchChannelShortnamesForChannel(channelEntity.id)

            channelEntity.toDomain(
                streamSources = streamSources,
                relatedChannels = relatedChannels,
                channelShortnames = channelShortnames
            )
        }
    }

    suspend fun fetchApiCallsForStreamSource(streamSourceId: Long): List<ApiCallItem> {
        val apiCalls = apiCallDao.getApiCallsForStreamSource(streamSourceId)
        return apiCalls.map { apiCallEntity ->
            // Fetch apiResponseKeys, apiCallHeaders
            val apiResponseKeys = fetchApiResponseKeysForApiCall(apiCallEntity.id)
            val apiCallHeaders = fetchApiCallHeadersForApiCall(apiCallEntity.id)
            apiCallEntity.toDomain(apiCallHeaders, apiResponseKeys)
        }
    }

    suspend fun fetchApiResponseKeysForApiCall(apiCallId: Long): List<ApiResponseKeyItem> {
        val apiResponseKeys = apiResponseKeyDao.getApiResponseKeysForApiCall(apiCallId)
        return apiResponseKeys.map { apiResponseKeyEntity ->
            apiResponseKeyEntity.toDomain()
        }
    }

    suspend fun fetchApiCallHeadersForApiCall(apiCallId: Long): List<ApiCallHeaderItem> {
        val apiCallHeaders = apiCallHeaderDao.getApiCallHeadersForApiCall(apiCallId)
        return apiCallHeaders.map { apiCallHeaderEntity ->
            apiCallHeaderEntity.toDomain()
        }
    }

    suspend fun fetchStreamSourceHeadersForStreamSource(streamSourceId: Long): List<StreamSourceHeaderItem> {
        val streamSourceHeaders = streamSourceHeaderDao.getStreamSourceHeadersForStreamSource(streamSourceId)
        return streamSourceHeaders.map {
            streamSourceHeaderEntity ->
            streamSourceHeaderEntity.toDomain()
        }
    }

    suspend fun fetchStreamSourcesForChannel(channelId: Long): List<StreamSourceItem> {
        val streamSources = streamSourceDao.getStreamSourcesForChannel(channelId)
        return streamSources.map { streamSourceEntity ->
            val streamSourceHeaders = fetchStreamSourceHeadersForStreamSource(streamSourceEntity.id)
            val apiCalls = fetchApiCallsForStreamSource(streamSourceEntity.id)
            streamSourceEntity.toDomain(headers = streamSourceHeaders, apiCalls = apiCalls)
        }
    }

    suspend fun fetchRelatedChannels(channelId: Long): List<ChannelItem> {
        // Fetch related channels from the database or elsewhere
        return listOf() // Empty for now
    }

    suspend fun fetchChannelShortnamesForChannel(channelId: Long): List<ChannelShortnameItem> {
        val channelShortnames = channelDao.getChannelShortnamesForChannel(channelId)
        return channelShortnames.map { channelShortnameEntity ->
            channelShortnameEntity.toDomain()
        }
    }

    suspend fun insertChannels(channels: List<ChannelEntity>) {
        channelDao.insertChannels(channels)
    }

    @Transaction
    suspend fun insertChannelWithAllData(channelItem: ChannelItem) {
        val channelId = channelDao.insertChannel(channelItem.toDatabase())

        val shortnames = channelItem.channelShortnames.map { shortname ->
            shortname.toDatabase(channelId)
        }
        channelDao.insertChannelShortnames(shortnames)

        for (streamSource in channelItem.streamSources) {
            val streamSourceEntity = streamSource.toDatabase(channelId)
            val streamSourceId = streamSourceDao.insertStreamSource(streamSourceEntity)
            for (header in streamSource.headers?: listOf()) {
                streamSourceHeaderDao.insertStreamSourceHeader(header.toDatabase(streamSourceId))
            }
            for (apiCall in streamSource.apiCalls ?: listOf()) {
                val apiCallEntity = apiCall.toDatabase(streamSourceId)
                val apiCallId = apiCallDao.insertApiCall(apiCallEntity)
                for (apiResponseKey in apiCall.apiResponseKeys) {
                    apiResponseKeyDao.insertApiResponseKey(apiResponseKey.toDatabase(apiCallId))
                }
                for (apiCallHeader in apiCall.headers ?: listOf()) {
                    apiCallHeaderDao.insertApiCallHeader(apiCallHeader.toDatabase(apiCallId))
                }
            }
        }
    }

    suspend fun deleteAll() {
        channelDao.deleteAll()
    }

}