package com.gaarx.iptvplayer.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gaarx.iptvplayer.domain.model.CategoryItem

@Entity(
    tableName = "category"
)
data class CategoryEntity (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String?
)

fun CategoryItem.toDatabase() = CategoryEntity(
    name = name,
    description = description
)