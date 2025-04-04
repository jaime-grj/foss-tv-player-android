package com.gaarx.iptvplayer.domain.model

import com.gaarx.iptvplayer.data.database.entities.ProxyEntity

data class ProxyItem (
    val id: Long,
    val hostname: String,
    val port: Int,
)

fun ProxyEntity.toDomain() = ProxyItem(
    id = id,
    hostname = hostname,
    port = port
)