package com.example.optimusnotes

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.optimusnotes.repository.NotesRepository
import com.example.optimusnotes.roomdb.Note
import com.example.optimusnotes.roomdb.NotesDB
import com.example.optimusnotes.screens.DisplayDialog
import com.example.optimusnotes.screens.DisplayNotesScreen
import com.example.optimusnotes.screens.NoteDetailScreen
import com.example.optimusnotes.ui.theme.OptimusNotesTheme
import com.example.optimusnotes.viewmodel.NoteViewModel
import com.example.optimusnotes.viewmodel.NoteViewModelFactory



class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = NotesDB.getInstance(applicationContext)
        val noteDao = database.noteDao
        val folderDao = database.folderDao  // Get the folderDao from the same NotesDB instance
        val repository = NotesRepository(noteDao) // Pass both noteDao and folderDao
        val viewModelFactory = NoteViewModelFactory(repository)
        val noteViewModel = ViewModelProvider(this, viewModelFactory)[NoteViewModel::class.java]

        setContent {
            val navController = rememberNavController()

            OptimusNotesTheme {
                Scaffold(
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { navController.navigate("addNote") }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Note"
                            )
                        }
                    }
                ) { paddingValues ->
                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        // Home Screen
                        composable("home") {
                            val notes by noteViewModel.getNotesByFolder("Uncategorized").observeAsState(emptyList())

                            DisplayNotesScreen(
                                notes = notes,
                                navController = navController,
                                onDeleteNote = { noteViewModel.deleteNote(it) }
                            )
                        }

                        // Add Note Screen
                        composable("addNote") {
                            var showDialog by remember { mutableStateOf(false) }

                            LaunchedEffect(Unit) {
                                showDialog = true
                            }

                            if (showDialog) {
                                DisplayDialog(
                                    viewModel = noteViewModel,
                                    showDialog = true
                                ) {
                                    showDialog = false
                                    navController.popBackStack()
                                }
                            }
                        }

                        // Edit Note Screen
                        composable(
                            route = "edit_note/{noteId}",
                            arguments = listOf(navArgument("noteId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val noteId = backStackEntry.arguments?.getInt("noteId") ?: 0
                            val note by noteViewModel.getNoteById(noteId).observeAsState()

                            note?.let { currentNote ->
                                NoteDetailScreen(
                                    note = currentNote,
                                    onNoteUpdate = { updatedNote ->
                                        noteViewModel.updateNote(updatedNote)
                                        navController.popBackStack()
                                    },
                                    onDeleteNote = {
                                        noteViewModel.deleteNote(currentNote)
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}



// Color Picker Component
@Composable
fun MyColorPicker(selectedColor: Color, onColorSelected: (Color) -> Unit) {
    val colors = listOf(
        Color.Red, Color.Green, Color.Blue,
        Color.Yellow, Color.Cyan, Color.White, Color.Magenta
    )

    LazyRow(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        items(colors) { color ->
            ColorOption(
                color = color,
                isSelected = color == selectedColor,
                onClick = { onColorSelected(color) }
            )
        }
    }
}

// Single Color Option in Picker
@Composable
fun ColorOption(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .padding(4.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 4.dp else 0.dp,
                color = if (isSelected) Color.Black else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}
