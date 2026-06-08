package com.omsingh.nwerf

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import java.io.File
import kotlinx.coroutines.launch
import kotlin.OptIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi

// Sleek HSL/Dark Color Palette matching the web application
val BackgroundColor = Color(0xFF0F0E17)
val CardColor = Color(0xFF1F1D2C)
val PrimaryColor = Color(0xFF8B5CF6) // Sleek Violet
val SecondaryColor = Color(0xFF06B6D4) // Cyan Accent
val TextColor = Color(0xFFFFFFFE)
val MutedTextColor = Color(0xFFA7A9BE)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NwerfTheme(content: @Composable () -> Unit) {
    val darkColors = darkColorScheme(
        background = BackgroundColor,
        surface = CardColor,
        primary = PrimaryColor,
        secondary = SecondaryColor,
        onBackground = TextColor,
        onSurface = TextColor
    )
    MaterialExpressiveTheme(
        colorScheme = darkColors,
        content = content
    )
}

@Composable
fun NwerfApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val err by viewModel.errorMessage.collectAsState()

    LaunchedEffect(err) {
        err?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        bottomBar = {
            Column {
                BottomPlayer(viewModel)
                BottomNavigationBar(navController)
            }
        },
        containerColor = BackgroundColor
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "library",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("library") { LibraryScreen(viewModel) }
            composable("upload") { UploadScreen(viewModel) }
            composable("settings") { SettingsScreen(viewModel) }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: androidx.navigation.NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = CardColor,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple("library", "Library", Icons.Default.List),
            Triple("upload", "Upload", Icons.Default.AddCircle),
            Triple("settings", "Settings", Icons.Default.Settings)
        )
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontSize = 11.sp) },
                selected = currentRoute == route,
                onClick = {
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryColor,
                    unselectedIconColor = MutedTextColor,
                    selectedTextColor = PrimaryColor,
                    unselectedTextColor = MutedTextColor,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun LibraryScreen(viewModel: MainViewModel) {
    val tracks by viewModel.tracks.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val filteredTracks = tracks.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Your Library", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextColor)
                Text("${tracks.size} tracks synced", fontSize = 12.sp, color = MutedTextColor)
            }
            IconButton(
                onClick = { viewModel.syncLibrary() },
                modifier = Modifier
                    .background(PrimaryColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PrimaryColor, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = PrimaryColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search songs or artists...", color = MutedTextColor) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MutedTextColor) },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CardColor,
                unfocusedContainerColor = CardColor,
                disabledContainerColor = CardColor,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredTracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tracks found", color = MutedTextColor, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredTracks) { track ->
                    val isCurrent = currentTrack?.id == track.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isCurrent) CardColor.copy(alpha = 0.5f) else CardColor)
                            .clickable { viewModel.playTrack(track) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(listOf(PrimaryColor, SecondaryColor))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isCurrent && isPlaying) Icons.Default.VolumeUp else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = TextColor
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                track.title,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCurrent) PrimaryColor else TextColor,
                                maxLines = 1
                            )
                            Text(track.artist, fontSize = 12.sp, color = MutedTextColor, maxLines = 1)
                        }

                        IconButton(onClick = { viewModel.deleteTrack(track) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UploadScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    val isUploading by viewModel.isUploading.collectAsState()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedFileUri = uri
        selectedFileName = uri?.path?.substringAfterLast("/") ?: "audio.mp3"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Upload Music", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextColor)
        Text("Upload MP3 files to your private Telegram channel.", fontSize = 13.sp, color = MutedTextColor)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CardColor)
                .clickable(enabled = !isUploading) { filePicker.launch("audio/*") }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = SecondaryColor,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (selectedFileUri != null) selectedFileName else "Tap to Select Audio File",
                    color = TextColor,
                    fontWeight = FontWeight.Medium
                )
                Text("Limit: 50MB per file", fontSize = 11.sp, color = MutedTextColor)
            }
        }

        TextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Track Title") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isUploading
        )

        TextField(
            value = artist,
            onValueChange = { artist = it },
            label = { Text("Artist Name") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isUploading
        )

        Button(
            onClick = {
                val uri = selectedFileUri
                if (uri != null && title.isNotBlank() && artist.isNotBlank()) {
                    val file = getFileFromUri(context, uri)
                    viewModel.uploadTrack(file, title, artist)
                    // Reset fields
                    selectedFileUri = null
                    title = ""
                    artist = ""
                } else {
                    Toast.makeText(context, "Please complete all fields", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isUploading && selectedFileUri != null && title.isNotBlank() && artist.isNotBlank(),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isUploading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = TextColor, strokeWidth = 2.dp)
            } else {
                Text("Upload Track", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val botToken by viewModel.settingsStore.botToken.collectAsState(initial = "")
    val chatId by viewModel.settingsStore.chatId.collectAsState(initial = "")
    val githubPat by viewModel.settingsStore.githubPat.collectAsState(initial = "")
    val gistId by viewModel.settingsStore.gistId.collectAsState(initial = "")

    var inputToken by remember { mutableStateOf("") }
    var inputChat by remember { mutableStateOf("") }
    var inputPat by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    LaunchedEffect(botToken, chatId, githubPat) {
        inputToken = botToken ?: ""
        inputChat = chatId ?: ""
        inputPat = githubPat ?: ""
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextColor)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardColor),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Telegram configuration", fontWeight = FontWeight.Bold, color = SecondaryColor)
                    TextField(
                        value = inputToken,
                        onValueChange = { inputToken = it },
                        label = { Text("Bot Token") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextField(
                        value = inputChat,
                        onValueChange = { inputChat = it },
                        label = { Text("Chat ID (Channel)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.settingsStore.saveTelegramSettings(inputToken, inputChat)
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save Details")
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardColor),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Library Sync (GitHub)", fontWeight = FontWeight.Bold, color = PrimaryColor)
                    TextField(
                        value = inputPat,
                        onValueChange = { inputPat = it },
                        label = { Text("GitHub PAT (gist scope)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!gistId.isNullOrEmpty()) {
                        Text("Active Gist ID: ${gistId!!.take(8)}...", fontSize = 12.sp, color = MutedTextColor)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (gistId.isNullOrEmpty()) {
                            Button(
                                onClick = { viewModel.createGist(inputPat) },
                                enabled = inputPat.isNotBlank()
                            ) {
                                Text("Create Gist")
                            }
                        } else {
                            Button(onClick = { viewModel.syncLibrary() }) {
                                Text("Sync Now")
                            }
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.settingsStore.saveGistSettings(inputPat, gistId ?: "")
                                }
                            }
                        ) {
                            Text("Save PAT")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomPlayer(viewModel: MainViewModel) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentTime by viewModel.currentTime.collectAsState()
    val duration by viewModel.duration.collectAsState()

    AnimatedVisibility(visible = currentTrack != null) {
        val track = currentTrack ?: return@AnimatedVisibility
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardColor)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, fontWeight = FontWeight.Bold, color = TextColor, maxLines = 1)
                    Text(track.artist, fontSize = 12.sp, color = MutedTextColor, maxLines = 1)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { viewModel.playPrevious() }) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = TextColor)
                    }
                    IconButton(
                        onClick = { viewModel.togglePlay() },
                        modifier = Modifier.background(PrimaryColor, RoundedCornerShape(50))
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = TextColor
                        )
                    }
                    IconButton(onClick = { viewModel.playNext() }) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = TextColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Seek slider progress bar
            val progress = if (duration > 0) currentTime.toFloat() / duration else 0f
            LinearProgressIndicator(
                progress = progress,
                color = SecondaryColor,
                trackColor = MutedTextColor.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }
    }
}

// Utility function to resolve Uri content stream into a temp file.
private fun getFileFromUri(context: Context, uri: Uri): File {
    val tempFile = File(context.cacheDir, "temp_upload.mp3")
    context.contentResolver.openInputStream(uri)?.use { input ->
        tempFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    return tempFile
}
