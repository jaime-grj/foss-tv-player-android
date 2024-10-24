package com.gaarj.iptvplayer.data

import androidx.room.Transaction
import com.gaarj.iptvplayer.data.dao.ApiCallDao
import com.gaarj.iptvplayer.data.dao.ApiCallHeaderDao
import com.gaarj.iptvplayer.data.dao.ApiResponseKeyDao
import com.gaarj.iptvplayer.data.dao.CategoryDao
import com.gaarj.iptvplayer.data.dao.ChannelDao
import com.gaarj.iptvplayer.data.dao.StreamSourceDao
import com.gaarj.iptvplayer.data.dao.StreamSourceHeaderDao
import com.gaarj.iptvplayer.data.database.entities.ApiCallEntity
import com.gaarj.iptvplayer.data.database.entities.ApiCallHeaderEntity
import com.gaarj.iptvplayer.data.database.entities.ApiResponseKeyEntity
import com.gaarj.iptvplayer.data.database.entities.CategoryEntity
import com.gaarj.iptvplayer.data.database.entities.ChannelEntity
import com.gaarj.iptvplayer.data.database.entities.ChannelShortnameEntity
import com.gaarj.iptvplayer.data.database.entities.StreamSourceEntity
import com.gaarj.iptvplayer.data.database.entities.StreamSourceHeaderEntity
import com.gaarj.iptvplayer.data.database.entities.toDatabase
import com.gaarj.iptvplayer.data.services.DataService
import com.gaarj.iptvplayer.domain.model.ApiCallHeaderItem
import com.gaarj.iptvplayer.domain.model.ApiCallItem
import com.gaarj.iptvplayer.domain.model.ApiResponseKeyItem
import com.gaarj.iptvplayer.domain.model.ChannelItem
import com.gaarj.iptvplayer.domain.model.ChannelShortnameItem
import com.gaarj.iptvplayer.domain.model.StreamSourceHeaderItem
import com.gaarj.iptvplayer.domain.model.StreamSourceItem
import com.gaarj.iptvplayer.domain.model.StreamSourceTypeItem
import com.gaarj.iptvplayer.domain.model.toDomain
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class ChannelRepository @Inject constructor(
    private val dataService: DataService,
    private val channelDao: ChannelDao,
    private val streamSourceDao: StreamSourceDao,
    private val apiCallDao: ApiCallDao,
    private val apiResponseKeyDao: ApiResponseKeyDao,
    private val streamSourceHeaderDao: StreamSourceHeaderDao,
    private val apiCallHeaderDao: ApiCallHeaderDao,
    private val categoryDao: CategoryDao
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

    suspend fun loadChannelsFromJSON(): Boolean {
        val jsonStr = DataService.getJSONString()
        if (jsonStr.isEmpty()) {
            return false
        }

        val jsonObj = JSONObject(jsonStr)

        val data = jsonObj.getJSONObject("data")
        val categories : JSONArray = data.getJSONArray("categories")

        for (i in 0 until categories.length()) {
            val category = categories.getJSONObject(i)
            val categoryName = category.getString("name")
            val categoryDescription = try{
                category.getString("description")
            } catch (e: Exception) {
                null
            }
            val categoryEntity = CategoryEntity(
                name = categoryName,
                description = categoryDescription
            )
            val categoryId = categoryDao.insertCategory(categoryEntity)
            val channels = category.getJSONArray("channels")

            for (j in 0 until channels.length()) {
                val channel = channels.getJSONObject(j)
                val channelName = channel.getString("name")
                val channelDescription = channel.getString("description")
                val channelLanguage = try{
                    channel.getString("language")
                } catch (e: Exception) {
                    null
                }
                val channelCountry = try {
                    channel.getString("country")
                } catch (e: Exception) {
                    null
                }
                val channelRegion = try {
                    channel.getString("region")
                } catch (e: Exception) {
                    null
                }
                val channelSubregion = try {
                    channel.getString("subregion")
                } catch (e: Exception) {
                    null
                }
                val channelShortnames = channel.getJSONArray("channelTags")
                val channelStreamSources = channel.getJSONArray("streamSources")
                val channelIndexFavourite = channel.getInt("indexFavourite")
                val channelIndexGroup = channel.getInt("indexGroup")

                val channelEntity = ChannelEntity(
                    name = channelName,
                    description = channelDescription,
                    language = channelLanguage,
                    country = channelCountry,
                    region = channelRegion,
                    subregion = channelSubregion,
                    indexFavourite = channelIndexFavourite,
                    indexGroup = channelIndexGroup,
                    categoryId = categoryId,
                    parentId = null
                )

                val channelId = channelDao.insertChannel(channelEntity)

                for (k in 0 until channelShortnames.length()) {
                    val channelShortname = channelShortnames.getString(k)
                    val channelShortnameEntity = ChannelShortnameEntity(
                        shortName = channelShortname,
                        channelId = channelId
                    )
                    channelDao.insertChannelShortname(channelShortnameEntity)
                }

                for (k in 0 until channelStreamSources.length()) {
                    val streamSource = channelStreamSources.getJSONObject(k)
                    val streamSourceName = streamSource.getString("name")
                    val streamSourceUrl = streamSource.getString("url")
                    val streamSourceHeaders = try {
                        streamSource.getJSONArray("headers")
                    } catch (e: Exception) {
                        JSONArray()
                    }
                    val streamSourceApiCalls = try {
                        streamSource.getJSONArray("apiCalls")
                    } catch (e: Exception) {
                        JSONArray()
                    }
                    val streamSourceRefreshRate = try {
                        streamSource.getString("refreshRate")
                    } catch (e: Exception) {
                        null
                    }
                    val streamSourceType = StreamSourceTypeItem.IPTV

                    val streamSourceEntity = StreamSourceEntity(
                        name = streamSourceName,
                        url = streamSourceUrl,
                        channelId = channelId,
                        index = streamSource.getInt("index"),
                        refreshRate = streamSourceRefreshRate?.toFloat(),
                        streamSourceType = streamSourceType,
                    )
                    val streamSourceId = streamSourceDao.insertStreamSource(streamSourceEntity)

                    for (l in 0 until streamSourceHeaders.length()) {
                        val header = streamSourceHeaders.getJSONObject(l)
                        val headerEntity = StreamSourceHeaderEntity(
                            key = header.getString("key"),
                            value = header.getString("value"),
                            streamSourceId = streamSourceId
                        )
                        streamSourceHeaderDao.insertStreamSourceHeader(headerEntity)
                    }
                    for (l in 0 until streamSourceApiCalls.length()) {
                        val apiCall = streamSourceApiCalls.getJSONObject(l)
                        val apiCallUrl = apiCall.getString("url")
                        val apiCallType = apiCall.getString("type")
                        val apiCallMethod = apiCall.getString("method")
                        val apiCallIndex = apiCall.getInt("index")
                        val apiCallBody = try{
                             apiCall.getString("body")
                        } catch (e: Exception) {
                            null
                        }
                        val apiCallStringSearch = try {
                            apiCall.getString("stringSearch")
                        } catch (e: Exception) {
                            null
                        }
                        val apiCallResponseKeys = try {
                            apiCall.getJSONArray("responseKeys")
                        } catch (e: Exception) {
                            JSONArray()
                        }
                        val apiCallHeaders = try {
                            apiCall.getJSONArray("headers")
                        } catch (e: Exception) {
                            JSONArray()
                        }
                        println("apiCallUrl: $apiCallUrl apiCallIndex: $apiCallIndex")
                        val apiCallEntity = ApiCallEntity(
                            url = apiCallUrl,
                            index = apiCallIndex,
                            method = apiCallMethod,
                            body = apiCallBody,
                            stringSearch = apiCallStringSearch,
                            type = apiCallType,
                            streamSourceId = streamSourceId
                        )
                        val apiCallId = apiCallDao.insertApiCall(apiCallEntity)

                        for (m in 0 until apiCallResponseKeys.length()) {
                            val responseKey = apiCallResponseKeys.getJSONObject(m)
                            val responseKeyIndex = responseKey.getInt("index")
                            val responseKeyJSONPath = responseKey.getString("jsonPath")
                            val responseKeyEntity = ApiResponseKeyEntity(
                                jsonPath = responseKeyJSONPath,
                                index = responseKeyIndex,
                                apiCallId = apiCallId
                            )
                            apiResponseKeyDao.insertApiResponseKey(responseKeyEntity)
                        }
                        for (m in 0 until apiCallHeaders.length()) {
                            val header = apiCallHeaders.getJSONObject(m)
                            val headerEntity = ApiCallHeaderEntity(
                                key = header.getString("key"),
                                value = header.getString("value"),
                                apiCallId = apiCallId
                            )
                            apiCallHeaderDao.insertApiCallHeader(headerEntity)
                        }
                    }
                }
            }
        }

        return true
    }

    suspend fun deleteAll() {
        channelDao.deleteAll()
    }

}