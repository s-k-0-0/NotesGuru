package com.example.optimusnotes.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.optimusnotes.MyColorPicker
import com.example.optimusnotes.roomdb.Note

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    note: Note,
    onNoteUpdate: (Note) -> Unit = {},
    onDeleteNote: () -> Unit = {}
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf(note.title) }
    var editedDescription by remember { mutableStateOf(note.description) }
    var selectedColor by remember { mutableStateOf(Color(note.color)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Note Details") },
                actions = {
                    IconButton(onClick = { isEditing = !isEditing }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = if (isEditing) "Save" else "Edit"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = selectedColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isEditing) {
                        OutlinedTextField(
                            value = editedTitle,
                            onValueChange = { editedTitle = it },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editedDescription,
                            onValueChange = { editedDescription = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                        Text(
                            "Select Color",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        MyColorPicker(
                            selectedColor = selectedColor,
                            onColorSelected = { selectedColor = it }
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onNoteUpdate(
                                        note.copy(
                                            title = editedTitle,
                                            description = editedDescription,
                                            color = selectedColor.toArgb()
                                        )
                                    )
                                    isEditing = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save")
                            }
                            OutlinedButton(
                                onClick = { isEditing = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                        }
                    } else {
                        Text(
                            text = editedTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = editedDescription,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            if (!isEditing) {
                Button(
                    onClick = onDeleteNote,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Delete Note")
                }
            }
        }
    }
}