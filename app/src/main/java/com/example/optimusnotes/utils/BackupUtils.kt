package com.example.optimusnotes.utils

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.optimusnotes.roomdb.NotesDB
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SimpleSQLiteQuery // Import SimpleSQLiteQuery


object BackupUtils {
    private const val DATABASE_NAME = "notes_db"
    private const val BACKUP_FOLDER_BASE_NAME = "OptimusNotesFullBackups"
    private const val PREFERENCES_NAME = "optimus_notes_preferences"

    /**
     * Checks for required permissions
     */
    fun hasRequiredPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Creates a full backup of the app's database and SharedPreferences.
     */
    fun createFullBackup(context: Context): Result<File> {
        if (!hasRequiredPermissions(context)) {
            return Result.failure(SecurityException("Storage permission not granted"))
        }

        val dbPath = context.getDatabasePath(DATABASE_NAME).absolutePath
        val prefs: SharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

        Log.d("BackupUtils", "Database file path being backed up: $dbPath")
        Log.d("BackupUtils", "Backing up SharedPreferences data.")

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val backupDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "$BACKUP_FOLDER_BASE_NAME/$timeStamp"
        ).apply {
            if (!mkdirs()) {
                Log.e("BackupUtils", "Failed to create backup directory: ${absolutePath}")
                return Result.failure(IOException("Failed to create backup directory: ${absolutePath}"))
            }
        }
        val backupDbFile = File(backupDir, "backup.db")
        val backupPrefsFile = File(backupDir, "backup_prefs.xml") // We will still create this file, but write prefs data differently

        Log.d("BackupUtils", "Backup database file path: ${backupDbFile.absolutePath}")
        Log.d("BackupUtils", "Backup SharedPreferences file path: ${backupPrefsFile.absolutePath}")

        val initialNoteCount = getNotesCount(context) // Count notes before backup
        Log.d("BackupUtils", "Number of notes before backup: $initialNoteCount")


        return try {
            // Close Room database
            Log.d("BackupUtils", "Attempting to close Room database instance...")
            try {
                NotesDB.getInstance(context)
                Log.d("BackupUtils", "Room database instance closed successfully.")
            } catch (dbCloseException: Exception) {
                Log.e("BackupUtils", "Error closing Room database", dbCloseException)
            }

            // Backup Database
            Log.d("BackupUtils", "Starting database file copy from: $dbPath to: ${backupDbFile.absolutePath}")
            FileInputStream(File(dbPath)).use { inputDb -> // Explicitly create File object
                FileOutputStream(backupDbFile).use { outputDb ->
                    inputDb.copyTo(outputDb)
                    outputDb.flush()
                    outputDb.fd.sync()
                }
            }
            Log.d("BackupUtils", "Database file copy completed.")

            // Backup SharedPreferences Data
            Log.d("BackupUtils", "Starting SharedPreferences data backup to: ${backupPrefsFile.absolutePath}")
            FileOutputStream(backupPrefsFile).use { outputStream ->
                prefs.all.forEach { (key, value) ->
                    val line = "$key=${value.toString()}\n" // Simple key=value format
                    outputStream.write(line.toByteArray())
                }
                outputStream.flush()
                outputStream.fd.sync()
            }
            Log.d("BackupUtils", "SharedPreferences data backup completed.")


            // Check database file sizes after backup
            val originalDbSize = File(dbPath).length()
            val backupDbFileSize = backupDbFile.length()
            Log.d("BackupUtils", "Original DB file size: $originalDbSize bytes")
            Log.d("BackupUtils", "Backup DB file size: $backupDbFileSize bytes")
            if (backupDbFileSize == 0L && originalDbSize != 0L) { // Check against original size
                Log.w("BackupUtils", "Backup DB file size is 0 bytes, but original is not! Possible DB copy problem.")
            }

            val noteCountAfterBackup = getNotesCount(context) // Count notes after backup file is created
            Log.d("BackupUtils", "Number of notes after backup file creation: $noteCountAfterBackup") // Log count after backup


            println("✅ Full backup successful: ${backupDir.absolutePath}")
            Result.success(backupDir)
        } catch (e: IOException) {
            Log.e("BackupUtils", "Full backup failed due to IOException", e)
            println("❌ Full backup failed: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("BackupUtils", "Full backup failed due to Exception", e)
            println("❌ Full backup failed: ${e.message}")
            Result.failure(e)
        }
    }


    /**
     * Restores a full backup of the app's database and SharedPreferences.
     */
    fun restoreFullBackup(context: Context, backupFolderPath: String): Result<Unit> {
        if (!hasRequiredPermissions(context)) {
            return Result.failure(SecurityException("Storage permission not granted"))
        }

        val dbPath = context.getDatabasePath(DATABASE_NAME).absolutePath
        val prefs: SharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val backupDir = File(backupFolderPath)
        val backupDbFile = File(backupDir, "backup.db")
        val backupPrefsFile = File(backupDir, "backup_prefs.xml")

        if (!backupDbFile.exists()) {
            return Result.failure(IOException("Backup database file does not exist: ${backupDbFile.absolutePath}"))
        }
        if (!backupPrefsFile.exists()) {
            Log.w("BackupUtils", "Backup SharedPreferences file does not exist, settings restore may be incomplete: ${backupPrefsFile.absolutePath}")
        }

        val noteCountBeforeRestore = getNotesCount(context) // Count notes before restore
        Log.d("BackupUtils", "Number of notes before restore: $noteCountBeforeRestore")


        return try {
            // Restore Database
            Log.d("BackupUtils", "Starting database restore from: ${backupDbFile.absolutePath} to: ${dbPath}")
            // Close any open database connections - important before restore
            context.getDatabasePath(DATABASE_NAME).let { dbFile ->
                if (dbFile.exists()) {
                    SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE).close()
                }
            }
            val tempDbFile = File("$dbPath.tmp")
            FileInputStream(backupDbFile).use { inputDb ->
                FileOutputStream(tempDbFile).use { outputDb ->
                    inputDb.copyTo(outputDb)
                }
            }
            if (!tempDbFile.renameTo(File(dbPath))) {
                throw IOException("Failed to rename temporary database file during restore")
            }
            Log.d("BackupUtils", "Database restore completed.")


            // Restore SharedPreferences Data
            if (backupPrefsFile.exists()) {
                Log.d("BackupUtils", "Starting SharedPreferences data restore from: ${backupPrefsFile.absolutePath}")
                val prefsEditor = prefs.edit()
                prefsEditor.clear() // Clear existing preferences before restoring

                FileInputStream(backupPrefsFile).bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        val parts = line.split("=", limit = 2) // Split into key and value
                        if (parts.size == 2) {
                            val key = parts[0]
                            val value = parts[1]
                            // Determine value type and restore accordingly (String, Boolean, Int, Long, Float, StringSet)
                            when {
                                value == "true" || value == "false" -> prefsEditor.putBoolean(key, value.toBoolean())
                                value.toIntOrNull() != null -> prefsEditor.putInt(key, value.toInt())
                                value.toLongOrNull() != null -> prefsEditor.putLong(key, value.toLong())
                                value.toFloatOrNull() != null -> prefsEditor.putFloat(key, value.toFloat())
                                else -> prefsEditor.putString(key, value) // Default to String
                            }
                        } else {
                            Log.w("BackupUtils", "Skipping invalid prefs line: $line")
                        }
                    }
                }
                prefsEditor.apply() // Apply all changes at once
                Log.d("BackupUtils", "SharedPreferences data restore completed.")
            } else {
                Log.w("BackupUtils", "No SharedPreferences backup file found, skipping settings restore.")
            }

            val noteCountAfterRestore = getNotesCount(context) // Count notes after restore
            Log.d("BackupUtils", "Number of notes after restore: $noteCountAfterRestore") // Log count after restore


            println("✅ Full restore successful from: ${backupFolderPath}")
            Result.success(Unit)
        } catch (e: IOException) {
            Log.e("BackupUtils", "Full restore failed due to IOException", e)
            println("❌ Full restore failed: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("BackupUtils", "Full restore failed due to Exception", e)
            println("❌ Full backup failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Retrieves the backup directory.
     */
    fun getBackupDirectory(context: Context): File? {
        if (!hasRequiredPermissions(context)) {
            return null
        }
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            BACKUP_FOLDER_BASE_NAME
        ).takeIf { it.exists() }
    }

    /**
     * Safely execute SELECT query.
     */
    fun executeSelectQuery(
        db: SupportSQLiteDatabase, // Parameter is SupportSQLiteDatabase
        query: String,
        selectionArgs: Array<String>? = null
    ): Result<Cursor> {
        return try {
            // Use SimpleSQLiteQuery and SupportSQLiteDatabase.rawQuery
            val simpleQuery = SimpleSQLiteQuery(query, selectionArgs)
            Result.success(db.query(simpleQuery)) // Use db.query with SimpleSQLiteQuery
        } catch (e: SQLiteException) {
            Log.e("BackupUtils", "SQL Error during query: $query", e)
            Result.failure(e)
        }
    }

    private fun getNotesCount(context: Context): Int {
        // Corrected line: Access readableDatabase via openHelper
        val db = NotesDB.getInstance(context).openHelper.readableDatabase
        val cursor: Cursor? = executeSelectQuery(db, "SELECT COUNT(*) FROM notes_table").getOrNull()
        var count = 0
        cursor?.use {
            if (it.moveToFirst()) {
                count = it.getInt(0)
            }
        }
        db.close() // Close the readable database
        return count
    }
}