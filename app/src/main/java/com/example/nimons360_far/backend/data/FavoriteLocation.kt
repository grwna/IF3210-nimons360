package com.example.nimons360_far.backend.data


//  Plain data class untuk lokasi yang ditandai oleh pengguna.
//  Penyimpanan dikelola oleh LocationDbHelper

data class FavoriteLocation(
    val id: String,
    val userId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val description: String = ""
)