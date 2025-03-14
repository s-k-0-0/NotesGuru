package com.example.optimusnotes.screens

import android.app.Activity
import android.app.Instrumentation
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.optimusnotes.roomdb.Note
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Int,
    note: Note,
    onNoteUpdate: (Note) -> Unit = {},
    onDeleteNote: () -> Unit = {}
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf(note.title) }
    var editedDescription by remember { mutableStateOf(TextFieldValue(note.description)) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // Default color if note.color is 0
    val defaultColor = Color(0xFFFBB195) // Light Orange
    val noteColor = if (note.color == 0) defaultColor else Color(note.color)

    val textFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    val noteText = "${note.title}\n\n${note.description}"
                    outputStream.bufferedWriter().use { writer -> writer.write(noteText) }
                }
                Toast.makeText(context, "Note exported to Text file", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Toast.makeText(context, "Error exporting to Text file", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    val pdfFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    val document = PdfDocument()
                    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                    val page = document.startPage(pageInfo)
                    val canvas: Canvas = page.canvas
                    val paint = Paint().apply {
                        color = Color.Black.toArgb()
                        textSize = 12f
                    }

                    val textMargin = 40f
                    var yPos = textMargin + 20f

                    // Draw Title
                    paint.textSize = 24f
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText(note.title, pageInfo.pageWidth / 2f, yPos, paint)
                    yPos += 50f

                    // Draw Description
                    paint.textSize = 12f
                    paint.textAlign = Paint.Align.LEFT
                    val textPaint = TextPaint(paint)
                    val textLines = StaticLayout.Builder.obtain(
                        note.description, 0, note.description.length,
                        textPaint, (pageInfo.pageWidth - 2 * textMargin).toInt()
                    ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1.2f)
                        .setIncludePad(false)
                        .build()

                    canvas.save()
                    canvas.translate(textMargin, yPos)
                    textLines.draw(canvas)
                    canvas.restore()

                    document.finishPage(page)
                    document.writeTo(outputStream)
                    document.close()
                    Toast.makeText(context, "Note exported to PDF file", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                Toast.makeText(context, "Error exporting to PDF file", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Note" else "Note Details") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = noteColor, // Match Note Color
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    if (isEditing) {
                        IconButton(onClick = { isEditing = false }) {
                            Icon(Icons.Default.Close, "Close")
                        }
                    }
                },
                actions = {
                    AnimatedVisibility(visible = !isEditing) {
                        Row {
                            IconButton(onClick = { isEditing = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Black)
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Black)
                            }
                            IconButton(onClick = { textFileLauncher.launch("${note.title}.txt") }) {
                                Icon(Icons.Default.TextSnippet, contentDescription = "Export Text", tint = Color.Black)
                            }
                            IconButton(onClick = { pdfFileLauncher.launch("${note.title}.pdf") }) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = Color.Black)
                            }
                        }
                    }
                    AnimatedVisibility(visible = isEditing) {
                        IconButton(
                            onClick = {
                                onNoteUpdate(note.copy(title = editedTitle, description = editedDescription.text))
                                isEditing = false
                            }
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save", tint = Color.Black)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isEditing) {
                EditMode(
                    editedTitle = editedTitle,
                    onTitleChange = { editedTitle = it },
                    editedDescription = editedDescription,
                    onDescriptionChange = { editedDescription = it }
                )
            } else {
                ViewMode(
                    title = editedTitle,
                    description = editedDescription.text,
                    onTextUpdate = { editedDescription = TextFieldValue(it) }
                    )
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Note") },
                text = { Text("Are you sure you want to delete this note? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteNote()
                            showDeleteDialog = false
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun EditMode(
    editedTitle: String,
    onTitleChange: (String) -> Unit,
    editedDescription: TextFieldValue,
    onDescriptionChange: (TextFieldValue) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title Field
            OutlinedTextField(
                value = editedTitle,
                onValueChange = onTitleChange,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                textStyle = MaterialTheme.typography.titleLarge
            )

            // Text Editor with Formatting Toolbar
            TextEditorWithToolbar(
                textFieldValue = editedDescription,
                onTextChange = onDescriptionChange
            )

            // Interactive Rich Text Viewer (for live preview)
            InteractiveRichTextViewer(
                text = editedDescription.text,
                onTextUpdate = { updatedText ->
                    onDescriptionChange(TextFieldValue(updatedText))
                }
            )
        }
    }
}

@Composable
private fun ViewMode(
    title: String,
    description: String,
    onTextUpdate: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Display Title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Divider for Separation
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Display Rich Text Content with Editing Support
            InteractiveRichTextViewer(
                text = description,
                onTextUpdate = onTextUpdate
            )
        }
    }
}


@Composable
fun TextEditorWithToolbar(
    textFieldValue: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit
) {
    var selectedFormatting by remember { mutableStateOf(setOf<FormattingOption>()) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    val colors = listOf(
        Color.Black to "Default",
        Color(0xFFD32F2F) to "Red",
        Color(0xFF1976D2) to "Blue",
        Color(0xFF388E3C) to "Green",
        Color(0xFF7B1FA2) to "Purple"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 1.dp,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Text Formatting Section
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Bold
                        IconToggleButton(
                            checked = selectedFormatting.contains(FormattingOption.BOLD),
                            onCheckedChange = { checked ->
                                selectedFormatting = if (checked) {
                                    selectedFormatting + FormattingOption.BOLD
                                } else {
                                    selectedFormatting - FormattingOption.BOLD
                                }
                                applyMarkdownFormatting(textFieldValue, onTextChange, "**")
                            }
                        ) {
                            Icon(
                                Icons.Default.FormatBold,
                                contentDescription = "Bold",
                                tint = if (selectedFormatting.contains(FormattingOption.BOLD))
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Italic
                        IconToggleButton(
                            checked = selectedFormatting.contains(FormattingOption.ITALIC),
                            onCheckedChange = { checked ->
                                selectedFormatting = if (checked) {
                                    selectedFormatting + FormattingOption.ITALIC
                                } else {
                                    selectedFormatting - FormattingOption.ITALIC
                                }
                                applyMarkdownFormatting(textFieldValue, onTextChange, "_")
                            }
                        ) {
                            Icon(
                                Icons.Default.FormatItalic,
                                contentDescription = "Italic",
                                tint = if (selectedFormatting.contains(FormattingOption.ITALIC))
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Underline
                        IconToggleButton(
                            checked = selectedFormatting.contains(FormattingOption.UNDERLINE),
                            onCheckedChange = { checked ->
                                selectedFormatting = if (checked) {
                                    selectedFormatting + FormattingOption.UNDERLINE
                                } else {
                                    selectedFormatting - FormattingOption.UNDERLINE
                                }
                                applyMarkdownFormatting(textFieldValue, onTextChange, "~")
                            }
                        ) {
                            Icon(
                                Icons.Default.FormatUnderlined,
                                contentDescription = "Underline",
                                tint = if (selectedFormatting.contains(FormattingOption.UNDERLINE))
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Lists Section
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconToggleButton(
                            checked = selectedFormatting.contains(FormattingOption.BULLET),
                            onCheckedChange = { checked ->
                                selectedFormatting = if (checked) {
                                    selectedFormatting + FormattingOption.BULLET - FormattingOption.CHECKBOX
                                } else {
                                    selectedFormatting - FormattingOption.BULLET
                                }
                                if (checked) {
                                    insertBulletPoint(textFieldValue, onTextChange)
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.List,
                                contentDescription = "Bullet List",
                                tint = if (selectedFormatting.contains(FormattingOption.BULLET))
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconToggleButton(
                            checked = selectedFormatting.contains(FormattingOption.CHECKBOX),
                            onCheckedChange = { checked ->
                                selectedFormatting = if (checked) {
                                    selectedFormatting + FormattingOption.CHECKBOX - FormattingOption.BULLET
                                } else {
                                    selectedFormatting - FormattingOption.CHECKBOX
                                }
                                if (checked) {
                                    insertCheckbox(textFieldValue, onTextChange)
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = "Checkbox List",
                                tint = if (selectedFormatting.contains(FormattingOption.CHECKBOX))
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Color Picker
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        colors.forEach { (color, description) ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .padding(4.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { selectedColor = color },
                                    shape = CircleShape,
                                    color = color,
                                    border = BorderStroke(
                                        width = if (selectedColor == color) 2.dp else 1.dp,
                                        color = if (selectedColor == color)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline
                                    )
                                ) {}
                            }
                        }
                    }
                }
            }
        }
        // Text Editor
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                // Automatically add formatting based on selected options
                val formattedValue = when {
                    selectedFormatting.contains(FormattingOption.BULLET) &&
                            newValue.text.endsWith("\n") -> {
                        val newText = "${newValue.text}* "
                        newValue.copy(
                            text = newText,
                            selection = TextRange(newText.length)
                        )
                    }
                    selectedFormatting.contains(FormattingOption.CHECKBOX) &&
                            newValue.text.endsWith("\n") -> {
                        val newText = "${newValue.text}[ ] "
                        newValue.copy(
                            text = newText,
                            selection = TextRange(newText.length)
                        )
                    }
                    else -> newValue
                }
                onTextChange(formattedValue)
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
                .padding(16.dp),
            textStyle = TextStyle(
                color = selectedColor,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                fontFamily = MaterialTheme.typography.bodyLarge.fontFamily
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onPrimary)
        )
    }
}

@Composable
fun InteractiveRichTextViewer(
    text: String,
    onTextUpdate: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        text.lines().forEach { line ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    line.startsWith("* ") -> {
                        Text(
                            "•",
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
                                val newLines = text.lines().toMutableList()
                                val index = text.lines().indexOf(line)
                                newLines[index] = if (checked) "[x]${line.substring(3)}" else "[ ]${line.substring(3)}"
                                onTextUpdate(newLines.joinToString("\n"))
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
                        append("**") // Handle unclosed marker
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
                        append("_") // Handle unclosed marker
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
                        append("~") // Handle unclosed marker
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


fun applyMarkdownFormatting(
    textFieldValue: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    markdown: String
) {
    val selection = textFieldValue.selection
    val selectedText = textFieldValue.text.substring(selection.start, selection.end)
    val newText = textFieldValue.text.replaceRange(
        selection.start,
        selection.end,
        "$markdown$selectedText$markdown"
    )
    onTextChange(
        textFieldValue.copy(
            text = newText,
            selection = TextRange(selection.start + markdown.length, selection.start + markdown.length + selectedText.length)
        )
    )
}


private fun insertBulletPoint(
    currentValue: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit
) {
    val currentPosition = currentValue.selection.start
    val lines = currentValue.text.lines()
    val currentLineIndex = currentValue.text.substring(0, currentPosition).count { it == '\n' }

    if (currentLineIndex < lines.size) {
        val currentLine = lines[currentLineIndex]
        if (!currentLine.startsWith("*")) {
            val newLines = lines.toMutableList()
            newLines[currentLineIndex] = "* $currentLine"
            val newText = newLines.joinToString("\n")
            onTextChange(TextFieldValue(
                text = newText,
                selection = TextRange(currentPosition + 2)
            ))
        }
    }
}

private fun insertCheckbox(
    currentValue: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit
) {
    val currentPosition = currentValue.selection.start
    val lines = currentValue.text.lines()
    val currentLineIndex = currentValue.text.substring(0, currentPosition).count { it == '\n' }

    if (currentLineIndex < lines.size) {
        val currentLine = lines[currentLineIndex]
        if (!currentLine.startsWith("[ ]")) {
            val newLines = lines.toMutableList()
            newLines[currentLineIndex] = "[ ] $currentLine"
            val newText = newLines.joinToString("\n")
            onTextChange(TextFieldValue(
                text = newText,
                selection = TextRange(currentPosition + 4)
            ))
        }
    }
}

private enum class FormattingOption {
    BOLD,
    ITALIC,
    UNDERLINE,
    BULLET,
    CHECKBOX
}
