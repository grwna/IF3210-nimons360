package com.example.nimons360_far.backend.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// DAO (Data Access Object) File
@Dao
interface PinnedFamilyDao {
    // Get All Pinned Families - Query To Database
    @Query("SELECT * FROM pinned_families")
    fun getAllPinned(): Flow<List<PinnedFamily>>

    // Pin Family - Insert / Replace Pin Family To Database
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun pinFamily(family: PinnedFamily)

    // Unpin Family - Delete From Database
    @Query("DELETE FROM pinned_families WHERE (familyId = :id)")
    suspend fun unpinFamily(id: Int)
}