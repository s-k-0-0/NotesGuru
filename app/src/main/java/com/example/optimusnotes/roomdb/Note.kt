package com.example.optimusnotes.roomdb


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes_table")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val content: String,
    val color: Int,
    val timestamp: Long,
    val folderName: String = "Uncategorized",
    val folderId: Int? = null,
    var drawingData: String? = null // ADDED: Field to store drawing data (nullable String)
)


@Entity(tableName = "folders_table")
data class Folder(
    @PrimaryKey val folderName: String
)