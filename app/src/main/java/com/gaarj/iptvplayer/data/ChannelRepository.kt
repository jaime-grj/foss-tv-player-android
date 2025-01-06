package com.gaarj.iptvplayer.data

import android.util.Log
import androidx.room.Transaction
import com.gaarj.iptvplayer.data.dao.ApiCallDao
import com.gaarj.iptvplayer.data.dao.ApiCallHeaderDao
import com.gaarj.iptvplayer.data.dao.ApiResponseKeyDao
import com.gaarj.iptvplayer.data.dao.CategoryDao
import com.gaarj.iptvplayer.data.dao.ChannelDao
import com.gaarj.iptvplayer.data.dao.DrmHeaderDao
import com.gaarj.iptvplayer.data.dao.EPGDao
import com.gaarj.iptvplayer.data.dao.ProxyDao
import com.gaarj.iptvplayer.data.dao.StreamSourceDao
import com.gaarj.iptvplayer.data.dao.StreamSourceHeaderDao
import com.gaarj.iptvplayer.data.database.entities.ApiCallEntity
import com.gaarj.iptvplayer.data.database.entities.ApiCallHeaderEntity
import com.gaarj.iptvplayer.data.database.entities.ApiResponseKeyEntity
import com.gaarj.iptvplayer.data.database.entities.CategoryEntity
import com.gaarj.iptvplayer.data.database.entities.ChannelEntity
import com.gaarj.iptvplayer.data.database.entities.ChannelShortnameEntity
import com.gaarj.iptvplayer.data.database.entities.DrmHeaderEntity
import com.gaarj.iptvplayer.data.database.entities.ProxyEntity
import com.gaarj.iptvplayer.data.database.entities.StreamSourceEntity
import com.gaarj.iptvplayer.data.database.entities.StreamSourceHeaderEntity
import com.gaarj.iptvplayer.data.database.entities.toDatabase
import com.gaarj.iptvplayer.data.services.DataService
import com.gaarj.iptvplayer.domain.model.ApiCallHeaderItem
import com.gaarj.iptvplayer.domain.model.ApiCallItem
import com.gaarj.iptvplayer.domain.model.ApiResponseKeyItem
import com.gaarj.iptvplayer.domain.model.CategoryItem
import com.gaarj.iptvplayer.domain.model.ChannelItem
import com.gaarj.iptvplayer.domain.model.ChannelShortnameItem
import com.gaarj.iptvplayer.domain.model.EPGProgramItem
import com.gaarj.iptvplayer.domain.model.ProxyItem
import com.gaarj.iptvplayer.domain.model.DrmHeaderItem
import com.gaarj.iptvplayer.domain.model.DrmTypeItem
import com.gaarj.iptvplayer.domain.model.StreamSourceHeaderItem
import com.gaarj.iptvplayer.domain.model.StreamSourceItem
import com.gaarj.iptvplayer.domain.model.StreamSourceTypeItem
import com.gaarj.iptvplayer.domain.model.toDomain
import com.gaarj.iptvplayer.exceptions.ChannelNotFoundException
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
    private val categoryDao: CategoryDao,
    private val proxyDao: ProxyDao,
    private val epgDao: EPGDao,
    private val drmHeaderDao: DrmHeaderDao,
    private val epgRepository: EPGRepository,
    private val settingsRepository: SettingsRepository
) {

    suspend fun getChannelCountByCategory(categoryId: Long): Int {
        if (categoryId == -1L) {
            return channelDao.getFavouriteChannelCount()
        }
        return channelDao.getChannelCountByGroup(categoryId)
    }

    suspend fun getPreviousChannel(categoryId: Long, groupId: Int): ChannelItem {
        Log.i("ChannelRepository", "getPreviousChannel: categoryId: $categoryId, groupId: $groupId")
        val channelEntity: ChannelEntity?
        if (categoryId == -1L){
            val firstChannelId = channelDao.getFirstFavouriteChannelId()
            if (groupId <= firstChannelId - 1) {
                val lastChannelId = channelDao.getLastFavouriteChannelId()
                channelEntity = channelDao.getPreviousChannelByFavouriteId(lastChannelId)
            } else {
                channelEntity = channelDao.getPreviousChannelByFavouriteId(groupId)
            }
        }
        else{
            val firstChannelId = channelDao.getFirstGroupChannelId(categoryId)
            if (groupId <= firstChannelId - 1) {
                val lastChannelId = channelDao.getLastGroupChannelId(categoryId)
                println("lastChannelId: $lastChannelId, groupId: $groupId")
                channelEntity = channelDao.getPreviousChannelByGroupId(categoryId, lastChannelId)
            } else {
                channelEntity = channelDao.getPreviousChannelByGroupId(categoryId, groupId)
            }
        }
        if (channelEntity != null) {
            val streamSources = fetchStreamSourcesForChannel(channelEntity.id)
            val relatedChannels = fetchRelatedChannels(channelEntity.id)
            val channelShortnames = fetchChannelShortnamesForChannel(channelEntity.id)
            return channelEntity.toDomain(
                streamSources = streamSources,
                relatedChannels = relatedChannels,
                channelShortnames = channelShortnames
            )
        } else {
            throw Exception("Channel not found")
        }
    }

    suspend fun getNextChannel(categoryId: Long, groupId: Int): ChannelItem {
        val channelEntity: ChannelEntity?
        if (categoryId == -1L){
            val lastChannelId = channelDao.getLastFavouriteChannelId()
            if (groupId > lastChannelId) {
                val firstChannelId = channelDao.getFirstFavouriteChannelId()
                channelEntity = channelDao.getNextChannelByFavouriteId(firstChannelId)
            } else {
                channelEntity = channelDao.getNextChannelByFavouriteId(groupId)
            }
        }
        else{
            val lastChannelId = channelDao.getLastGroupChannelId(categoryId)
            if (groupId > lastChannelId) {
                val firstChannelId = channelDao.getFirstGroupChannelId(categoryId)
                channelEntity = channelDao.getNextChannelByGroupId(categoryId, firstChannelId)
            } else {
                channelEntity = channelDao.getNextChannelByGroupId(categoryId, groupId)
            }
        }
        if (channelEntity != null) {
            val streamSources = fetchStreamSourcesForChannel(channelEntity.id)
            val relatedChannels = fetchRelatedChannels(channelEntity.id)
            val channelShortnames = fetchChannelShortnamesForChannel(channelEntity.id)
            return channelEntity.toDomain(
                streamSources = streamSources,
                relatedChannels = relatedChannels,
                channelShortnames = channelShortnames
            )
        } else {
            throw Exception("Channel not found")
        }
    }

    suspend fun getNextChannelIndex(categoryId: Long, groupId: Int): Int {
        val channelIndex: Int
        if (categoryId == -1L){
            val lastChannelId = channelDao.getLastFavouriteChannelId()
            if (groupId >= lastChannelId) {
                val firstChannelId = channelDao.getFirstFavouriteChannelId()
                channelIndex = channelDao.getNextChannelFavouriteIndex(firstChannelId - 1)
            } else {
                channelIndex = channelDao.getNextChannelFavouriteIndex(groupId)
            }
        }
        else{
            val lastChannelId = channelDao.getLastGroupChannelId(categoryId)
            println("lastChannelId: $lastChannelId, groupId: $groupId")
            if (groupId >= lastChannelId) {
                val firstChannelId = channelDao.getFirstGroupChannelId(categoryId)
                channelIndex = channelDao.getNextChannelGroupIndex(categoryId, firstChannelId - 1)
            } else {
                channelIndex = channelDao.getNextChannelGroupIndex(categoryId, groupId)
            }
        }
        return channelIndex
    }

    suspend fun getPreviousChannelIndex(categoryId: Long, groupId: Int): Int {
        val channelIndex: Int
        if (categoryId == -1L){
            val firstChannelId = channelDao.getFirstFavouriteChannelId()
            if (groupId <= firstChannelId) {
                val lastChannelId = channelDao.getLastFavouriteChannelId()
                channelIndex = channelDao.getPreviousChannelFavouriteIndex(lastChannelId + 1)
            } else {
                channelIndex = channelDao.getPreviousChannelFavouriteIndex(groupId)
            }
        }
        else{
            val firstChannelId = channelDao.getFirstGroupChannelId(categoryId)
            if (groupId <= firstChannelId) {
                val lastChannelId = channelDao.getLastGroupChannelId(categoryId)
                channelIndex = channelDao.getPreviousChannelGroupIndex(categoryId, lastChannelId + 1)
            } else {
                channelIndex = channelDao.getPreviousChannelGroupIndex(categoryId, groupId)
            }
        }
        return channelIndex
    }

    suspend fun getChannel(categoryId: Long, groupId: Int): ChannelItem {
        val channelEntity: ChannelEntity? = if (categoryId == -1L){
            channelDao.getChannelByFavouriteId(groupId)
        } else{
            channelDao.getChannelByGroupId(categoryId, groupId)
        }
        if (channelEntity != null) {
            val streamSources = fetchStreamSourcesForChannel(channelEntity.id)
            val relatedChannels = fetchRelatedChannels(channelEntity.id)
            val channelShortnames = fetchChannelShortnamesForChannel(channelEntity.id)
            return channelEntity.toDomain(
                streamSources = streamSources,
                relatedChannels = relatedChannels,
                channelShortnames = channelShortnames
            )
        } else {
            throw ChannelNotFoundException("Channel not found")
        }
    }

    suspend fun getFavouriteChannels(): List<ChannelItem> {
        val channelEntities = channelDao.getFavouriteChannels()
        Log.d("ChannelRepository", "Fetched ${channelEntities.size} favourite channels")

        return channelEntities.map { channelEntity ->
            // Fetch streamSources, relatedChannels, channelShortnames as needed
            val streamSources = fetchStreamSourcesForChannel(channelEntity.id)
            val relatedChannels = fetchRelatedChannels(channelEntity.id)
            val channelShortnames = fetchChannelShortnamesForChannel(channelEntity.id)
            //val epgPrograms = epgRepository.getEPGProgramsForChannel(channelEntity.id)

            channelEntity.toDomain(
                streamSources = streamSources,
                relatedChannels = relatedChannels,
                channelShortnames = channelShortnames,
                //epgPrograms = epgPrograms
            )
        }
    }

    suspend fun getCategoriesWithChannels(): List<CategoryItem> {
        val favChannelItems = getFavouriteChannels()
        val favCategory = CategoryItem(
            id = -1,
            name = "Favoritos",
            description = "",
            isSelected = false,
            channels = favChannelItems
        )

        val categoryWithChannelItems: MutableList<CategoryItem> = mutableListOf()
        categoryWithChannelItems.add(favCategory)

        val categories = fetchAllCategories()
        for (category in categories) {
            val channels = getChannelsByCategory(category.id)
            val categoryWithChannels = CategoryItem(
                id = category.id,
                name = category.name,
                description = category.description,
                isSelected = false,
                channels = channels
            )
            categoryWithChannelItems.add(categoryWithChannels)
        }

        return categoryWithChannelItems
    }

    suspend fun fetchAllCategories(): List<CategoryItem> {
        val categories = categoryDao.getAllCategories()
        return categories.map { categoryEntity ->
            categoryEntity.toDomain()
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
            val proxies = fetchProxiesForStreamSource(streamSourceEntity.id)
            val drmHeaders = fetchDrmHeadersForStreamSource(streamSourceEntity.id)
            streamSourceEntity.toDomain(
                headers = streamSourceHeaders,
                apiCalls = apiCalls,
                proxies = proxies,
                drmHeaders = drmHeaders
            )
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

    suspend fun fetchProxiesForStreamSource(streamSourceId: Long): List<ProxyItem> {
        val proxies = proxyDao.getProxiesForStreamSource(streamSourceId)
        return proxies.map { proxyEntity ->
            proxyEntity.toDomain()
        }
    }

    suspend fun fetchDrmHeadersForStreamSource(streamSourceId: Long): List<DrmHeaderItem> {
        val drmHeaders = drmHeaderDao.getDrmHeadersForStreamSource(streamSourceId)
        return drmHeaders.map { drmHeaderEntity ->
            drmHeaderEntity.toDomain()
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
            Log.d("Category", "$categoryId - $categoryName")
            val channels = category.getJSONArray("channels")

            for (j in 0 until channels.length()) {
                val channel = channels.getJSONObject(j)
                val channelName = channel.getString("name")
                val channelDescription = try{
                    channel.getString("description")
                } catch (e: Exception) {
                    null
                }
                val channelLogo = try{
                    channel.getString("logo")
                } catch (e: Exception) {
                    null
                }
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
                val channelShortnames = try{
                    channel.getJSONArray("channelTags")
                } catch (e: Exception) {
                    JSONArray()
                }
                val channelStreamSources = try{
                    channel.getJSONArray("streamSources")
                } catch (e: Exception) {
                    JSONArray()
                }
                val channelIndexFavourite = try{
                    channel.getInt("indexFavourite")
                } catch (e: Exception) {
                    null
                }
                val channelIndexGroup = try{
                    channel.getInt("indexGroup")
                } catch (e: Exception) {
                    null
                }


                val channelEntity = ChannelEntity(
                    name = channelName,
                    description = channelDescription,
                    language = channelLanguage,
                    logo = channelLogo,
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
                    val proxies = try {
                        streamSource.getJSONArray("proxies")
                    } catch (e: Exception) {
                        JSONArray()
                    }
                    val streamSourceDrmHeaders = try {
                        streamSource.getJSONArray("drmHeaders")
                    } catch (e: Exception) {
                        JSONArray()
                    }
                    val drmTypeInt = try {
                        streamSource.getInt("drmType")
                    } catch (e: Exception) {
                        0
                    }
                    val drmKeysJSONArray = try {
                        streamSource.getJSONArray("drmKeys")
                    } catch (e: Exception) {
                        JSONArray()
                    }
                    val pssh = try {
                        streamSource.getString("pssh")
                    } catch (e: Exception) {
                        null
                    }
                    val licenseUrl = try {
                        streamSource.getString("licenseUrl")
                    } catch (e: Exception) {
                        null
                    }
                    val useUnofficialDrmLicenseMethod = try {
                        streamSource.getBoolean("useUnofficialDrmLicenseMethod")
                    } catch (e: Exception) {
                        false
                    }
                    val forceUseBestVideoResolution = try {
                        streamSource.getBoolean("forceUseBestVideoResolution")
                    } catch (e: Exception) {
                        false
                    }

                    val streamSourceTypeInt = try{
                        streamSource.getInt("type")
                    } catch (e: Exception) {
                        0
                    }

                    val streamSourceType = StreamSourceTypeItem.fromInt(streamSourceTypeInt) ?: StreamSourceTypeItem.IPTV
                    val drmType = DrmTypeItem.fromInt(drmTypeInt) ?: DrmTypeItem.NONE
                    val drmKeys = drmKeysJSONArray.join("\n").replace("\"", "")

                    val streamSourceEntity = StreamSourceEntity(
                        name = streamSourceName,
                        url = streamSourceUrl,
                        channelId = channelId,
                        index = streamSource.getInt("index"),
                        refreshRate = streamSourceRefreshRate?.toFloat(),
                        streamSourceType = streamSourceType,
                        drmType = drmType,
                        drmKeys = drmKeys,
                        pssh = pssh,
                        licenseUrl = licenseUrl,
                        useUnofficialDrmLicenseMethod = useUnofficialDrmLicenseMethod,
                        forceUseBestVideoResolution = forceUseBestVideoResolution
                    )
                    val streamSourceId = streamSourceDao.insertStreamSource(streamSourceEntity)

                    for (l in 0 until proxies.length()) {
                        val proxy = proxies.getJSONObject(l)
                        val proxyHostname = proxy.getString("hostname")
                        val proxyPort = proxy.getInt("port")
                        val proxyEntity = ProxyEntity(
                            hostname = proxyHostname,
                            port = proxyPort,
                            streamSourceId = streamSourceId
                        )
                        proxyDao.insertProxy(proxyEntity)
                    }

                    for (l in 0 until streamSourceHeaders.length()) {
                        val header = streamSourceHeaders.getJSONObject(l)
                        val headerEntity = StreamSourceHeaderEntity(
                            key = header.getString("key"),
                            value = header.getString("value"),
                            streamSourceId = streamSourceId
                        )
                        streamSourceHeaderDao.insertStreamSourceHeader(headerEntity)
                    }

                    for (l in 0 until streamSourceDrmHeaders.length()) {
                        val header = streamSourceDrmHeaders.getJSONObject(l)
                        val headerEntity = DrmHeaderEntity(
                            key = header.getString("key"),
                            value = header.getString("value"),
                            streamSourceId = streamSourceId
                        )
                        drmHeaderDao.insertDrmHeader(headerEntity)
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

        val epgSources : JSONArray = data.getJSONArray("epgSources")
        for (i in 0 until epgSources.length()) {
            val epgSourceUrl = epgSources.getString(i)
            if (epgSourceUrl.isNotEmpty()){
                settingsRepository.updateEpgSource(epgSourceUrl)
            }
        }
        return true
    }

    suspend fun deleteAll() {
        categoryDao.deleteAll()
        channelDao.deleteAll()
    }

    suspend fun getChannelsByCategory(categoryId: Long): List<ChannelItem> {
        if (categoryId == -1L) {
            return channelDao.getFavouriteChannels().map { channelEntity ->
                // Fetch streamSources, relatedChannels, channelShortnames as needed
                val streamSources = fetchStreamSourcesForChannel(channelEntity.id)
                val relatedChannels = fetchRelatedChannels(channelEntity.id)
                val channelShortnames = fetchChannelShortnamesForChannel(channelEntity.id)
                //val epgPrograms = epgRepository.getEPGProgramsForChannel(channelEntity.id)

                channelEntity.toDomain(
                    streamSources = streamSources,
                    relatedChannels = relatedChannels,
                    channelShortnames = channelShortnames,
                    //epgPrograms = epgPrograms
                )
            }
        }
        else{
            val channelEntities = channelDao.getChannelsForCategory(categoryId)
            Log.d("ChannelRepository", "Fetched ${channelEntities.size} channels for category $categoryId")

            return channelEntities.map { channelEntity ->
                // Fetch streamSources, relatedChannels, channelShortnames as needed
                val streamSources = fetchStreamSourcesForChannel(channelEntity.id)
                val relatedChannels = fetchRelatedChannels(channelEntity.id)
                val channelShortnames = fetchChannelShortnamesForChannel(channelEntity.id)
                //val epgPrograms = epgRepository.getEPGProgramsForChannel(channelEntity.id)

                channelEntity.toDomain(
                    streamSources = streamSources,
                    relatedChannels = relatedChannels,
                    channelShortnames = channelShortnames,
                    //epgPrograms = epgPrograms
                )
            }
        }
    }

    suspend fun getSmChannelsByCategory(categoryId: Long): List<ChannelItem> {
        if (categoryId == -1L) {
            return channelDao.getFavouriteChannels().map { channelEntity ->
                val currentProgram = epgRepository.getCurrentProgramForChannel(channelEntity.id)?.toDomain()
                if (currentProgram != null) {
                    channelEntity.toDomain(
                        epgPrograms = listOf(currentProgram)
                    )
                }
                else{
                    channelEntity.toDomain()
                }
            }
        }
        else{
            val channelEntities = channelDao.getChannelsForCategory(categoryId)
            return channelEntities.map { channelEntity ->
                val currentProgram = epgRepository.getCurrentProgramForChannel(channelEntity.id)?.toDomain()
                if (currentProgram != null) {
                    channelEntity.toDomain(
                        epgPrograms = listOf(currentProgram)
                    )
                }
                else{
                    channelEntity.toDomain()
                }
            }
        }
    }

    suspend fun getSmChannelsWithSchedule(): List<ChannelItem> {
        /*val favChannels = channelDao.getFavouriteChannels()
        val categoryChannels : MutableList<ChannelEntity> = mutableListOf()
        val categories = categoryDao.getAllCategories()
        for (category in categories) {
            categoryChannels+= channelDao.getChannelsForCategory(category.id)
        }
        val channels = favChannels + categoryChannels
        return channels.map { channelEntity ->
            val epgPrograms = epgRepository.getEPGProgramsForChannel(channelEntity.id)
            channelEntity.toDomain(
                epgPrograms = epgPrograms
            )
        }*/
        return channelDao.getFavouriteChannels().map { channelEntity ->
            /*val epgPrograms = epgRepository.getEPGProgramsForChannel(channelEntity.id)
            val epgProgramsNoDup = epgPrograms.distinctBy { it.title }
            println(epgProgramsNoDup.size.toString() + " " + epgPrograms.size)*/
            channelEntity.toDomain(
                epgPrograms = listOf()
            )
        }
    }

    suspend fun getCategories(): List<CategoryItem> {
        val categories = categoryDao.getAllCategories()
        return categories.map { categoryEntity ->
            categoryEntity.toDomain()
        }
    }

    suspend fun getChannelById(id: Long): ChannelItem? {
        val channelEntity = channelDao.getChannelById(id)
        // Fetch streamSources, relatedChannels, channelShortnames as needed
        if (channelEntity != null) {
            val streamSources = fetchStreamSourcesForChannel(channelEntity.id)
            val relatedChannels = fetchRelatedChannels(channelEntity.id)
            val channelShortnames = fetchChannelShortnamesForChannel(channelEntity.id)

            return channelEntity.toDomain(
                streamSources = streamSources,
                relatedChannels = relatedChannels,
                channelShortnames = channelShortnames,
            )
        }
        return null
    }

    suspend fun getCategoryById(id: Long): CategoryItem? {
        val categoryEntity = categoryDao.getCategoryById(id)
        return categoryEntity?.toDomain()
    }

    suspend fun getChannelCount(): Int {
        return channelDao.getChannelCount()
    }
}