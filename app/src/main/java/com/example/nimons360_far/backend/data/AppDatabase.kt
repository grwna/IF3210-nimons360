package com.example.nimons360_far.backend.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Room database, mengelola LocationPhoto dan PinnedFamily.
@Database(
    entities = [PinnedFamily::class, LocationPhoto::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // Provides DAO Access
    abstract fun pinnedFamilyDao(): PinnedFamilyDao
    abstract fun locationPhotoDao(): LocationPhotoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nimons360_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}