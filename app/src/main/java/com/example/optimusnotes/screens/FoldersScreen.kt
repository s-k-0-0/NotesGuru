package com.example.optimusnotes.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.optimusnotes.roomdb.Folder
import com.example.optimusnotes.viewmodel.FolderViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import com.example.optimusnotes.viewmodel.FolderViewModelFactory


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(folderViewModelFactory: FolderViewModelFactory) {
    // 1. Get FolderViewModel
    val folderViewModel: FolderViewModel = viewModel(factory = folderViewModelFactory)
    val folders by folderViewModel.folders.observeAsState(initial = emptyList())

    // 2. State for Add Folder Dialog
    var showDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    // 3. State for Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, // SnackbarHost to display snackbars
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) { // **Show dialog on FAB click**
                Icon(Icons.Filled.Add, "Add Folder")
            }
        },
        topBar = {
            TopAppBar(title = { Text("Folders") }) // TopAppBar for "Folders" screen
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (folders.isEmpty()) {
                Text(
                    "No folders created yet. Click '+' to add a folder.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                // 4. Display Folder List using LazyColumn
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(folders) { folder ->
                        FolderItem(
                            folder = folder,
                            onDeleteClick = { folderToDelete ->
                                folderViewModel.deleteFolder(folderToDelete)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("${folderToDelete.folderName} deleted")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // 5. Add Folder Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Create New Folder") },
            text = {
                TextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            folderViewModel.insertFolder(Folder(folderName = newFolderName).toString()) // **Insert Folder object**
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("${newFolderName} folder created")
                            }
                            newFolderName = ""
                            showDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderItem(folder: Folder, onDeleteClick: (Folder) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = folder.folderName, style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = { onDeleteClick(folder) }) {
                Icon(Icons.Filled.Delete, "Delete Folder")
            }
        }
    }
}
