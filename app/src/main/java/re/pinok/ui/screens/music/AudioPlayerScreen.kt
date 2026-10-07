// File: ui/screens/music/AudioPlayerScreen.kt
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
import re.pinok.SovaApp
import re.pinok.data.model.AudioPlaylist
import re.pinok.data.model.EqualizerPreset
import re.pinok.data.model.PlayerState
import re.pinok.data.model.Track
import re.pinok.media.EqualizerHelper
import re.pinok.media.CustomPreset
import re.pinok.media.CustomPresetStore
import re.pinok.media.PlayerConnection
import re.pinok.media.TrackDownloadManager
import re.pinok.ui.components.shareTrack
import re.pinok.util.AppLog
import re.pinok.util.toDurationString
import kotlin.math.abs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Fix #62: Полноэкранный аудиоплеер.
 *
 * Моделирован по скриншоту SOVA reference (Screenshot_20260628_185029.png):
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
    onOpenFullEqualizer: () -> Unit = {},
) {
    BackHandler { onBack() }

    val playerState by PlayerConnection.playerState.collectAsState()
    val downloads by TrackDownloadManager.downloads.collectAsState()

    // Адаптивная палитра: привязка к MaterialTheme (Material You / SOVATheme)
    val colorScheme = MaterialTheme.colorScheme
    val vkBlack = colorScheme.background
    val vkCard = colorScheme.surface
    val vkTextPrimary = colorScheme.onSurface
    val vkTextSecondary = colorScheme.onSurfaceVariant
    val vkAccent = colorScheme.primary

    val track = playerState.currentTrack
    val app = SovaApp.get()
    val scope = rememberCoroutineScope()
    // Fix #249: Activity-context для startActivity(chooser) — иначе
    // Application-context (SovaApp.get()) кидает AndroidRuntimeException
    // «Calling startActivity() from outside of an Activity context
    // requires the FLAG_ACTIVITY_NEW_TASK flag».
    val localContext = LocalContext.current

    // ─── Lyrics state ──────────────────────────────────────────────
    var showLyrics by remember { mutableStateOf(false) }
    var lyricsText by remember { mutableStateOf<String?>(null) }
    var lyricsLoading by remember { mutableStateOf(false) }

    // ─── Equalizer state ──────────────────────────────────────────
    var showEqualizer by remember { mutableStateOf(false) }

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
                .clip(RoundedCornerShape(10.dp))
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
                                re.pinok.data.audio.AudioLibraryPager.get().removeTrack(t)
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
                                re.pinok.data.audio.AudioLibraryPager.get().addTrackFront(t)
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
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
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
                modifier = Modifier.size(46.dp),
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

            // Эквалайзер
            IconButton(
                onClick = { showEqualizer = true },
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = "Эквалайзер",
                    tint = if (showEqualizer) vkAccent else Color(0xFF8E8E8E),
                    modifier = Modifier.size(24.dp),
                )
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
                            .clip(RoundedCornerShape(2.dp))
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

    // ─── Equalizer Bottom Sheet (упрощённый) ──────────────────────
    // Этап 2 (#Equalizer): здесь оставляем компактную панель — пресеты +
    // master switch + quick bass/virt. Полная настройка всех 6 эффектов
    // (9 полос, reverb, loudness) — в полноэкранном EqualizerScreen,
    // кнопка «Открыть полный эквалайзер» внизу панели.
    if (showEqualizer) {
        val eqSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        var eqEnabled by remember { mutableStateOf(false) }
        var eqPresetName by remember { mutableStateOf<String?>(null) }

        // Quick-эффекты (только если включены в feature-флагах)
        val featureFlags = remember { re.pinok.media.EqualizerFeatureFlags.snapshot() }
        var bassOn by remember { mutableStateOf(false) }
        var bassStr by remember { mutableStateOf(0) }
        var virtOn by remember { mutableStateOf(false) }
        var virtStr by remember { mutableStateOf(0) }

        // #EQ-UI2: полосы EQ видны сразу в шите, пресеты — dropdown
        // (встроенные + мои), сохранение пресета — кнопкой на полосах.
        val customPresets = remember { mutableStateListOf<CustomPreset>() }
        var showPresetMenu by remember { mutableStateOf(false) }
        var showSaveDialog by remember { mutableStateOf(false) }
        var newPresetName by remember { mutableStateOf("") }
        var bands by remember { mutableStateOf(List(eqFrequencyLabels.size) { 0f }) }

        LaunchedEffect(showEqualizer) {
            if (showEqualizer) {
                eqEnabled = EqualizerHelper.isEnabled() || EqualizerHelper.isSavedEnabled()
                eqPresetName = EqualizerHelper.currentPresetName ?: EqualizerHelper.getSavedPresetName()
                val engine = EqualizerHelper.engine()
                if (engine != null) {
                    bassOn = engine.isBassBoostEnabled() || engine.isBassBoostSavedEnabled()
                    bassStr = engine.getBassBoostStrength()
                    virtOn = engine.isVirtualizerEnabled() || engine.isVirtualizerSavedEnabled()
                    virtStr = engine.getVirtualizerStrength()
                }
                customPresets.clear()
                customPresets.addAll(CustomPresetStore.list())
                // #EQ-BANDS-PERSIST: приоритет сохранённому списку (9 слотов),
                // live-полосы устройства могут быть короче (5 полос на HOTWAV).
                val liveBands = PlayerConnection.getEqualizerBands()
                val savedBands = EqualizerHelper.getSavedBands()
                val source = if (savedBands.isNotEmpty()) savedBands else liveBands
                val mapped = MutableList(eqFrequencyLabels.size) { 0f }
                if (source.isNotEmpty()) {
                    val upper = minOf(source.lastIndex, eqFrequencyLabels.size - 1)
                    for (i in 0..upper) mapped[i] = source[i].toInt() / 100f
                }
                bands = mapped
            }
        }

        val displayPresetName = eqPresetName
            ?: "Пользовательский"

        ModalBottomSheet(
            onDismissRequest = { showEqualizer = false },
            sheetState = eqSheetState,
            containerColor = vkCard,
            dragHandle = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(vkTextSecondary.copy(alpha = 0.4f)),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Эквалайзер",
                            color = vkTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = displayPresetName,
                            color = vkAccent,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        IconButton(onClick = { showEqualizer = false }) {
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                // ─── Вкл / Выкл ───────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Вкл / Выкл",
                        color = vkTextPrimary,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = eqEnabled,
                        onCheckedChange = { enabled ->
                            eqEnabled = enabled
                            PlayerConnection.setEqualizerEnabled(enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = vkAccent,
                            checkedTrackColor = vkAccent,
                            uncheckedThumbColor = vkTextSecondary,
                            uncheckedTrackColor = vkTextSecondary.copy(alpha = 0.3f),
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ─── #EQ-UI2: Пресеты — выпадающий список ──────────
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(vkTextSecondary.copy(alpha = 0.12f))
                            .clickable { showPresetMenu = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.GraphicEq,
                            contentDescription = null,
                            tint = vkAccent,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Пресет", color = vkTextSecondary, fontSize = 11.sp)
                            Text(
                                text = displayPresetName,
                                color = vkTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            contentDescription = "Выбрать пресет",
                            tint = vkTextSecondary,
                        )
                    }
                    DropdownMenu(
                        expanded = showPresetMenu,
                        onDismissRequest = { showPresetMenu = false },
                    ) {
                        EqualizerPreset.ALL.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(if (eqPresetName == preset.name) "✓ ${preset.name}" else preset.name) },
                                onClick = {
                                    PlayerConnection.setEqualizerPreset(preset)
                                    eqPresetName = preset.name
                                    showPresetMenu = false
                                },
                            )
                        }
                        if (customPresets.isNotEmpty()) {
                            Text(
                                "Мои пресеты",
                                color = vkTextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            )
                            customPresets.forEach { custom ->
                                DropdownMenuItem(
                                    text = { Text(if (eqPresetName == custom.name) "✓ ${custom.name}" else custom.name) },
                                    onClick = {
                                        EqualizerHelper.applyCustomPresetPersist(custom)
                                        eqPresetName = custom.name
                                        showPresetMenu = false
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ─── #EQ-UI2: Полосы эквалайзера — видны сразу ─────
                Text(
                    "Полосы эквалайзера",
                    color = vkTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    eqFrequencyLabels.forEachIndexed { index, freq ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(44.dp),
                        ) {
                            Text(
                                text = "${bands.getOrElse(index) { 0f }.toInt()}",
                                color = if (bands.getOrElse(index) { 0f } != 0f) vkAccent else vkTextSecondary,
                                fontSize = 11.sp,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            EqVerticalSliderThemed(
                                value = bands.getOrElse(index) { 0f },
                                onValueChange = { v ->
                                    val updated = bands.toMutableList()
                                    updated[index] = v
                                    bands = updated
                                    PlayerConnection.setEqualizerBand(index, (v * 100).toInt().toShort())
                                },
                                enabled = eqEnabled,
                                modifier = Modifier.height(180.dp),
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = freq, color = vkTextSecondary, fontSize = 10.sp)
                        }
                    }
                }
                // ─── #EQ-UI2: Сохранить пресет — кнопка на полосах ─
                TextButton(
                    onClick = {
                        newPresetName = ""
                        showSaveDialog = true
                    },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Icon(
                        Icons.Filled.Save,
                        contentDescription = null,
                        tint = vkAccent,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Сохранить пресет", color = vkAccent, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ─── Quick: BassBoost (если включён в feature-флагах) ────
                if (featureFlags.bassEnabled) {
                    val engine = EqualizerHelper.engine()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Басы",
                            color = vkTextPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = bassOn,
                            onCheckedChange = { on ->
                                bassOn = on
                                engine?.setBassBoostEnabled(on)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = vkAccent,
                                checkedTrackColor = vkAccent,
                                uncheckedThumbColor = vkTextSecondary,
                                uncheckedTrackColor = vkTextSecondary.copy(alpha = 0.3f),
                            ),
                        )
                    }
                    Slider(
                        value = bassStr.toFloat(),
                        onValueChange = { v ->
                            bassStr = v.toInt()
                            engine?.setBassBoostStrength(v.toInt())
                        },
                        enabled = bassOn,
                        valueRange = 0f..1000f,
                        colors = SliderDefaults.colors(
                            thumbColor = vkAccent,
                            activeTrackColor = vkAccent,
                            inactiveTrackColor = vkTextSecondary.copy(alpha = 0.3f),
                        ),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ─── Quick: Virtualizer (если включён в feature-флагах) ───
                if (featureFlags.virtualizerEnabled) {
                    val engine = EqualizerHelper.engine()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Объём",
                            color = vkTextPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = virtOn,
                            onCheckedChange = { on ->
                                virtOn = on
                                engine?.setVirtualizerEnabled(on)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = vkAccent,
                                checkedTrackColor = vkAccent,
                                uncheckedThumbColor = vkTextSecondary,
                                uncheckedTrackColor = vkTextSecondary.copy(alpha = 0.3f),
                            ),
                        )
                    }
                    Slider(
                        value = virtStr.toFloat(),
                        onValueChange = { v ->
                            virtStr = v.toInt()
                            engine?.setVirtualizerStrength(v.toInt())
                        },
                        enabled = virtOn,
                        valueRange = 0f..1000f,
                        colors = SliderDefaults.colors(
                            thumbColor = vkAccent,
                            activeTrackColor = vkAccent,
                            inactiveTrackColor = vkTextSecondary.copy(alpha = 0.3f),
                        ),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ─── Кнопка «Открыть полный эквалайзер» ──────────────
                // Ведёт на полноэкранный EqualizerScreen (9 полос, reverb,
                // loudness, тонкая настройка).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(vkAccent.copy(alpha = 0.15f))
                        .clickable {
                            showEqualizer = false
                            onOpenFullEqualizer()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Equalizer,
                        contentDescription = null,
                        tint = vkAccent,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Расширенные настройки (Reverb, Loudness, визуализатор)",
                        color = vkAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    Text("→", color = vkAccent, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // ─── #EQ-UI2: диалог сохранения пресета (с полос) ────────────
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("Сохранить пресет") },
                text = {
                    Column {
                        Text(
                            "Текущие настройки всех эффектов (полосы EQ, Bass, " +
                            "Virtualizer, Loudness, Reverb) будут сохранены как новый пресет.",
                            color = vkTextSecondary,
                            fontSize = 13.sp,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newPresetName,
                            onValueChange = { newPresetName = it },
                            label = { Text("Название") },
                            singleLine = true,
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val name = newPresetName.trim().ifBlank { "Мой пресет" }
                            // #EQ-SAVE-NULL-ENGINE: без требования живого engine.
                            run {
                                val snapshot = EqualizerHelper.snapshotCustomPreset(name)
                                CustomPresetStore.upsert(
                                    name = snapshot.name,
                                    eqBands = snapshot.eqBands,
                                    eqEnabled = snapshot.eqEnabled,
                                    bassEnabled = snapshot.bassEnabled,
                                    bassStrength = snapshot.bassStrength,
                                    virtEnabled = snapshot.virtEnabled,
                                    virtStrength = snapshot.virtStrength,
                                    loudEnabled = snapshot.loudEnabled,
                                    loudGainmB = snapshot.loudGainmB,
                                    reverbEnabled = snapshot.reverbEnabled,
                                    reverbPreset = snapshot.reverbPreset,
                                )
                                eqPresetName = name
                                customPresets.clear()
                                customPresets.addAll(CustomPresetStore.list())
                            }
                            showSaveDialog = false
                        },
                    ) { Text("Сохранить") }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) { Text("Отмена") }
                },
            )
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
            shape = RoundedCornerShape(16.dp),
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
                                    .clip(RoundedCornerShape(10.dp))
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
                                        .clip(RoundedCornerShape(8.dp))
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
            shape = RoundedCornerShape(16.dp),
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
                    shape = RoundedCornerShape(12.dp),
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

