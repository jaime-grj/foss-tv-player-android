package com.gaarx.tvplayer.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.gaarx.tvplayer.domain.model.ChannelItem

@Entity(
    tableName = "channel",
    foreignKeys = [ForeignKey(
        entity = CategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["category_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ChannelEntity (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo(name = "logo")
    val logo: String?,

    @ColumnInfo(name = "language")
    val language: String?,

    @ColumnInfo(name = "country")
    val country: String?,

    @ColumnInfo(name = "region")
    val region: String?,

    @ColumnInfo(name = "subregion")
    val subregion: String?,

    @ColumnInfo(name = "index_favourite")
    val indexFavourite: Int?,

    @ColumnInfo(name = "index_group")
    val indexGroup: Int?,

    @ColumnInfo(name = "category_id")
    val categoryId: Long?,

    @ColumnInfo(name = "parent_id")
    val parentId: Long?,
)

fun ChannelItem.toDatabase(categoryId: Long? = null, parentId: Long? = null) = ChannelEntity(
    name = name,
    description = description,
    language = language,
    logo = logo,
    country = country,
    region = region,
    subregion = subregion,
    indexFavourite = indexFavourite,
    indexGroup = indexGroup,
    categoryId = categoryId,
    parentId = parentId
)
