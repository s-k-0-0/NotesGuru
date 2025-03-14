package com.example.optimusnotes.screens

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.optimusnotes.repository.NotesRepository
import com.example.optimusnotes.roomdb.Note
import com.example.optimusnotes.roomdb.NotesDB
import com.example.optimusnotes.viewmodel.FolderViewModel
import com.example.optimusnotes.viewmodel.NoteViewModel
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    onDeleteNote: (Note) -> Unit = {},
    folderViewModel: FolderViewModel
) {
    var isGridView by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(SortOrder.NEWEST_FIRST) }
    var isFilterVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val viewModel = NoteViewModel(NotesRepository(NotesDB.getInstance(LocalContext.current).noteDao))
    val showDialog = remember { mutableStateOf(false) }

    var filteredNotes by remember { mutableStateOf(performFilterAndSort(notes, searchQuery, sortOrder)) }

    LaunchedEffect(searchQuery, sortOrder, notes) {
        filteredNotes = performFilterAndSort(notes, searchQuery, sortOrder)
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog.value = true },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, "Add Note")
                    Text("New Note")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {


                SearchBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    isGridView = isGridView,
                    onViewToggle = { isGridView = it },
                    onFilterClick = { isFilterVisible = !isFilterVisible }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            BannerAdView(adUnitId = "ca-app-pub-3940256099942544/6300978111")

            DisplayDialog(
                viewModel = viewModel,
                folderViewModel = folderViewModel,
                showDialog = showDialog.value,
                onDismiss = { showDialog.value = false }
            )

            AnimatedVisibility(
                visible = isFilterVisible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                FilterPanel(
                    currentSortOrder = sortOrder,
                    onSortOrderSelected = { sortOrder = it }
                )
            }

            if (filteredNotes.isEmpty()) {
                EmptyStateView(isSearching = searchQuery.isNotBlank())
            } else {
                NotesGrid(
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
                                viewModel.insert(note)
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun NotesGrid(
    notes: List<Note>,
    isGridView: Boolean,
    navController: NavController,
    onDeleteNote: (Note) -> Unit
) {
    val contentPadding = PaddingValues(12.dp)
    val itemSpacing = 12.dp

    if (notes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No notes found",
                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    } else {
        if (isGridView) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalItemSpacing = itemSpacing
            ) {
                items(notes, key = { it.id }) { note ->
                    ModernNoteCard(
                        note = note,
                        navController = navController,
                        onDelete = onDeleteNote,
                        modifier = Modifier.animateItemPlacement(),
                        isGridView = isGridView
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
                items(notes, key = { it.id }) { note ->
                    ModernNoteCard(
                        note = note,
                        navController = navController,
                        onDelete = onDeleteNote,
                        modifier = Modifier.animateItemPlacement(),
                        isGridView = isGridView
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isGridView: Boolean,
    onViewToggle: (Boolean) -> Unit,
    onFilterClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onBackground
            )

            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.secondary
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search notes",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )

            IconButton(onClick = { onViewToggle(!isGridView) }) {
                Icon(
                    imageVector = if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                    contentDescription = "Toggle View",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(onClick = onFilterClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Sort",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}



@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterPanel(
    currentSortOrder: SortOrder,
    onSortOrderSelected: (SortOrder) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Sort by",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
            { Icon(Icons.Default.Check, "Selected", tint = MaterialTheme.colorScheme.primary) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.surface,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun EmptyStateView(isSearching: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                if (isSearching) Icons.Default.SearchOff else Icons.Default.NoteAdd,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Text(
                if (isSearching) "No matching notes" else "Start your first note",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                if (isSearching) "Try different search terms" else "Tap the + button to begin",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}



@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernNoteCard(
    note: Note,
    navController: NavController,
    onDelete: (Note) -> Unit,
    modifier: Modifier = Modifier,
    isGridView: Boolean,
    onUpdate: (Note) -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val formattedDate = remember(note.timestamp) {
        val dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(note.timestamp),
            ZoneId.systemDefault()
        )
        dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    }

    val defaultColor = Color(0xFFFBB195)
    val noteColor = if (note.color == 0) defaultColor else Color(note.color)

    Card(
        onClick = { navController.navigate("edit_note/${note.id}") },
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isGridView) {
                    Modifier.height(250.dp)
                } else {
                    Modifier.heightIn(min = 200.dp, max = 250.dp)
                }
            )
            .clip(RoundedCornerShape(30.dp))
            .shadow(
                elevation = 15.dp,
                shape = RoundedCornerShape(30.dp),
                spotColor = noteColor.copy(alpha = 0.5f),
                ambientColor = noteColor.copy(alpha = 0.5f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = noteColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isGridView) 20.sp else 24.sp,
                            color = Color.Black
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                var richTextContent by remember { mutableStateOf(note.description) }
                RichTextViewer(
                    text = richTextContent,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    onTextUpdate = { updatedText ->
                        richTextContent = updatedText
                        val updatedNote = note.copy(description = updatedText)
                        onUpdate(updatedNote)
                    }
                )

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.End),
                    color = Color.Black.copy(alpha = 0.7f)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        navController.navigate("edit_note/${note.id}")
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        showDeleteDialog = true
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete this note?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(note)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}




@Composable
fun RichTextViewer(text: String, modifier: Modifier = Modifier, style: TextStyle = TextStyle.Default, onTextUpdate: (String) -> Unit) {
    Column(modifier = modifier.fillMaxWidth()) {
        val lines = text.lines().toMutableList()

        lines.forEachIndexed { index, line ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    line.startsWith("*") -> {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        RichTextLine(line.substring(2))
                    }

                    line.startsWith("[ ]") || line.startsWith("[x]") -> {
                        val isChecked = line.startsWith("[x]")
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                lines[index] = if (checked) "[x] ${line.substring(3)}" else "[ ] ${line.substring(3)}"
                                onTextUpdate(lines.joinToString("\n")) // Update full text
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.secondary,
                                uncheckedColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        RichTextLine(line.substring(3))
                    }

                    else -> {
                        RichTextLine(line)
                    }
                }
            }
        }
    }
}

@Composable
private fun RichTextLine(text: String) {
    val formattedText = buildAnnotatedString {
        var currentText = text
        while (currentText.isNotEmpty()) {
            val boldIndex = currentText.indexOf("**")
            val italicIndex = currentText.indexOf("_")
            val underlineIndex = currentText.indexOf("~")

            val nextIndex = listOf(boldIndex, italicIndex, underlineIndex)
                .filter { it >= 0 }
                .minOrNull()

            if (nextIndex == null) {
                append(currentText)
                break
            }

            val textBeforeMarker = currentText.substring(0, nextIndex)
            append(textBeforeMarker)

            when (nextIndex) {
                boldIndex -> {
                    val endIndex = currentText.indexOf("**", boldIndex + 2)
                    if (endIndex != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(currentText.substring(boldIndex + 2, endIndex))
                        }
                        currentText = currentText.substring(endIndex + 2)
                    } else {
                        append("**")
                        currentText = currentText.substring(boldIndex + 2)
                    }
                }
                italicIndex -> {
                    val endIndex = currentText.indexOf("_", italicIndex + 1)
                    if (endIndex != -1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(currentText.substring(italicIndex + 1, endIndex))
                        }
                        currentText = currentText.substring(endIndex + 1)
                    } else {
                        append("_")
                        currentText = currentText.substring(italicIndex + 1)
                    }
                }
                underlineIndex -> {
                    val endIndex = currentText.indexOf("~", underlineIndex + 1)
                    if (endIndex != -1) {
                        withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append(currentText.substring(underlineIndex + 1, endIndex))
                        }
                        currentText = currentText.substring(endIndex + 1)
                    } else {
                        append("~")
                        currentText = currentText.substring(underlineIndex + 1)
                    }
                }
            }
        }
    }

    Text(
        text = formattedText,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(start = 8.dp)
    )
}



@Composable
fun BannerAdView(adUnitId: String) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val retryCount = remember { mutableStateOf(0) } // Track retry attempts
    val maxRetries = 3 // Maximum number of retries
    val fixedDelayMillis = 2000L // Fixed delay of 2 seconds for each retry

    val adView = remember {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            this.adUnitId = adUnitId
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    Log.d("Ads", "Banner Ad loaded successfully")
                    retryCount.value = 0 // Reset retry count on success
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(
                        "Ads",
                        "Banner Ad failed to load: ${adError.code}, ${adError.message}, Domain: ${adError.domain}, Cause: ${adError.cause}, Retry Count: ${retryCount.value}"
                    )
                    super.onAdFailedToLoad(adError)

                    if (retryCount.value < maxRetries) {
                        retryCount.value++
                        Log.d(
                            "Ads",
                            "Retrying ad load in ${fixedDelayMillis / 1000.0} seconds. Retry attempt: ${retryCount.value}"
                        )
                        Handler(Looper.getMainLooper()).postDelayed({
                            loadAd(AdRequest.Builder().build()) // Retry ad load with fixed delay
                        }, fixedDelayMillis)
                    } else {
                        Log.e("Ads", "Max retries reached. Ad load failed.")
                    }
                }
            }
        }
    }

    AndroidView(
        factory = {
            adView.apply {
                loadAd(AdRequest.Builder().build()) // Initial ad load
            }
        },
        update = { view ->
            // No update needed for BannerAdView in this example
        }
    )
}

// **Extracted filtering and sorting logic into a separate function**
private fun performFilterAndSort(
    notes: List<Note>,
    searchQuery: String,
    sortOrder: SortOrder
): List<Note> {
    val searchedNotes = notes.filter { note ->
        searchQuery.isBlank() || note.title.contains(searchQuery, ignoreCase = true) ||
                note.description.contains(searchQuery, ignoreCase = true)
    }
    return when (sortOrder) {
        SortOrder.NEWEST_FIRST -> searchedNotes.sortedByDescending { it.timestamp }
        SortOrder.OLDEST_FIRST -> searchedNotes.sortedBy { it.timestamp }
        SortOrder.TITLE_ASC -> searchedNotes.sortedBy { it.title }
        SortOrder.TITLE_DESC -> searchedNotes.sortedByDescending { it.title }
    }
}