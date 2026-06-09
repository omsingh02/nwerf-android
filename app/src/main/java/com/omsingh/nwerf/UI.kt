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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NwerfTheme(content: @Composable () -> Unit) {
    // We use the default expressive color scheme directly instead of overriding everything
    // so that the true Material 3 Expressive vibrant palette shines through.
    val expressiveColors = darkColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF8B5CF6),
        secondary = androidx.compose.ui.graphics.Color(0xFF06B6D4)
    )
    MaterialExpressiveTheme(
        colorScheme = expressiveColors,
        content = content
    )
}

@Composable
fun NwerfApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val err by viewModel.errorMessage.collectAsState()
    val hasSeenTutorial by viewModel.settingsStore.hasSeenTutorial.collectAsState(initial = null)

    LaunchedEffect(err) {
        err?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute != "tutorial" && currentRoute != "splash") {
                Column {
                    BottomPlayer(viewModel)
                    BottomNavigationBar(navController)
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("splash") {
                LaunchedEffect(hasSeenTutorial) {
                    if (hasSeenTutorial != null) {
                        val dest = if (hasSeenTutorial == true) "library" else "tutorial"
                        navController.navigate(dest) {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            composable("tutorial") { TutorialScreen(viewModel, navController) }
            composable("library") { LibraryScreen(viewModel) }
            composable("identify") { IdentifyScreen(viewModel) }
            composable("upload") { UploadScreen(viewModel) }
            composable("settings") { SettingsScreen(viewModel) }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: androidx.navigation.NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        val items = listOf(
            Triple("library", "Library", Icons.Default.List),
            Triple("identify", "Identify", Icons.Default.Mic),
            Triple("upload", "Upload", Icons.Default.AddCircle),
            Triple("settings", "Settings", Icons.Default.Settings)
        )
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                selected = currentRoute == route,
                onClick = {
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
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
                Text("Your Library", style = MaterialTheme.typography.headlineLarge)
                Text("${tracks.size} tracks synced", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalIconButton(onClick = { viewModel.syncLibrary() }) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Sync")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search songs or artists...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (filteredTracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tracks found", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTracks) { track ->
                    val isCurrent = currentTrack?.id == track.id
                    ElevatedCard(
                        onClick = { viewModel.playTrack(track) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (isCurrent) CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.elevatedCardColors()
                    ) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                            headlineContent = { Text(track.title) },
                            supportingContent = { Text(track.artist) },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.medium),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isCurrent && isPlaying) Icons.Default.VolumeUp else Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            },
                            trailingContent = {
                                IconButton(onClick = { viewModel.deleteTrack(track) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
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
    val identifiedTrack by viewModel.identifiedTrack.collectAsState()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedFileUri = uri
        selectedFileName = uri?.path?.substringAfterLast("/") ?: "audio.mp3"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column {
            Text("Upload Music", style = MaterialTheme.typography.headlineLarge)
            Text("Upload MP3 files to your private Telegram channel.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        OutlinedCard(
            onClick = { if (!isUploading) filePicker.launch("audio/*") },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        if (selectedFileUri != null) selectedFileName else "Tap to Select Audio File",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("Limit: 50MB per file", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Track Title") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isUploading
        )

        OutlinedTextField(
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
                    selectedFileUri = null
                    title = ""
                    artist = ""
                } else {
                    Toast.makeText(context, "Please complete all fields", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isUploading && selectedFileUri != null && title.isNotBlank() && artist.isNotBlank()
        ) {
            if (isUploading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            } else {
                Text("Upload Track", style = MaterialTheme.typography.titleMedium)
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
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineLarge)
        }

        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Telegram Configuration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = inputToken,
                        onValueChange = { inputToken = it },
                        label = { Text("Bot Token") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
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
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Library Sync (GitHub)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = inputPat,
                        onValueChange = { inputPat = it },
                        label = { Text("GitHub PAT (gist scope)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!gistId.isNullOrEmpty()) {
                        Text("Active Gist ID: ${gistId!!.take(8)}...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (gistId.isNullOrEmpty()) {
                            FilledTonalButton(
                                onClick = { viewModel.createGist(inputPat) },
                                enabled = inputPat.isNotBlank()
                            ) {
                                Text("Create Gist")
                            }
                        } else {
                            FilledTonalButton(onClick = { viewModel.syncLibrary() }) {
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

        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Developed by Omsingh02",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "nwerf v0.1.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun TutorialScreen(viewModel: MainViewModel, navController: androidx.navigation.NavHostController) {
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = "Welcome",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Welcome to nwerf!",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "A private, self-hosted music streaming app powered by Telegram and GitHub Gists.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Setup Instructions", style = MaterialTheme.typography.titleMedium)
                Text("1. Create a Telegram Bot and Channel.", style = MaterialTheme.typography.bodyMedium)
                Text("2. Get a GitHub PAT with 'gist' scope.", style = MaterialTheme.typography.bodyMedium)
                Text("3. Enter them in the Settings tab to sync your music library across devices.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = {
                scope.launch {
                    viewModel.settingsStore.setTutorialSeen()
                    navController.navigate("library") {
                        popUpTo("tutorial") { inclusive = true }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Get Started", style = MaterialTheme.typography.titleMedium)
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
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { viewModel.playPrevious() }) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                        }
                        FloatingActionButton(
                            onClick = { viewModel.togglePlay() },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            elevation = FloatingActionButtonDefaults.elevation(0.dp)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause"
                            )
                        }
                        IconButton(onClick = { viewModel.playNext() }) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Seek slider progress bar
                val progress = if (duration > 0) currentTime.toFloat() / duration else 0f
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
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

@Composable
fun IdentifyScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val isUploading by viewModel.isUploading.collectAsState()
    val identifiedTracks by viewModel.identifiedTracks.collectAsState()
    val downloadingTrackIds by viewModel.downloadingTrackIds.collectAsState()

    var isRecording by remember { mutableStateOf(false) }
    var recordedFile by remember { mutableStateOf<java.io.File?>(null) }
    val recorder = remember { AudioRecorder(context) }
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            isRecording = true
            recordedFile = recorder.startRecording()
            Toast.makeText(context, "Listening for 7 seconds...", Toast.LENGTH_SHORT).show()
            coroutineScope.launch {
                kotlinx.coroutines.delay(7000)
                if (isRecording) {
                    isRecording = false
                    recorder.stopRecording()
                    recordedFile?.let { file ->
                        viewModel.identifyTrack(file)
                    }
                }
            }
        } else {
            Toast.makeText(context, "Microphone permission is required to identify songs", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (identifiedTracks.isEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
        }

        Text("Identify Song", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Tap to listen and identify the track playing around you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (isRecording) {
                    isRecording = false
                    recorder.stopRecording()
                    recordedFile?.let { file ->
                        viewModel.identifyTrack(file)
                    }
                } else {
                    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.RECORD_AUDIO
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        isRecording = true
                        recordedFile = recorder.startRecording()
                        Toast.makeText(context, "Listening for 7 seconds...", Toast.LENGTH_SHORT).show()
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(7000)
                            if (isRecording) {
                                isRecording = false
                                recorder.stopRecording()
                                recordedFile?.let { file ->
                                    viewModel.identifyTrack(file)
                                }
                            }
                        }
                    } else {
                        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            enabled = !isUploading,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            if (isUploading && recordedFile != null && !isRecording) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onTertiary, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Mic, contentDescription = "Identify")
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isRecording) "Listening..." else "Identify Song", style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (identifiedTracks.isEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(identifiedTracks) { track ->
                    val isDownloading = downloadingTrackIds.contains(track.id)
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (track.cover_art != null) {
                                    AsyncImage(
                                        model = track.cover_art,
                                        contentDescription = "Cover Art",
                                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(track.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (track.album != null) {
                                        Text(track.album, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = {
                                    coroutineScope.launch {
                                        val url = YouTubePlayer.searchYouTube("${track.title} ${track.artist}")
                                        if (url != null) {
                                            uriHandler.openUri(url)
                                        } else {
                                            Toast.makeText(context, "Could not find on YouTube", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }) {
                                    Text("YouTube")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { viewModel.downloadIdentifiedTrack(track) },
                                    enabled = !isDownloading
                                ) {
                                    if (isDownloading) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                    } else {
                                        Text("Add to Library")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
