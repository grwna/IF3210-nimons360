package com.example.nimons360_far.backend.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper


// SQLite helper untuk tabel favorite_locations.

class LocationDbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "favorite_locations.db"
        private const val DATABASE_VERSION = 1

        // Nama tabel & kolom
        const val TABLE_NAME = "favorite_locations"
        const val COL_ID = "id"
        const val COL_USER_ID = "userId"
        const val COL_NAME = "name"
        const val COL_LATITUDE = "latitude"
        const val COL_LONGITUDE = "longitude"
        const val COL_DESCRIPTION = "description"

        @Volatile
        private var INSTANCE: LocationDbHelper? = null

        fun getInstance(context: Context): LocationDbHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocationDbHelper(context).also { INSTANCE = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_NAME (
                $COL_ID          TEXT PRIMARY KEY,
                $COL_USER_ID     TEXT NOT NULL,
                $COL_NAME        TEXT NOT NULL,
                $COL_LATITUDE    REAL NOT NULL,
                $COL_LONGITUDE   REAL NOT NULL,
                $COL_DESCRIPTION TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun getFavoritesByUser(userId: String): List<FavoriteLocation> {
        val result = mutableListOf<FavoriteLocation>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_NAME WHERE $COL_USER_ID = ?",
            arrayOf(userId)
        )
        cursor.use {
            while (it.moveToNext()) {
                result.add(
                    FavoriteLocation(
                        id          = it.getString(it.getColumnIndexOrThrow(COL_ID)),
                        userId      = it.getString(it.getColumnIndexOrThrow(COL_USER_ID)),
                        name        = it.getString(it.getColumnIndexOrThrow(COL_NAME)),
                        latitude    = it.getDouble(it.getColumnIndexOrThrow(COL_LATITUDE)),
                        longitude   = it.getDouble(it.getColumnIndexOrThrow(COL_LONGITUDE)),
                        description = it.getString(it.getColumnIndexOrThrow(COL_DESCRIPTION))
                    )
                )
            }
        }
        return result
    }

    fun insertFavorite(location: FavoriteLocation) {
        val values = buildContentValues(location)
        writableDatabase.insertWithOnConflict(
            TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun updateFavorite(location: FavoriteLocation) {
        val values = buildContentValues(location)
        writableDatabase.update(
            TABLE_NAME, values, "$COL_ID = ?", arrayOf(location.id)
        )
    }

    fun deleteFavorite(id: String) {
        writableDatabase.delete(TABLE_NAME, "$COL_ID = ?", arrayOf(id))
    }

    private fun buildContentValues(location: FavoriteLocation) = ContentValues().apply {
        put(COL_ID,          location.id)
        put(COL_USER_ID,     location.userId)
        put(COL_NAME,        location.name)
        put(COL_LATITUDE,    location.latitude)
        put(COL_LONGITUDE,   location.longitude)
        put(COL_DESCRIPTION, location.description)
    }
}
