package com.example.optimusnotes.repository

import androidx.lifecycle.LiveData
import com.example.optimusnotes.roomdb.Folder
import com.example.optimusnotes.roomdb.Note
import com.example.optimusnotes.roomdb.NoteDao
import com.example.optimusnotes.roomdb.FolderDao

class NotesRepository(private val noteDao: NoteDao) {

    fun getNotesByFolder(folder: String): LiveData<List<Note>> {
        return noteDao.getNotesByFolder(folder)
    }

    fun getAllFolders(): LiveData<List<String>> {
        return noteDao.getAllFolders()
    }

    suspend fun insertNote(note: Note) {
        noteDao.insert(note)
    }

    suspend fun updateNote(note: Note) {
        noteDao.update(note)
    }

    suspend fun deleteNote(note: Note) {
        noteDao.delete(note)
    }
    fun getNoteById(id: Int): LiveData<Note?> {
        return noteDao.getNoteByIdLive(id)
    }
}
