package com.example.nimons360_far.backend.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationPhotoDao {
    @Query("SELECT * FROM location_photos WHERE locationId = :locationId")
    fun getPhotosByLocation(locationId: String): Flow<List<LocationPhoto>>

    @Query("SELECT * FROM location_photos WHERE locationId = :locationId")
    suspend fun getPhotosByLocationSync(locationId: String): List<LocationPhoto>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: LocationPhoto)

    @Query("DELETE FROM location_photos WHERE locationId = :locationId")
    suspend fun deletePhotosByLocation(locationId: String)

    @Query("DELETE FROM location_photos WHERE id = :id")
    suspend fun deletePhoto(id: String)
}
