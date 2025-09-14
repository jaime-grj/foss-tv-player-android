package com.gaarx.tvplayer.domain.model

import com.gaarx.tvplayer.data.database.entities.ApiCallEntity

class ApiCallItem(
    val id: Long,
    val url: String,
    val method: String?,
    val body: String?,
    val stringSearch: String?,
    val type: String?,
    val index: Int,
    val headers: List<ApiCallHeaderItem>?,
    val apiResponseKeys: List<ApiResponseKeyItem>
)

fun ApiCallEntity.toDomain(
    headers: List<ApiCallHeaderItem> = listOf(),
    apiResponseKeys: List<ApiResponseKeyItem> = listOf()
) = ApiCallItem(
    id = id,
    url = url,
    method = method,
    body = body,
    stringSearch = stringSearch,
    type = type,
    index = index,
    headers = headers,
    apiResponseKeys = apiResponseKeys
)