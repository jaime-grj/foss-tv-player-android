package com.gaarx.iptvplayer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gaarx.iptvplayer.data.database.entities.CategoryEntity

@Dao
interface CategoryDao {

    @Query("SELECT * FROM category")
    fun getAll(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity) : Long

    @Query("SELECT * FROM category")
    suspend fun getAllCategories() : List<CategoryEntity>

    @Query("DELETE FROM category")
    suspend fun deleteAll()

    @Query("SELECT * FROM category WHERE id = :id")
    suspend fun getCategoryById(id: Long) : CategoryEntity?
}