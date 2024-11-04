package com.gaarj.iptvplayer.domain.model

import com.gaarj.iptvplayer.data.database.entities.ProxyEntity

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