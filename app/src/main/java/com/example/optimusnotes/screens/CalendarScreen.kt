package com.example.optimusnotes.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.optimusnotes.roomdb.Note
import com.example.optimusnotes.roomdb.NotesDB
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(navController: NavController) { // NavController parameter is here
    val datePickerState = rememberDatePickerState()
    var openDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val notesForSelectedDate = remember { mutableStateListOf<Note>() }
    val context = LocalContext.current
    val noteDatabase = remember { NotesDB.getInstance(context) }
    val noteDao = remember { noteDatabase.noteDao }
    val scope = rememberCoroutineScope()

    //theme
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.primary

    // Function to fetch notes for a given date
    val fetchNotesForDate: suspend (LocalDate) -> List<Note> = { date ->
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        noteDao.getNotesBetweenTimestamps(startOfDay, endOfDay)
    }

    // LaunchedEffect to fetch notes when selected date changes
    LaunchedEffect(selectedDate) {
        scope.launch {
            notesForSelectedDate.clear()
            notesForSelectedDate.addAll(fetchNotesForDate(selectedDate))
        }
    }

    if (openDialog) {
        DatePickerDialog(
            onDismissRequest = {
                openDialog = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        openDialog = false
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { openDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                actions = {
                    IconButton(onClick = { openDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = "Pick Date",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                )
            )
        }


    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Notes for ${selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))}",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (notesForSelectedDate.isEmpty()) {
                Text(
                    text = "No notes for this date.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn {
                    items(notesForSelectedDate) { note ->
                        NoteItem(note = note, navController = navController) // Correct: Using the navController parameter
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            BannerAdView(adUnitId = "ca-app-pub-3940256099942544/6300978111")

        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NoteItem(note: Note, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                navController.navigate("edit_note/${note.id}")
            },
        colors = CardDefaults.cardColors(containerColor = Color(note.color)), // Apply note color
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Display Rich Text instead of Plain Text
            InteractiveRichTextViewer(
                text = note.description,
                onTextUpdate = {} // No need for updates in read mode
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Created: ${
                    LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(note.timestamp),
                        ZoneId.systemDefault()
                    ).format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))
                }",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

