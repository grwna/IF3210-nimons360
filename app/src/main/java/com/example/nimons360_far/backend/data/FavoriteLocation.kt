package com.example.nimons360_far.backend.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_locations")
data class FavoriteLocation(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)