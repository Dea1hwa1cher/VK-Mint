// File: ui/screens/music/AudioPlayerScreen.kt
package vk.mint.ui.screens.music

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import vk.mint.VkMintApp
import vk.mint.data.model.AudioPlaylist
import vk.mint.data.model.PlayerState
import vk.mint.media.PlayerConnection
import vk.mint.media.TrackDownloadManager
import vk.mint.ui.components.shareTrack
import vk.mint.util.AppLog
import vk.mint.util.toDurationString
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Fix #62: Полноэкранный аудиоплеер.
 *
 * Моделирован по скриншоту VK Mint reference (Screenshot_20260628_185029.png):
 *  — Крупная обложка трека в центре (с gradient-плейсхолдером если нет albumThumb)
 *  — Прогресс-бар с таймингами (текущее / оставшееся)
 *  — Название трека (белым) + артист (синим, кликабельный → поиск артиста)
 *  — Контролы: prev / play-pause / next + download + 3-dots
 *  — Второй ряд: shuffle / queue / repeat
 *
 * Открывается тапом по мини-плееру в MusicScreen.
 * System back → onBack (popBackStack).
 */
@Composable
fun AudioPlayerScreen(
    onBack: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    BackHandler { onBack() }

    val playerState by PlayerConnection.playerState.collectAsState()
    val downloads by TrackDownloadManager.downloads.collectAsState()

    // Адаптивная палитра: привязка к MaterialTheme (Material You / VkMintTheme)
    val colorScheme = MaterialTheme.colorScheme
    val vkBlack = colorScheme.background
    val vkCard = colorScheme.surface
    val vkTextPrimary = colorScheme.onSurface
    val vkTextSecondary = colorScheme.onSurfaceVariant
    val vkAccent = colorScheme.primary

    val track = playerState.currentTrack
    val app = VkMintApp.get()
    val scope = rememberCoroutineScope()
    // Fix #249: Activity-context для startActivity(chooser) — иначе
    // Application-context (VkMintApp.get()) кидает AndroidRuntimeException
    // «Calling startActivity() from outside of an Activity context
    // requires the FLAG_ACTIVITY_NEW_TASK flag».
    val localContext = LocalContext.current

    // ─── Lyrics state ──────────────────────────────────────────────
    var showLyrics by remember { mutableStateOf(false) }
    var lyricsText by remember { mutableStateOf<String?>(null) }
    var lyricsLoading by remember { mutableStateOf(false) }

    // ─── Fix #258: Add-to-playlist dialog state ───────────────────
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var playlists by remember { mutableStateOf<List<AudioPlaylist>>(emptyList()) }
    var playlistsLoading by remember { mutableStateOf(false) }
    // #AUDIO-PAGING-ALL (волна 38): догрузка плейлистов в диалоге
    // (было 50 — при большем числе плейлистов до конца не добраться).
    var playlistsOffset by remember { mutableStateOf(0) }
    var playlistsHasMore by remember { mutableStateOf(false) }
    var playlistsLoadingMore by remember { mutableStateOf(false) }
    val playlistsListState = rememberLazyListState()
    var addingToPlaylist by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // ─── Fix #258: Create-new-playlist dialog ─────────────────────
    // Волна 40 #BOOKMARKS-TRACKS: локальные закладки треков — реактивный стейт
    // для toggle-метки меню плеера («В закладки»/«Удалить из закладок»).
    val bookmarkTracks by app.trackBookmarksRepository.tracks.collectAsState()
    val currentTrackForBookmark = track
    val trackBookmarkedNow = currentTrackForBookmark != null &&
        bookmarkTracks.any { it.id == currentTrackForBookmark.id && it.ownerId == currentTrackForBookmark.ownerId }

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistTitle by remember { mutableStateOf("") }

    // Fix #161: убран verticalScroll из главного Column.
    // В скроллящемся Column Modifier.weight() игнорируется (получает 0 высоты),
    // поэтому Spacer(weight(1f)) не работал → контент сжимался вверху, а
    // fillMaxSize() оставлял ~1/4 экрана пустым внизу. Без scroll weight
    // корректно распределяет свободное место: обложка+заголовок вверху,
    // прогресс+контролы внизу.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(vkBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // ─── Top bar: collapse + source title + share ─────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Свернуть",
                    tint = vkTextPrimary,
                    modifier = Modifier.size(30.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Сейчас играет",
                    color = vkTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
                Text(
                    text = "Моя музыка",
                    color = vkTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = {
                    if (track != null) shareTrack(localContext, track)
                },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = "Поделиться",
                    tint = vkTextPrimary,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer(scaleX = -1f),
                )
            }
        }

        if (track == null) {
            // Нет трека — показываем заглушку
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = vkTextSecondary,
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Ничего не играет", color = vkTextSecondary, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Выберите трек в разделе «Музыка»", color = vkTextSecondary, fontSize = 13.sp)
                }
            }
            return@Column
        }

        Spacer(modifier = Modifier.weight(0.3f))

        // ─── Крупная обложка трека (1:1, square) ──────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(Color.Black),
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
            } else {
                // Стильный плейсхолдер с белыми сдвоенными нотами (по образу и подобию референса)
                Canvas(modifier = Modifier.size(130.dp)) {
                    val strokeW = 12.dp.toPx()
                    val headR = size.width * 0.13f

                    // Левая головка ноты
                    val leftCenter = Offset(size.width * 0.30f, size.height * 0.72f)
                    drawCircle(
                        color = Color.White,
                        radius = headR,
                        center = leftCenter,
                        style = Stroke(width = strokeW),
                    )

                    // Правая головка ноты (выше левой)
                    val rightCenter = Offset(size.width * 0.70f, size.height * 0.58f)
                    drawCircle(
                        color = Color.White,
                        radius = headR,
                        center = rightCenter,
                        style = Stroke(width = strokeW),
                    )

                    // Левый штиль
                    val leftStemX = leftCenter.x + headR - strokeW * 0.35f
                    val leftStemBottom = leftCenter.y
                    val leftStemTop = size.height * 0.28f
                    drawLine(
                        color = Color.White,
                        start = Offset(leftStemX, leftStemBottom),
                        end = Offset(leftStemX, leftStemTop),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round,
                    )

                    // Правый штиль
                    val rightStemX = rightCenter.x + headR - strokeW * 0.35f
                    val rightStemBottom = rightCenter.y
                    val rightStemTop = size.height * 0.16f
                    drawLine(
                        color = Color.White,
                        start = Offset(rightStemX, rightStemBottom),
                        end = Offset(rightStemX, rightStemTop),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round,
                    )

                    // Верхняя связующая перекладина (beam)
                    drawLine(
                        color = Color.White,
                        start = Offset(leftStemX - strokeW * 0.2f, leftStemTop),
                        end = Offset(rightStemX + strokeW * 0.2f, rightStemTop),
                        strokeWidth = strokeW * 1.6f,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }

        // Фоновый эффект кассетных бобин / кругов свечения под обложкой (как на референсе)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val ringRadius = size.width * 0.16f
                val strokeRing = Stroke(width = 20.dp.toPx())
                val glowColor = Color.White.copy(alpha = 0.05f)
                drawCircle(
                    color = glowColor,
                    radius = ringRadius,
                    center = Offset(size.width * 0.28f, size.height * 0.4f),
                    style = strokeRing,
                )
                drawCircle(
                    color = glowColor,
                    radius = ringRadius,
                    center = Offset(size.width * 0.72f, size.height * 0.4f),
                    style = strokeRing,
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.7f))

        // ─── Название трека, артист и быстрые действия (Dislike + Добавить) ─────
        val myUserId = app.exchangeAuthRepository.userId()
        val ownedByMe = myUserId != 0L && track.ownerId == myUserId
        var ownOverride by remember(track.id, track.ownerId) { mutableStateOf<Boolean?>(null) }
        var ownToggling by remember(track.id, track.ownerId) { mutableStateOf(false) }
        val ownOverrideNow = ownOverride
        val ownedNow = if (ownOverrideNow != null) ownOverrideNow else ownedByMe

        var isDisliked by remember(track.id) { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val displayTitle = if (!track.subtitle.isNullOrBlank()) {
                    "${track.title} (${track.subtitle})"
                } else {
                    track.title
                }
                Text(
                    text = displayTitle,
                    color = vkTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = track.artist,
                    color = vkTextSecondary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Дизлайк
            IconButton(
                onClick = {
                    val nextDisliked = !isDisliked
                    isDisliked = nextDisliked
                    scope.launch {
                        try {
                            val audioId = "${track.ownerId}_${track.id}"
                            val ok = if (nextDisliked) {
                                app.apiClient.audioAddDislike(listOf(audioId))
                            } else {
                                app.apiClient.audioRemoveDislike(listOf(audioId))
                            }
                            if (ok) {
                                snackbarHostState.showSnackbar(if (nextDisliked) "Не нравится" else "Дизлайк снят")
                            }
                        } catch (e: Exception) {
                            AppLog.e("AudioPlayerScreen", "dislike error", e)
                        }
                    }
                },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                    contentDescription = "Не нравится",
                    tint = if (isDisliked) vkAccent else vkTextSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // В моей музыке (галочка как на референсе)
            IconButton(
                onClick = {
                    if (ownToggling) return@IconButton
                    val t = track
                    val wasOwned = if (ownOverrideNow != null) ownOverrideNow else ownedByMe
                    scope.launch {
                        ownToggling = true
                        if (wasOwned) {
                            var ok = false
                            var errText: String? = null
                            try {
                                ok = app.apiClient.audioDelete(t.id, t.ownerId)
                                if (!ok) errText = app.apiClient.lastApiError
                            } catch (e: Exception) {
                                AppLog.e("AudioPlayerScreen", "#AUDIO-TOGGLE-OWNING delete error", e)
                                errText = e.message
                            }
                            if (ok) {
                                ownOverride = false
                                vk.mint.data.audio.AudioLibraryPager.get().removeTrack(t)
                                snackbarHostState.showSnackbar("Удалено из моей музыки")
                            } else {
                                val shown = if (errText != null) errText else "ошибка сети"
                                snackbarHostState.showSnackbar("Не удалось удалить: $shown")
                            }
                        } else {
                            var ok = false
                            var errText: String? = null
                            try {
                                val (addOk, addErr) = app.apiClient.audioAddReliable(t)
                                ok = addOk
                                if (!addOk) errText = addErr
                            } catch (e: Exception) {
                                AppLog.e("AudioPlayerScreen", "#AUDIO-TOGGLE-OWNING add error", e)
                                errText = e.message
                            }
                            if (ok) {
                                ownOverride = true
                                vk.mint.data.audio.AudioLibraryPager.get().addTrackFront(t)
                                snackbarHostState.showSnackbar("Добавлено в мою музыку")
                            } else {
                                val shown = if (errText != null) errText else "ошибка сети"
                                snackbarHostState.showSnackbar("Не удалось добавить: $shown")
                            }
                        }
                        ownToggling = false
                    }
                },
                modifier = Modifier.size(40.dp),
            ) {
                when {
                    ownToggling -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = vkAccent,
                        trackColor = Color.Transparent,
                    )
                    ownedNow -> Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "В моей музыке",
                        tint = vkTextPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                    else -> Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Добавить в мою музыку",
                        tint = vkTextSecondary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ─── Прогресс-бар с таймингами ─────────────────────────────────
        val effectiveDuration = if (playerState.durationMs > 0) playerState.durationMs else track.duration * 1000L
        var sliderDragging by remember { mutableStateOf(false) }
        var sliderPos by remember { mutableStateOf(playerState.positionMs.toFloat()) }

        LaunchedEffect(playerState.positionMs) {
            if (!sliderDragging) sliderPos = playerState.positionMs.toFloat()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Slider(
                value = if (sliderDragging) sliderPos else playerState.positionMs.toFloat(),
                onValueChange = {
                    sliderDragging = true
                    sliderPos = it
                },
                onValueChangeFinished = {
                    sliderDragging = false
                    PlayerConnection.seekTo(sliderPos.toLong())
                },
                valueRange = 0f..effectiveDuration.coerceAtLeast(1L).toFloat(),
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color(0xFF333333),
                    thumbColor = Color.White,
                ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = playerState.positionMs.toDurationString(),
                    color = vkTextSecondary,
                    fontSize = 11.sp,
                )
                Text(
                    text = effectiveDuration.toDurationString(),
                    color = vkTextSecondary,
                    fontSize = 11.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ─── Основные контролы (Repeat squircle, Prev, Play/Pause circle, Next, Shuffle) ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 1. Повтор (в скруглённом квадратном контейнере, как на референсе)
            val isRepeatActive = playerState.repeatMode != PlayerState.REPEAT_MODE_OFF
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color(0xFF262626))
                    .clickable { PlayerConnection.cycleRepeatMode() },
                contentAlignment = Alignment.Center,
            ) {
                val repeatIcon = when (playerState.repeatMode) {
                    PlayerState.REPEAT_MODE_ONE, PlayerState.REPEAT_MODE_TWO -> Icons.Filled.RepeatOne
                    else -> Icons.Filled.Repeat
                }
                Icon(
                    imageVector = repeatIcon,
                    contentDescription = "Повтор",
                    tint = if (isRepeatActive) Color.White else Color(0xFF8E8E8E),
                    modifier = Modifier.size(24.dp),
                )
                if (playerState.repeatMode == PlayerState.REPEAT_MODE_TWO) {
                    Text(
                        text = "2",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(vkAccent, shape = CircleShape)
                            .padding(horizontal = 3.dp)
                            .offset(x = 2.dp, y = (-2).dp),
                    )
                }
            }

            // 2. Предыдущий
            IconButton(
                onClick = { PlayerConnection.prev() },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Предыдущий",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp),
                )
            }

            // 3. Играть / Пауза (крупная круглая кнопка тёмно-серого цвета)
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF424242))
                    .clickable { PlayerConnection.togglePlayPause() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playerState.isPlaying) "Пауза" else "Играть",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp),
                )
            }

            // 4. Следующий
            IconButton(
                onClick = { PlayerConnection.next() },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Следующий",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp),
                )
            }

            // 5. Перемешать
            IconButton(
                onClick = { PlayerConnection.setShuffleModeEnabled(!playerState.shuffleModeEnabled) },
            ) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = "Перемешать",
                    tint = if (playerState.shuffleModeEnabled) vkAccent else Color(0xFF8E8E8E),
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // ─── Нижняя панель действий (Текст Тт, Эквалайзер, Очередь, Ещё) ───
        var showMoreMenu by remember { mutableStateOf(false) }
        var showSpeedDialog by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Текст песни (Тт)
            IconButton(
                onClick = {
                    lyricsText = null
                    showLyrics = true
                },
                modifier = Modifier.size(44.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "T",
                        color = if (showLyrics) vkAccent else Color(0xFF8E8E8E),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "т",
                        color = if (showLyrics) vkAccent else Color(0xFF8E8E8E),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.offset(y = (-1).dp),
                    )
                }
            }



            // Очередь
            IconButton(
                onClick = onOpenQueue,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                    contentDescription = "Очередь",
                    tint = Color(0xFF8E8E8E),
                    modifier = Modifier.size(28.dp),
                )
            }

            // Меню «Ещё» (три точки)
            Box {
                IconButton(
                    onClick = { showMoreMenu = true },
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Ещё",
                        tint = Color(0xFF8E8E8E),
                        modifier = Modifier.size(24.dp),
                    )
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                ) {
                    // Скачать
                    val dl = downloads[track.id]
                    DropdownMenuItem(
                        text = {
                            Text(
                                when {
                                    dl?.isCompleted == true -> "Скачано"
                                    dl?.isInProgress == true -> "Скачивание (${dl.progress}%)"
                                    else -> "Скачать трек"
                                },
                                color = vkTextPrimary,
                            )
                        },
                        leadingIcon = {
                            when {
                                dl?.isInProgress == true -> CircularProgressIndicator(
                                    progress = { if (dl.progress >= 0) dl.progress / 100f else 0f },
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = vkAccent,
                                )
                                dl?.isCompleted == true -> Icon(Icons.Filled.DownloadDone, null, tint = vkAccent)
                                else -> Icon(Icons.Filled.Download, null, tint = vkTextSecondary)
                            }
                        },
                        onClick = {
                            showMoreMenu = false
                            if (dl != null && (dl.isCompleted || dl.isInProgress)) {
                                TrackDownloadManager.removeDownload(track.id)
                                return@DropdownMenuItem
                            }
                            if (track.url != null) {
                                TrackDownloadManager.enqueueDownload(track)
                                return@DropdownMenuItem
                            }
                            scope.launch {
                                val resolved = app.apiClient.audioGetById(track)
                                if (resolved != null && resolved.url != null) {
                                    TrackDownloadManager.enqueueDownload(resolved)
                                }
                            }
                        },
                    )

                    // Скорость воспроизведения
                    DropdownMenuItem(
                        text = { Text("Скорость: ${"%.2f".format(playerState.speed)}x", color = vkTextPrimary) },
                        onClick = {
                            showMoreMenu = false
                            showSpeedDialog = true
                        },
                    )

                    // Добавить в плейлист
                    DropdownMenuItem(
                        text = { Text("Добавить в плейлист", color = vkTextPrimary) },
                        onClick = {
                            showMoreMenu = false
                            showPlaylistDialog = true
                        },
                    )

                    // Закладки
                    DropdownMenuItem(
                        text = { Text(if (trackBookmarkedNow) "Удалить из закладок" else "В закладки", color = vkTextPrimary) },
                        onClick = {
                            showMoreMenu = false
                            scope.launch {
                                val now = try {
                                    app.trackBookmarksRepository.toggle(track)
                                } catch (e: Exception) {
                                    null
                                }
                                snackbarHostState.showSnackbar(
                                    when (now) {
                                        true -> "Добавлено в закладки"
                                        false -> "Удалено из закладок"
                                        null -> "Не удалось обновить закладку"
                                    }
                                )
                            }
                        },
                    )

                    // Поделиться
                    DropdownMenuItem(
                        text = { Text("Поделиться", color = vkTextPrimary) },
                        onClick = {
                            showMoreMenu = false
                            shareTrack(localContext, track)
                        },
                    )
                }
            }
        }

        if (showSpeedDialog) {
            AlertDialog(
                onDismissRequest = { showSpeedDialog = false },
                title = { Text("Скорость воспроизведения", color = vkTextPrimary) },
                text = {
                    Column {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                            TextButton(
                                onClick = {
                                    PlayerConnection.setPlaybackSpeed(spd)
                                    showSpeedDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "${spd}x" + if (playerState.speed == spd) " ✓" else "",
                                    color = if (playerState.speed == spd) vkAccent else vkTextPrimary,
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSpeedDialog = false }) {
                        Text("Закрыть", color = vkAccent)
                    }
                },
                containerColor = vkCard,
            )
        }

        // Небольшой нижний отступ для визуального баланса
        Spacer(modifier = Modifier.height(8.dp))
    }

    // ─── Lyrics Bottom Sheet ────────────────────────────────────────
    if (showLyrics) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showLyrics = false },
            sheetState = sheetState,
            containerColor = vkCard,
            dragHandle = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Drag handle pill
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(4.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(vkTextSecondary.copy(alpha = 0.4f)),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    // Title row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Текст песни",
                            color = vkTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { showLyrics = false }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Закрыть",
                                tint = vkTextSecondary,
                            )
                        }
                    }
                }
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                when {
                    lyricsLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = vkAccent)
                        }
                    }
                    lyricsText == null -> {
                        Text(
                            text = "Текст песни недоступен",
                            color = vkTextSecondary,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 48.dp),
                        )
                    }
                    else -> {
                        val lt = lyricsText
                        if (lt != null) {
                        Text(
                            text = lt,
                            style = MaterialTheme.typography.bodyLarge,
                            color = vkTextPrimary,
                            lineHeight = 28.sp,
                        )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Fetch lyrics when sheet opens
        LaunchedEffect(showLyrics) {
            // #ARCH-CONTAINERS 3.7-1: lyricsId в :core:data — захват ДО проверки
            // (lyricsId != null одновременно гарантирует track != null).
            val lyricsId = track?.lyricsId
            if (showLyrics && lyricsId != null) {
                lyricsLoading = true
                lyricsText = app.apiClient.audioGetLyrics(lyricsId)
                lyricsLoading = false
            }
        }
    }

    // ─── Fix #258: SnackbarHost для уведомлений ─────────────────────
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    )

    // ─── Fix #258: Диалог «Добавить в плейлист» ─────────────────────
    if (showPlaylistDialog) {
        val myId = app.exchangeAuthRepository.userId()
        // Загружаем плейлисты при открытии диалога.
        LaunchedEffect(showPlaylistDialog) {
            playlistsLoading = true
            try {
                val (_, result) = app.apiClient.audioGetPlaylists(count = 50, offset = 0)
                playlists = result
                playlistsOffset = result.size
                // #AUDIO-PAGING-HOLE (волна 41): непустая страница ≠ конец списка.
                playlistsHasMore = result.isNotEmpty()
            } catch (e: Exception) {
                AppLog.e("AudioPlayerScreen", "load playlists failed", e)
                playlists = emptyList()
            } finally {
                playlistsLoading = false
            }
        }
        // #AUDIO-PAGING-ALL: догрузка следующей страницы плейлистов.
        fun loadMorePlaylists() {
            if (playlistsLoadingMore || !playlistsHasMore) return
            scope.launch {
                playlistsLoadingMore = true
                try {
                    val (_, page) = app.apiClient.audioGetPlaylists(count = 50, offset = playlistsOffset)
                    playlistsOffset += page.size
                    playlists = (playlists + page).distinctBy { "${it.ownerId}_${it.id}" }
                    // #AUDIO-PAGING-HOLE (волна 41): непустая страница ≠ конец списка.
                    playlistsHasMore = page.isNotEmpty()
                } catch (e: Exception) {
                    AppLog.e("AudioPlayerScreen", "load more playlists failed", e)
                } finally {
                    playlistsLoadingMore = false
                }
            }
        }
        LaunchedEffect(playlistsListState) {
            snapshotFlow {
                val info = playlistsListState.layoutInfo
                val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                val total = info.totalItemsCount
                if (total > 0 && lastVisible >= total - 3) total else -1
            }
                .distinctUntilChanged()
                .collect { if (it > 0) loadMorePlaylists() }
        }
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            containerColor = vkCard,
            shape = MaterialTheme.shapes.large,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Добавить в плейлист",
                        color = vkTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        showPlaylistDialog = false
                        newPlaylistTitle = ""
                        showCreatePlaylistDialog = true
                    }) {
                        Text("Новый", color = vkAccent, fontSize = 14.sp)
                    }
                }
            },
            text = {
                val t = track
                if (t == null) {
                    Text("Нет трека", color = vkTextSecondary, fontSize = 14.sp)
                } else if (playlistsLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = vkAccent)
                    }
                } else if (playlists.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Нет плейлистов. Создайте новый.",
                            color = vkTextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    LazyColumn(
                        state = playlistsListState,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    ) {
                        items(playlists) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        if (addingToPlaylist) return@clickable
                                        addingToPlaylist = true
                                        scope.launch {
                                            try {
                                                val audioIds = listOf("${t.ownerId}_${t.id}")
                                                val result = app.apiClient.audioAddToPlaylist(
                                                    ownerId = myId,
                                                    playlistId = playlist.id,
                                                    audioIds = audioIds,
                                                )
                                                if (result.isNotEmpty()) {
                                                    snackbarHostState.showSnackbar(
                                                        "Добавлено в «${playlist.title}»",
                                                    )
                                                } else {
                                                    val err = app.apiClient.lastApiError
                                                    snackbarHostState.showSnackbar(
                                                        "Не удалось: ${err ?: "трек уже в плейлисте?"}",
                                                    )
                                                }
                                            } catch (e: Exception) {
                                                AppLog.e("AudioPlayerScreen", "addToPlaylist failed", e)
                                                snackbarHostState.showSnackbar("Ошибка: ${e.message}")
                                            } finally {
                                                addingToPlaylist = false
                                                showPlaylistDialog = false
                                            }
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Обложка плейлиста
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(vkAccent),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val coverUrl = playlist.coverUrl
                                    if (!coverUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = coverUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.MusicNote,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.title,
                                        color = vkTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (playlist.count > 0) {
                                        Text(
                                            text = "${playlist.count} треков",
                                            color = vkTextSecondary,
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                                if (addingToPlaylist) {
                                    CircularProgressIndicator(
                                        color = vkAccent,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                    )
                                }
                            }
                        }
                        // #AUDIO-PAGING-ALL: футер догрузки плейлистов диалога.
                        if (playlistsLoadingMore) {
                            item(key = "pl_dlg_more") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        color = vkAccent,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistDialog = false }) {
                    Text("Закрыть", color = vkAccent)
                }
            },
        )
    }

    // ─── Fix #258: Диалог «Создать плейлист» ────────────────────────
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreatePlaylistDialog = false
                newPlaylistTitle = ""
            },
            containerColor = vkCard,
            shape = MaterialTheme.shapes.large,
            title = {
                Text(
                    "Новый плейлист",
                    color = vkTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                OutlinedTextField(
                    value = newPlaylistTitle,
                    onValueChange = { newPlaylistTitle = it },
                    placeholder = { Text("Название", color = vkTextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = vkTextPrimary,
                        fontSize = 15.sp,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val title = newPlaylistTitle.trim()
                        if (title.isEmpty()) return@TextButton
                        val t = track
                        scope.launch {
                            try {
                                val myId = app.exchangeAuthRepository.userId()
                                val newId = app.apiClient.audioCreatePlaylist(
                                    ownerId = myId,
                                    title = title,
                                    description = "",
                                )
                                if (newId > 0 && t != null) {
                                    val audioIds = listOf("${t.ownerId}_${t.id}")
                                    app.apiClient.audioAddToPlaylist(
                                        ownerId = myId,
                                        playlistId = newId,
                                        audioIds = audioIds,
                                    )
                                    snackbarHostState.showSnackbar("Создан «$title», трек добавлен")
                                } else {
                                    val err = app.apiClient.lastApiError
                                    snackbarHostState.showSnackbar("Не удалось создать: ${err ?: "ошибка"}")
                                }
                            } catch (e: Exception) {
                                AppLog.e("AudioPlayerScreen", "createPlaylist failed", e)
                                snackbarHostState.showSnackbar("Ошибка: ${e.message}")
                            } finally {
                                showCreatePlaylistDialog = false
                                newPlaylistTitle = ""
                            }
                        }
                    },
                ) {
                    Text("Создать", color = vkAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreatePlaylistDialog = false
                    newPlaylistTitle = ""
                }) {
                    Text("Отмена", color = vkTextSecondary)
                }
            },
        )
    }
}

