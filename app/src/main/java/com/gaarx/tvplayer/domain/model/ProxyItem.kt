package com.gaarx.tvplayer.domain.model

import com.gaarx.tvplayer.data.database.entities.ProxyEntity

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