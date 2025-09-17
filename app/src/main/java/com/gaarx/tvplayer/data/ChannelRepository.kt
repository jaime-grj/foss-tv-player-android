package com.gaarx.tvplayer.data

import android.util.Log
import androidx.room.Transaction
import com.gaarx.tvplayer.data.dao.ApiCallDao
import com.gaarx.tvplayer.data.dao.ApiCallHeaderDao
import com.gaarx.tvplayer.data.dao.ApiResponseKeyDao
import com.gaarx.tvplayer.data.dao.CategoryDao
import com.gaarx.tvplayer.data.dao.ChannelDao
import com.gaarx.tvplayer.data.dao.DrmHeaderDao
import com.gaarx.tvplayer.data.dao.EPGDao
import com.gaarx.tvplayer.data.dao.ProxyDao
import com.gaarx.tvplayer.data.dao.StreamSourceDao
import com.gaarx.tvplayer.data.dao.StreamSourceHeaderDao
import com.gaarx.tvplayer.data.database.entities.ApiCallEntity
import com.gaarx.tvplayer.data.database.entities.ApiCallHeaderEntity
import com.gaarx.tvplayer.data.database.entities.ApiResponseKeyEntity
import com.gaarx.tvplayer.data.database.entities.CategoryEntity
import com.gaarx.tvplayer.data.database.entities.ChannelEntity
import com.gaarx.tvplayer.data.database.entities.ChannelShortnameEntity
import com.gaarx.tvplayer.data.database.entities.DrmHeaderEntity
import com.gaarx.tvplayer.data.database.entities.ProxyEntity
import com.gaarx.tvplayer.data.database.entities.StreamSourceEntity
import com.gaarx.tvplayer.data.database.entities.StreamSourceHeaderEntity
import com.gaarx.tvplayer.data.database.entities.toDatabase
import com.gaarx.tvplayer.data.services.DataService
import com.gaarx.tvplayer.domain.model.ApiCallHeaderItem
import com.gaarx.tvplayer.domain.model.ApiCallItem
import com.gaarx.tvplayer.domain.model.ApiResponseKeyItem
import com.gaarx.tvplayer.domain.model.CategoryItem
import com.gaarx.tvplayer.domain.model.ChannelItem
import com.gaarx.tvplayer.domain.model.ChannelShortnameItem
import com.gaarx.tvplayer.domain.model.ProxyItem
import com.gaarx.tvplayer.domain.model.DrmHeaderItem
import com.gaarx.tvplayer.domain.model.DrmTypeItem
import com.gaarx.tvplayer.domain.model.StreamSourceHeaderItem
import com.gaarx.tvplayer.domain.model.StreamSourceItem
import com.gaarx.tvplayer.domain.model.StreamSourceTypeItem
import com.gaarx.tvplayer.domain.model.toDomain
import com.gaarx.tvplayer.exceptions.ChannelNotFoundException
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
        val jsonStr = dataService.getJSONString()
        if (jsonStr.isEmpty()) return false

        val jsonObj = JSONObject(jsonStr)
        val data = jsonObj.getJSONObject("data")
        val categories = data.getJSONArray("categories")

        for (i in 0 until categories.length()) {
            val category = categories.getJSONObject(i)
            val categoryId = processCategory(category)

            val channels = category.getJSONArray("channels")
            for (j in 0 until channels.length()) {
                val channel = channels.getJSONObject(j)
                processChannel(channel, categoryId)
            }
        }

        processEpgSources(data)
        return true
    }

    private suspend fun processCategory(category: JSONObject): Long {
        val name = category.getString("name")
        val description = category.optString("description", null)

        val categoryEntity = CategoryEntity(name = name, description = description)
        val categoryId = categoryDao.insertCategory(categoryEntity)

        Log.d("Category", "$categoryId - $name")
        return categoryId
    }

    private suspend fun processChannel(channel: JSONObject, categoryId: Long) {
        val channelEntity = ChannelEntity(
            name = channel.getString("name"),
            description = channel.optString("description", null),
            language = channel.optString("language", null),
            logo = channel.optString("logo", null),
            country = channel.optString("country", null),
            region = channel.optString("region", null),
            subregion = channel.optString("subregion", null),
            indexFavourite = channel.optIntOrNull("indexFavourite"),
            indexGroup = channel.optIntOrNull("indexGroup"),
            categoryId = categoryId,
            parentId = null
        )

        val channelId = channelDao.insertChannel(channelEntity)

        processChannelShortnames(channel.optJSONArray("channelTags"), channelId)
        processStreamSources(channel.optJSONArray("streamSources"), channelId)
    }

    private fun JSONObject.optIntOrNull(key: String): Int? =
        runCatching { getInt(key) }.getOrNull()

    private suspend fun processChannelShortnames(tags: JSONArray?, channelId: Long) {
        tags ?: return
        for (i in 0 until tags.length()) {
            val tag = tags.getString(i)
            val entity = ChannelShortnameEntity(shortName = tag, channelId = channelId)
            channelDao.insertChannelShortname(entity)
        }
    }

    private suspend fun processStreamSources(sources: JSONArray?, channelId: Long) {
        sources ?: return
        for (i in 0 until sources.length()) {
            val source = sources.getJSONObject(i)
            processStreamSource(source, channelId)
        }
    }

    private suspend fun processStreamSource(source: JSONObject, channelId: Long) {
        val streamSourceType = StreamSourceTypeItem.fromInt(source.optInt("type")) ?: StreamSourceTypeItem.IPTV
        val drmType = DrmTypeItem.fromInt(source.optInt("drmType")) ?: DrmTypeItem.NONE
        val drmKeys = source.optJSONArray("drmKeys")?.join("\n")?.replace("\"", "") ?: ""

        val streamSourceEntity = StreamSourceEntity(
            name = source.getString("name"),
            url = source.getString("url"),
            channelId = channelId,
            index = source.getInt("index"),
            refreshRate = source.optString("refreshRate")?.toFloatOrNull(),
            streamSourceType = streamSourceType,
            drmType = drmType,
            drmKeys = drmKeys,
            pssh = source.optString("pssh", null),
            licenseUrl = source.optString("licenseUrl", null),
            forceUseBestVideoResolution = source.optBoolean("forceUseBestVideoResolution", false)
        )

        val streamSourceId = streamSourceDao.insertStreamSource(streamSourceEntity)

        processProxies(source.optJSONArray("proxies"), streamSourceId)
        processHeaders(source.optJSONArray("headers"), streamSourceId)
        processDrmHeaders(source.optJSONArray("drmHeaders"), streamSourceId)
        processApiCalls(source.optJSONArray("apiCalls"), streamSourceId)
    }

    private suspend fun processProxies(proxies: JSONArray?, streamSourceId: Long) {
        proxies ?: return
        for (i in 0 until proxies.length()) {
            val proxy = proxies.getJSONObject(i)
            val proxyEntity = ProxyEntity(
                hostname = proxy.getString("hostname"),
                port = proxy.getInt("port"),
                streamSourceId = streamSourceId
            )
            proxyDao.insertProxy(proxyEntity)
        }
    }

    private suspend fun processHeaders(headers: JSONArray?, streamSourceId: Long) {
        headers ?: return
        for (i in 0 until headers.length()) {
            val header = headers.getJSONObject(i)
            val headerEntity = StreamSourceHeaderEntity(
                key = header.getString("key"),
                value = header.getString("value"),
                streamSourceId = streamSourceId
            )
            streamSourceHeaderDao.insertStreamSourceHeader(headerEntity)
        }
    }

    private suspend fun processDrmHeaders(headers: JSONArray?, streamSourceId: Long) {
        headers ?: return
        for (i in 0 until headers.length()) {
            val header = headers.getJSONObject(i)
            val headerEntity = DrmHeaderEntity(
                key = header.getString("key"),
                value = header.getString("value"),
                streamSourceId = streamSourceId
            )
            drmHeaderDao.insertDrmHeader(headerEntity)
        }
    }

    private suspend fun processApiCalls(apiCalls: JSONArray?, streamSourceId: Long) {
        apiCalls ?: return
        for (i in 0 until apiCalls.length()) {
            val apiCall = apiCalls.getJSONObject(i)

            val apiCallEntity = ApiCallEntity(
                url = apiCall.getString("url"),
                index = apiCall.getInt("index"),
                method = apiCall.getString("method"),
                body = apiCall.optString("body", null),
                stringSearch = apiCall.optString("stringSearch", null),
                type = apiCall.getString("type"),
                streamSourceId = streamSourceId
            )
            val apiCallId = apiCallDao.insertApiCall(apiCallEntity)

            processApiCallResponseKeys(apiCall.optJSONArray("responseKeys"), apiCallId)
            processApiCallHeaders(apiCall.optJSONArray("headers"), apiCallId)
        }
    }

    private suspend fun processApiCallResponseKeys(responseKeys: JSONArray?, apiCallId: Long) {
        responseKeys ?: return
        for (i in 0 until responseKeys.length()) {
            val key = responseKeys.getJSONObject(i)
            val responseKeyEntity = ApiResponseKeyEntity(
                jsonPath = key.getString("jsonPath"),
                index = key.getInt("index"),
                storeKey = key.getString("storeKey"),
                apiCallId = apiCallId
            )
            apiResponseKeyDao.insertApiResponseKey(responseKeyEntity)
        }
    }

    private suspend fun processApiCallHeaders(headers: JSONArray?, apiCallId: Long) {
        headers ?: return
        for (i in 0 until headers.length()) {
            val header = headers.getJSONObject(i)
            val headerEntity = ApiCallHeaderEntity(
                key = header.getString("key"),
                value = header.getString("value"),
                apiCallId = apiCallId
            )
            apiCallHeaderDao.insertApiCallHeader(headerEntity)
        }
    }

    private suspend fun processEpgSources(data: JSONObject) {
        val epgSources = data.optJSONArray("epgSources") ?: return
        for (i in 0 until epgSources.length()) {
            val url = epgSources.getString(i)
            if (url.isNotEmpty()) settingsRepository.updateEpgSource(url)
        }
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