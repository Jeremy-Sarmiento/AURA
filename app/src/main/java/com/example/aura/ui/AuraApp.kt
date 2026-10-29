package com.example.aura.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aura.ui.components.CreatePlaylistDialog
import com.example.aura.ui.components.FileManagerSheet
import com.example.aura.ui.components.SleepTimerDialog
import com.example.aura.ui.components.SongInfoDialog
import com.example.aura.ui.components.MiniPlayerBar
import com.example.aura.ui.screens.ArtistScreen
import com.example.aura.ui.screens.HomeScreen
import com.example.aura.ui.screens.LibraryScreen
import com.example.aura.ui.screens.LyricsScreen
import com.example.aura.ui.screens.PlayerScreen
import com.example.aura.ui.screens.PlaylistScreen
import com.example.aura.ui.screens.QueueScreen
import com.example.aura.ui.screens.SearchScreen
import com.example.aura.ui.screens.SettingsScreen
import com.example.aura.ui.screens.WelcomeScreen
import com.example.ui.theme.AuraDarkBackground
import com.example.ui.theme.AuraDarkSurface
import com.example.ui.theme.AuraDarkSurfaceVariant
import com.example.ui.theme.AuraPrimaryPurple
import com.example.ui.theme.AuraTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraApp(viewModel: AuraViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val selectedArtist by viewModel.selectedArtist.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val parsedLyrics by viewModel.parsedLyrics.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val currentDirPath by viewModel.currentDirectoryPath.collectAsState()
    val directoryFiles by viewModel.directoryFiles.collectAsState()
    val hifiLosslessEnabled by viewModel.hifiLosslessEnabled.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()

    // Player state
    val playerManager = viewModel.playerManager
    val currentSong by playerManager.currentSong.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val currentPosMs by playerManager.currentPositionMs.collectAsState()
    val durationMs by playerManager.durationMs.collectAsState()
    val queue by playerManager.queue.collectAsState()
    val currentIndex by playerManager.currentIndex.collectAsState()
    val isShuffle by playerManager.isShuffle.collectAsState()
    val repeatMode by playerManager.repeatMode.collectAsState()
    val sleepTimerRemainingMs by playerManager.sleepTimerRemainingMs.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showFileManagerSheet by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSongInfoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    // Android Hardware/System Back Button Handling
    BackHandler(enabled = currentScreen != AuraScreen.HOME && currentScreen != AuraScreen.WELCOME) {
        viewModel.navigateBack()
    }

    val progress = if (durationMs > 0) (currentPosMs.toFloat() / durationMs.toFloat()) else 0f

    // Dialogs
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name, desc, color ->
                viewModel.createPlaylist(name, desc, color)
                showCreatePlaylistDialog = false
            }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            remainingMs = sleepTimerRemainingMs,
            onSetTimer = { mins -> playerManager.setSleepTimer(mins) },
            onCancelTimer = { playerManager.cancelSleepTimer() },
            onDismiss = { showSleepTimerDialog = false }
        )
    }

    if (showSongInfoDialog && currentSong != null) {
        SongInfoDialog(
            song = currentSong!!,
            onDismiss = { showSongInfoDialog = false }
        )
    }

    if (showFileManagerSheet) {
        FileManagerSheet(
            sheetState = sheetState,
            isScanning = isScanning,
            statusMessage = statusMessage,
            currentPath = currentDirPath,
            files = directoryFiles,
            onDismiss = { showFileManagerSheet = false },
            onScanMediaStore = { viewModel.scanDeviceStorage() },
            onImportUri = { uri -> viewModel.importSongFromUri(uri) },
            onImportFolder = { treeUri -> viewModel.importFolderFromTreeUri(treeUri) },
            onNavigateDir = { path -> viewModel.loadDirectory(path) },
            onPlayFileItem = { fileItem ->
                viewModel.importSongFromUri(android.net.Uri.fromFile(java.io.File(fileItem.absolutePath)))
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraDarkBackground),
        containerColor = AuraDarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Show bottom navigation & miniplayer only on main tab screens
            val isMainTab = currentScreen in listOf(
                AuraScreen.HOME,
                AuraScreen.SEARCH,
                AuraScreen.LIBRARY,
                AuraScreen.SETTINGS
            )

            if (isMainTab) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    // Floating Mini Player Bar
                    MiniPlayerBar(
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        progress = progress,
                        onTogglePlayPause = { playerManager.togglePlayPause() },
                        onNext = { playerManager.next() },
                        onClick = { viewModel.navigateTo(AuraScreen.PLAYER) }
                    )

                    // Bottom Navigation Bar matching Mockup 02
                    NavigationBar(
                        containerColor = AuraDarkSurface,
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == AuraScreen.HOME,
                            onClick = { viewModel.navigateTo(AuraScreen.HOME) },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                            label = { Text("Inicio", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor = AuraPrimaryPurple,
                                unselectedIconColor = AuraTextSecondary,
                                unselectedTextColor = AuraTextSecondary
                            )
                        )

                        NavigationBarItem(
                            selected = currentScreen == AuraScreen.SEARCH,
                            onClick = { viewModel.navigateTo(AuraScreen.SEARCH) },
                            icon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                            label = { Text("Buscar", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor = AuraPrimaryPurple,
                                unselectedIconColor = AuraTextSecondary,
                                unselectedTextColor = AuraTextSecondary
                            )
                        )

                        NavigationBarItem(
                            selected = currentScreen == AuraScreen.LIBRARY,
                            onClick = { viewModel.navigateTo(AuraScreen.LIBRARY) },
                            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Biblioteca") },
                            label = { Text("Biblioteca", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor = AuraPrimaryPurple,
                                unselectedIconColor = AuraTextSecondary,
                                unselectedTextColor = AuraTextSecondary
                            )
                        )

                        NavigationBarItem(
                            selected = currentScreen == AuraScreen.SETTINGS,
                            onClick = { viewModel.navigateTo(AuraScreen.SETTINGS) },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Ajustes") },
                            label = { Text("Ajustes", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor = AuraPrimaryPurple,
                                unselectedIconColor = AuraTextSecondary,
                                unselectedTextColor = AuraTextSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    AuraScreen.WELCOME -> {
                        WelcomeScreen(
                            onStartClick = { viewModel.navigateTo(AuraScreen.HOME) },
                            onRegisterClick = { viewModel.navigateTo(AuraScreen.HOME) }
                        )
                    }

                    AuraScreen.HOME -> {
                        HomeScreen(
                            songs = songs,
                            playlists = playlists,
                            onSongClick = { song -> playerManager.playSong(song, songs) },
                            onPlaylistClick = { pl -> viewModel.openPlaylist(pl) },
                            onOpenFileExplorer = { showFileManagerSheet = true }
                        )
                    }

                    AuraScreen.SEARCH -> {
                        SearchScreen(
                            query = searchQuery,
                            selectedGenre = selectedGenre,
                            results = searchResults,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            onSelectGenre = { viewModel.selectGenre(it) },
                            onSongClick = { song -> playerManager.playSong(song, searchResults) }
                        )
                    }

                    AuraScreen.LIBRARY -> {
                        LibraryScreen(
                            songs = songs,
                            playlists = playlists,
                            onSongClick = { song -> playerManager.playSong(song, songs) },
                            onPlaylistClick = { pl -> viewModel.openPlaylist(pl) },
                            onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                            onAddSongToPlaylist = { plId, sId -> viewModel.addSongToPlaylist(plId, sId) },
                            onCreatePlaylistClick = { showCreatePlaylistDialog = true },
                            onOpenFileExplorer = { showFileManagerSheet = true }
                        )
                    }

                    AuraScreen.PLAYLIST_DETAIL -> {
                        val playlist = selectedPlaylist
                        if (playlist != null) {
                            val playlistSongs = when (playlist.id) {
                                "playlist_favorites" -> songs.filter { it.isFavorite }
                                "playlist_lossless" -> songs.filter { it.isLossless }
                                else -> songs.filter { playlist.songIds.contains(it.id) }
                            }
                            PlaylistScreen(
                                playlist = playlist,
                                songs = playlistSongs,
                                onBack = { viewModel.navigateBack() },
                                onPlayAll = {
                                    if (playlistSongs.isNotEmpty()) {
                                        playerManager.playSong(playlistSongs.first(), playlistSongs)
                                    }
                                },
                                onSongClick = { song ->
                                    playerManager.playSong(song, playlistSongs)
                                },
                                onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                                onDeletePlaylist = if (playlist.isCustom && playlist.id != "playlist_lossless" && playlist.id != "playlist_favorites") {
                                    { viewModel.deletePlaylist(playlist.id) }
                                } else null
                            )
                        } else {
                            viewModel.navigateTo(AuraScreen.HOME)
                        }
                    }

                    AuraScreen.ARTIST_DETAIL -> {
                        val artist = selectedArtist
                        if (artist != null) {
                            val artistSongs = songs.filter { it.artist.equals(artist.name, ignoreCase = true) }
                            ArtistScreen(
                                artist = artist,
                                songs = artistSongs.ifEmpty { songs.take(5) },
                                onBack = { viewModel.navigateBack() },
                                onFollowClick = { viewModel.toggleFollowArtist() },
                                onSongClick = { song -> playerManager.playSong(song, artistSongs) }
                            )
                        } else {
                            viewModel.navigateTo(AuraScreen.HOME)
                        }
                    }

                    AuraScreen.PLAYER -> {
                        PlayerScreen(
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            currentPositionMs = currentPosMs,
                            durationMs = durationMs,
                            isShuffle = isShuffle,
                            repeatMode = repeatMode,
                            sleepTimerRemainingMs = sleepTimerRemainingMs,
                            onBack = { viewModel.navigateBack() },
                            onTogglePlayPause = { playerManager.togglePlayPause() },
                            onSeekTo = { playerManager.seekTo(it) },
                            onNext = { playerManager.next() },
                            onPrevious = { playerManager.previous() },
                            onToggleShuffle = { playerManager.toggleShuffle() },
                            onToggleRepeat = { playerManager.toggleRepeat() },
                            onOpenLyrics = { viewModel.navigateTo(AuraScreen.LYRICS) },
                            onOpenQueue = { viewModel.navigateTo(AuraScreen.QUEUE) },
                            onOpenSleepTimer = { showSleepTimerDialog = true },
                            onOpenSongInfo = { showSongInfoDialog = true }
                        )
                    }

                    AuraScreen.LYRICS -> {
                        LyricsScreen(
                            currentSong = currentSong,
                            lyrics = parsedLyrics,
                            currentPositionMs = currentPosMs,
                            onBack = { viewModel.navigateBack() },
                            onSeekTo = { playerManager.seekTo(it) }
                        )
                    }

                    AuraScreen.QUEUE -> {
                        QueueScreen(
                            currentSong = currentSong,
                            queue = queue,
                            currentIndex = currentIndex,
                            onBack = { viewModel.navigateBack() },
                            onSongClick = { song -> playerManager.playSong(song, queue) },
                            onRemoveFromQueue = { idx -> playerManager.removeFromQueue(idx) }
                        )
                    }

                    AuraScreen.SETTINGS -> {
                        SettingsScreen(
                            hifiLosslessEnabled = hifiLosslessEnabled,
                            notificationsEnabled = notificationsEnabled,
                            onToggleHiFi = { viewModel.toggleHiFiLossless(it) },
                            onToggleNotifications = { viewModel.toggleNotifications(it) },
                            onOpenFileExplorer = { showFileManagerSheet = true }
                        )
                    }
                }
            }
        }
    }
}
