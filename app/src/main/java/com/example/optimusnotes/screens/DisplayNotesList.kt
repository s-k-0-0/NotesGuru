package com.example.optimusnotes.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.optimusnotes.roomdb.Note
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST,
    TITLE_ASC,
    TITLE_DESC
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DisplayNotesScreen(
    notes: List<Note>,
    navController: NavController,
    onDeleteNote: (Note) -> Unit = {}
) {
    var isGridView by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(SortOrder.NEWEST_FIRST) }
    var isFilterVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Filter and search logic
    val filteredNotes = remember(searchQuery, sortOrder, notes) {
        notes.filter { note ->
            note.title.contains(searchQuery, ignoreCase = true) ||
                    note.description.contains(searchQuery, ignoreCase = true)
        }.let { filtered ->
            when (sortOrder) {
                SortOrder.NEWEST_FIRST -> filtered.sortedByDescending { it.timestamp }
                SortOrder.OLDEST_FIRST -> filtered.sortedBy { it.timestamp }
                SortOrder.TITLE_ASC -> filtered.sortedBy { it.title }
                SortOrder.TITLE_DESC -> filtered.sortedByDescending { it.title }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SearchAndFilterBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                isGridView = isGridView,
                onViewToggle = { isGridView = it },
                onFilterClick = { isFilterVisible = !isFilterVisible }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedVisibility(
                visible = isFilterVisible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SortOptionsSection(
                    currentSortOrder = sortOrder,
                    onSortOrderSelected = { sortOrder = it }
                )
            }

            if (filteredNotes.isEmpty()) {
                EmptyNotesMessage(searchQuery.isNotEmpty())
            } else {
                NotesContent(
                    notes = filteredNotes,
                    isGridView = isGridView,
                    navController = navController,
                    onDeleteNote = { note ->
                        onDeleteNote(note)
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Note deleted",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                // Handle undo operation
                            }
                        }
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isGridView: Boolean,
    onViewToggle: (Boolean) -> Unit,
    onFilterClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search notes...") },
                    leadingIcon = { Icon(Icons.Default.Search, "Search") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                IconButton(onClick = { onViewToggle(!isGridView) }) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.List else Icons.Default.GridView,
                        contentDescription = "Toggle view",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onFilterClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun SortOptionsSection(
    currentSortOrder: SortOrder,
    onSortOrderSelected: (SortOrder) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Sort by",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortChip(
                    selected = currentSortOrder == SortOrder.NEWEST_FIRST,
                    onClick = { onSortOrderSelected(SortOrder.NEWEST_FIRST) },
                    label = "Newest"
                )
                SortChip(
                    selected = currentSortOrder == SortOrder.OLDEST_FIRST,
                    onClick = { onSortOrderSelected(SortOrder.OLDEST_FIRST) },
                    label = "Oldest"
                )
                SortChip(
                    selected = currentSortOrder == SortOrder.TITLE_ASC,
                    onClick = { onSortOrderSelected(SortOrder.TITLE_ASC) },
                    label = "A-Z"
                )
                SortChip(
                    selected = currentSortOrder == SortOrder.TITLE_DESC,
                    onClick = { onSortOrderSelected(SortOrder.TITLE_DESC) },
                    label = "Z-A"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null) }
        } else null
    )
}

@Composable
private fun EmptyNotesMessage(isSearching: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSearching) Icons.Default.SearchOff else Icons.Default.NoteAdd,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isSearching) "No matching notes found" else "No notes yet",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (isSearching) "Try different search terms" else "Create your first note!",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotesContent(
    notes: List<Note>,
    isGridView: Boolean,
    navController: NavController,
    onDeleteNote: (Note) -> Unit
) {
    if (isGridView) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalItemSpacing = 16.dp
        ) {
            items(
                items = notes,
                key = { it.id }
            ) { note ->
                NoteCard(
                    note = note,
                    navController = navController,
                    onDelete = onDeleteNote,
                    modifier = Modifier.animateItemPlacement()
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = notes,
                key = { it.id }
            ) { note ->
                NoteCard(
                    note = note,
                    navController = navController,
                    onDelete = onDeleteNote,
                    modifier = Modifier.animateItemPlacement()
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteCard(
    note: Note,
    navController: NavController,
    onDelete: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    // Format timestamp
    val formattedDate = remember(note.timestamp) {
        val dateTime = java.time.LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(note.timestamp),
            java.time.ZoneId.systemDefault()
        )
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
        dateTime.format(formatter)
    }

    Card(
        onClick = { navController.navigate("edit_note/${note.id}") },
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = note.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { showUpdateDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Update note",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = { showDeleteDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete note",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete this note?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(note)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Update Dialog
    if (showUpdateDialog) {
        var updatedTitle by remember { mutableStateOf(note.title) }
        var updatedDescription by remember { mutableStateOf(note.description) }

        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Update Note") },
            text = {
                Column {
                    OutlinedTextField(
                        value = updatedTitle,
                        onValueChange = { updatedTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = updatedDescription,
                        onValueChange = { updatedDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        navController.navigate("edit_note/${note.id}") {
                            popUpTo("home")
                        }
                        showUpdateDialog = false
                    }
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
