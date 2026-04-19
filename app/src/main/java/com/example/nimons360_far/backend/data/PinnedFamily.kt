package com.example.nimons360_far.backend.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Entity File - Define Database Table Pinned Families
@Entity(tableName = "pinned_families")
data class PinnedFamily(
    @PrimaryKey val familyId: Int
)