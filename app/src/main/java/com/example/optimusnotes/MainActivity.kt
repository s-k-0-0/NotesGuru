package com.example.optimusnotes

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.optimusnotes.repository.NotesRepository
import com.example.optimusnotes.roomdb.NotesDB
import com.example.optimusnotes.screens.AppFontStyle
import com.example.optimusnotes.screens.AppTheme
import com.example.optimusnotes.screens.CalendarScreen
import com.example.optimusnotes.screens.DisplayDialog
import com.example.optimusnotes.screens.DisplayNotesScreen
import com.example.optimusnotes.screens.DrawingScreen
import com.example.optimusnotes.screens.FoldersScreen
import com.example.optimusnotes.screens.FontStyleState
import com.example.optimusnotes.screens.NoteDetailScreen
import com.example.optimusnotes.screens.SettingsScreen
import com.example.optimusnotes.screens.ThemePreferenceManager
import com.example.optimusnotes.screens.ThemeState
import com.example.optimusnotes.screens.ThemeSwitcher
import com.example.optimusnotes.ui.theme.OptimusNotesTheme
import com.example.optimusnotes.viewmodel.FolderViewModel
import com.example.optimusnotes.viewmodel.FolderViewModelFactory
import com.example.optimusnotes.viewmodel.NoteViewModel
import com.example.optimusnotes.viewmodel.NoteViewModelFactory
//ads
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {
    private lateinit var noteViewModel: NoteViewModel // Declare ViewModels at Activity level
    private lateinit var folderViewModel: FolderViewModel
    private lateinit var themePreferenceManager: ThemePreferenceManager

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Mobile Ads on a background thread
        lifecycleScope.launch(Dispatchers.IO) {
            MobileAds.initialize(this@MainActivity) {}
        }

        val database = NotesDB.getInstance(applicationContext)
        val noteRepository = NotesRepository(database.noteDao)
        val noteViewModelFactory = NoteViewModelFactory(noteRepository)
        noteViewModel =
            ViewModelProvider(this, noteViewModelFactory)[NoteViewModel::class.java] // Initialize here

        // Get FolderDao and create FolderViewModelFactory and FolderViewModel
        val folderDao = database.folderDao
        val folderViewModelFactory = FolderViewModelFactory(folderDao)
        folderViewModel =
            ViewModelProvider(this, folderViewModelFactory)[FolderViewModel::class.java] // Initialize here

        //themes
        themePreferenceManager = ThemePreferenceManager(this) // Initialize here


        setContent {
            val themeState = remember { mutableStateOf(AppTheme.SYSTEM_DEFAULT) }
            val fontStyleState = remember { mutableStateOf(AppFontStyle.DEFAULT) }

            CompositionLocalProvider(
                ThemeState provides themeState,
                FontStyleState provides fontStyleState
            ) { // Opening brace of CompositionLocalProvider
                ThemeSwitcher { // ThemeSwitcher now *inside* CompositionLocalProvider
                    val navController = rememberNavController()
                    val showDialog = remember { mutableStateOf(false) }
                    // State for Bottom Navigation selection
                    var selectedItem by rememberSaveable { mutableStateOf("notes") } // Start with "notes" selected

                    Scaffold(
                        bottomBar = { // **Add BottomNavigation here**
                            NavigationBar(modifier = Modifier) {

                                NavigationBarItem(
                                    selected = selectedItem == "notes",
                                    onClick = {
                                        if (selectedItem != "notes") { // ADDED check
                                            selectedItem = "notes"
                                            navController.navigate("home")
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            Icons.Filled.Note,
                                            contentDescription = "Notes"
                                        )
                                    },
                                    label = { Text("Notes") }
                                )
                                NavigationBarItem(
                                    selected = selectedItem == "calendar",
                                    onClick = {
                                        if (selectedItem != "calendar") { // ADDED check
                                            selectedItem = "calendar"
                                            navController.navigate("calendar")
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            Icons.Filled.CalendarMonth,
                                            contentDescription = "Calendar"
                                        )
                                    },
                                    label = { Text("Calendar") }
                                )

                                NavigationBarItem( // ADDED DRAWING NAVIGATION ITEM
                                    selected = selectedItem == "drawing",
                                    onClick = {
                                        if (selectedItem != "drawing") {
                                            selectedItem = "drawing"
                                            navController.navigate("drawing")
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            Icons.Filled.Create, // You can choose a different icon
                                            contentDescription = "Drawing"
                                        )
                                    },
                                    label = { Text("Drawing") }
                                )
                                NavigationBarItem(
                                    selected = selectedItem == "settings",
                                    onClick = {
                                        if (selectedItem != "settings") { // ADDED check
                                            selectedItem = "settings"
                                            navController.navigate("settings")
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            Icons.Filled.Settings,
                                            contentDescription = "Settings"
                                        )
                                    },
                                    label = { Text("Settings") }
                                )
                            }
                        },
                        content = { paddingValues ->
                            Column(modifier = Modifier.padding(paddingValues)) {

                                val navBackStackEntry by navController.currentBackStackEntryAsState() // Get current back stack entry
                                val currentRoute =
                                    navBackStackEntry?.destination?.route // Extract current route

                                // Update selectedItem based on currentRoute
                                selectedItem = when (currentRoute) {
                                    "home" -> "notes"
                                    "settings" -> "settings"
                                    "drawing" -> "drawing" // ADDED DRAWING ROUTE
                                    "calendar" -> "calendar"
                                    else -> selectedItem // Keep current selection for other routes (like noteDetail, edit_note)
                                }

                                DisplayDialog(
                                    viewModel = noteViewModel,
                                    showDialog = showDialog.value,
                                    onDismiss = { showDialog.value = false },
                                    folderViewModel = folderViewModel
                                )
                                NavHost(
                                    navController = navController,
                                    startDestination = "home" // Start at "home" (notes)
                                ) {
                                    composable("home") {
                                        val notes by noteViewModel.getNotesByFolder("Uncategorized")
                                            .observeAsState(emptyList())

                                        DisplayNotesScreen(
                                            notes = notes,
                                            navController = navController,
                                            onDeleteNote = { noteViewModel.deleteNote(it) },
                                            folderViewModel = folderViewModel // Pass folderViewModel
                                        )
                                    }
                                    composable(
                                        route = "edit_note/{noteId}",
                                        arguments = listOf(
                                            navArgument("noteId") { type = NavType.IntType }
                                        )
                                    ) { backStackEntry ->
                                        val noteId =
                                            backStackEntry.arguments?.getInt("noteId") ?: 0
                                        val note by noteViewModel.getNoteById(noteId)
                                            .observeAsState()

                                        note?.let { currentNote ->
                                            NoteDetailScreen(
                                                noteId = noteId,
                                                note = currentNote,
                                                onNoteUpdate = { updatedNote ->
                                                    noteViewModel.updateNote(updatedNote)
                                                },
                                                onDeleteNote = {
                                                    noteViewModel.deleteNote(currentNote)
                                                    navController.popBackStack()
                                                }
                                            )
                                        }
                                    }
                                    composable(
                                        route = "noteDetail/{noteId}",
                                        arguments = listOf(
                                            navArgument("noteId") { type = NavType.IntType }
                                        )
                                    ) { backStackEntry ->
                                        val noteId =
                                            backStackEntry.arguments?.getInt("noteId") ?: 0
                                        val note by noteViewModel.getNoteById(noteId)
                                            .observeAsState()
                                        note?.let {
                                            NoteDetailScreen(
                                                noteId = noteId,
                                                note = it,
                                                onNoteUpdate = { updatedNote ->
                                                    noteViewModel.updateNote(updatedNote)
                                                },
                                                onDeleteNote = {
                                                    noteViewModel.deleteNote(it)
                                                    navController.popBackStack()
                                                }
                                            )
                                        }
                                    }

                                    composable("settings") {
                                        SettingsScreen(
                                            themePreferenceManager = themePreferenceManager,
                                            modifier = Modifier
                                        ) // PASS themePreferenceManager HERE
                                    }
                                    composable("drawing") {
                                        DrawingScreen()
                                    }
                                    composable("calendar") {
                                        CalendarScreen(navController = navController) // **Passed navController here!**
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
