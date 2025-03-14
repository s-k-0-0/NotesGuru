package com.example.optimusnotes.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.optimusnotes.roomdb.FolderDao
import com.example.optimusnotes.viewmodel.FolderViewModel // Import FolderViewModel


class FolderViewModelFactory(private val folderDao: FolderDao) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FolderViewModel::class.java)) {
            return FolderViewModel(folderDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}