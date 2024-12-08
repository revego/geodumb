package com.code4you.geodumb.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.code4you.geodumb.database.dao.ImageDao

@Database(entities = [Image::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    // Funzione astratta che fornisce l'accesso al DAO
    abstract fun imageDao(): ImageDao

    companion object {
        // Il database è un singleton, per evitare la creazione di più istanze
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"  // Nome del database
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
