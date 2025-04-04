package com.gaarx.iptvplayer.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.gaarx.iptvplayer.domain.model.DrmTypeItem
import com.gaarx.iptvplayer.domain.model.StreamSourceItem
import com.gaarx.iptvplayer.domain.model.StreamSourceTypeItem

@Entity(
    tableName = "stream_source",
    foreignKeys = [
        ForeignKey(
            entity = ChannelEntity::class,
            parentColumns = ["id"],
            childColumns = ["channel_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
class StreamSourceEntity (

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String?,

    @ColumnInfo(name = "index")
    val index: Int,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "refresh_rate")
    val refreshRate: Float?,

    @ColumnInfo(name = "channel_id")
    val channelId : Long,

    @ColumnInfo(name = "stream_source_type")
    val streamSourceType : StreamSourceTypeItem,

    @ColumnInfo(name = "drm_type")
    val drmType : DrmTypeItem,

    @ColumnInfo(name = "drm_keys")
    val drmKeys : String?,

    @ColumnInfo(name = "pssh")
    val pssh : String?,

    @ColumnInfo(name = "license_url")
    val licenseUrl : String?,

    @ColumnInfo(name = "use_unofficial_drm_method")
    val useUnofficialDrmLicenseMethod : Boolean,

    @ColumnInfo(name = "force_use_best_video_resolution")
    val forceUseBestVideoResolution: Boolean

)

fun StreamSourceItem.toDatabase(channelId: Long) = StreamSourceEntity(
    name = name,
    index = index,
    url = url,
    channelId = channelId,
    refreshRate = refreshRate,
    streamSourceType = streamSourceType,
    drmType = drmType,
    drmKeys = drmKeys,
    pssh = pssh,
    licenseUrl = licenseUrl,
    useUnofficialDrmLicenseMethod = useUnofficialDrmLicenseMethod,
    forceUseBestVideoResolution = forceUseBestVideoResolution
)
