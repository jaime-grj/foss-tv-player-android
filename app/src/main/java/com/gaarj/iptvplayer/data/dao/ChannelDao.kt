package com.gaarj.iptvplayer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gaarj.iptvplayer.data.database.entities.ChannelEntity
import com.gaarj.iptvplayer.data.database.entities.ChannelShortnameEntity
import com.gaarj.iptvplayer.data.database.entities.StreamSourceEntity

@Dao
interface ChannelDao {

    @Query("SELECT * FROM channel WHERE index_favourite IS NOT NULL ORDER BY index_favourite ASC")
    suspend fun getFavouriteChannels(): List<ChannelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity) : Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreamSources(streamSources: List<StreamSourceEntity>)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannelShortnames(shortnames: List<ChannelShortnameEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannelShortname(shortname: ChannelShortnameEntity) : Long

    @Query("SELECT * FROM channel_shortname WHERE channel_id = :channelId")
    suspend fun getChannelShortnamesForChannel(channelId: Long): List<ChannelShortnameEntity>

    @Query("DELETE FROM channel")
    suspend fun deleteAll()

    @Query("SELECT * FROM channel WHERE category_id = :categoryId ORDER BY index_group ASC")
    suspend fun getChannelsForCategory(categoryId: Long) : List<ChannelEntity>

    @Query("""
        SELECT * FROM channel 
        WHERE index_favourite <= :favouriteId
        ORDER BY index_group DESC 
        LIMIT 1
    """)
    suspend fun getPreviousChannelByFavouriteId(favouriteId: Int) : ChannelEntity

    @Query("""
        SELECT * FROM channel 
        WHERE index_group <= :groupId AND category_id = :categoryId
        ORDER BY index_group DESC 
        LIMIT 1
    """)
    suspend fun getPreviousChannelByGroupId(categoryId: Long, groupId: Int) : ChannelEntity?

    @Query("SELECT index_favourite FROM channel WHERE index_favourite IS NOT NULL ORDER BY index_favourite DESC LIMIT 1")
    suspend fun getLastFavouriteChannelId() : Int

    @Query("SELECT index_group FROM channel WHERE category_id = :categoryId ORDER BY index_group DESC LIMIT 1")
    suspend fun getLastGroupChannelId(categoryId: Long) : Int

    @Query("""
        SELECT * FROM channel 
        WHERE index_favourite >= :favouriteId
        ORDER BY index_group ASC 
        LIMIT 1
    """)
    suspend fun getNextChannelByFavouriteId(favouriteId: Int) : ChannelEntity

    @Query("""
        SELECT * FROM channel 
        WHERE index_group >= :groupId AND category_id = :categoryId
        ORDER BY index_group ASC 
        LIMIT 1
    """)
    suspend fun getNextChannelByGroupId(categoryId: Long, groupId: Int) : ChannelEntity?

    @Query("SELECT index_favourite FROM channel WHERE index_favourite IS NOT NULL ORDER BY index_favourite ASC LIMIT 1")
    suspend fun getFirstFavouriteChannelId() : Int

    @Query("SELECT name FROM channel WHERE index_favourite IS NOT NULL ORDER BY index_favourite ASC LIMIT 1")
    suspend fun getFirstNFavouriteChannelId() : String

    @Query("SELECT index_group FROM channel WHERE category_id = :categoryId ORDER BY index_group ASC LIMIT 1")
    suspend fun getFirstGroupChannelId(categoryId: Long) : Int

    @Query("SELECT COUNT(*) FROM channel WHERE category_id = :groupId")
    suspend fun getChannelCountByGroup(groupId: Long): Int

    @Query("SELECT COUNT(*) FROM channel WHERE index_favourite IS NOT NULL")
    suspend fun getFavouriteChannelCount(): Int

    @Query("""
        SELECT index_favourite FROM channel 
        WHERE index_favourite > :favouriteId
        ORDER BY index_favourite ASC 
        LIMIT 1
    """)
    suspend fun getNextChannelFavouriteIndex(favouriteId: Int) : Int

    @Query("""
        SELECT index_group FROM channel 
        WHERE index_group > :groupId
        AND category_id = :categoryId
        ORDER BY index_group ASC 
        LIMIT 1
    """)
    suspend fun getNextChannelGroupIndex(categoryId: Long, groupId: Int) : Int

    @Query("""
        SELECT index_favourite FROM channel 
        WHERE index_favourite < :favouriteId
        ORDER BY index_favourite DESC 
        LIMIT 1
    """)
    suspend fun getPreviousChannelFavouriteIndex(favouriteId: Int) : Int

    @Query("""
        SELECT index_group FROM channel 
        WHERE index_group < :groupId
        AND category_id = :categoryId
        ORDER BY index_group DESC 
        LIMIT 1
    """)
    suspend fun getPreviousChannelGroupIndex(categoryId: Long, groupId: Int) : Int

    @Query("SELECT * FROM channel WHERE id = :id")
    suspend fun getChannelById(id: Long): ChannelEntity?

    @Query("SELECT COUNT(*) FROM channel")
    suspend fun getChannelCount(): Int

    @Query("""
        SELECT * FROM channel 
        WHERE index_favourite = :favouriteId
        ORDER BY index_group ASC 
        LIMIT 1
    """)
    suspend fun getChannelByFavouriteId(favouriteId: Int) : ChannelEntity?

    @Query("""
        SELECT * FROM channel 
        WHERE index_group = :groupId AND category_id = :categoryId
        ORDER BY index_group ASC 
        LIMIT 1
    """)
    suspend fun getChannelByGroupId(categoryId: Long, groupId: Int) : ChannelEntity?

}
