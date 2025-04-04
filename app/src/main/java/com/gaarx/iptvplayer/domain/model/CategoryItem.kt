package com.gaarx.iptvplayer.domain.model

import com.gaarx.iptvplayer.data.database.entities.CategoryEntity

data class CategoryItem (
    val id: Long,
    val name: String,
    val description: String?,
    val isSelected : Boolean = false,
    val channels: List<ChannelItem>
)

fun CategoryEntity.toDomain() = CategoryItem(
    id = id,
    name = name,
    description = description,
    channels = listOf()
)