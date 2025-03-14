package com.example.optimusnotes.screens

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Square
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

// Data classes and enums (same as before)
enum class BrushStyle {
    NORMAL, SQUARE, CALLIGRAPHY
}

data class DrawingAction(
    val path: Path,
    val paint: Paint
)

data class DrawingTool(
    val isEraser: Boolean = false,
    val color: Color = Color.Black,
    val strokeWidth: Float = 5f,
    val brushStyle: BrushStyle = BrushStyle.NORMAL
)

// SharedPreferences key for storing the directory path
private const val PREF_DRAWING_DIRECTORY = "drawing_directory_path"

// Main screen composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen() { // Removed noteId and navController parameters
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val showColorPicker = remember { mutableStateOf(false) }
    val undoStack = remember { mutableStateListOf<DrawingAction>() }
    val redoStack = remember { mutableStateListOf<DrawingAction>() }
    val selectedTool = remember { mutableStateOf(DrawingTool()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val prefs: SharedPreferences = remember { context.getSharedPreferences("drawing_prefs", Context.MODE_PRIVATE) }

    // Load saved directory path from SharedPreferences
    var savedDirectoryPath by remember {
        mutableStateOf(prefs.getString(PREF_DRAWING_DIRECTORY, null))
    }

    val screenWidth = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx().toInt() }
    val screenHeight = with(LocalDensity.current) { (LocalConfiguration.current.screenHeightDp.dp.toPx() * 0.8).toInt() }

    // Initialize Bitmap
    val bitmapState = remember {
        mutableStateOf(
            Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888).apply {
                eraseColor(android.graphics.Color.WHITE)
            }
        )
    }
    val bitmap = bitmapState.value!!

    // Permission handling (same as before)
    val hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        true
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            scope.launch {
                saveDrawingToGallery(context, bitmap, snackbarHostState, savedDirectoryPath) // Pass savedDirectoryPath
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Storage permission required to save to Gallery.",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    // Directory selection launcher
    val directoryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val directoryPath = it.toString() // Store URI as string for persistence
            savedDirectoryPath = directoryPath
            prefs.edit().putString(PREF_DRAWING_DIRECTORY, directoryPath).apply() // Save to prefs
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Saving drawings to: $directoryPath",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    //theme
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.primary


    Surface(modifier = Modifier.fillMaxSize()) {
        Column {
            TopAppBar(
                title = { Text("Drawing Pad") },
                actions = {
                    IconButton(onClick = {
                        android.graphics.Canvas(bitmap).drawColor(android.graphics.Color.WHITE)
                        undoStack.clear()
                        redoStack.clear()
                    }) {
                        Icon(Icons.Default.Delete, "Clear Canvas", tint = Color.Black)
                    }
                    IconButton(onClick = {
                        directoryLauncher.launch(null) // Launch directory picker
                    }) {
                        Icon(Icons.Default.Folder, "Choose Save Folder",tint = Color.Black)
                    }
                    IconButton(onClick = {
                        if (hasStoragePermission) {
                            scope.launch {
                                saveDrawingToGallery(context, bitmap, snackbarHostState, savedDirectoryPath)
                            }
                        } else {
                            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    }) {
                        Icon(imageVector = Icons.Filled.Save, contentDescription = "Save", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )


            DrawingToolbar(
                selectedTool = selectedTool,
                onColorPickerClick = { showColorPicker.value = true }
            )

            DrawingCanvas(
                selectedTool = selectedTool,
                undoStack = undoStack,
                redoStack = redoStack,
                bitmap = bitmap
            )
        }
    }

    if (showColorPicker.value) {
        ColorPickerDialog(
            onColorSelected = { color ->
                selectedTool.value = selectedTool.value.copy(color = color)
                showColorPicker.value = false
            },
            onDismiss = { showColorPicker.value = false }
        )
    }
    SnackbarHost(hostState = snackbarHostState)
}

// Toolbar composable, DrawingCanvas, ColorButton, StrokeWidthButton, ColorPickerDialog - No changes needed (same as before)
@Composable
fun DrawingToolbar(
    selectedTool: MutableState<DrawingTool>,
    onColorPickerClick: () -> Unit
) {
    val colors = listOf(
        Color.Black, Color.Red, Color.Blue, Color.Green,
        Color.Yellow, Color.Magenta, Color.Cyan
    )
    val strokeWidths = listOf(2f, 5f, 8f, 12f, 16f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F0F0))
            .padding(8.dp)
    ) {
        // Colors
        Text("Colors", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 4.dp))
        LazyRow(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            items(colors) { color ->
                ColorButton(
                    color = color,
                    selected = selectedTool.value.color == color && !selectedTool.value.isEraser,
                    onClick = {
                        selectedTool.value = selectedTool.value.copy(
                            color = color,
                            isEraser = false
                        )
                    }
                )
            }
            item {
                IconButton(
                    onClick = {
                        selectedTool.value = selectedTool.value.copy(
                            isEraser = true,
                            color = Color.White
                        )
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (selectedTool.value.isEraser) Color(0xFFE0E0E0) else Color.Transparent,
                            shape = CircleShape
                        )
                        .border(
                            width = if (selectedTool.value.isEraser) 2.dp else 1.dp,
                            color = Color.Gray,
                            shape = CircleShape
                        )
                ) {
                    Icon(Icons.Filled.Close, "Eraser", tint = Color.Black)
                }
            }
            item {
                IconButton(
                    onClick = onColorPickerClick,
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, Color.Gray, CircleShape)
                ) {
                    Icon(Icons.Default.Palette, "Custom Color", tint = Color.Black)
                }
            }


        }

        // Stroke widths
        Text("Stroke Width", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 4.dp))
        LazyRow(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            items(strokeWidths) { width ->
                StrokeWidthButton(
                    width = width,
                    selected = selectedTool.value.strokeWidth == width,
                    onClick = {
                        selectedTool.value = selectedTool.value.copy(strokeWidth = width)
                    }
                )
            }
        }
    }
}

// Canvas composable - No changes needed
@Composable
fun DrawingCanvas(
    selectedTool: MutableState<DrawingTool>,
    undoStack: SnapshotStateList<DrawingAction>,
    redoStack: SnapshotStateList<DrawingAction>,
    bitmap: Bitmap
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current.density

    val bitmapWidth = (configuration.screenWidthDp * density).toInt()
    val bitmapHeight = ((configuration.screenHeightDp * density).toInt() * 0.8).toInt() // 80% of screen height


    val currentPath = remember { mutableStateOf(ComposePath()) }
    var currentPathPoints = remember { mutableStateListOf<Offset>() }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height((configuration.screenHeightDp * 0.8).dp)
            .background(Color(0xFFFAFAFA)) // Very light gray canvas background
            .border(0.5.dp, Color.LightGray) // Optional: Thin border for canvas
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentPathPoints.clear()
                        currentPathPoints.add(offset)
                        currentPath.value = ComposePath().apply {
                            moveTo(offset.x, offset.y)
                        }
                    },
                    onDrag = { change, _ ->
                        val newPoint = change.position
                        currentPathPoints.add(newPoint)

                        currentPath.value = ComposePath().apply {
                            moveTo(currentPathPoints.first().x, currentPathPoints.first().y)
                            for (i in 1 until currentPathPoints.size) {
                                lineTo(currentPathPoints[i].x, currentPathPoints[i].y)
                            }
                        }

                        change.consume()
                    },
                    onDragEnd = {
                        val paint = Paint().apply {
                            color = if (selectedTool.value.isEraser) android.graphics.Color.WHITE else selectedTool.value.color.toArgb()
                            isAntiAlias = true
                            strokeWidth = selectedTool.value.strokeWidth * density
                            style = Paint.Style.STROKE
                            strokeJoin = Paint.Join.ROUND
                            strokeCap = Paint.Cap.ROUND
                        }

                        val path = Path()
                        path.moveTo(currentPathPoints.first().x, currentPathPoints.first().y)
                        for (i in 1 until currentPathPoints.size) {
                            path.lineTo(currentPathPoints[i].x, currentPathPoints[i].y)
                        }

                        android.graphics.Canvas(bitmap).drawPath(path, paint)
                        undoStack.add(DrawingAction(path, paint))
                        redoStack.clear()

                        currentPathPoints.clear()
                        currentPath.value = ComposePath()
                    }
                )
            }
    ) {
        drawRect(Color.White) // Solid white background for the drawable area itself, even with canvas background color

        drawImage(bitmap.asImageBitmap())

        drawPath(
            path = currentPath.value,
            color = selectedTool.value.color,
            style = Stroke(
                width = selectedTool.value.strokeWidth * density
            )
        )
    }
}

// Helper composables - No changes needed
@Composable
fun ColorButton(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 2.dp else 2.dp, // Thicker border when selected
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, // Use primary color for selection
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

@Composable
fun StrokeWidthButton(
    width: Float,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(40.dp)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray // Use primary color for selection
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .height(width.dp) // Dynamic height based on width
                .width(24.dp)
                .background(Color.Black)
        )
    }
}

@Composable
fun ColorPickerDialog(
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(1f) }
    var value by remember { mutableStateOf(1f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Color") },
        text = {
            Column {
                // Hue slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hue:", modifier = Modifier.width(80.dp)) // Label for slider
                    Slider(
                        value = hue,
                        onValueChange = { hue = it },
                        valueRange = 0f..360f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Saturation slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Saturation:", modifier = Modifier.width(80.dp)) // Label for slider
                    Slider(
                        value = saturation,
                        onValueChange = { saturation = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Value slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Value:", modifier = Modifier.width(80.dp)) // Label for slider
                    Slider(
                        value = value,
                        onValueChange = { value = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }


                // Color preview
                Box(
                    modifier = Modifier
                        .size(120.dp) // Larger color preview
                        .padding(16.dp)
                        .background(Color.hsv(hue, saturation, value))
                        .border(1.dp, Color.Black)
                        .align(Alignment.CenterHorizontally) // Center preview
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onColorSelected(Color.hsv(hue, saturation, value))
                }
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private suspend fun saveDrawingToGallery(
    context: Context,
    bitmap: Bitmap,
    snackbarHostState: SnackbarHostState,
    directoryPath: String? // Added directoryPath parameter
) {
    val filename = "Drawing_${System.currentTimeMillis()}.png"
    val imageOutStream: OutputStream?

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")

        if (directoryPath != null) {
            // Use user-selected directory if available
            val relativePath = Uri.parse(directoryPath).pathSegments.drop(1).joinToString("/") // Extract relative path from URI
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + relativePath)
        } else {
            // Default to "Drawings" subdirectory in Pictures if no directory selected
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Drawings")
        }
    }


    val imageUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    if (imageUri != null) {
        imageOutStream = context.contentResolver.openOutputStream(imageUri)
        if (imageOutStream != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, imageOutStream)
            } finally {
                imageOutStream.close()
            }
            snackbarHostState.showSnackbar(
                message = "Drawing saved to Gallery",
                duration = SnackbarDuration.Short
            )
            println("Drawing saved to Gallery: URI = $imageUri")

        } else {
            snackbarHostState.showSnackbar(
                message = "Error saving drawing: OutputStream is null",
                duration = SnackbarDuration.Short
            )
            println("Error saving drawing: OutputStream is null")
        }
    } else {
        snackbarHostState.showSnackbar(
            message = "Error saving drawing: URI is null",
            duration = SnackbarDuration.Short
        )
        println("Error saving drawing: URI is null")
    }
}