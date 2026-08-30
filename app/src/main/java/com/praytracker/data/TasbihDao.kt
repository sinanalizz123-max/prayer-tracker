package com.praytracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TasbihDao {
    @Query("SELECT * FROM tasbih_items ORDER BY id ASC")
    fun getAllTasbihItems(): Flow<List<TasbihItem>>

    @Query("SELECT * FROM tasbih_items WHERE id = :id LIMIT 1")
    suspend fun getTasbihItemById(id: Int): TasbihItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasbihItem(item: TasbihItem): Long

    @Update
    suspend fun updateTasbihItem(item: TasbihItem)

    @Delete
    suspend fun deleteTasbihItem(item: TasbihItem)
}
