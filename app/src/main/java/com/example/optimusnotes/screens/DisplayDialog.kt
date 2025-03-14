package com.example.optimusnotes.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.optimusnotes.roomdb.Folder
import com.example.optimusnotes.roomdb.Note
import com.example.optimusnotes.viewmodel.FolderViewModel
import com.example.optimusnotes.viewmodel.NoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayDialog(
    viewModel: NoteViewModel,
    folderViewModel: FolderViewModel,
    showDialog: Boolean,
    onDismiss: () -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by remember { mutableStateOf(TextFieldValue()) }
    var color by rememberSaveable { mutableStateOf(0xFFFBB195.toInt()) } // Default color as Int

    val colorsList = listOf(
        0xFFFBB195.toInt(), 0xFFA5D6A7.toInt(), 0xFF90CAF9.toInt(),
        0xFFF48FB1.toInt(), 0xFFFFF59D.toInt(), 0xFFCE93D8.toInt(), 0xFFB0BEC5.toInt()
    )

    if (showDialog) {
        // Reset state when opening dialog
        title = ""
        description = TextFieldValue()
        color = 0xFFFBB195.toInt()

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color(color), // Now the dialog background changes
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    // Title Field
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title", color = Color.Black) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            cursorColor = Color.Black,
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Enhanced Text Editor
                    TextEditorWithToolbar(
                        textFieldValue = description,
                        onTextChange = { description = it }
                    )


                    Spacer(modifier = Modifier.height(16.dp))

                    // Color Picker (Changes Dialog Background)
                    Text(text = "Choose Note Color", fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(colorsList) { colorValue ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorValue))
                                    .border(
                                        width = if (color == colorValue) 3.dp else 1.dp,
                                        color = if (color == colorValue) Color.Black else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { color = colorValue } // Updates the dialog color dynamically
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Black) }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (title.isNotEmpty() && description.text.isNotEmpty()) {
                                    viewModel.insert(
                                        Note(
                                            title = title.trim(),
                                            description = description.text.trim(),
                                            content = description.text,
                                            color = color, // Stores selected color
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )
                                    onDismiss()
                                }
                            },
                            enabled = title.isNotEmpty() && description.text.isNotEmpty()
                        ) {
                            Text("Create Note")
                        }
                    }
                }
            }
        }
    }
}

