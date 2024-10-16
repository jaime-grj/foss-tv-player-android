package com.gaarj.iptvplayer.domain.model

import com.gaarj.iptvplayer.data.database.entities.StreamSourceEntity


data class StreamSourceItem (
    val id: Long,
    val name: String?,
    val index: Int,
    val url: String,
    val streamSourceType: StreamSourceTypeItem? = null,
    val headers: List<StreamSourceHeaderItem>? = null,
    val apiCalls: List<ApiCallItem>? = null,
    val refreshRate: Float = 0f,
    var isSelected: Boolean = false
)

fun StreamSourceEntity.toDomain(
    streamSourceType: StreamSourceTypeItem? = null,
    headers: List<StreamSourceHeaderItem> = listOf(),
    apiCalls: List<ApiCallItem> = listOf()
) = StreamSourceItem(
    id = id,
    name = name,
    index = index,
    url = url,
    refreshRate = refreshRate,
    streamSourceType = streamSourceType,
    headers = headers,
    apiCalls = apiCalls
)