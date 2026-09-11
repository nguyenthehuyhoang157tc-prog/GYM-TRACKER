package com.hoctin.gymtracker.data.catalog

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data access for the Exercise Catalog. Reads return the whole (small, ~150-max) library; search and
 * filtering are done in-memory by [CatalogSearch] so the logic is pure, deterministic, and unit-testable
 * without Room.
 */
@Dao
interface CatalogDao {
    @Query("SELECT COUNT(*) FROM catalog_exercises")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CatalogExerciseEntity>)

    @Query("DELETE FROM catalog_exercises")
    suspend fun clearAll()

    @Query("SELECT * FROM catalog_exercises ORDER BY name")
    suspend fun getAll(): List<CatalogExerciseEntity>

    @Query("SELECT * FROM catalog_exercises ORDER BY name")
    fun observeAll(): Flow<List<CatalogExerciseEntity>>
}
