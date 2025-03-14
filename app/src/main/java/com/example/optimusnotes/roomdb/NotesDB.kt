package com.example.optimusnotes.roomdb

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

@Database(entities = [Note::class, Folder::class], version = 6) // **Version is now 6**
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

        // Migration from version 3 to version 4 (Empty migration if no schema change)
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // No schema changes between version 3 and 4
            }
        }

        // **NEW MIGRATION: Version 4 to 5 (Adding folderId column)**
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notes_table ADD COLUMN folderId INTEGER") // Add folderId column
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notes_table ADD COLUMN drawingData TEXT") // Add drawingData column, TEXT type
            }
        }


        fun getInstance(context: Context): NotesDB {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NotesDB::class.java,
                    "notes_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigrationOnDowngrade() // 🔴 Force Room to accept restored DB
                    .allowMainThreadQueries() // 🔴 TEMPORARY for debugging
                    .build().also { INSTANCE = it }
            }
        }

        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null // Ensure Room reloads the DB next time
        }
    }
}