package com.example.optimusnotes.roomdb


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes_table")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description : String,
    val content: String,
    val color: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val folderName: String // New column to store folder name
)



@Entity(tableName = "folders_table")
data class Folder(
    @PrimaryKey val folderName: String
)
