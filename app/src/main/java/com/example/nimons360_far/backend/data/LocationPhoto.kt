package com.example.nimons360_far.backend.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_photos")
data class LocationPhoto(
    @PrimaryKey val id: String,
    val locationId: String,
    val filePath: String
)