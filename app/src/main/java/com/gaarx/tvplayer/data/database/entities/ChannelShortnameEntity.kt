package com.gaarx.tvplayer.data.database.entities
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.gaarx.tvplayer.domain.model.ChannelShortnameItem

@Entity(
    tableName = "channel_shortname",
    foreignKeys = [ForeignKey(
        entity = ChannelEntity::class,
        parentColumns = ["id"],
        childColumns = ["channel_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
class ChannelShortnameEntity (
    @ColumnInfo(name = "id")
    @PrimaryKey(autoGenerate = true)
    val id : Long = 0,

    @ColumnInfo(name = "shortname")
    val shortName : String,

    @ColumnInfo(name = "channel_id")
    val channelId: Long,

)

fun ChannelShortnameItem.toDatabase(channelId: Long) = ChannelShortnameEntity(
    shortName = name,
    channelId = channelId
)