package com.example.nimons360_far.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pinned_families")
data class PinnedFamily(
    @PrimaryKey val familyId: Int
)