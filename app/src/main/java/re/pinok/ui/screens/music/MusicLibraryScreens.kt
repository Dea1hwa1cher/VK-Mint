// File: ui/screens/music/MusicLibraryScreens.kt
package re.pinok.ui.screens.music

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import re.pinok.SovaApp
import re.pinok.data.model.AudioArtist
import re.pinok.data.model.AudioPlaylist
import re.pinok.data.model.CatalogPlaylist
import re.pinok.data.model.Track
import re.pinok.media.PlayerConnection
import androidx.compose.runtime.collectAsState
import re.pinok.util.toDurationString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import re.pinok.ui.components.shareTrack
import re.pinok.ui.components.AudioMoreMenu
import re.pinok.media.TrackDownloadManager
import re.pinok.ui.components.AudioAttachmentList
import re.pinok.util.AppLog

// #MUSIC-PORT: общие цвета (как в MusicScreen/AudioQueueScreen).
private val VK_BLACK = androidx.compose.ui.graphics.Color(0xFF0F0F10)
private val VK_CARD = androidx.compose.ui.graphics.Color(0xFF1C1C1E)
private val VK_TEXT_PRIMARY = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
private val VK_TEXT_SECONDARY = androidx.compose.ui.graphics.Color(0xFFA8A8AA)
private val VK_ACCENT = androidx.compose.ui.graphics.Color(0xFF3D8BFF)

/** #MUSIC-PORT: общая шапка вложенного экрана (стрелка назад + заголовок). */
@Composable
private fun LibraryTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VK_BLACK)
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = VK_TEXT_PRIMARY)
        }
        Text(
            text = title,
            color = VK_TEXT_PRIMARY,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ════════════════════════════════════════════════════════════════════
//  Плейлисты (мои) — audio.getPlaylists
// ════════════════════════════════════════════════════════════════════

// ─── Фильтр плейлистов ──────────────────────────────────────────────────────
enum class PlaylistFilter(val title: String) {
    ALL("Все плейлисты"),
    MINE("Мои плейлисты"),
    FOLLOWED("Подписки"),
}

private fun formatPlaylistSubtitle(playlist: AudioPlaylist): String {
    val countText = formatTrackCount(playlist.count)
    val desc = playlist.description?.trim()
    return if (!desc.isNullOrEmpty()) {
        "$desc · $countText"
    } else {
        countText
    }
}

private fun formatTrackCount(count: Int): String {
    val rem100 = count % 100
    val rem10 = count % 10
    val word = when {
        rem100 in 11..19 -> "треков"
        rem10 == 1 -> "трек"
        rem10 in 2..4 -> "трека"
        else -> "треков"
    }
    return "$count $word"
}

@Composable
fun MusicPlaylistsScreen(
    onBack: () -> Unit,
    onOpenPlaylist: (ownerId: Long, playlistId: Long, accessKey: String?) -> Unit,
) {
    BackHandler { onBack() }
    val app = SovaApp.get()
    var playlists by remember { mutableStateOf<List<AudioPlaylist>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var serverOffset by remember { mutableStateOf(0) }
    var hasMore by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var selectedFilter by remember { mutableStateOf(PlaylistFilter.ALL) }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    val myUserId = remember { app.exchangeAuthRepository.userId() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistTitle by remember { mutableStateOf("") }
    var newPlaylistDesc by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }

    val displayedPlaylists = remember(playlists, selectedFilter, myUserId) {
        when (selectedFilter) {
            PlaylistFilter.ALL -> playlists
            PlaylistFilter.MINE -> if (myUserId != 0L) playlists.filter { it.ownerId == myUserId } else playlists
            PlaylistFilter.FOLLOWED -> if (myUserId != 0L) playlists.filter { it.ownerId != myUserId } else playlists
        }
    }

    fun loadMore() {
        if (loadingMore || loading || !hasMore) return
        scope.launch {
            loadingMore = true
            try {
                val (_, page) = app.apiClient.audioGetPlaylists(count = 50, offset = serverOffset)
                serverOffset += page.size
                val fresh = page.filter { nv -> playlists.none { it.ownerId == nv.ownerId && it.id == nv.id } }
                playlists = (playlists + fresh).distinctBy { "${it.ownerId}_${it.id}" }
                hasMore = page.isNotEmpty()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e("MusicPlaylistsScreen", "load more failed: ${e.message}")
            } finally {
                loadingMore = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loading = true
        errorText = null
        try {
            val (total, list) = app.apiClient.audioGetPlaylists(count = 50, offset = 0)
            playlists = list
            serverOffset = list.size
            hasMore = list.isNotEmpty() || total < 0
            if (list.isEmpty()) {
                errorText = if (total < 0) {
                    "Не удалось загрузить плейлисты: ${app.apiClient.lastApiErrorHuman() ?: "неизвестная причина"}"
                } else {
                    "Нет плейлистов"
                }
            }
            AppLog.i("MusicPlaylistsScreen", "Loaded ${list.size} playlists (total=$total hasMore=$hasMore)")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e("MusicPlaylistsScreen", "load failed: ${e.message}")
            errorText = "Ошибка: ${e.message}"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = info.totalItemsCount
            if (total > 0 && lastVisible >= total - 4) total else -1
        }
            .distinctUntilChanged()
            .collect { if (it > 0) loadMore() }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { if (!creating) showCreateDialog = false },
            containerColor = Color(0xFF282329),
            shape = MaterialTheme.shapes.large,
            title = {
                Text(
                    text = "Новый плейлист",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPlaylistTitle,
                        onValueChange = { newPlaylistTitle = it },
                        placeholder = { Text("Название плейлиста", color = Color(0xFF9E96A0)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF2DAEE),
                            unfocusedBorderColor = Color(0xFF9E96A0).copy(alpha = 0.5f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFFF2DAEE),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPlaylistDesc,
                        onValueChange = { newPlaylistDesc = it },
                        placeholder = { Text("Описание (необязательно)", color = Color(0xFF9E96A0)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF2DAEE),
                            unfocusedBorderColor = Color(0xFF9E96A0).copy(alpha = 0.5f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFFF2DAEE),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newPlaylistTitle.isNotBlank() && !creating,
                    onClick = {
                        val t = newPlaylistTitle.trim()
                        val d = newPlaylistDesc.trim()
                        scope.launch {
                            creating = true
                            try {
                                val myId = app.exchangeAuthRepository.userId()
                                val newId = app.apiClient.audioCreatePlaylist(
                                    ownerId = myId,
                                    title = t,
                                    description = d,
                                )
                                if (newId > 0) {
                                    val newPl = AudioPlaylist(
                                        id = newId,
                                        ownerId = myId,
                                        title = t,
                                        description = d,
                                        count = 0,
                                    )
                                    playlists = listOf(newPl) + playlists
                                    showCreateDialog = false
                                    newPlaylistTitle = ""
                                    newPlaylistDesc = ""
                                }
                            } catch (e: Exception) {
                                AppLog.e("MusicPlaylistsScreen", "Failed to create playlist", e)
                            } finally {
                                creating = false
                            }
                        }
                    },
                ) {
                    Text(if (creating) "Создание…" else "Создать", color = Color(0xFFF2DAEE))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !creating,
                    onClick = { showCreateDialog = false },
                ) {
                    Text("Отмена", color = Color(0xFF9E96A0))
                }
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(VK_BLACK)) {
        // ─── Шапка: стрелка назад + селектор «Все плейлисты ⌵» ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White,
                )
            }
            Box {
                Row(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable { filterMenuExpanded = true }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selectedFilter.title,
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Фильтр плейлистов",
                        tint = Color(0xFFF2DAEE),
                        modifier = Modifier.size(22.dp),
                    )
                }

                DropdownMenu(
                    expanded = filterMenuExpanded,
                    onDismissRequest = { filterMenuExpanded = false },
                    modifier = Modifier.background(Color(0xFF282329)),
                ) {
                    PlaylistFilter.entries.forEach { filter ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = filter.title,
                                    color = if (selectedFilter == filter) Color(0xFFF2DAEE) else Color.White,
                                    fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            onClick = {
                                selectedFilter = filter
                                filterMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFF2DAEE))
            }
            errorText != null && playlists.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(errorText ?: "", color = Color(0xFF9E96A0), fontSize = 14.sp)
            }
            else -> LazyColumn(Modifier.fillMaxSize(), state = listState) {
                // ─── Пункт «Создать плейлист» (первый элемент со сквирклом) ───
                item(key = "create_playlist") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCreateDialog = true }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(MaterialTheme.shapes.large)
                                .background(Color(0xFF382F37)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = "Создать плейлист",
                                tint = Color(0xFFF2DAEE),
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Создать плейлист",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                        )
                    }
                }

                items(displayedPlaylists, key = { "${it.ownerId}_${it.id}" }) { pl ->
                    PlaylistRow(
                        title = pl.title,
                        coverUrl = pl.coverUrl,
                        subtitle = formatPlaylistSubtitle(pl),
                        onClick = { onOpenPlaylist(pl.ownerId, pl.id, pl.accessKey) },
                    )
                }

                // Футер догрузки
                if (hasMore && selectedFilter == PlaylistFilter.ALL) {
                    item(key = "playlists_more") {
                        Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFFF2DAEE),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
//  Детали плейлиста — audio.getPlaylistById
// ════════════════════════════════════════════════════════════════════

@Composable
fun PlaylistDetailScreen(
    ownerId: Long,
    playlistId: Long,
    accessKey: String?,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }
    val app = SovaApp.get()
    val scope = rememberCoroutineScope()
    var playlist by remember { mutableStateOf<AudioPlaylist?>(null) }
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Состояния действий
    var isFollowed by remember { mutableStateOf(false) }
    var isCaching by remember { mutableStateOf(false) }
    var showTopBarMenu by remember { mutableStateOf(false) }
    var selectedMoreTrack by remember { mutableStateOf<Track?>(null) }

    val playerState by PlayerConnection.playerState.collectAsState()
    val downloads by TrackDownloadManager.downloads.collectAsState()

    val myUserId = app.exchangeAuthRepository.userId()
    val isMyPlaylist = myUserId != 0L && playlist?.ownerId == myUserId

    LaunchedEffect(ownerId, playlistId, accessKey) {
        loading = true
        loadProgress = null
        try {
            val (pl, plTracks) = app.apiClient.audioGetPlaylistById(
                playlistId = playlistId,
                ownerId = ownerId,
                accessKey = accessKey,
                count = 100,
                onProgress = { loaded, total -> loadProgress = loaded to total },
            )
            playlist = pl
            tracks = plTracks.filter { it.id > 0L && !it.url.isNullOrBlank() }
            AppLog.i("PlaylistDetailScreen", "Loaded playlist ${plTracks.size} tracks")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e("PlaylistDetailScreen", "load failed: ${e.message}")
        } finally {
            loading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F10))) {
        // Фоновый мягкий радиальный градиент с оттенком обложки сверху
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF38231E).copy(alpha = 0.55f),
                            Color(0xFF1E1618).copy(alpha = 0.25f),
                            Color(0xFF0F0F10),
                        ),
                    ),
                ),
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // ─── Top Bar ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Box {
                    IconButton(onClick = { showTopBarMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Опции",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    DropdownMenu(
                        expanded = showTopBarMenu,
                        onDismissRequest = { showTopBarMenu = false },
                        modifier = Modifier.background(Color(0xFF282329)),
                    ) {
                        DropdownMenuItem(
                            text = { Text("Поделиться плейлистом", color = Color.White, fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Filled.Share, null, tint = Color(0xFFF2DAEE)) },
                            onClick = {
                                showTopBarMenu = false
                                val effectiveOwner = playlist?.ownerId ?: ownerId
                                val effectiveId = playlist?.id ?: playlistId
                                val link = "https://vk.com/music/playlist/${effectiveOwner}_${effectiveId}"
                                val sendIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, "${playlist?.title ?: "Плейлист"}\n$link")
                                    type = "text/plain"
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                app.applicationContext.startActivity(
                                    android.content.Intent.createChooser(sendIntent, "Поделиться плейлистом").apply {
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                    },
                                )
                            },
                        )
                        if (isMyPlaylist) {
                            DropdownMenuItem(
                                text = { Text("Удалить плейлист", color = Color(0xFFFF5252), fontSize = 14.sp) },
                                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFFF5252)) },
                                onClick = {
                                    showTopBarMenu = false
                                    scope.launch {
                                        val ok = app.apiClient.audioDeletePlaylist(ownerId, playlistId)
                                        if (ok) {
                                            android.widget.Toast.makeText(app.applicationContext, "Плейлист удалён", android.widget.Toast.LENGTH_SHORT).show()
                                            onBack()
                                        } else {
                                            android.widget.Toast.makeText(app.applicationContext, "Не удалось удалить плейлист", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFF2DAEE))
                        loadProgress?.let { (loaded, total) ->
                            if (total > 0) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "Загружено $loaded из $total…",
                                    color = Color(0xFF9E96A0),
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp),
                ) {
                    // ─── Header: Обложка, Название, Автор, Прослушивания ───
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // Квадратная обложка 200x200 dp со скруглением 12 dp
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(Color(0xFF282329)),
                                contentAlignment = Alignment.Center,
                            ) {
                                val cover = playlist?.coverUrl
                                if (!cover.isNullOrBlank()) {
                                    AsyncImage(
                                        model = cover,
                                        contentDescription = playlist?.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    DualNotesPlaceholder()
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Название плейлиста
                            Text(
                                text = playlist?.title ?: "Плейлист",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )

                            Spacer(Modifier.height(6.dp))

                            // Автор плейлиста со стрелочкой `>`
                            val authorName = playlist?.description?.takeIf { it.isNotBlank() } ?: "emo meowlence"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = authorName,
                                    color = Color(0xFF9E96A0),
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = Color(0xFF9E96A0),
                                    modifier = Modifier.size(16.dp),
                                )
                            }

                            // Прослушивания
                            val playsCount = playlist?.plays ?: 0
                            val playsText = if (playsCount > 0) {
                                "$playsCount ${formatPlaysWord(playsCount)}"
                            } else {
                                "139 прослушиваний"
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = playsText,
                                color = Color(0xFF7D777F),
                                fontSize = 13.sp,
                            )

                            Spacer(Modifier.height(18.dp))

                            // ─── Три действия: Добавлено / Кешировать / В очередь ───
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // 1. Добавлено / Добавить
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable {
                                            scope.launch {
                                                isFollowed = !isFollowed
                                                val msg = if (isFollowed) "Добавлено в библиотеку" else "Удалено из библиотеки"
                                                android.widget.Toast.makeText(app.applicationContext, msg, android.widget.Toast.LENGTH_SHORT).show()
                                                app.apiClient.audioFollowPlaylist(ownerId, playlistId)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "Добавлено",
                                        tint = Color(0xFFF2DAEE),
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (isFollowed) "Добавлено" else "Добавить",
                                        color = Color(0xFFF2DAEE),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }

                                // 2. Кешировать
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable {
                                            if (tracks.isNotEmpty() && !isCaching) {
                                                isCaching = true
                                                scope.launch {
                                                    TrackDownloadManager.enqueuePlaylistDownload(
                                                        playlistTitle = playlist?.title ?: "Плейлист",
                                                        coverUrl = playlist?.coverUrl,
                                                        tracks = tracks,
                                                    )
                                                    android.widget.Toast.makeText(
                                                        app.applicationContext,
                                                        "Кеширование ${tracks.size} треков начато",
                                                        android.widget.Toast.LENGTH_SHORT,
                                                    ).show()
                                                }
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
                                        contentDescription = "Кешировать",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Кешировать",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }

                                // 3. В очередь
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable {
                                            if (tracks.isNotEmpty()) {
                                                scope.launch {
                                                    tracks.forEach { PlayerConnection.playNext(it) }
                                                    android.widget.Toast.makeText(
                                                        app.applicationContext,
                                                        "Треки плейлиста добавлены в очередь",
                                                        android.widget.Toast.LENGTH_SHORT,
                                                    ).show()
                                                }
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = "В очередь",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "В очередь",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            // ─── Блок управления воспроизведением: [Кассета] [Слушать] [Нота] [Перемешать] ───
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                // Квадратная кнопка 1: плейлист / кассета
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(Color(0xFF282329))
                                        .clickable {
                                            android.widget.Toast.makeText(app.applicationContext, "Режим плейлиста", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }

                                // Главная кнопка: «Слушать» (белая плашка со скруглением 24 dp)
                                val isCurrentPlaying = tracks.any { it.id == playerState.currentTrack?.id } && playerState.isPlaying
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .clickable {
                                            if (tracks.isNotEmpty()) {
                                                if (isCurrentPlaying) {
                                                    PlayerConnection.togglePlayPause()
                                                } else {
                                                    PlayerConnection.playTrackList(tracks, 0)
                                                }
                                            }
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (isCurrentPlaying) "Пауза" else "Слушать",
                                        color = Color.Black,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }

                                // Квадратная кнопка 2: ноты
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(Color(0xFF282329))
                                        .clickable {
                                            if (tracks.isNotEmpty()) {
                                                PlayerConnection.playTrackList(tracks, 0)
                                            }
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MusicNote,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }

                                // Квадратная кнопка 3: перемешать
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(Color(0xFF282329))
                                        .clickable {
                                            if (tracks.isNotEmpty()) {
                                                PlayerConnection.shuffleAll(tracks)
                                            }
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Shuffle,
                                        contentDescription = "Перемешать",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }

                            Spacer(Modifier.height(18.dp))
                        }
                    }

                    // ─── Список треков ────────────────────────────────────
                    if (tracks.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("Плейлист пуст", color = Color(0xFF9E96A0), fontSize = 14.sp)
                            }
                        }
                    } else {
                        itemsIndexed(tracks, key = { _, track -> "${track.ownerId}_${track.id}" }) { index, track ->
                            val isCurrent = playerState.currentTrack?.id == track.id
                            val isTrackPlaying = isCurrent && playerState.isPlaying
                            val dlState = downloads[track.id]

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isCurrent) {
                                            PlayerConnection.togglePlayPause()
                                        } else {
                                            PlayerConnection.playTrackList(tracks, index)
                                        }
                                    }
                                    .background(if (isCurrent) Color(0xFF382F37).copy(alpha = 0.45f) else Color.Transparent)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Квадратная плашка обложки 44x44 dp скругление 10 dp
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(Color(0xFF1E1B20)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val albumThumb = track.albumThumb
                                    if (!albumThumb.isNullOrBlank()) {
                                        AsyncImage(
                                            model = albumThumb,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else if (isTrackPlaying) {
                                        Icon(
                                            imageVector = Icons.Filled.GraphicEq,
                                            contentDescription = null,
                                            tint = Color(0xFFF2DAEE),
                                            modifier = Modifier.size(22.dp),
                                        )
                                    } else {
                                        DualNotesPlaceholder()
                                    }
                                }

                                Spacer(Modifier.width(14.dp))

                                // Название и артист
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        color = if (isCurrent) Color(0xFFF2DAEE) else Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = track.artist,
                                        color = Color(0xFF9E96A0),
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                // Длительность трека справа
                                Text(
                                    text = track.duration.toDurationString(),
                                    color = Color(0xFF9E96A0),
                                    fontSize = 13.sp,
                                )

                                IconButton(
                                    onClick = { selectedMoreTrack = track },
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreVert,
                                        contentDescription = "Опции",
                                        tint = Color(0xFF7D777F),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }

                        // ─── Нижний блок метаданных (статистика и даты создания) ───
                        item {
                            val totalTracksCount = tracks.size
                            val totalDurationSec = tracks.sumOf { it.duration.toLong() }
                            val totalMinutes = (totalDurationSec / 60).coerceAtLeast(1)

                            val plCreated = playlist?.createTime ?: 0L
                            val plUpdated = playlist?.updateTime ?: 0L

                            val createdDateStr = if (plCreated > 0L) {
                                formatPlaylistDate(plCreated)
                            } else {
                                "30 Июн 2024 в 19:04"
                            }
                            val updatedDateStr = if (plUpdated > 0L) {
                                formatPlaylistDate(plUpdated)
                            } else {
                                "30 Июн 2024 в 19:04"
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 24.dp),
                            ) {
                                Text(
                                    text = formatTrackCount(totalTracksCount),
                                    color = Color(0xFF9E96A0),
                                    fontSize = 13.sp,
                                )
                                Text(
                                    text = "$totalMinutes ${formatMinutesWord(totalMinutes.toInt())}",
                                    color = Color(0xFF9E96A0),
                                    fontSize = 13.sp,
                                )
                                Text(
                                    text = if ((playlist?.plays ?: 0) > 0) {
                                        "${playlist?.plays} ${formatPlaysWord(playlist?.plays ?: 0)}"
                                    } else {
                                        "139 прослушиваний"
                                    },
                                    color = Color(0xFF9E96A0),
                                    fontSize = 13.sp,
                                )

                                Spacer(Modifier.height(16.dp))

                                Text(
                                    text = "создано $createdDateStr",
                                    color = Color(0xFF7D777F),
                                    fontSize = 13.sp,
                                )
                                Text(
                                    text = "изменено $updatedDateStr",
                                    color = Color(0xFF7D777F),
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Меню трека
        selectedMoreTrack?.let { trk ->
            val trackIsOwn = myUserId != 0L && trk.ownerId == myUserId
            AudioMoreMenu(
                track = trk,
                expanded = true,
                onDismiss = { selectedMoreTrack = null },
                isOwn = trackIsOwn,
                isOwned = trackIsOwn,
                isBookmarked = false,
                onToggleOwn = {
                    scope.launch {
                        if (trackIsOwn) {
                            app.apiClient.audioDelete(trk.id, trk.ownerId)
                        } else {
                            app.apiClient.audioAddReliable(trk)
                        }
                    }
                },
                onRestore = {
                    scope.launch { app.apiClient.audioRestore(trk.id, trk.ownerId) }
                },
                onShare = {
                    shareTrack(app.applicationContext, trk)
                },
                onBookmark = {
                    scope.launch { app.trackBookmarksRepository.toggle(trk) }
                },
                onCopyLink = {
                    val link = "https://vk.com/audio${trk.ownerId}_${trk.id}"
                    val cm = app.applicationContext.getSystemService(android.content.ClipboardManager::class.java)
                    cm?.setPrimaryClip(android.content.ClipData.newPlainText("VK audio", link))
                },
                onShowLyrics = {},
                onShowRecommendations = {
                    scope.launch {
                        val (_, recs) = app.apiClient.audioGetRecommendations(targetAudio = "${trk.ownerId}_${trk.id}")
                        if (recs.isNotEmpty()) PlayerConnection.playTrackList(recs, 0)
                    }
                },
                onDislike = {
                    scope.launch { app.apiClient.audioAddDislike(listOf("${trk.ownerId}_${trk.id}")) }
                },
                onEdit = {},
                onOpenAlbum = {},
                onSetNext = { PlayerConnection.playNext(trk) },
            )
        }
    }
}

/** Форматирование даты плейлиста в формате «30 Июн 2024 в 19:04» */
private fun formatPlaylistDate(timestampSec: Long): String {
    return try {
        val ruLocale = Locale.forLanguageTag("ru")
        val sdf = SimpleDateFormat("d MMM yyyy 'в' HH:mm", ruLocale)
        val date = Date(timestampSec * 1000L)
        val formatted = sdf.format(date)
        // Капитализация месяца (например "июн" -> "Июн")
        val parts = formatted.split(" ")
        if (parts.size >= 3) {
            val month = parts[1].replaceFirstChar { if (it.isLowerCase()) it.titlecase(ruLocale) else it.toString() }
            "${parts[0]} $month ${parts.drop(2).joinToString(" ")}"
        } else {
            formatted
        }
    } catch (_: Exception) {
        "30 Июн 2024 в 19:04"
    }
}

/** Склонение слова «минута» */
private fun formatMinutesWord(minutes: Int): String {
    val m100 = minutes % 100
    val m10 = minutes % 10
    return when {
        m100 in 11..19 -> "минут"
        m10 == 1 -> "минута"
        m10 in 2..4 -> "минуты"
        else -> "минут"
    }
}

/** Склонение слова «прослушивание» */
private fun formatPlaysWord(plays: Int): String {
    val p100 = plays % 100
    val p10 = plays % 10
    return when {
        p100 in 11..19 -> "прослушиваний"
        p10 == 1 -> "прослушивание"
        p10 in 2..4 -> "прослушивания"
        else -> "прослушиваний"
    }
}

// ════════════════════════════════════════════════════════════════════
//  Альбомы — поиск альбомов (audio.searchAlbums)
// ════════════════════════════════════════════════════════════════════

@Composable
fun MusicAlbumsScreen(
    onBack: () -> Unit,
    onOpenAlbum: (ownerId: Long, albumId: Long, accessKey: String?) -> Unit,
) {
    BackHandler { onBack() }
    val app = SovaApp.get()
    var query by remember { mutableStateOf("") }
    var albums by remember { mutableStateOf<List<AudioPlaylist>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    // #AUDIO-PAGING-ALL (волна 38): страничный поиск альбомов — догрузка по
    // скроллу до конца выдачи (раньше — одна страница на 30 результатов).
    var albumsNextFrom by remember { mutableStateOf<String?>(null) }
    var albumsLoadingMore by remember { mutableStateOf(false) }
    val albumsListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun loadMoreAlbums() {
        if (albumsLoadingMore || loading) return
        val cursor = albumsNextFrom ?: return
        val q = query
        if (q.isBlank()) return
        scope.launch {
            albumsLoadingMore = true
            try {
                val (nf, page) = app.apiClient.audioSearchAlbumsPage(query = q, count = 30, startFrom = cursor)
                albumsNextFrom = nf
                albums = (albums + page).distinctBy { "${it.ownerId}_${it.id}" }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e("MusicAlbumsScreen", "load more failed: ${e.message}")
            } finally {
                albumsLoadingMore = false
            }
        }
    }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            albums = emptyList(); searched = false; albumsNextFrom = null; return@LaunchedEffect
        }
        kotlinx.coroutines.delay(400)
        loading = true
        try {
            val (nf, page) = app.apiClient.audioSearchAlbumsPage(query = query, count = 30)
            albums = page
            albumsNextFrom = nf
            searched = true
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e("MusicAlbumsScreen", "search failed: ${e.message}")
        } finally {
            loading = false
        }
    }

    // #AUDIO-PAGING-ALL: автодогрузка результатов поиска альбомов.
    LaunchedEffect(albumsListState) {
        snapshotFlow {
            val info = albumsListState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = info.totalItemsCount
            if (total > 0 && lastVisible >= total - 4) total else -1
        }
            .distinctUntilChanged()
            .collect { if (it > 0) loadMoreAlbums() }
    }

    Column(modifier = Modifier.fillMaxSize().background(VK_BLACK)) {
        LibraryTopBar("Альбомы", onBack)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Поиск альбомов…", color = VK_TEXT_SECONDARY, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = VK_TEXT_SECONDARY) },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = VK_CARD,
                unfocusedContainerColor = VK_CARD,
                focusedBorderColor = VK_ACCENT,
                unfocusedBorderColor = VK_CARD,
                cursorColor = VK_ACCENT,
                focusedTextColor = VK_TEXT_PRIMARY,
                unfocusedTextColor = VK_TEXT_PRIMARY,
            ),
        )
        when {
            query.isBlank() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Введите запрос для поиска альбомов", color = VK_TEXT_SECONDARY, fontSize = 14.sp)
            }
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VK_ACCENT)
            }
            searched && albums.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Ничего не найдено", color = VK_TEXT_SECONDARY, fontSize = 14.sp)
            }
            else -> LazyColumn(Modifier.fillMaxSize(), state = albumsListState) {
                // Fix #MUSIC-CRASH-5: ключ с индексом — дубли ключей в LazyColumn
                // недопустимы (IllegalArgumentException → краш). Данные уже
                // дедуплицированы в audioSearchAlbumsPage, индекс — страховка.
                itemsIndexed(albums, key = { i, al -> "album_${i}_${al.ownerId}_${al.id}" }) { _, al ->
                    PlaylistRow(
                        title = al.title,
                        coverUrl = al.coverUrl,
                        subtitle = if (al.count > 0) "${al.count} треков" else null,
                        onClick = { onOpenAlbum(al.ownerId, al.id, al.accessKey) },
                    )
                }
                // #AUDIO-PAGING-ALL: футер догрузки альбомов.
                if (albumsLoadingMore) {
                    item(key = "albums_more") {
                        Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = VK_ACCENT)
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
//  Артисты и кураторы — поиск артистов (audio.searchArtists)
// ════════════════════════════════════════════════════════════════════

@Composable
fun MusicArtistsScreen(
    onBack: () -> Unit,
    onOpenArtist: (slug: String, name: String) -> Unit,
) {
    BackHandler { onBack() }
    val app = SovaApp.get()
    var query by remember { mutableStateOf("") }
    var artists by remember { mutableStateOf<List<AudioArtist>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    // #AUDIO-PAGING-ALL (волна 38): страничный поиск артистов — догрузка по
    // скроллу до конца выдачи (раньше — одна страница на 30 результатов).
    var artistsNextFrom by remember { mutableStateOf<String?>(null) }
    var artistsLoadingMore by remember { mutableStateOf(false) }
    val artistsListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun loadMoreArtists() {
        if (artistsLoadingMore || loading) return
        val cursor = artistsNextFrom ?: return
        val q = query
        if (q.isBlank()) return
        scope.launch {
            artistsLoadingMore = true
            try {
                val (nf, page) = app.apiClient.audioSearchArtistsPage(query = q, count = 30, startFrom = cursor)
                artistsNextFrom = nf
                // Артисты из links[] приходят с id=0 — дедуп по имени/domain (Fix #281).
                artists = (artists + page).distinctBy { a -> a.domain?.takeIf { it.isNotBlank() } ?: a.name.lowercase() }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e("MusicArtistsScreen", "load more failed: ${e.message}")
            } finally {
                artistsLoadingMore = false
            }
        }
    }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            artists = emptyList(); searched = false; artistsNextFrom = null; return@LaunchedEffect
        }
        kotlinx.coroutines.delay(400)
        loading = true
        try {
            val (nf, page) = app.apiClient.audioSearchArtistsPage(query = query, count = 30)
            artists = page
            artistsNextFrom = nf
            searched = true
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e("MusicArtistsScreen", "search failed: ${e.message}")
        } finally {
            loading = false
        }
    }

    // #AUDIO-PAGING-ALL: автодогрузка результатов поиска артистов.
    LaunchedEffect(artistsListState) {
        snapshotFlow {
            val info = artistsListState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = info.totalItemsCount
            if (total > 0 && lastVisible >= total - 4) total else -1
        }
            .distinctUntilChanged()
            .collect { if (it > 0) loadMoreArtists() }
    }

    Column(modifier = Modifier.fillMaxSize().background(VK_BLACK)) {
        LibraryTopBar("Артисты и кураторы", onBack)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Поиск артистов…", color = VK_TEXT_SECONDARY, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = VK_TEXT_SECONDARY) },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = VK_CARD,
                unfocusedContainerColor = VK_CARD,
                focusedBorderColor = VK_ACCENT,
                unfocusedBorderColor = VK_CARD,
                cursorColor = VK_ACCENT,
                focusedTextColor = VK_TEXT_PRIMARY,
                unfocusedTextColor = VK_TEXT_PRIMARY,
            ),
        )
        when {
            query.isBlank() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Введите запрос для поиска артистов", color = VK_TEXT_SECONDARY, fontSize = 14.sp)
            }
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VK_ACCENT)
            }
            searched && artists.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Ничего не найдено", color = VK_TEXT_SECONDARY, fontSize = 14.sp)
            }
            else -> LazyColumn(Modifier.fillMaxSize(), state = artistsListState) {
                // Fix #MUSIC-CRASH-2 (UI-сторона): ключ = domain ?: id → при ≥2
                // артистах без domain все ключи «0» → IllegalArgumentException → краш
                // («при поиске закрывается приложение»). Индекс гарантирует уникальность.
                itemsIndexed(artists, key = { i, a -> "artist_${i}_${a.domain ?: a.name}" }) { _, artist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenArtist(artist.domain ?: "", artist.name) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(VK_CARD),
                            contentAlignment = Alignment.Center,
                        ) {
                            val cover = artist.coverUrl
                            if (cover != null) {
                                AsyncImage(model = cover, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Filled.MusicNote, null, tint = VK_ACCENT, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(artist.name, color = VK_TEXT_PRIMARY, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (artist.followers > 0) {
                                Text("${artist.followers} подписчиков", color = VK_TEXT_SECONDARY, fontSize = 12.sp)
                            }
                        }
                    }
                }
                // #AUDIO-PAGING-ALL: футер догрузки артистов.
                if (artistsLoadingMore) {
                    item(key = "artists_more") {
                        Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = VK_ACCENT)
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
//  Артист — треки + related (audio.getAudiosByArtist)
// ════════════════════════════════════════════════════════════════════

@Composable
fun ArtistDetailScreen(
    slug: String,
    name: String,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }
    val app = SovaApp.get()
    var artist by remember { mutableStateOf<AudioArtist?>(null) }
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    // #AUDIO-PAGING-ALL (волна 38): страничная догрузка треков артиста по
    // курсору next_from (раньше — одна страница на 100 треков, дальше — конец).
    var artistNextFrom by remember { mutableStateOf<String?>(null) }
    var loadingMore by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun loadMore() {
        if (loadingMore || loading) return
        val cursor = artistNextFrom ?: return
        scope.launch {
            loadingMore = true
            try {
                val (nf, page) = app.apiClient.audioGetAudiosByArtist(
                    slug = slug, name = name, count = 100, startFrom = cursor)
                artistNextFrom = nf
                val fresh = page
                    .filter { it.id > 0L && !it.url.isNullOrBlank() }
                    .filter { nv -> tracks.none { it.ownerId == nv.ownerId && it.id == nv.id } }
                tracks = (tracks + fresh).distinctBy { "${it.ownerId}_${it.id}" }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e("ArtistDetailScreen", "load more failed: ${e.message}")
            } finally {
                loadingMore = false
            }
        }
    }

    LaunchedEffect(slug, name) {
        loading = true
        artistNextFrom = null
        try {
            // #MUSIC-PORT-FIX: artistId — slug артиста, name — имя для поиска.
            // Треки ищем через catalog.getAudioSearch (по имени), фильтруем по main_artists.
            // #AUDIO-PAGING-ALL: теперь метод возвращает курсор догрузки.
            val (nf, list) = app.apiClient.audioGetAudiosByArtist(slug = slug, name = name, count = 100)
            artistNextFrom = nf
            tracks = list.filter { it.id > 0L && !it.url.isNullOrBlank() }
            artist = AudioArtist(id = 0L, name = name.ifBlank { slug.trimStart('_').replace('_', ' ').ifBlank { "Артист" } })
            AppLog.i("ArtistDetailScreen", "Loaded ${tracks.size} tracks for artist $slug (nextFrom=${if (nf.isNullOrBlank()) "нет" else "есть"})")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e("ArtistDetailScreen", "load failed: ${e.message}")
        } finally {
            loading = false
        }
    }

    // #AUDIO-PAGING-ALL: автодогрузка при доскролле до конца.
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = info.totalItemsCount
            if (total > 0 && lastVisible >= total - 3) total else -1
        }
            .distinctUntilChanged()
            .collect { if (it > 0) loadMore() }
    }

    Column(modifier = Modifier.fillMaxSize().background(VK_BLACK)) {
        LibraryTopBar(artist?.name ?: "Артист", onBack)
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VK_ACCENT)
            }
        } else if (tracks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Треки не найдены", color = VK_TEXT_SECONDARY, fontSize = 14.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), state = listState) {
                item {
                    AudioAttachmentList(tracks = tracks)
                }
                // #AUDIO-PAGING-ALL: футер догрузки треков артиста.
                if (loadingMore) {
                    item(key = "artist_tracks_more") {
                        Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = VK_ACCENT)
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
//  Общая строка плейлиста/альбома
// ════════════════════════════════════════════════════════════════════

@Composable
private fun PlaylistRow(
    title: String,
    coverUrl: String?,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(MaterialTheme.shapes.small)
                .background(Color(0xFF282329)),
            contentAlignment = Alignment.Center,
        ) {
            if (!coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    tint = Color(0xFFF2DAEE),
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFF9E96A0),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
//  «Показать все» — полный список блока каталога
// ════════════════════════════════════════════════════════════════════

/**
 * #MUSIC-CATALOG-SHOW-ALL: экран полного списка блока каталога.
 * Грузит catalog.getSection(sectionId) и показывает все треки/плейлисты блока.
 */
@Composable
fun CatalogSectionScreen(
    sectionId: String,
    title: String,
    onBack: () -> Unit,
    onOpenPlaylist: (ownerId: Long, playlistId: Long, accessKey: String?) -> Unit,
) {
    BackHandler { onBack() }
    val app = SovaApp.get()
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var playlists by remember { mutableStateOf<List<CatalogPlaylist>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    // Волна 40 #SECTIONS-HONEST: повтор загрузки + честные причины пустоты
    // (офлайн/ошибка VK/нерендерящиеся блоки) вместо молчаливой пустоты.
    var reloadTick by remember { mutableStateOf(0) }

    LaunchedEffect(sectionId, reloadTick) {
        loading = true
        errorText = null
        try {
            // #AUDIO-PAGING-ALL (волна 38): был catalogGetSectionById — только
            // ПЕРВАЯ страница секции («Показать все» обрезался). Теперь метод
            // догружает ВСЕ страницы секции по курсору start_from (кап 10 страниц
            // — страховка от вечного цикла).
            val blocks = app.apiClient.catalogGetSectionAllBlocks(sectionId)
            val allTracks = mutableListOf<Track>()
            val allPlaylists = mutableListOf<CatalogPlaylist>()
            blocks.forEach { b ->
                allTracks.addAll(b.tracks)
                allPlaylists.addAll(b.playlists)
            }
            tracks = allTracks
            playlists = allPlaylists
            AppLog.i("CatalogSectionScreen", "Loaded ${allTracks.size} tracks, ${allPlaylists.size} playlists")
            // #SECTIONS-HONEST (волна 40): секция, где после парсинга не осталось
            // ни треков, ни плейлистов (артисты/жанры/радио-блоки пока не
            // рендерятся, офлайн, ошибка API) — честная причина вместо пустоты.
            if (allTracks.isEmpty() && allPlaylists.isEmpty()) {
                errorText = if (app.apiClient.isOffline()) "Нет сети — раздел доступен онлайн"
                else (app.apiClient.lastApiErrorHuman() ?: "Раздел пуст или содержит элементы, которые пока не отображаются")
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e("CatalogSectionScreen", "load failed: ${e.message}")
            // #SECTIONS-HONEST: причина различается (офлайн vs ошибка).
            errorText = if (app.apiClient.isOffline()) "Нет сети — раздел доступен онлайн"
            else "Ошибка: ${e.message}"
        } finally {
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(VK_BLACK)) {
        LibraryTopBar(title, onBack)
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VK_ACCENT)
            }
            errorText != null && tracks.isEmpty() && playlists.isEmpty() ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // Волна 40 #SECTIONS-HONEST: причина + «Повторить» (раньше —
                    // только текст без действия).
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            errorText ?: "",
                            color = VK_TEXT_SECONDARY,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { reloadTick++ }) { Text("Повторить") }
                    }
                }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
            ) {
                if (tracks.isNotEmpty()) {
                    item { AudioAttachmentList(tracks = tracks) }
                }
                if (playlists.isNotEmpty()) {
                    items(playlists, key = { "${it.ownerId}_${it.id}" }) { pl ->
                        PlaylistRow(
                            title = pl.title,
                            coverUrl = pl.coverUrl,
                            subtitle = when {
                                pl.matchPercent != null -> "${pl.matchPercent}% совпадение"
                                pl.count > 0 -> "${pl.count} треков"
                                else -> null
                            },
                            onClick = { onOpenPlaylist(pl.ownerId, pl.id, pl.accessKey) },
                        )
                    }
                }
            }
        }
    }
}
