package com.gaarx.tvplayer.domain.model

import com.gaarx.tvplayer.data.database.entities.StreamSourceEntity


data class StreamSourceItem (
    val id: Long,
    val name: String?,
    val index: Int,
    val url: String,
    val streamSourceType: StreamSourceTypeItem,
    val headers: List<StreamSourceHeaderItem>? = null,
    val apiCalls: List<ApiCallItem>? = null,
    val proxies: List<ProxyItem>? = null,
    val refreshRate: Float? = null,
    var isSelected: Boolean = false,
    val drmType: DrmTypeItem,
    val drmKeys: String? = null,
    val drmHeaders: List<DrmHeaderItem>? = null,
    val pssh: String? = null,
    val licenseUrl : String? = null,
    val useUnofficialDrmLicenseMethod: Boolean = false,
    val forceUseBestVideoResolution: Boolean = false
)

fun StreamSourceEntity.toDomain(
    headers: List<StreamSourceHeaderItem> = listOf(),
    apiCalls: List<ApiCallItem> = listOf(),
    proxies: List<ProxyItem> = listOf(),
    drmHeaders: List<DrmHeaderItem> = listOf(),
) = StreamSourceItem(
    id = id,
    name = name,
    index = index,
    url = url,
    refreshRate = refreshRate,
    streamSourceType = streamSourceType,
    headers = headers,
    apiCalls = apiCalls,
    proxies = proxies,
    drmType = drmType,
    drmKeys = drmKeys,
    pssh = pssh,
    licenseUrl = licenseUrl,
    useUnofficialDrmLicenseMethod = useUnofficialDrmLicenseMethod,
    forceUseBestVideoResolution = forceUseBestVideoResolution,
    drmHeaders = drmHeaders
)