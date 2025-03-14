package com.example.optimusnotes.viewmodel


import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optimusnotes.roomdb.Folder
import com.example.optimusnotes.roomdb.FolderDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FolderViewModel(private val folderDao: FolderDao) : ViewModel() {

    val folders: LiveData<List<Folder>> = folderDao.getFolders()

    fun insertFolder(folderName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val folder = Folder(folderName = folderName)
            folderDao.insertFolder(folder)
        }
    }

    fun deleteFolder(folder: Folder) { // Function to delete a folder
        viewModelScope.launch(Dispatchers.IO) {
            folderDao.deleteFolder(folder)
        }
    }
}