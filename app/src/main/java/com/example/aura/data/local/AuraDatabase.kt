package com.example.aura.data.local

/**
 * ARCHIVO: AuraDatabase.kt
 * FUNCIÓN: Configuración de Room: entidades, versión, DAO y singleton de la base de datos "aura_music.db".
 * NAVEGACIÓN: AuraViewModel pide la instancia con AuraDatabase.getInstance(application).
 */

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AuraDatabase : RoomDatabase() {
    // DAO de canciones (tabla songs).
    abstract fun songDao(): SongDao
    // DAO de listas y su relación con canciones.
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: AuraDatabase? = null

        // Devuelve (o crea) la única instancia de la base de datos de la app.
        fun getInstance(context: Context): AuraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AuraDatabase::class.java,
                    "aura_music.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
