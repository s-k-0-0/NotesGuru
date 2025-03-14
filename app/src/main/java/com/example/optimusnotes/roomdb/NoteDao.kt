package com.example.optimusnotes.roomdb

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update


@Dao
interface NoteDao {
    @Insert
    suspend fun insert(note: Note)

    @Query("SELECT * FROM notes_table WHERE id = :id LIMIT 1")
    fun getNoteByIdLive(id: Int): LiveData<Note?>

    @Query("SELECT * FROM notes_table WHERE folderName = :folder")
    fun getNotesByFolder(folder: String): LiveData<List<Note>>

    @Query("SELECT DISTINCT folderName FROM notes_table")
    fun getAllFolders(): LiveData<List<String>>

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("SELECT * FROM notes_table WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getNotesBetweenTimestamps(startTime: Long, endTime: Long): List<Note>

    @Query("SELECT * FROM notes_table")
    fun getAllNotesDebug(): List<Note>





}




@Dao
interface FolderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder)

    @Update
    suspend fun updateFolder(folder: Folder)

    @Delete
    suspend fun deleteFolder(folder: Folder)

    @Query("SELECT * FROM folders_table ORDER BY folderName ASC")
    fun getFolders(): LiveData<List<Folder>>


}

