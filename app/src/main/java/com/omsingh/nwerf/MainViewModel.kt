package com.omsingh.nwerf

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val trackDao = db.trackDao()
    val settingsStore = SettingsStore(application)

    // Tracks List Flow
    val tracks: StateFlow<List<Track>> = trackDao.getAllTracksFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Player States
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _currentTime = MutableStateFlow(0L)
    val currentTime: StateFlow<Long> = _currentTime.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private var controller: MediaController? = null

    // UI Progress States
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        initializePlayer(application)
        startProgressPoller()
    }

    private fun initializePlayer(application: Application) {
        val sessionToken = SessionToken(application, ComponentName(application, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()
        controllerFuture.addListener({
            controller = controllerFuture.get()
            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                    _isPlaying.value = isPlayingChanged
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    _duration.value = controller?.duration ?: 0L
                    if (playbackState == Player.STATE_ENDED) {
                        playNext()
                    }
                }
            })
        }, ContextCompat.getMainExecutor(application))
    }

    private fun startProgressPoller() {
        viewModelScope.launch {
            while (true) {
                controller?.let {
                    if (it.isPlaying) {
                        _currentTime.value = it.currentPosition
                        _duration.value = it.duration
                    }
                }
                kotlinx.coroutines.delay(500)
            }
        }
    }

    fun playTrack(track: Track) {
        val activeController = controller ?: return
        viewModelScope.launch {
            try {
                val token = settingsStore.botToken.first() ?: throw Exception("Missing Telegram Bot Token")
                val chatId = settingsStore.chatId.first() ?: throw Exception("Missing Telegram Chat ID")
                val client = TelegramClient(token, chatId)
                
                // Fetch dynamic streaming URL (prevents URL expirations)
                val streamUrl = client.getStreamUrl(track.file_id)

                val mediaMetadataBuilder = MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle("Nwerf")
                
                if (track.cover_art != null) {
                    mediaMetadataBuilder.setArtworkUri(Uri.parse(track.cover_art))
                }

                val mediaMetadata = mediaMetadataBuilder.build()

                val mediaItem = MediaItem.Builder()
                    .setMediaId(track.id)
                    .setUri(Uri.parse(streamUrl))
                    .setMediaMetadata(mediaMetadata)
                    .build()

                activeController.setMediaItem(mediaItem)
                activeController.prepare()
                activeController.play()
                _currentTrack.value = track

            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to play audio"
            }
        }
    }

    fun togglePlay() {
        val player = controller ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun playNext() {
        val current = _currentTrack.value ?: return
        val list = tracks.value
        val index = list.indexOf(current)
        if (index != -1 && index < list.size - 1) {
            playTrack(list[index + 1])
        }
    }

    fun playPrevious() {
        val current = _currentTrack.value ?: return
        val list = tracks.value
        val index = list.indexOf(current)
        if (index > 0) {
            playTrack(list[index - 1])
        }
    }

    fun seekTo(position: Long) {
        controller?.seekTo(position)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun syncLibrary() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val pat = settingsStore.githubPat.first() ?: throw Exception("Missing GitHub PAT")
                val gistId = settingsStore.gistId.first()
                val client = GithubClient(pat)

                if (gistId.isNullOrEmpty()) {
                    // Create new gist backup
                    val newGistId = client.createLibraryGist()
                    settingsStore.saveGistSettings(pat, newGistId)
                    // Push local tracks
                    client.updateLibraryGist(newGistId, tracks.value)
                } else {
                    // Merge local and remote
                    val remoteTracks = client.fetchLibraryTracks(gistId)
                    val localTracks = tracks.value

                    // Merge strategy: unique by ID, Union sorted by added_at
                    val merged = (localTracks + remoteTracks).distinctBy { it.id }.sortedByDescending { it.added_at }
                    trackDao.deleteAll()
                    trackDao.insertAll(merged)
                    client.updateLibraryGist(gistId, merged)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Sync failed"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun createGist(pat: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val client = GithubClient(pat)
                val newGistId = client.createLibraryGist()
                settingsStore.saveGistSettings(pat, newGistId)
                client.updateLibraryGist(newGistId, tracks.value)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to create Gist"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun uploadTrack(file: File, title: String, artist: String) {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                val token = settingsStore.botToken.first() ?: throw Exception("Missing Bot Token")
                val chatId = settingsStore.chatId.first() ?: throw Exception("Missing Chat ID")
                val pat = settingsStore.githubPat.first()
                val gistId = settingsStore.gistId.first()

                val tg = TelegramClient(token, chatId)
                val fileId = tg.uploadAudio(file, title, artist)

                val newTrack = Track(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    artist = artist,
                    file_id = fileId,
                    added_at = System.currentTimeMillis()
                )

                // Save locally
                trackDao.insert(newTrack)

                // Sync to GitHub if configured
                if (!pat.isNullOrEmpty() && !gistId.isNullOrEmpty()) {
                    val gh = GithubClient(pat)
                    val updatedTracks = trackDao.getAllTracks()
                    gh.updateLibraryGist(gistId, updatedTracks)
                }

            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Upload failed"
            } finally {
                _isUploading.value = false
                try {
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private val _identifiedTracks = MutableStateFlow<List<Track>>(emptyList())
    val identifiedTracks: StateFlow<List<Track>> = _identifiedTracks.asStateFlow()

    private val _downloadingTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingTrackIds: StateFlow<Set<String>> = _downloadingTrackIds.asStateFlow()

    fun clearIdentifiedTracks() {
        _identifiedTracks.value = emptyList()
    }

    fun identifyTrack(file: File) {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                // Run network call on IO dispatcher
                val track = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val aura = AuraClient("dev_api_key_123")
                    aura.identify(file)
                } ?: throw Exception("Identification returned null")

                _identifiedTracks.value = listOf(track) + _identifiedTracks.value
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Identify failed"
            } finally {
                _isUploading.value = false
                try {
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun downloadIdentifiedTrack(track: Track) {
        viewModelScope.launch {
            _downloadingTrackIds.value = _downloadingTrackIds.value + track.id
            try {
                val token = settingsStore.botToken.first() ?: throw Exception("Missing Bot Token")
                val chatId = settingsStore.chatId.first() ?: throw Exception("Missing Chat ID")
                val pat = settingsStore.githubPat.first()
                val gistId = settingsStore.gistId.first()

                // Run network call on IO dispatcher
                val fileId = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val aura = AuraClient("dev_api_key_123")
                    aura.downloadAndUpload(track.title, track.artist, token, chatId)
                }
                
                val finalTrack = track.copy(file_id = fileId)

                // Save locally
                trackDao.insert(finalTrack)

                // Sync to GitHub if configured
                if (!pat.isNullOrEmpty() && !gistId.isNullOrEmpty()) {
                    val gh = GithubClient(pat)
                    val updatedTracks = trackDao.getAllTracks()
                    gh.updateLibraryGist(gistId, updatedTracks)
                }
                
                _identifiedTracks.value = _identifiedTracks.value.filter { it.id != track.id }

            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Download failed"
            } finally {
                _downloadingTrackIds.value = _downloadingTrackIds.value - track.id
            }
        }
    }

    fun deleteTrack(track: Track) {
        viewModelScope.launch {
            try {
                trackDao.delete(track)
                val pat = settingsStore.githubPat.first()
                val gistId = settingsStore.gistId.first()
                if (!pat.isNullOrEmpty() && !gistId.isNullOrEmpty()) {
                    val gh = GithubClient(pat)
                    val updatedTracks = trackDao.getAllTracks()
                    gh.updateLibraryGist(gistId, updatedTracks)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to delete track"
            }
        }
    }
}
