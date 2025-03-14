package com.example.optimusnotes.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.res.stringResource
import com.example.optimusnotes.R // Import your R file for string resources
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.core.content.ContextCompat
import com.example.optimusnotes.utils.BackupUtils
import java.io.File
import kotlin.system.exitProcess


// Define Theme Options
enum class AppTheme {
    LIGHT, DARK, SYSTEM_DEFAULT
}

// Define Font Style Options
enum class AppFontStyle {
    DEFAULT, MONOSPACE, SERIF, SANS_SERIF,CURSIVE
}



// Modern Vibrant Color Palette
private val DarkColorSchemePastel = darkColorScheme(
    primary = Color(0xFFFBB195),      // Soft Peach - Main accent color
    secondary = Color(0xFFF7C8D0),     // Rose Pink - Secondary actions
    tertiary = Color(0xFFB6E2D3),      // Mint Green - Decorative elements
    background = Color(0xFF1A1A1F),     // Deep Navy Black - Rich background
    surface = Color(0xFF2A2A30),        // Lighter Navy Black - Card surfaces
    surfaceVariant = Color(0xFF252529), // Medium Navy Black - Alternative surfaces
    onPrimary = Color(0xFF2A2A30),     // Dark text on pastel
    onSecondary = Color(0xFF2A2A30),    // Dark text on pastel
    onTertiary = Color(0xFF2A2A30),     // Dark text on pastel
    onBackground = Color(0xFFF2F2F7),   // Light text on dark background
    onSurface = Color(0xFFF2F2F7)       // Light text on dark surface
)

private val LightColorSchemePastel = lightColorScheme(
    primary = Color(0xFFFBB195),      // Soft Peach - Maintaining consistency
    secondary = Color(0xFFF7C8D0),     // Rose Pink - Soft secondary
    tertiary = Color(0xFFB6E2D3),      // Mint Green - Subtle accent
    background = Color(0xFFF8F9FA),     // Off-White - Clean background
    surface = Color(0xFFFFFFFF),        // Pure White - Card surfaces
    surfaceVariant = Color(0xFFF2F3F5), // Light Gray - Alternative surfaces
    onPrimary = Color(0xFF2A2A30),      // Dark text on pastel
    onSecondary = Color(0xFF2A2A30),    // Dark text on pastel
    onTertiary = Color(0xFF2A2A30),     // Dark text on pastel
    onBackground = Color(0xFF2A2A30),   // Dark text on light background
    onSurface = Color(0xFF2A2A30)       // Dark text on light surface
)

// Additional pastel colors for custom elements
object PastelColors {
    val PastelYellow = Color(0xFFFFF4BD)
    val PastelLavender = Color(0xFFE0D3F5)
    val PastelBlue = Color(0xFFB5DEFF)
    val PastelCoral = Color(0xFFFFBEB8)
    val PastelMint = Color(0xFFBDEDDF)
    val PastelLilac = Color(0xFFE2D1F9)
    val PastelSage = Color(0xFFCAE4D8)

    // Darker pastel variants for emphasis
    val DeepPeach = Color(0xFFF79B79)
    val DeepRose = Color(0xFFEBA5B3)
    val DeepMint = Color(0xFF8FCBB8)
}



// CompositionLocals
val ThemeState = compositionLocalOf { mutableStateOf(AppTheme.SYSTEM_DEFAULT) }
val FontStyleState = compositionLocalOf { mutableStateOf(AppFontStyle.DEFAULT) }


class ThemePreferenceManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)

    private val themeKey = "app_theme"
    private val fontStyleKey = "app_font_style" // New key for font style

    // MutableStateFlow for Theme and Font Style
    private val _themeFlow = MutableStateFlow(getSavedTheme()) // Initialize with saved theme
    val getTheme: Flow<AppTheme> = _themeFlow

    private val _fontStyleFlow = MutableStateFlow(getSavedFontStyle()) // Initialize with saved font style
    val getFontStyle: Flow<AppFontStyle> = _fontStyleFlow


    init {
        // Initialize flows with saved values when ThemePreferenceManager is created
        _themeFlow.value = getSavedTheme()
        _fontStyleFlow.value = getSavedFontStyle()
    }


    // Function to save the selected theme
    fun saveTheme(theme: AppTheme) {
        sharedPreferences.edit()
            .putString(themeKey, theme.name)
            .apply()
        _themeFlow.value = theme // Emit the new theme value to the flow
    }

    private fun getSavedTheme(): AppTheme {
        val themeString = sharedPreferences.getString(themeKey, AppTheme.SYSTEM_DEFAULT.name) ?: AppTheme.SYSTEM_DEFAULT.name
        return AppTheme.valueOf(themeString)
    }


    // Function to save the selected font style
    fun saveFontStyle(fontStyle: AppFontStyle) {
        sharedPreferences.edit()
            .putString(fontStyleKey, fontStyle.name)
            .apply()
        _fontStyleFlow.value = fontStyle // Emit the new font style value to the flow
    }


    private fun getSavedFontStyle(): AppFontStyle {
        val fontStyleString = sharedPreferences.getString(fontStyleKey, AppFontStyle.DEFAULT.name) ?: AppFontStyle.DEFAULT.name
        return AppFontStyle.valueOf(fontStyleString)
    }
}


// Theme Switcher Composable
@Composable
fun ThemeSwitcher(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val themePreferenceManager = remember { ThemePreferenceManager(context) }
    val appTheme by themePreferenceManager.getTheme.collectAsState(initial = AppTheme.SYSTEM_DEFAULT)
    val themeSetter = ThemeState.current
    themeSetter.value = appTheme

    val appFontStyle by themePreferenceManager.getFontStyle.collectAsState(initial = AppFontStyle.DEFAULT)
    val fontStyleSetter = FontStyleState.current
    fontStyleSetter.value = appFontStyle

    val colors = when (appTheme) {
        AppTheme.LIGHT -> LightColorSchemePastel
        AppTheme.DARK -> DarkColorSchemePastel
        AppTheme.SYSTEM_DEFAULT -> if (isSystemInDarkTheme()) DarkColorSchemePastel else LightColorSchemePastel
    }

    val typography = MaterialTheme.typography.let { defaultTypography ->
        val fontFamily = when (appFontStyle) {
            AppFontStyle.DEFAULT -> FontFamily.Default
            AppFontStyle.MONOSPACE -> FontFamily.Monospace
            AppFontStyle.SERIF -> FontFamily.Serif
            AppFontStyle.SANS_SERIF -> FontFamily.SansSerif
            AppFontStyle.CURSIVE -> FontFamily.Cursive
        }

        Typography(
            displayLarge = defaultTypography.displayLarge.copy(fontFamily = fontFamily),
            displayMedium = defaultTypography.displayMedium.copy(fontFamily = fontFamily),
            displaySmall = defaultTypography.displaySmall.copy(fontFamily = fontFamily),
            headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = fontFamily),
            headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = fontFamily),
            headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = fontFamily),
            titleLarge = defaultTypography.titleLarge.copy(fontFamily = fontFamily),
            titleMedium = defaultTypography.titleMedium.copy(fontFamily = fontFamily),
            titleSmall = defaultTypography.titleSmall.copy(fontFamily = fontFamily),
            bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = fontFamily),
            bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = fontFamily),
            bodySmall = defaultTypography.bodySmall.copy(fontFamily = fontFamily),
            labelLarge = defaultTypography.labelLarge.copy(fontFamily = fontFamily),
            labelMedium = defaultTypography.labelMedium.copy(fontFamily = fontFamily),
            labelSmall = defaultTypography.labelSmall.copy(fontFamily = fontFamily)
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themePreferenceManager: ThemePreferenceManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedTheme = ThemeState.current.value
    val themeState = ThemeState.current
    val selectedFontStyle = FontStyleState.current.value
    val fontStyleState = FontStyleState.current

    var showRestartDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }

    var backupFoldersList by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedBackupFolder by remember { mutableStateOf<File?>(null) }

    // Permission launcher for Android 10 and below
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            showPermissionRationale = true
        }
    }

    fun handleStoragePermission(onGranted: () -> Unit) {
        when {
            BackupUtils.hasRequiredPermissions(context) -> {
                onGranted()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Section
            item {
                SectionCard(
                    title = "Theme",
                    content = {
                        RadioButtonOption(
                            text = "Light Theme",
                            selected = selectedTheme == AppTheme.LIGHT,
                            onClick = {
                                themeState.value = AppTheme.LIGHT
                                themePreferenceManager.saveTheme(AppTheme.LIGHT)
                                showRestartDialog = true
                            }
                        )
                        RadioButtonOption(
                            text = "Dark Theme",
                            selected = selectedTheme == AppTheme.DARK,
                            onClick = {
                                themeState.value = AppTheme.DARK
                                themePreferenceManager.saveTheme(AppTheme.DARK)
                                showRestartDialog = true
                            }
                        )
                        RadioButtonOption(
                            text = "System Default",
                            selected = selectedTheme == AppTheme.SYSTEM_DEFAULT,
                            onClick = {
                                themeState.value = AppTheme.SYSTEM_DEFAULT
                                themePreferenceManager.saveTheme(AppTheme.SYSTEM_DEFAULT)
                                showRestartDialog = true
                            }
                        )
                    }
                )
            }

            // Font Style Section
            item {
                SectionCard(
                    title = "Font Style",
                    content = {
                        RadioButtonOption(
                            text = "Default",
                            selected = selectedFontStyle == AppFontStyle.DEFAULT,
                            onClick = {
                                fontStyleState.value = AppFontStyle.DEFAULT
                                themePreferenceManager.saveFontStyle(AppFontStyle.DEFAULT)
                                showRestartDialog = true
                            }
                        )
                        RadioButtonOption(
                            text = "Monospace",
                            selected = selectedFontStyle == AppFontStyle.MONOSPACE,
                            onClick = {
                                fontStyleState.value = AppFontStyle.MONOSPACE
                                themePreferenceManager.saveFontStyle(AppFontStyle.MONOSPACE)
                                showRestartDialog = true
                            }
                        )
                        RadioButtonOption(
                            text = "Serif",
                            selected = selectedFontStyle == AppFontStyle.SERIF,
                            onClick = {
                                fontStyleState.value = AppFontStyle.SERIF
                                themePreferenceManager.saveFontStyle(AppFontStyle.SERIF)
                                showRestartDialog = true
                            }
                        )
                        RadioButtonOption(
                            text = "Sans Serif",
                            selected = selectedFontStyle == AppFontStyle.SANS_SERIF,
                            onClick = {
                                fontStyleState.value = AppFontStyle.SANS_SERIF
                                themePreferenceManager.saveFontStyle(AppFontStyle.SANS_SERIF)
                                showRestartDialog = true
                            }
                        )
                        RadioButtonOption(
                            text = "Cursive",
                            selected = selectedFontStyle == AppFontStyle.CURSIVE,
                            onClick = {
                                fontStyleState.value = AppFontStyle.CURSIVE
                                themePreferenceManager.saveFontStyle(AppFontStyle.CURSIVE)
                                showRestartDialog = true
                            }
                        )
                    }
                )
            }

            // Backup Section
            item {
                BannerAdView(adUnitId = "ca-app-pub-3940256099942544/6300978111")
                SectionCard(
                    title = "Local Backup",
                    content = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    handleStoragePermission {
                                        BackupUtils.createFullBackup(context).fold(
                                            onSuccess = {
                                                Toast.makeText(
                                                    context,
                                                    "Backup created successfully.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            },
                                            onFailure = { error ->
                                                Toast.makeText(
                                                    context,
                                                    "Backup failed: ${error.message}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Create Local Backup")
                            }

                            Button(
                                onClick = {
                                    handleStoragePermission {
                                        val backupsDir = BackupUtils.getBackupDirectory(context)
                                        val folders = backupsDir?.listFiles()?.filter { it.isDirectory } ?: emptyList()
                                        backupFoldersList = folders
                                        showRestoreDialog = folders.isNotEmpty()
                                        if (folders.isEmpty()) {
                                            Toast.makeText(
                                                context,
                                                "No backups found.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Restore Local Backup")
                            }
                        }
                    }
                )
            }
        }
    }

    // Dialogs
    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text("Restart App?") },
            text = { Text("Changes will be applied after restarting the app.") },
            confirmButton = {
                Button(
                    onClick = {
                        Process.killProcess(Process.myPid())
                        exitProcess(0)
                    }
                ) {
                    Text("Restart")
                }
            },
            dismissButton = {
                Button(onClick = { showRestartDialog = false }) {
                    Text("Later")
                }
            }
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Backup") },
            text = {
                Column {
                    Text("Select a backup to restore from:")
                    Spacer(modifier = Modifier.height(8.dp))
                    backupFoldersList.forEach { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedBackupFolder == folder,
                                onClick = { selectedBackupFolder = folder }
                            )
                            Text(
                                text = folder.name,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedBackupFolder?.let { backupFolder ->
                            BackupUtils.restoreFullBackup(context, backupFolder.absolutePath).fold(
                                onSuccess = {
                                    Toast.makeText(
                                        context,
                                        "Restore successful. Restarting...",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    Process.killProcess(Process.myPid())
                                    exitProcess(0)
                                },
                                onFailure = { error ->
                                    Toast.makeText(
                                        context,
                                        "Restore failed: ${error.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        } ?: Toast.makeText(context, "Please select a backup.", Toast.LENGTH_SHORT).show()
                        showRestoreDialog = false
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                Button(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Permission Required") },
            text = {
                Text(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        "This app needs access to manage all files for backup operations. " +
                                "Please grant 'Allow management of all files' permission in Settings."
                    } else {
                        "Storage permission is required for backup operations. " +
                                "Please grant the permission in Settings."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationale = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } else {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                Button(onClick = { showPermissionRationale = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
fun RadioButtonOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}




