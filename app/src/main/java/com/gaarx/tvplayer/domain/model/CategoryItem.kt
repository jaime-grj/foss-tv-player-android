package com.gaarx.tvplayer.domain.model

import com.gaarx.tvplayer.data.database.entities.CategoryEntity

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