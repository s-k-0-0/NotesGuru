package com.example.optimusnotes.roomdb

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

@Database(entities = [Note::class, Folder::class], version = 3) // Updated version
abstract class NotesDB : RoomDatabase() {
    abstract val noteDao: NoteDao
    abstract val folderDao: FolderDao

    companion object {
        @Volatile
        private var INSTANCE: NotesDB? = null

        // Migration from version 1 to version 2 (Adding timestamp column)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notes_table ADD COLUMN timestamp INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Migration from version 2 to version 3 (Adding folderName column + folders_table)
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notes_table ADD COLUMN folderName TEXT NOT NULL DEFAULT 'Uncategorized'")
            }
        }

        fun getInstance(context: Context): NotesDB {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NotesDB::class.java,
                    "notes_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    // Include all migrations here
                    .build().also { INSTANCE = it }
            }
        }
    }
}
