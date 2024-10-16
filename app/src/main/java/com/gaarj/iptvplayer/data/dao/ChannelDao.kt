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

    // This is an optional helper method to insert a list of channels
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity) : Long

    // If you want to insert multiple channels at once
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)



    // Inserting stream sources associated with a channel
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreamSources(streamSources: List<StreamSourceEntity>)

    // Inserting channel shortnames associated with a channel
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannelShortnames(shortnames: List<ChannelShortnameEntity>)

    @Query("SELECT * FROM channel_shortname WHERE channel_id = :channelId")
    suspend fun getChannelShortnamesForChannel(channelId: Long): List<ChannelShortnameEntity>

    @Query("DELETE FROM channel")
    suspend fun deleteAll()

}
