package com.example.optimusnotes.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optimusnotes.repository.NotesRepository
import com.example.optimusnotes.roomdb.Note
import kotlinx.coroutines.launch

class NoteViewModel(private val repository: NotesRepository) : ViewModel() {

    fun getNotesByFolder(folder: String): LiveData<List<Note>> {
        return repository.getNotesByFolder(folder)
    }

    fun getAllFolders(): LiveData<List<String>> {
        return repository.getAllFolders()
    }

    fun insert(note: Note) = viewModelScope.launch {
        repository.insertNote(note)
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }
    fun getNoteById(id: Int): LiveData<Note?> {
        return repository.getNoteById(id)
    }
}
