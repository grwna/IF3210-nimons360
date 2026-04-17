package com.example.nimons360_far.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PinnedFamilyDAO {
    @Query("SELECT * FROM pinned_families")
    fun getAllPinned(): Flow<List<PinnedFamily>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun pinFamily(family: PinnedFamily)

    @Query("DELETE FROM pinned_families WHERE (familyId = :id)")
    suspend fun unpinFamily(id: Int)
}