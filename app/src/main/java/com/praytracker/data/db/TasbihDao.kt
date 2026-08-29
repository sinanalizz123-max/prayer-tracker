package com.praytracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TasbihDao {

    @Query("SELECT * FROM tasbih ORDER BY orderIndex ASC, id ASC")
    fun observeAll(): Flow<List<TasbihEntity>>

    @Query("SELECT * FROM tasbih ORDER BY orderIndex ASC, id ASC")
    suspend fun getAll(): List<TasbihEntity>

    @Query("SELECT * FROM tasbih WHERE id = :id")
    suspend fun get(id: Long): TasbihEntity?

    @Query("SELECT IFNULL(MAX(id), 0) FROM tasbih")
    suspend fun maxId(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TasbihEntity): Long

    @Query("DELETE FROM tasbih WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM tasbih")
    suspend fun clearAll()
}