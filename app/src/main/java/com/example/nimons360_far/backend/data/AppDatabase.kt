package com.example.nimons360_far.backend.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Room DB Setup - PinnedFamily & FavoriteLocation Entity
@Database(entities = [PinnedFamily::class, FavoriteLocation::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    // Provides DAO Access
    abstract fun pinnedFamilyDao(): PinnedFamilyDao
    abstract fun favoriteLocationDao(): FavoriteLocationDao

    // Singleton Pattern : Ensures Only 1 Database Instance
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Return / Create Instance (Synchronized)
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