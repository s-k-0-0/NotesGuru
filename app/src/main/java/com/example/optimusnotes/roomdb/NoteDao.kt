package com.example.optimusnotes.roomdb

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
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
}





@Dao
interface FolderDao {

    @Insert
    suspend fun addFolder(folder: Folder)

    @Query("SELECT * FROM folders_table")
    fun getAllFolders(): LiveData<List<Folder>>

    @Query("DELETE FROM folders_table WHERE folderName = :folderName")
    suspend fun deleteFolder(folderName: String)

    @Query("UPDATE folders_table SET folderName = :newFolder WHERE folderName = :oldFolder")
    suspend fun renameFolder(oldFolder: String, newFolder: String)
}
