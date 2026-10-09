// File: ui/screens/music/MusicScreen.kt
package vk.mint.ui.screens.music

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import vk.mint.ui.anim.expressiveSpring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vk.mint.VkMintApp
import vk.mint.data.model.AudioArtist
import vk.mint.data.model.AudioPlaylist
import vk.mint.data.model.AudioSearchResult
import vk.mint.data.model.DownloadState
import vk.mint.util.toDurationString
import vk.mint.data.model.DownloadStatus
import vk.mint.data.model.Track
import vk.mint.media.PlayerConnection
import vk.mint.media.TrackDownloadManager
import vk.mint.ui.components.shareTrack
import vk.mint.util.AppLog

/**
 * Экран «Музыка» — нативный интерфейс VK Music (моделирован по VK Mint).
 *
 * Fix #62 → P0: переработан на 3 вкладки по реальному каталогу VK.
 *  — 3 вкладки: Моя музыка / Главная / Обзор
 *  — «Главная» и «Обзор» загружают catalog.getAudio
 *  — «Моя музыка»: меню (Недавнее/Плейлисты/Альбомы/Артисты/Скачанная музыка) +
 *    «Мои треки» (count, «Перемешать все», «Списки») + список треков
 *  — Мини-плеер внизу тапабелен → открывает полноэкранный плеер.
 *
 * Волна 31 #AUDIO-BG-PAGER: список «Мои треки» (tracks/total/loading/hasMore)
 * — это StateFlow app-level пейджера AudioLibraryPager (источник истины).
 * Прежний приватный MusicTracksCache и in-композиции циклы догрузки удалены:
 * пейджер живёт на уровне приложения (VkMintApp.onCreate/appScope), переживает
 * уход с экрана, персистит чекпоинт в VkMintPrefs и продолжает после перезапуска
 * процесса. Скролл-догрузка осталась как UX — вызывает pager.kick() (сокращает
 * паузы пейджера, Fix #173 pacing сохранён).
 *
 * Волна 31 #AUDIO-ADD-INSTANT: «Добавить/удалить в моей музыке» из этого экрана
 * И из AudioPlayerScreen немедленно мутирует общий стейт пейджера
 * (addTrackFront/removeTrack) — трек виден без перезапуска приложения.
 *
 * Воспроизведение: PlayerConnection → PlayerService (Media3).
 * Скачивание: TrackDownloadManager → MusicDownloadService.
 */
@OptIn(kotlinx.coroutines.FlowPreview::class)
@Composable
fun MusicScreen(
    onOpenPlayer: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    // #MUSIC-PORT: навигация на экраны музыкальной библиотеки.
    onOpenPlaylists: () -> Unit = {},
    onOpenAlbums: () -> Unit = {},
    onOpenArtists: () -> Unit = {},
    onOpenPlaylist: (ownerId: Long, playlistId: Long, accessKey: String?) -> Unit = { _, _, _ -> },
    onOpenAlbum: (ownerId: Long, albumId: Long, accessKey: String?) -> Unit = { _, _, _ -> },
    onOpenArtist: (slug: String, name: String) -> Unit = { _, _ -> },
    onShowAll: (sectionId: String, title: String) -> Unit = { _, _ -> },
) {
    val app = VkMintApp.get()
    val scope = rememberCoroutineScope()
    // Волна 31 #AUDIO-BG-PAGER: источник истины «Моей музыки» — app-level
    // пейджер. Ленивый старт (если VkMintApp.onCreate его пропустил — auth flow).
    val pager = remember { vk.mint.data.audio.AudioLibraryPager.get() }
    val pagerState by pager.state.collectAsState()
    val tracks = pagerState.tracks
    val totalCount = pagerState.total
    val loading = pagerState.initialLoading
    val loadingMore = pagerState.fetchingPage
    val hasMore = pagerState.hasMore
    // Волна 31: ошибка загрузки живёт в пейджере (показывается при пустом списке).
    val apiErrorMessage = pagerState.error

    LaunchedEffect(Unit) {
        pager.ensureStarted()
    }

    // Fix #86: AudioMoreMenu + LyricsSheet (из ui/components/AudioMoreMenu.kt).
    // Состояние меню трека и открытой лирики — общее для всех VKTrackRow на экране.
    var moreMenuTrack by remember { mutableStateOf<Track?>(null) }
    var moreMenuExpanded by remember { mutableStateOf(false) }
    var lyricsSheetTrackId by remember { mutableStateOf<Long?>(null) }
    // Волна 40 #BOOKMARKS-TRACKS: локальные закладки треков — реактивный стейт
    // для toggle-метки меню («В закладки»/«Удалить из закладок»).
    val bookmarkTracks by app.trackBookmarksRepository.tracks.collectAsState()
    // Fix #362 #AUDIO-MENU-REAL: диалог правки трека («Редактировать трек» —
    // audio.edit, только свои треки). track — что правим.
    var editTrackDialog by remember { mutableStateOf<Track?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0=Моя музыка, 1=Главная, 2=Обзор

    // ─── Поиск (S5-1) ────────────────────────────────────────────────
    // Fix #266: расширенный поиск через audioSearchWithSections.
    // Возвращает треки + артистов + плейлисты одним запросом (catalog.getAudioSearch)
    // — работает с веб-токенами, которые не имеют доступа к audio.search.
    var searchQuery by remember { mutableStateOf("") }
    var searchResult by remember { mutableStateOf<AudioSearchResult?>(null) }
    var searchLoading by remember { mutableStateOf(false) }
    // #AUDIO-PAGING-ALL (волна 38): бесконечная догрузка результатов поиска.
    // Курсор следующей страницы (response.next_from) хранится в searchResult.nextFrom;
    // null/пусто = конец выдачи (или fallback-путь без курсора) — догрузка честно останавливается.
    var searchLoadingMore by remember { mutableStateOf(false) }
    val searchListState = rememberLazyListState()
    // Fix #269: поиск «опущен» во вкладку «Моя музыка» — поле всегда видно
    // вверху вкладки (inline OutlinedTextField в Column). Иконку поиска
    // из TopAppBar убрали — она была незаметна и пользователь жаловался
    // «поле поиска не вызывается». Теперь поиск активен = (tab==1 && query не пуст).
    // Локальная val без геттера: т.к. selectedTab/searchQuery — mutableStateOf,
    // чтение их здесь триггерит рекомпозицию, и searchActive пересчитывается.
    val searchActive = (selectedTab == 0 || selectedTab == 1) && searchQuery.isNotBlank()

    // Fix #268: FocusRequester для автофокуса при переходе на вкладку «Моя музыка»
    // с непустым запросом (или при первом показе). Без этого поле не получает
    // фокус автоматически → клавиатура не открывается.
    val searchFocusRequester = remember { FocusRequester() }

    // Дебаунс 500мс для поискового запроса.
    // Fix #281 (ANR/crash музыки-поиска):
    //  1) БЫЛО LaunchedEffect(searchQuery) — эффект перезапускался на КАЖДОМ
    //     нажатии клавиши; collect внутри ловил CancellationException в
    //     catch(Exception) и затирал searchResult пустым AudioSearchResult()
    //     → вспышка «Ничего не найдено» при наборе + потеря результатов.
    //  2) СЕЙЧАС: эффект стартует ОДИН раз (LaunchedEffect(Unit)),
    //     snapshotFlow отдаёт изменения запроса, collectLatest ОТМЕНЯЕТ
    //     предыдущий запрос при новом вводе (in-flight OkHttp-cancel через
    //     suspendCancellableCoroutine в call()), а CancellationException
    //     пробрасывается явно — структурная конкурентность не нарушается.
    LaunchedEffect(Unit) {
        snapshotFlow { searchQuery }
            .debounce(500)
            .collectLatest { query ->
                if (query.isBlank()) {
                    searchResult = null
                    return@collectLatest
                }
                searchLoading = true
                try {
                    // Запуск на IO — catalog.getAudioSearch возвращает большой JSON
                    // (200-500KB с блоками tracks+artists+playlists).
                    val result = withContext(Dispatchers.IO) {
                        app.apiClient.audioSearchWithSections(query, count = 50)
                    }
                    searchResult = result
                } catch (e: kotlinx.coroutines.CancellationException) {
                    // Fix #281: отмена (новое нажатие / уход с экрана) — НЕ ошибка.
                    // Раньше сюда попадало в catch(Exception) и затирало результат.
                    throw e
                } catch (e: Exception) {
                    AppLog.e("MusicScreen", "Search error", e)
                    searchResult = AudioSearchResult()
                } finally {
                    searchLoading = false
                }
            }
    }

    // ─── #AUDIO-PAGING-ALL: автодогрузка результатов поиска ──────────────
    // Доскроллили до конца списка треков → догружаем следующую страницу
    // catalog.getAudioSearch по курсору. Поток эмитит total при активном
    // триггере (-1 в покое): после аппенда total растёт → условие
    // перепроверяется без лишнего скролла. Гварды: in-flight страница,
    // отсутствие курсора, смена запроса во время запроса.
    LaunchedEffect(searchListState) {
        snapshotFlow {
            val info = searchListState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = info.totalItemsCount
            if (total > 0 && lastVisible >= total - 6) total else -1
        }
            .distinctUntilChanged()
            .filter { it > 0 }
            .collect {
                if (searchLoading || searchLoadingMore) return@collect
                val current = searchResult ?: return@collect
                val cursor = current.nextFrom
                if (cursor.isNullOrBlank()) return@collect
                val queryNow = searchQuery
                if (queryNow.isBlank()) return@collect
                searchLoadingMore = true
                try {
                    val more = withContext(Dispatchers.IO) {
                        app.apiClient.audioSearchWithSections(queryNow, count = 50, startFrom = cursor)
                    }
                    searchResult = AudioSearchResult(
                        tracks = (current.tracks + more.tracks)
                            .distinctBy { t -> "${t.ownerId}_${t.id}" },
                        artists = (current.artists + more.artists)
                            .distinctBy { a ->
                                if (a.id > 0L) "id_${a.id}" else "name_${a.name.lowercase()}"
                            },
                        playlists = (current.playlists + more.playlists)
                            .distinctBy { p -> "${p.ownerId}_${p.id}" },
                        nextFrom = more.nextFrom,
                    )
                    AppLog.i("MusicScreen",
                        "#AUDIO-PAGING-ALL search: +${more.tracks.size} треков (всего ${searchResult?.tracks?.size}, курсор ${if (more.nextFrom.isNullOrBlank()) "конец" else "есть"})")
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    AppLog.w("MusicScreen", "#AUDIO-PAGING-ALL search load-more failed: ${e.message}")
                } finally {
                    searchLoadingMore = false
                }
            }
    }

    val playerState by PlayerConnection.playerState.collectAsState()
    val downloads by TrackDownloadManager.downloads.collectAsState()

    // ─── Адаптивная палитра: привязка к MaterialTheme (Material You / VkMintTheme) ───
    val colorScheme = MaterialTheme.colorScheme
    val vkBlack = colorScheme.background
    val vkCard = colorScheme.surface
    val vkSurface = colorScheme.surfaceVariant
    val vkTextPrimary = colorScheme.onSurface
    val vkTextSecondary = colorScheme.onSurfaceVariant
    val vkAccent = colorScheme.primary

    val listState = rememberLazyListState()

    // ─── Скролл-догрузка (только на вкладке «Моя музыка») ─────────────
    // Волна 31 #AUDIO-BG-PAGER: источник истины — app-level пейджер
    // (AudioLibraryPager), который сам грузит страницы последовательно
    // (Fix #173: пауза 1500мс, стоп при offline, одна in-flight страница)
    // независимо от того, открыт ли этот экран. Скролл к концу списка
    // лишь сокращает текущую паузу пейджера (pager.kick()) — UX отзывчивый,
    // но без дублирующих запросов. Прежние in-композиции первичная загрузка,
    // loadMoreTracksSuspend и фоновый preload с maxPreloadPages=10 удалены —
    // их функции переехали в пейджер (включая честное логирование страниц:
    // offset/got/fresh/total/hasMore/took — тег AudioLibraryPager).
    LaunchedEffect(listState, selectedTab) {
        if (selectedTab != 0) return@LaunchedEffect

        // #AUDIO-PAGING-KICK (волна 39): без distinctUntilChanged — поток эмитит
        // при КАЖДОМ изменении layout (догрузка страницы растит totalItemsCount),
        // и пока пользователь стоит на дне списка, kick() повторяется и пейджер
        // идёт без паузы 1500мс между страницами. Прежний distinctUntilChanged
        // пропускал повторные эмиссии «true» → kick срабатывал ОДИН раз за
        // приход на дно, дальше страницы грузились по 1.5с — на библиотеке
        // 3000+ треков выглядело как «не могу долистать до конца». Троттлинг
        // 250мс держит частоту вызовов дешёвой.
        var lastKickMs = 0L
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = layoutInfo.totalItemsCount
            lastVisible >= total - 5 && total > 0
        }
            .filter { it }
            .collect {
                val now = System.currentTimeMillis()
                if (now - lastKickMs >= 250) {
                    lastKickMs = now
                    pager.kick()
                }
            }
    }

    // Fix #269: поиск «опущен» во вкладку «Моя музыка» — поле всегда видно
    // вверху вкладки как inline OutlinedTextField (см. MusicMyTracksTab).
    // Иконку поиска из TopAppBar убрали — она была незаметна, и пользователь
    // жаловался «поле поиска не вызывается». Теперь TopAppBar остаётся чистым
    // (только заголовок «Музыка»), а поиск живет внутри вкладки.
    // Сбрасываем поиск при уходе с экрана.
    DisposableEffect(Unit) {
        onDispose {
            searchQuery = ""
            searchResult = null
        }
    }

    // ─── FAB «наверх» — как в ленте, появляется при скролле вниз ────
    val showScrollToTopFab by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 200
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(vkBlack)) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(vkBlack),
    ) {
        // ─── Шапка: только вкладки ────────────────────────────────────
        MusicTabsBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            textColor = vkTextPrimary,
            secondaryColor = vkTextSecondary,
        )

        // ─── Inline поле поиска (Fix #269) ────────────────────────────
        // Поле всегда видно вверху вкладки «Моя музыка». При вводе текста
        // searchActive становится true → ниже показываются результаты поиска
        // вместо основного контента вкладки. Поле остаётся видимым всегда,
        // чтобы пользователь мог изменить запрос.
        if (selectedTab == 0 || selectedTab == 1) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    if (it.isEmpty()) {
                        searchResult = null
                        searchLoadingMore = false
                    }
                },
                placeholder = {
                    Text(
                        if (selectedTab == 1) "Поиск в загруженном" else "Поиск в библиотеке",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8F858F),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .focusRequester(searchFocusRequester),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = Color(0xFFB0A4AF),
                        modifier = Modifier.size(20.dp),
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            searchResult = null
                        }) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Очистить",
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFFB0A4AF),
                            )
                        }
                    }
                },
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedContainerColor = Color(0xFF1E1B20),
                    focusedContainerColor = Color(0xFF1E1B20),
                    cursorColor = Color(0xFFF2DAEE),
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            )
        }

        // ─── Поиск: результаты вместо контента вкладок (S5-1) ────────
        // Fix #266: расширенный UI с секциями Артисты / Плейлисты / Треки.
        // Если catalog.getAudioSearch вернул артистов — показываем их в горизонтальном
        // слайдере сверху, плейлисты — ниже, затем треки списком.
        if (searchActive) {
            val resultTracks = searchResult?.tracks ?: emptyList()
            val resultArtists = searchResult?.artists ?: emptyList()
            val resultPlaylists = searchResult?.playlists ?: emptyList()
            val hasAny = resultTracks.isNotEmpty() ||
                resultArtists.isNotEmpty() ||
                resultPlaylists.isNotEmpty()

            if (searchLoading && !hasAny) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = vkAccent)
                }
            } else if (searchQuery.isBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Введите запрос для поиска", color = vkTextSecondary, fontSize = 14.sp)
                }
            } else if (!hasAny) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Ничего не найдено", color = vkTextSecondary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("По запросу «$searchQuery»", color = vkTextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    state = searchListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = if (playerState.currentTrack != null) 100.dp else 16.dp),
                ) {
                    // ─── Секция «Артисты» (горизонтальный слайдер) ───
                    if (resultArtists.isNotEmpty()) {
                        item(key = "search_section_artists") {
                            SearchSectionHeader(
                                title = "Артисты",
                                count = resultArtists.size,
                                textColor = vkTextPrimary,
                                secondaryColor = vkTextSecondary,
                            )
                        }
                        item(key = "search_artists_row") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                // Fix #281 (crash): ключ с индексом — гарантированно
                                // уникальный. Артисты из links[] приходят с id=0 у всех,
                                // прежний key="artist_${it.id}" давал дубли →
                                // IllegalArgumentException «Key was already used» → падение.
                                itemsIndexed(
                                    resultArtists,
                                    key = { idx, artist -> "artist_${idx}_${artist.id}" },
                                ) { _, artist ->
                                    SearchArtistCard(
                                        artist = artist,
                                        cardColor = vkCard,
                                        textColor = vkTextPrimary,
                                        secondaryColor = vkTextSecondary,
                                    )
                                }
                            }
                        }
                        item(key = "search_artists_divider") {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                thickness = 0.5.dp,
                                color = vkTextSecondary.copy(alpha = 0.15f),
                            )
                        }
                    }

                    // ─── Секция «Плейлисты» (горизонтальный слайдер) ───
                    if (resultPlaylists.isNotEmpty()) {
                        item(key = "search_section_playlists") {
                            SearchSectionHeader(
                                title = "Плейлисты",
                                count = resultPlaylists.size,
                                textColor = vkTextPrimary,
                                secondaryColor = vkTextSecondary,
                            )
                        }
                        item(key = "search_playlists_row") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                // Fix #281 (crash): ключ с индексом — дубли плейлистов
                                // (VK может вернуть один плейлист в albums[] и playlists[])
                                // больше не крашат LazyColumn.
                                itemsIndexed(
                                    resultPlaylists,
                                    key = { idx, pl -> "pl_${idx}_${pl.ownerId}_${pl.id}" },
                                ) { _, pl ->
                                    SearchPlaylistCard(
                                        playlist = pl,
                                        cardColor = vkCard,
                                        textColor = vkTextPrimary,
                                        secondaryColor = vkTextSecondary,
                                    )
                                }
                            }
                        }
                        item(key = "search_playlists_divider") {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                thickness = 0.5.dp,
                                color = vkTextSecondary.copy(alpha = 0.15f),
                            )
                        }
                    }

                    // ─── Секция «Треки» (вертикальный список) ───
                    if (resultTracks.isNotEmpty()) {
                        item(key = "search_section_tracks") {
                            SearchSectionHeader(
                                title = "Треки",
                                count = resultTracks.size,
                                textColor = vkTextPrimary,
                                secondaryColor = vkTextSecondary,
                            )
                        }
                    }
                    // Fix #281 (crash): ключ с индексом — belt-and-braces к дедупу
                    // finalizeAudioSearchResult: даже если данные придут с дублями,
                    // LazyColumn не упадёт с «Key was already used».
                    itemsIndexed(
                        resultTracks,
                        key = { idx, track -> "track_${idx}_${track.ownerId}_${track.id}" },
                    ) { _, track ->
                        val current = playerState.currentTrack
                        val isCurrent = current != null &&
                            track.id == current.id &&
                            track.ownerId == current.ownerId
                        val isPlaying = isCurrent && playerState.isPlaying
                        val dl = downloads[track.id]
                        VKTrackRow(
                            track = track,
                            isCurrent = isCurrent,
                            isPlaying = isPlaying,
                            downloadState = dl,
                            cardColor = vkCard,
                            accentColor = vkAccent,
                            textColor = vkTextPrimary,
                            secondaryColor = vkTextSecondary,
                            onPlayClick = {
                                val isCur = track.id == playerState.currentTrack?.id &&
                                    track.ownerId == playerState.currentTrack?.ownerId
                                if (isCur) {
                                    PlayerConnection.togglePlayPause()
                                } else {
                                    PlayerConnection.playTrackList(resultTracks, resultTracks.indexOf(track))
                                }
                            },
                            onDownloadClick = {
                                val dl = downloads[track.id]
                                if (dl?.isCompleted == true || dl?.isInProgress == true) {
                                    TrackDownloadManager.removeDownload(track.id)
                                } else {
                                    TrackDownloadManager.enqueueDownload(track)
                                }
                            },
                            onMoreClick = {
                                moreMenuTrack = track
                                moreMenuExpanded = true
                            },
                        )
                    }
                    if (searchLoading) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = vkAccent,
                                )
                            }
                        }
                    }
                    // #AUDIO-PAGING-ALL: футер догрузки результатов поиска.
                    if (searchLoadingMore) {
                        item(key = "search_more_footer") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = vkAccent,
                                )
                            }
                        }
                    }
                }
            }
        } else {

        // #PLAYLIST-OPEN: открыть плейлист каталога — грузим треки через
        // audio.getPlaylistById и запускаем воспроизведение с первого.
        fun openPlaylistAndPlay(pl: vk.mint.data.model.CatalogPlaylist) {
            scope.launch {
                try {
                    val (_, plTracks) = app.apiClient.audioGetPlaylistById(
                        playlistId = pl.id,
                        ownerId = pl.ownerId,
                        accessKey = pl.accessKey,
                        // Fix #283: страницы по 100 (метод сам догружает весь плейлист).
                        count = 100,
                    )
                    val filtered = plTracks.filter { it.id > 0L && !it.url.isNullOrBlank() }
                    if (filtered.isNotEmpty()) {
                        PlayerConnection.playTrackList(filtered, 0)
                        android.widget.Toast.makeText(
                            app.applicationContext,
                            "Плейлист «${pl.title}» — ${filtered.size} треков",
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        android.widget.Toast.makeText(
                            app.applicationContext,
                            "Плейлист пуст или недоступен",
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                    }
                } catch (e: Exception) {
                    AppLog.e("MusicScreen", "openPlaylistAndPlay failed: ${e.message}")
                }
            }
        }

        // ─── Контент по выбранной вкладке ──────────────────────────────
        when (selectedTab) {
            0 -> MusicMyTracksTab(
                tracks = tracks,
                totalCount = totalCount,
                loading = loading,
                loadingMore = loadingMore,
                hasMore = hasMore,
                listState = listState,
                playerState = playerState,
                downloads = downloads,
                cardColor = vkCard,
                textColor = vkTextPrimary,
                secondaryColor = vkTextSecondary,
                accentColor = vkAccent,
                apiErrorMessage = apiErrorMessage,
                // Волна 31 #AUDIO-BG-PAGER: retry будит пейджер (сокращает
                // паузу/бэкофф, догружает страницу).
                // #AUTO-OFFLINE-REMOVAL (W41): clearAutoOffline убран вместе
                // с авто-офлайном #38.
                onRetry = {
                    scope.launch {
                        pager.ensureStarted()
                        pager.kick()
                    }
                },
                onOpenPlaylists = onOpenPlaylists,
                onOpenAlbums = onOpenAlbums,
                onOpenArtists = onOpenArtists,
                onOpenDownloaded = { selectedTab = 1 },
                // Волна 31 #AUDIO-QUEUE-PLAYLIST: очередь из «Моей музыки» —
                // все загруженные пейджером треки + append новых страниц в живую
                // очередь (fromMyMusic=true).
                onShuffleAll = { PlayerConnection.shuffleAll(tracks, fromMyMusic = true) },
                onPlayTrack = { track ->
                    val isCurrent = track.id == playerState.currentTrack?.id &&
                        track.ownerId == playerState.currentTrack?.ownerId
                    if (isCurrent) {
                        PlayerConnection.togglePlayPause()
                    } else {
                        PlayerConnection.playTrackList(tracks, tracks.indexOf(track), fromMyMusic = true)
                    }
                },
                onDownloadToggle = { track ->
                    val dl = downloads[track.id]
                    if (dl?.isCompleted == true || dl?.isInProgress == true) {
                        TrackDownloadManager.removeDownload(track.id)
                    } else {
                        TrackDownloadManager.enqueueDownload(track)
                    }
                },
                onMoreClick = { track ->
                    moreMenuTrack = track
                    moreMenuExpanded = true
                },
            )
            1 -> MusicDownloadedTab(
                downloads = downloads,
                libraryTracks = tracks,
                playerState = playerState,
                cardColor = vkCard,
                textColor = vkTextPrimary,
                secondaryColor = vkTextSecondary,
                accentColor = vkAccent,
                onPlayTrack = { track ->
                    val isCurrent = track.id == playerState.currentTrack?.id &&
                        track.ownerId == playerState.currentTrack?.ownerId
                    if (isCurrent) {
                        PlayerConnection.togglePlayPause()
                    } else {
                        val downloadedTrackList = downloads.values
                            .filter { it.isCompleted }
                            .map { ds ->
                                val lib = tracks.find { it.id == ds.trackId }
                                Track(
                                    id = ds.trackId,
                                    ownerId = ds.ownerId,
                                    artist = ds.artist,
                                    title = ds.title,
                                    duration = lib?.duration ?: 0,
                                    albumThumb = lib?.albumThumb,
                                )
                            }
                        val idx = downloadedTrackList.indexOfFirst { it.id == track.id }
                        PlayerConnection.playTrackList(downloadedTrackList, if (idx >= 0) idx else 0)
                    }
                },
                onShuffleAll = {
                    val downloadedTrackList = downloads.values
                        .filter { it.isCompleted }
                        .map { ds ->
                            val lib = tracks.find { it.id == ds.trackId }
                            Track(
                                id = ds.trackId,
                                ownerId = ds.ownerId,
                                artist = ds.artist,
                                title = ds.title,
                                duration = lib?.duration ?: 0,
                                albumThumb = lib?.albumThumb,
                            )
                        }
                    if (downloadedTrackList.isNotEmpty()) {
                        PlayerConnection.shuffleAll(downloadedTrackList)
                    }
                },
                onMoreClick = { track ->
                    moreMenuTrack = track
                    moreMenuExpanded = true
                },
            )
            2 -> DiscoverTab(
                textColor = vkTextPrimary,
                secondaryColor = vkTextSecondary,
                accentColor = vkAccent,
                cardColor = vkCard,
                playerState = playerState,
                downloads = downloads,
                onPlaylistClick = { pl -> openPlaylistAndPlay(pl) },
                onShowAll = onShowAll,
            )
            // #MUSIC-UPDATES: «Обновления» — catalog.getSection(updates),
            // section-id из «музыка_Обновления.html». Рендер как DiscoverTab.
            3 -> DiscoverTab(
                textColor = vkTextPrimary,
                secondaryColor = vkTextSecondary,
                accentColor = vkAccent,
                cardColor = vkCard,
                playerState = playerState,
                downloads = downloads,
                onPlaylistClick = { pl -> openPlaylistAndPlay(pl) },
                onShowAll = onShowAll,
                section = "updates",
            )
            // #MUSIC-RADIO: «Радио» — список радиостанций.
            // Section-id радио пока не найден в снапшотах → заглушка.
            // Инфраструктура готова: AudioRadioStation, audio.radioGetById,
            // audio.followRadioStation. Нужен section-id из живого m.vk.ru
            // после JS: m.vk.ru/audios<id>?section=radiostations.
            4 -> RadioStubTab(
                textColor = vkTextPrimary,
                secondaryColor = vkTextSecondary,
                accentColor = vkAccent,
                cardColor = vkCard,
            )
        }
        } // end else (!searchActive)

        // ─── Баннер ошибки воспроизведения (Fix #59) ───
        val errorMessage = playerState.error
        if (errorMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFB3261E).copy(alpha = 0.92f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = errorMessage,
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { PlayerConnection.next() }) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Следующий",
                        tint = Color.White,
                    )
                }
            }
        }

        // Fix #71: мини-плеер удалён из MusicScreen — теперь он глобальный
        // (рендерится в VkMintNavHost над NavigationBar на всех экранах).
        // На экране Музыки он тоже виден через VkMintNavHost.

        // Fix #86: AudioMoreMenu — контекстное меню трека.
        // Состояние moreMenuTrack/moreMenuExpanded меняется из VKTrackRow.onMoreClick.
        // DropdownMenu рендерится как overlay — достаточно один Box с якорем.
        moreMenuTrack?.let { track ->
            Box(modifier = Modifier.fillMaxWidth()) {
                // Fix #388 #AUDIO-TOGGLE-OWNING: владение треком = ownerId трека
                // равен моему userId (userId()==0 — идентификатор не получен,
                // тогда тумблер всегда «Добавить»).
                val myUserId = app.exchangeAuthRepository.userId()
                val trackIsOwn = myUserId != 0L && track.ownerId == myUserId
                // Волна 40 #BOOKMARKS-TRACKS: трек уже в локальных закладках?
                val trackBookmarked = bookmarkTracks.any { it.id == track.id && it.ownerId == track.ownerId }
                vk.mint.ui.components.AudioMoreMenu(
                    track = track,
                    expanded = moreMenuExpanded,
                    onDismiss = {
                        moreMenuExpanded = false
                        moreMenuTrack = null
                    },
                    // Fix #388 #AUDIO-TOGGLE-OWNING: см. trackIsOwn выше.
                    isOwn = trackIsOwn,
                    // Fix #388 #AUDIO-TOGGLE-OWNING: тумблер «В моей музыке» с
                    // состоянием (паритет веб-VK MusicAudio_ToggleOwning):
                    // не в моей музыке → audioAddReliable (audio.add → web-fallback
                    // al_audio.php?act=add); в моей → audio.delete СРАЗУ, как в
                    // вебе. Тосты с РЕАЛЬНОЙ ошибкой VK; при ошибке состояние
                    // списка НЕ меняется (честность).
                    isOwned = trackIsOwn,
                    // Волна 40 #BOOKMARKS-TRACKS: toggle-метка пункта меню.
                    isBookmarked = trackBookmarked,
                    onToggleOwn = {
                        val t = track
                        scope.launch {
                            if (trackIsOwn) {
                                var ok = false
                                var errText: String? = null
                                try {
                                    ok = app.apiClient.audioDelete(t.id, t.ownerId)
                                    if (!ok) errText = app.apiClient.lastApiError
                                } catch (e: Exception) {
                                    AppLog.e("MusicScreen", "#AUDIO-TOGGLE-OWNING delete error", e)
                                    errText = e.message
                                }
                                if (ok) {
                                    // Волна 31 #AUDIO-ADD-INSTANT: удаление сразу
                                    // отражается в общем стейте пейджера (в т.ч. на
                                    // других экранах/при возврате — без перезапуска).
                                    pager.removeTrack(t)
                                    android.widget.Toast.makeText(
                                        app.applicationContext,
                                        "Удалено из моей музыки",
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                                } else {
                                    // NULL-ЯВНО: errText nullable — явный if вместо ?:.
                                    val shown = if (errText != null) errText else "ошибка сети"
                                    android.widget.Toast.makeText(
                                        app.applicationContext,
                                        "Не удалось удалить: $shown",
                                        android.widget.Toast.LENGTH_LONG,
                                    ).show()
                                }
                            } else {
                                // #AUDIO-ADD-WEB (2026-09-08): раньше результат
                                // audio.add игнорировался — при ошибке VK (для
                                // web-токенов audio.add обычно закрыт правами)
                                // меню просто закрывалось и трек молча НЕ
                                // добавлялся. audioAddReliable: audio.add →
                                // web-fallback al_audio.php?act=add (паттерн VK
                                // web) + честные тосты с РЕАЛЬНОЙ ошибкой.
                                val (ok, err) = app.apiClient.audioAddReliable(t)
                                if (ok) {
                                    // Волна 31 #AUDIO-ADD-INSTANT: трек немедленно
                                    // в начале «Моей музыки» (общий стейт пейджера,
                                    // позиция как у VK — новейшие первыми).
                                    pager.addTrackFront(t)
                                    android.widget.Toast.makeText(
                                        app.applicationContext,
                                        "Добавлено в мою музыку",
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                                } else {
                                    AppLog.w("MusicScreen", "#AUDIO-ADD-WEB add failed: $err")
                                    // NULL-ЯВНО: err — nullable Pair-компонент; для
                                    // тоста честный фолбэк на общий текст сети.
                                    val shownErr = if (err != null) err else "ошибка сети"
                                    android.widget.Toast.makeText(
                                        app.applicationContext,
                                        "Не удалось добавить: $shownErr",
                                        android.widget.Toast.LENGTH_LONG,
                                    ).show()
                                }
                            }
                        }
                    },
                    onRestore = {
                        val t = track
                        scope.launch {
                            // Fix #362 #AUDIO-MENU-REAL: фидбек на «Восстановить»
                            // (audio.restore). Трек вернётся в конец библиотеки —
                            // попадёт в список при следующей подгрузке страниц.
                            var ok = false
                            var errText: String? = null
                            try {
                                ok = app.apiClient.audioRestore(t.id, t.ownerId)
                                if (!ok) errText = app.apiClient.lastApiError
                            } catch (e: Exception) {
                                AppLog.e("MusicScreen", "audioRestore error", e)
                                errText = e.message
                            }
                            val msg = if (ok) "Трек восстановлен — появится в конце списка"
                                      else "Не удалось восстановить: ${errText ?: "ошибка сети"}"
                            android.widget.Toast.makeText(app.applicationContext, msg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    onShare = {
                        // #SHARE-AUDIO (2026-08-03): реализован Android ACTION_SEND
                        // chooser с текстом "Title — Artist\nhttps://vk.com/audio...".
                        // Волна 46: заменено на общий helper vk.mint.ui.components.shareTrack.
                        shareTrack(app.applicationContext, track)
                    },
                    // #FAVE-AUDIO (2026-08-03): fave.add(type="audio") НЕ существует —
                    // у fave.* нет аудио-раздела. #BOOKMARKS-FIX (2026-09-12) сводил
                    // «закладку трека» к «Моей музыке» — но тестер (обратная связь
                    // 2026-09-12) ожидает РАЗДЕЛЬНЫЕ сущности: треки в Закладках.
                    // Волна 40 #BOOKMARKS-TRACKS: «В закладки» = ЛОКАЛЬНАЯ закладка
                    // (TrackBookmarksRepository → VkMintPrefs), видна в BookmarksScreen
                    // раздел «Треки» (воспроизведение + удаление). Toggle-семантика:
                    // повторный тап по «Удалить из закладок» убирает. «Моя музыка»
                    // осталась отдельным первым пунктом меню (onToggleOwn выше).
                    onBookmark = {
                        val t = track
                        scope.launch {
                            val nowBookmarked = try {
                                app.trackBookmarksRepository.toggle(t)
                            } catch (e: Exception) {
                                AppLog.e("MusicScreen", "track bookmark toggle error", e)
                                null
                            }
                            // Честный фидбек: успех/снятие/сбой — три разных текста.
                            try {
                                val msg = when (nowBookmarked) {
                                    true -> "Добавлено в закладки"
                                    false -> "Удалено из закладок"
                                    null -> "Не удалось обновить закладку — попробуйте ещё раз"
                                }
                                android.widget.Toast.makeText(app.applicationContext, msg, android.widget.Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                AppLog.w("MusicScreen", "Toast display failed: ${e.message}")
                            }
                        }
                    },
                    onCopyLink = {
                        // Формируем прямую ссылку и кладём в ClipboardManager.
                        val link = "https://vk.com/audio${track.ownerId}_${track.id}"
                        try {
                            val ctx = app.applicationContext
                            val cm = ctx.getSystemService(android.content.ClipboardManager::class.java)
                            cm?.setPrimaryClip(android.content.ClipData.newPlainText("VK audio", link))
                            AppLog.i("MusicScreen", "Audio link copied: $link")
                        } catch (e: Exception) {
                            AppLog.e("MusicScreen", "copyLink error", e)
                        }
                    },
                    onShowLyrics = {
                        track.lyricsId?.let { lyricsSheetTrackId = it }
                    },
                    onShowRecommendations = {
                        val t = track
                        scope.launch {
                            try {
                                // Fix #362 #AUDIO-MENU-REAL: «Похожие» считаются по
                                // ЭТОМУ треку (audio.getRecommendations target_audio
                                // = ownerId_id), а не общий поток рекомендаций.
                                val (_, recs) = app.apiClient.audioGetRecommendations(
                                    count = 30,
                                    targetAudio = "${t.ownerId}_${t.id}",
                                )
                                if (recs.isNotEmpty()) {
                                    PlayerConnection.playTrackList(recs, 0)
                                    android.widget.Toast.makeText(
                                        app.applicationContext,
                                        "Похожие на «${t.title}» — ${recs.size} треков",
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                                } else {
                                    android.widget.Toast.makeText(
                                        app.applicationContext,
                                        "Похожие не найдены",
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            } catch (e: Exception) {
                                AppLog.e("MusicScreen", "getRecommendations error", e)
                            }
                        }
                    },
                    onDislike = {
                        val t = track
                        scope.launch {
                            // Fix #362 #AUDIO-MENU-REAL: фидбек на «Не нравится».
                            var ok = false
                            var errText: String? = null
                            try {
                                ok = app.apiClient.audioAddDislike(
                                    listOf("${t.ownerId}_${t.id}")
                                )
                                if (!ok) errText = app.apiClient.lastApiError
                            } catch (e: Exception) {
                                AppLog.e("MusicScreen", "addDislike error", e)
                                errText = e.message
                            }
                            val msg = if (ok) "Учтено в рекомендациях"
                                      else "Не удалось: ${errText ?: "ошибка сети"}"
                            android.widget.Toast.makeText(app.applicationContext, msg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    // Fix #362 #AUDIO-MENU-REAL: «Редактировать трек» (свои) —
                    // audio.edit через диалог правки артиста/названия.
                    onEdit = { editTrackDialog = track },
                    // Fix #362 #AUDIO-MENU-REAL: «Открыть альбом» — навигация
                    // (параметр onOpenAlbum у экрана уже есть, раньше не
                    // подключался к меню — мёртвая кнопка).
                    onOpenAlbum = {
                        val albumId = track.albumId
                        if (albumId != null && albumId != 0L) {
                            onOpenAlbum(track.ownerId, albumId, track.accessKey)
                        }
                    },
                    // Fix #362 #AUDIO-MENU-REAL: «Воспроизвести следующей» —
                    // вставка в очередь после текущего (PlayerConnection.playNext).
                    onSetNext = { PlayerConnection.playNext(track) },
                )
            }
        }

        // Fix #86: LyricsSheet — открыт когда lyricsSheetTrackId != null.
        lyricsSheetTrackId?.let { lid ->
            vk.mint.ui.components.LyricsSheet(
                lyricsId = lid,
                onDismiss = { lyricsSheetTrackId = null },
            )
        }

        // Fix #362 #AUDIO-MENU-REAL: диалог правки трека — открыт когда
        // editTrackDialog != null («Редактировать трек» из меню ⋮).
        // NULL-ЯВНО: паттерн moreMenuTrack?/lyricsSheetTrackId? выше по файлу.
        editTrackDialog?.let { t ->  // NULL-ЯВНО (паттерн moreMenuTrack? выше)
            EditTrackDialog(
                track = t,
                onDismiss = { editTrackDialog = null },
                onSaved = { updated ->
                    editTrackDialog = null
                    // Обновляем трек в общем стейте пейджера (волна 31:
                    // MusicTracksCache удалён, источник истины — пейджер).
                    pager.replaceTrack(updated)
                },
            )
        }
    } // end Column

        // ─── FAB «наверх» ────────────────────────────────────────────
        AnimatedVisibility(
            visible = showScrollToTopFab && selectedTab == 0 && !searchActive,
            enter = fadeIn(expressiveSpring()) + scaleIn(expressiveSpring()),
            exit = fadeOut(expressiveSpring()) + scaleOut(expressiveSpring()),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp),
        ) {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                containerColor = vkSurface,
                contentColor = vkTextPrimary,
            ) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Наверх")
            }
        }
    } // end outer Box
}

// ─── Fix #362 #AUDIO-MENU-REAL: диалог правки трека (audio.edit) ─────────────

/**
 * Диалог «Редактировать трек» — правка артиста и названия СВОЕГО трека через
 * audio.edit (VKA.audioEdit). Поля предзаполнены текущими значениями.
 *
 * onSaved вызывается ТОЛЬКО при успешном ответе VK (response=1) и получает
 * обновлённый Track — caller обновляет список/кэш. При ошибке — честный
 * тост с текстом ошибки VK (audio.edit может быть закрыт для web-токена,
 * как audio.add — замалчивать нельзя, no-stub политика).
 */
@Composable
private fun EditTrackDialog(
    track: Track,
    onDismiss: () -> Unit,
    onSaved: (Track) -> Unit,
) {
    val app = VkMintApp.get()
    // Управляемый scope диалога (НЕ MainScope — тот утекал бы при закрытии).
    val scope = rememberCoroutineScope()
    var artist by remember { mutableStateOf(track.artist) }
    var title by remember { mutableStateOf(track.title) }
    var saving by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Редактировать трек") },
        text = {
            Column {
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Артист") },
                    singleLine = true,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    singleLine = true,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                )
                // NULL-ЯВНО: errorText — nullable state, рендер только при значении.
                if (errorText != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorText ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && title.isNotBlank(),
                onClick = {
                    saving = true
                    errorText = null
                    scope.launch {
                        var ok = false
                        var err: String? = null
                        try {
                            ok = app.apiClient.audioEdit(
                                ownerId = track.ownerId,
                                audioId = track.id,
                                artist = artist.trim(),
                                title = title.trim(),
                            )
                            if (!ok) err = app.apiClient.lastApiError
                        } catch (e: Exception) {
                            AppLog.e("MusicScreen", "audioEdit error", e)
                            err = e.message
                        }
                        saving = false
                        if (ok) {
                            android.widget.Toast.makeText(
                                app.applicationContext,
                                "Трек обновлён",
                                android.widget.Toast.LENGTH_SHORT,
                            ).show()
                            onSaved(
                                track.copy(artist = artist.trim(), title = title.trim()),
                            )
                        } else {
                            // Честный текст ошибки VK (права/сеть) — не молчим.
                            errorText = "Не удалось сохранить: ${err ?: "ошибка сети"}"
                        }
                    }
                },
            ) {
                Text(if (saving) "Сохранение…" else "Сохранить")
            }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) { Text("Отмена") }
        },
    )
}

// ─── Header с 5 вкладками ────────────────────────────────────────────────────

@Composable
private fun MusicTabsBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    textColor: Color,
    secondaryColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 10.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val isLibrary = selectedTab == 0
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (isLibrary) Color(0xFF382F37) else Color.Transparent)
                .clickable { onTabSelected(0) }
                .padding(horizontal = 22.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Библиотека",
                color = if (isLibrary) Color(0xFFF2DAEE) else textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        val isDownloaded = selectedTab == 1
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (isDownloaded) Color(0xFF382F37) else Color.Transparent)
                .clickable { onTabSelected(1) }
                .padding(horizontal = 22.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Загружено",
                color = if (isDownloaded) Color(0xFFF2DAEE) else textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ─── Режим сортировки треков ────────────────────────────────────────────────
enum class TrackSortOrder(val title: String) {
    DEFAULT("по умолчанию"),
    NEWEST_FIRST("сначала новые"),
    OLDEST_FIRST("сначала старые"),
    BY_TITLE("по названию"),
    BY_ARTIST("по исполнителю"),
}

// ─── Плейсхолдер сдвоенных нот из VK Music ───────────────────────────────────
@Composable
internal fun DualNotesPlaceholder(
    modifier: Modifier = Modifier,
    tint: Color = Color.White.copy(alpha = 0.85f),
) {
    Canvas(modifier = modifier.size(22.dp)) {
        val w = size.width
        val h = size.height

        val headRadiusX = w * 0.15f
        val headRadiusY = headRadiusY(w, h)

        // Левая нотная головка
        val leftHeadCenter = Offset(w * 0.28f, h * 0.78f)
        drawOval(
            color = tint,
            topLeft = Offset(leftHeadCenter.x - headRadiusX, leftHeadCenter.y - headRadiusY),
            size = androidx.compose.ui.geometry.Size(headRadiusX * 2, headRadiusY * 2),
        )

        // Правая нотная головка
        val rightHeadCenter = Offset(w * 0.72f, h * 0.68f)
        drawOval(
            color = tint,
            topLeft = Offset(rightHeadCenter.x - headRadiusX, rightHeadCenter.y - headRadiusY),
            size = androidx.compose.ui.geometry.Size(headRadiusX * 2, headRadiusY * 2),
        )

        // Левый штиль
        val stemWidth = 1.8.dp.toPx()
        val leftStemX = leftHeadCenter.x + headRadiusX * 0.7f
        val leftStemTop = h * 0.22f
        drawLine(
            color = tint,
            start = Offset(leftStemX, leftHeadCenter.y),
            end = Offset(leftStemX, leftStemTop),
            strokeWidth = stemWidth,
            cap = StrokeCap.Round,
        )

        // Правый штиль
        val rightStemX = rightHeadCenter.x + headRadiusX * 0.7f
        val rightStemTop = h * 0.14f
        drawLine(
            color = tint,
            start = Offset(rightStemX, rightHeadCenter.y),
            end = Offset(rightStemX, rightStemTop),
            strokeWidth = stemWidth,
            cap = StrokeCap.Round,
        )

        // Связующая перекладина (вязка)
        drawLine(
            color = tint,
            start = Offset(leftStemX, leftStemTop),
            end = Offset(rightStemX, rightStemTop),
            strokeWidth = stemWidth * 1.5f,
            cap = StrokeCap.Round,
        )
    }
}

internal fun headRadiusY(w: Float, h: Float): Float = h * 0.11f

// ─── Вкладка «Моя музыка»: меню + мои треки с пагинацией ─────────────────────

@Composable
private fun MusicMyTracksTab(
    tracks: List<Track>,
    totalCount: Int,
    loading: Boolean,
    loadingMore: Boolean,
    hasMore: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    playerState: vk.mint.data.model.PlayerState,
    downloads: Map<Long, DownloadState>,
    cardColor: Color,
    textColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    apiErrorMessage: String?,
    onRetry: (() -> Unit)? = null,
    onOpenPlaylists: () -> Unit = {},
    onOpenAlbums: () -> Unit = {},
    onOpenArtists: () -> Unit = {},
    onOpenDownloaded: () -> Unit = {},
    onShuffleAll: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onDownloadToggle: (Track) -> Unit,
    onMoreClick: (Track) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val currentTrack = playerState.currentTrack
    val hasCurrentInList = currentTrack != null &&
        tracks.any { it.id == currentTrack.id && it.ownerId == currentTrack.ownerId }

    var sortOrder by remember { mutableStateOf(TrackSortOrder.DEFAULT) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val displayedTracks = remember(tracks, sortOrder) {
        when (sortOrder) {
            TrackSortOrder.DEFAULT -> tracks
            TrackSortOrder.NEWEST_FIRST -> tracks
            TrackSortOrder.OLDEST_FIRST -> tracks.reversed()
            TrackSortOrder.BY_TITLE -> tracks.sortedBy { it.title.lowercase() }
            TrackSortOrder.BY_ARTIST -> tracks.sortedBy { it.artist.lowercase() }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = if (playerState.currentTrack != null) 100.dp else 16.dp),
    ) {
        // ─── Меню разделов (7 сквиркл-категорий) ───
        item {
            MyMusicMenuList(
                textColor = textColor,
                secondaryColor = secondaryColor,
                cardColor = cardColor,
                onOpenPlaylists = onOpenPlaylists,
                onOpenAlbums = onOpenAlbums,
                onOpenArtists = onOpenArtists,
                onOpenDownloaded = onOpenDownloaded,
            )
        }

        // ─── Заголовок «Мои треки» + счётчик + селектор сортировки ───
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Мои треки",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        hasMore && totalCount > 0 -> "${tracks.size} / ~$totalCount"
                        hasMore -> "${tracks.size}…"
                        else -> "${tracks.size}"
                    },
                    color = Color(0xFF9E96A0),
                    fontSize = 14.sp,
                )
                Spacer(modifier = Modifier.weight(1f))

                // Кнопка «К треку» — прокрутка к текущему играющему треку
                if (hasCurrentInList) {
                    val target = currentTrack // captured val
                    Row(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable {
                                val idx = displayedTracks.indexOfFirst {
                                    it.id == target.id && it.ownerId == target.ownerId
                                }
                                if (idx >= 0) {
                                    // LazyColumn items: 0=меню, 1=заголовок, 2=перемешать, 3...=треки
                                    val targetIndex = 3 + idx
                                    scope.launch { listState.animateScrollToItem(targetIndex) }
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = null,
                            tint = Color(0xFFF2DAEE),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "К треку",
                            color = Color(0xFFF2DAEE),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Селектор сортировки «по умолчанию ⌵»
                Box {
                    Row(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable { sortMenuExpanded = true }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = sortOrder.title,
                            color = Color(0xFF9E96A0),
                            fontSize = 13.sp,
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Сортировка",
                            tint = Color(0xFF9E96A0),
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        modifier = Modifier.background(Color(0xFF282329)),
                    ) {
                        TrackSortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = order.title,
                                        color = if (sortOrder == order) Color(0xFFF2DAEE) else Color.White,
                                        fontWeight = if (sortOrder == order) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                onClick = {
                                    sortOrder = order
                                    sortMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        }

        // ─── Пункт «Перемешать» (первый элемент списка треков со сквирклом) ───
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onShuffleAll)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color(0xFF282329)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Перемешать",
                        tint = Color(0xFFF2DAEE),
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Перемешать",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
        }

        // ─── Состояния ───
        if (loading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = accentColor)
                }
            }
        }

        if (apiErrorMessage != null && tracks.isEmpty() && !loading) {
            item {
                Text(
                    text = apiErrorMessage,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                )
                // Fix #367: честный retry — снимает авто-офлайн и перезагружает
                // первую страницу (раньше экран мог застрять в «Нет музыки» навсегда).
                if (onRetry != null) {
                    TextButton(
                        onClick = onRetry,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        Text("Повторить", color = accentColor)
                    }
                }
            }
        }

        // ─── Список треков ───
        items(displayedTracks, key = { "${it.ownerId}_${it.id}" }) { track ->
            val current = playerState.currentTrack
            val isCurrent = current != null &&
                track.id == current.id &&
                track.ownerId == current.ownerId
            val isPlaying = isCurrent && playerState.isPlaying
            val dl = downloads[track.id]
            VKTrackRow(
                track = track,
                isCurrent = isCurrent,
                isPlaying = isPlaying,
                downloadState = dl,
                cardColor = cardColor,
                accentColor = accentColor,
                textColor = textColor,
                secondaryColor = secondaryColor,
                onPlayClick = { onPlayTrack(track) },
                onDownloadClick = { onDownloadToggle(track) },
                onMoreClick = { onMoreClick(track) },
            )
        }

        // ─── Футер: «Загрузка…» / «Больше нет» / пусто ───
        if (loadingMore) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = accentColor,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Загрузка…", color = secondaryColor, fontSize = 13.sp)
                    }
                }
            }
        } else if (!hasMore && tracks.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Это все треки", color = secondaryColor, fontSize = 12.sp)
                }
            }
        }
    }
}

// ─── Меню «Моя музыка»: Недавнее / Плейлисты / Альбомы / ... ─────────────────

@Composable
private fun MyMusicMenuList(
    textColor: Color,
    secondaryColor: Color,
    cardColor: Color,
    onOpenPlaylists: () -> Unit = {},
    onOpenAlbums: () -> Unit = {},
    onOpenArtists: () -> Unit = {},
    onOpenDownloaded: () -> Unit = {},
) {
    val app = VkMintApp.get()
    val scope = rememberCoroutineScope()
    var showPlaylists by remember { mutableStateOf(false) }
    var showDownloaded by remember { mutableStateOf(false) }
    var playlists by remember { mutableStateOf<List<AudioPlaylist>>(emptyList()) }
    var playlistsLoading by remember { mutableStateOf(false) }
    // #AUDIO-PAGING-ALL (волна 38): догрузка плейлистов в диалоге
    // (было 30 — при большем числе плейлистов до конца не добраться).
    var playlistsOffset by remember { mutableStateOf(0) }
    var playlistsHasMore by remember { mutableStateOf(false) }
    var playlistsLoadingMore by remember { mutableStateOf(false) }
    val playlistsListState = rememberLazyListState()

    // #AUDIO-PAGING-ALL: догрузка следующей страницы плейлистов.
    fun loadMoreMenuPlaylists() {
        if (playlistsLoadingMore || !playlistsHasMore) return
        scope.launch {
            playlistsLoadingMore = true
            try {
                val (_, page) = app.apiClient.audioGetPlaylists(count = 50, offset = playlistsOffset)
                playlistsOffset += page.size
                playlists = (playlists + page).distinctBy { "${it.ownerId}_${it.id}" }
                // #AUDIO-PAGING-HOLE (волна 41): непустая страница ≠ конец списка
                // (VK может отдать короткую страницу в середине листинга).
                playlistsHasMore = page.isNotEmpty()
            } catch (e: Exception) {
                AppLog.e("MyMusicMenuList", "Failed to load more playlists", e)
            } finally {
                playlistsLoadingMore = false
            }
        }
    }

    // Загрузка плейлистов при открытии диалога
    LaunchedEffect(showPlaylists) {
        if (showPlaylists) {
            playlistsLoading = true
            try {
                val (_, result) = app.apiClient.audioGetPlaylists(count = 50, offset = 0)
                playlists = result
                playlistsOffset = result.size
                // #AUDIO-PAGING-HOLE (волна 41): непустая страница ≠ конец списка.
                playlistsHasMore = result.isNotEmpty()
            } catch (e: Exception) {
                AppLog.e("MyMusicMenuList", "Failed to load playlists", e)
                playlists = emptyList()
            } finally {
                playlistsLoading = false
            }
        }
    }

    // #AUDIO-PAGING-ALL: автодогрузка при доскролле до конца списка диалога.
    LaunchedEffect(showPlaylists, playlistsListState) {
        if (!showPlaylists) return@LaunchedEffect
        snapshotFlow {
            val info = playlistsListState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = info.totalItemsCount
            if (total > 0 && lastVisible >= total - 3) total else -1
        }
            .distinctUntilChanged()
            .collect { if (it > 0) loadMoreMenuPlaylists() }
    }

    // Диалог плейлистов
    if (showPlaylists) {
        AlertDialog(
            onDismissRequest = { showPlaylists = false },
            containerColor = cardColor,
            shape = MaterialTheme.shapes.large,
            title = {
                Text(
                    text = "Плейлисты",
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                if (playlistsLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (playlists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Нет плейлистов", color = secondaryColor, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        state = playlistsListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                    ) {
                        items(playlists) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable {
                                        scope.launch {
                                            try {
                                                val (_, tracks) = app.apiClient.audioGetPlaylistTracks(
                                                    playlistId = playlist.id,
                                                    ownerId = playlist.ownerId,
                                                    accessKey = playlist.accessKey,
                                                )
                                                val filtered = tracks
                                                    .filter { it.id > 0L && it.ownerId != 0L && !it.url.isNullOrBlank() }
                                                if (filtered.isNotEmpty()) {
                                                    PlayerConnection.playTrackList(filtered, 0)
                                                }
                                            } catch (e: Exception) {
                                                AppLog.e("PlaylistsDialog", "Failed to load playlist tracks", e)
                                            }
                                            showPlaylists = false
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Обложка плейлиста
                                val coverUrl = playlist.coverUrl
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                                ),
                                            ),
                                        ),
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
                                            imageVector = Icons.Filled.MusicNote,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                // Название + инфо
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.title,
                                        color = textColor,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Row {
                                        if (playlist.count > 0) {
                                            Text(
                                                text = "${playlist.count} треков",
                                                color = secondaryColor,
                                                fontSize = 12.sp,
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        if (playlist.followers > 0) {
                                            Text(
                                                text = "${playlist.followers} подписчиков",
                                                color = secondaryColor,
                                                fontSize = 12.sp,
                                            )
                                        }
                                    }
                                }
                                // Кнопка play
                                FilledIconButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                val (_, tracks) = app.apiClient.audioGetPlaylistTracks(
                                                    playlistId = playlist.id,
                                                    ownerId = playlist.ownerId,
                                                    accessKey = playlist.accessKey,
                                                )
                                                val filtered = tracks
                                                    .filter { it.id > 0L && it.ownerId != 0L && !it.url.isNullOrBlank() }
                                                if (filtered.isNotEmpty()) {
                                                    PlayerConnection.playTrackList(filtered, 0)
                                                }
                                            } catch (e: Exception) {
                                                AppLog.e("PlaylistsDialog", "Failed to play playlist", e)
                                            }
                                            showPlaylists = false
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Играть",
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                // #VK-MUSIC-SAVER-PORT: кнопка «скачать плейлист»
                                // (папка + обложка + tracklist.txt + все треки).
                                FilledTonalIconButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                val (_, tracks) = app.apiClient.audioGetPlaylistTracks(
                                                    playlistId = playlist.id,
                                                    ownerId = playlist.ownerId,
                                                    accessKey = playlist.accessKey,
                                                )
                                                val filtered = tracks
                                                    .filter { it.id > 0L && it.ownerId != 0L && !it.url.isNullOrBlank() }
                                                if (filtered.isNotEmpty()) {
                                                    TrackDownloadManager.enqueuePlaylistDownload(
                                                        playlistTitle = playlist.title,
                                                        coverUrl = playlist.coverUrl,
                                                        tracks = filtered,
                                                    )
                                                    android.widget.Toast.makeText(
                                                        app.applicationContext,
                                                        "Скачивание плейлиста: ${filtered.size} треков",
                                                        android.widget.Toast.LENGTH_SHORT,
                                                    ).show()
                                                }
                                            } catch (e: Exception) {
                                                AppLog.e("PlaylistsDialog", "Failed to download playlist", e)
                                            }
                                            showPlaylists = false
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
                                        contentDescription = "Скачать плейлист",
                                    )
                                }
                            }
                        }
                        // #AUDIO-PAGING-ALL: футер догрузки плейлистов меню.
                        if (playlistsLoadingMore) {
                            item(key = "pl_menu_more") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
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
                TextButton(onClick = { showPlaylists = false }) {
                    Text("Закрыть", color = MaterialTheme.colorScheme.primary)
                }
            },
        )
    }

    // ─── Диалог «Скачанная музыка» ──────────────────────────────
    if (showDownloaded) {
        val downloadedStates by TrackDownloadManager.downloads.collectAsState()
        val downloadedTracks = downloadedStates.values
            .filter { it.isCompleted }
            .sortedByDescending { it.trackId }
        // #ANR-MAIN-IO (2026-09-04): getTotalDownloadedBytes делает getLocalFile
        // (stat + magic-byte валидация с открытием файла) на каждый трек — для
        // большой скачанной библиотеки это тысячи файловых операций на КАЖДУЮ
        // рекомпозицию диалога. Считаем на Dispatchers.IO по ключу размера
        // набора завершённых.
        var totalBytesState by remember { mutableStateOf(0L) }
        LaunchedEffect(downloadedTracks.size) {
            totalBytesState = withContext(Dispatchers.IO) {
                TrackDownloadManager.getTotalDownloadedBytes()
            }
        }
        val totalBytes = totalBytesState
        val sizeStr = when {
            totalBytes < 1024 -> "$totalBytes Б"
            totalBytes < 1024 * 1024 -> "${"%.1f".format(totalBytes / 1024.0)} КБ"
            totalBytes < 1024 * 1024 * 1024 -> "${"%.1f".format(totalBytes / (1024.0 * 1024))} МБ"
            else -> "${"%.2f".format(totalBytes / (1024.0 * 1024 * 1024))} ГБ"
        }

        AlertDialog(
            onDismissRequest = { showDownloaded = false },
            containerColor = cardColor,
            shape = MaterialTheme.shapes.large,
            title = {
                Column {
                    Text("Скачанная музыка", color = textColor, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${downloadedTracks.size} треков • $sizeStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryColor,
                    )
                }
            },
            text = {
                if (downloadedTracks.isEmpty()) {
                    Text("Нет скачанных треков", color = secondaryColor)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                    ) {
                        items(
                            items = downloadedTracks,
                            key = { it.trackId },
                        ) { state ->
                            // Audit #40: используем collectAsState() вместо .value —
                            // иначе UI не рекомпозится при смене текущего трека.
                            val playerState by PlayerConnection.playerState.collectAsState()
                            val isCurrentTrack = playerState.currentTrack?.id == state.trackId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .background(if (isCurrentTrack) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable {
                                        // Fix #162: ранее трек искался в playerState.queue.
                                        // Если трека там не было (плеер пуст / играл другой
                                        // плейлист) — matchingTrack == null и playTrackById
                                        // НЕ вызывался → тап по скачанному треку ничего не
                                        // делал. Теперь строим Track-объекты из DownloadState
                                        // и запускаем playTrackList. toMediaItem берёт
                                        // локальный файл через getLocalFile(id) — url=null
                                        // не проблема (Fix #55).
                                        val downloadedTrackList = downloadedTracks.map { ds ->
                                            Track(
                                                id = ds.trackId,
                                                ownerId = ds.ownerId,
                                                artist = ds.artist,
                                                title = ds.title,
                                                duration = 0,
                                            )
                                        }
                                        val idx = downloadedTrackList.indexOfFirst { it.id == state.trackId }
                                        if (idx >= 0) {
                                            AppLog.i("MusicScreen", "Play downloaded: #${state.trackId} idx=$idx of ${downloadedTrackList.size}")
                                            PlayerConnection.playTrackList(downloadedTrackList, idx)
                                        } else {
                                            AppLog.w("MusicScreen", "Play downloaded: track #${state.trackId} not found in downloaded list")
                                        }
                                        showDownloaded = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = state.title.ifEmpty { "Трек #${state.trackId}" },
                                        color = if (isCurrentTrack) MaterialTheme.colorScheme.primary else textColor,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = state.artist.ifEmpty { "—" },
                                        color = secondaryColor,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                IconButton(onClick = {
                                    TrackDownloadManager.removeDownload(state.trackId)
                                }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Удалить",
                                        tint = Color(0xFFE53935),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (downloadedTracks.isNotEmpty()) {
                    TextButton(onClick = {
                        downloadedTracks.forEach { TrackDownloadManager.removeDownload(it.trackId) }
                        showDownloaded = false
                    }) {
                        Text("Удалить все", color = Color(0xFFE53935))
                    }
                }
                TextButton(onClick = { showDownloaded = false }) {
                    Text("Закрыть", color = MaterialTheme.colorScheme.primary)
                }
            },
        )
    }

    var showRecentDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showSubscriptionDialog by remember { mutableStateOf(false) }

    if (showRecentDialog) {
        AlertDialog(
            onDismissRequest = { showRecentDialog = false },
            containerColor = Color(0xFF282329),
            shape = MaterialTheme.shapes.large,
            title = {
                Text(
                    text = "Недавнее",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "История недавно воспроизведённых треков сохраняется автоматически во время прослушивания.",
                    color = Color(0xFFDDD5DC),
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { showRecentDialog = false }) {
                    Text("Понятно", color = Color(0xFFF2DAEE))
                }
            },
        )
    }

    if (showTransferDialog) {
        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            containerColor = Color(0xFF282329),
            shape = MaterialTheme.shapes.large,
            title = {
                Text(
                    text = "Перенос из других сервисов",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "Перенос вашей медиатеки из сторонних сервисов (Яндекс Музыка, Spotify, Apple Music) доступен через официальный инструмент в веб-версии VK Музыки.",
                    color = Color(0xFFDDD5DC),
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { showTransferDialog = false }) {
                    Text("Понятно", color = Color(0xFFF2DAEE))
                }
            },
        )
    }

    if (showSubscriptionDialog) {
        AlertDialog(
            onDismissRequest = { showSubscriptionDialog = false },
            containerColor = Color(0xFF282329),
            shape = MaterialTheme.shapes.large,
            title = {
                Text(
                    text = "Подписка на VK Музыку",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "В приложении VK Mint вся музыка доступна без аудиорекламы и без ограничений на фоновое воспроизведение.",
                    color = Color(0xFFDDD5DC),
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { showSubscriptionDialog = false }) {
                    Text("Отлично", color = Color(0xFFF2DAEE))
                }
            },
        )
    }

    val menuItems = listOf(
        Pair("Недавнее", Icons.Filled.History),
        Pair("Плейлисты", Icons.AutoMirrored.Filled.QueueMusic),
        Pair("Альбомы", Icons.Filled.Album),
        Pair("Артисты и кураторы", Icons.AutoMirrored.Filled.PlaylistAddCheck),
        Pair("Скачанная музыка", Icons.Filled.DownloadDone),
        Pair("Перенос из других сервисов", Icons.Filled.MoveToInbox),
        Pair("Подписка на VK Музыку", Icons.Filled.GraphicEq),
    )
    Column(modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)) {
        menuItems.forEach { (title, icon) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        when (title) {
                            "Недавнее" -> showRecentDialog = true
                            "Плейлисты" -> onOpenPlaylists()
                            "Альбомы" -> onOpenAlbums()
                            "Артисты и кураторы" -> onOpenArtists()
                            "Скачанная музыка" -> onOpenDownloaded()
                            "Перенос из других сервисов" -> showTransferDialog = true
                            "Подписка на VK Музыку" -> showSubscriptionDialog = true
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color(0xFF282329)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFFF2DAEE),
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ─── Вкладка «Главная»: каталог VK (catalog.getAudio section=general) ─

@Composable
private fun MusicHomeTab(
    tracks: List<Track>,
    loading: Boolean,
    playerState: vk.mint.data.model.PlayerState,
    downloads: Map<Long, DownloadState>,
    cardColor: Color,
    textColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    apiErrorMessage: String?,
    onPlayTrack: (Int) -> Unit,
    onToggleTrack: (Track) -> Unit,
    onDownloadToggle: (Track) -> Unit,
    onPlaylistClick: (vk.mint.data.model.CatalogPlaylist) -> Unit = {},
    onShowAll: (sectionId: String, title: String) -> Unit = { _, _ -> },
) {
    val app = VkMintApp.get()
    val scope = rememberCoroutineScope()
    var catalogBlocks by remember { mutableStateOf<List<vk.mint.data.model.CatalogBlock>>(emptyList()) }
    var catalogLoading by remember { mutableStateOf(true) }
    // Волна 40 #SECTIONS-HONEST: молчаливый провал каталога выглядел как
    // «Каталог пуст» (жалоба «одни разделы работают другие нет») — теперь
    // честная причина (офлайн/ошибка VK) + «Повторить».
    var catalogError by remember { mutableStateOf<String?>(null) }
    var catalogReloadTick by remember { mutableStateOf(0) }

    // Загрузка каталога
    LaunchedEffect(catalogReloadTick) {
        catalogLoading = true
        catalogError = null
        try {
            val blocks = app.apiClient.catalogGetAudio(section = "general", count = 10)
            catalogBlocks = blocks
            AppLog.i("MusicHomeTab", "Loaded ${blocks.size} catalog blocks")
            // #SECTIONS-HONEST: catalogGetAudio отдаёт emptyList и при офлайне,
            // и при ошибке API/парсинга — различаем причину честным текстом.
            if (blocks.isEmpty()) {
                catalogError = if (app.apiClient.isOffline()) "Нет сети — раздел доступен онлайн"
                else (app.apiClient.lastApiErrorHuman() ?: "Каталог временно недоступен — попробуйте позже")
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Fix #252: корректная отмена (пользователь ушёл со экрана)
            throw e
        } catch (e: Exception) {
            AppLog.e("MusicHomeTab", "catalog.getAudio error", e)
            // #SECTIONS-HONEST: причина видна в UI (было — только AppLog).
            catalogError = if (app.apiClient.isOffline()) "Нет сети — раздел доступен онлайн"
            else "Ошибка: ${e.message}"
        } finally {
            catalogLoading = false
        }
    }

    val homeListState = rememberLazyListState()
    LazyColumn(
        state = homeListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = if (playerState.currentTrack != null) 100.dp else 16.dp),
    ) {
        if (catalogLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = accentColor) }
            }
        } else if (catalogError != null && catalogBlocks.isEmpty()) {
            // Волна 40 #SECTIONS-HONEST: честная причина сбоя + «Повторить»
            // (раньше ветка ловила ошибку только в AppLog → «Каталог пуст»).
            item {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.MusicNote, null, tint = secondaryColor, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            catalogError ?: "",
                            color = textColor,
                            fontSize = 15.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { catalogReloadTick++ }) { Text("Повторить") }
                    }
                }
            }
        } else if (catalogBlocks.isEmpty() && tracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.MusicNote, null, tint = secondaryColor, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Каталог пуст", color = textColor, fontSize = 16.sp)
                    }
                }
            }
        } else {
            // Рендерим блоки каталога.
            // #MUSIC-CATALOG-NO-DUP-HEADERS: HEADER/HEADER_EXTENDED блоки НЕ
            // рендерим отдельно — их title уже дублируется в SectionHeader
            // слайдера (контент-блок несёт тот же title). Отдельный рендер давал
            // двойной заголовок («МОИ ТРЕКИ» ×2).
            catalogBlocks.forEach { block ->
                when (block.viewType) {
                    vk.mint.data.model.CatalogViewType.HEADER,
                    vk.mint.data.model.CatalogViewType.HEADER_EXTENDED -> Unit
                    vk.mint.data.model.CatalogViewType.SEPARATOR -> {
                        item { HorizontalDivider(color = secondaryColor.copy(alpha = 0.1f), thickness = 1.dp) }
                    }
                    vk.mint.data.model.CatalogViewType.TRIPLE_STACKED_SLIDER -> {
                        // Горизонтальная карусель треков
                        if (block.tracks.isNotEmpty()) {
                            item {
                                // #ARCH-CONTAINERS 3.7-1: модели в :core:data — smart cast
                                // свойства чужого модуля невозможен; захват в локальный val.
                                val blockTitle = block.title
                                if (blockTitle != null) {
                                    SectionHeader(
                                        blockTitle.uppercase(), textColor, secondaryColor,
                                        onShowAll = block.showAllId?.let { sid ->
                                            { onShowAll(sid, blockTitle) }
                                        },
                                    )
                                }
                            }
                            item {
                                TrackSliderRow(
                                    tracks = block.tracks,
                                    playerState = playerState,
                                    downloads = downloads,
                                    cardColor = cardColor,
                                    textColor = textColor,
                                    secondaryColor = secondaryColor,
                                    accentColor = accentColor,
                                    onPlayTrack = { idx -> onPlayTrack(idx) },
                                    onToggleTrack = onToggleTrack,
                                    onDownloadToggle = onDownloadToggle,
                                )
                            }
                        }
                    }
                    // #MUSIC-CATALOG-RECOMMS: «Собрано алгоритмами» — это слайдер
                    // ПЛЕЙЛИСТОВ (recomms_slider), а не треков. Рендерим как плейлисты.
                    vk.mint.data.model.CatalogViewType.RECOMMS_SLIDER -> {
                        if (block.playlists.isNotEmpty()) {
                            item {
                                // #ARCH-CONTAINERS 3.7-1: модели в :core:data — smart cast
                                // свойства чужого модуля невозможен; захват в локальный val.
                                val blockTitle = block.title
                                if (blockTitle != null) {
                                    SectionHeader(
                                        blockTitle.uppercase(), textColor, secondaryColor,
                                        onShowAll = block.showAllId?.let { sid ->
                                            { onShowAll(sid, blockTitle) }
                                        },
                                    )
                                }
                            }
                            item {
                                PlaylistSliderRow(
                                    playlists = block.playlists,
                                    textColor = textColor,
                                    secondaryColor = secondaryColor,
                                    accentColor = accentColor,
                                    onPlaylistClick = onPlaylistClick,
                                )
                            }
                        }
                    }
                    vk.mint.data.model.CatalogViewType.LARGE_SLIDER -> {
                        // Горизонтальная карусель плейлистов
                        if (block.playlists.isNotEmpty()) {
                            item {
                                // #ARCH-CONTAINERS 3.7-1: модели в :core:data — smart cast
                                // свойства чужого модуля невозможен; захват в локальный val.
                                val blockTitle = block.title
                                if (blockTitle != null) {
                                    SectionHeader(
                                        blockTitle.uppercase(), textColor, secondaryColor,
                                        onShowAll = block.showAllId?.let { sid ->
                                            { onShowAll(sid, blockTitle) }
                                        },
                                    )
                                }
                            }
                            item {
                                PlaylistSliderRow(
                                    playlists = block.playlists,
                                    textColor = textColor,
                                    secondaryColor = secondaryColor,
                                    accentColor = accentColor,
                                    onPlaylistClick = onPlaylistClick,
                                )
                            }
                        }
                    }
                    else -> { /* UNKNOWN, LIST — skip for now on Home tab */ }
                }
            }

            // Фоллбэк: если каталог пуст — показываем «Мои треки» как раньше
            if (catalogBlocks.isEmpty() && tracks.isNotEmpty()) {
                item { SectionHeader("МОИ ТРЕКИ", textColor, secondaryColor) }
                items(tracks.take(6), key = { it.id }) { track ->
                    val isCurrent = playerState.currentTrack?.id == track.id
                    val isPlaying = isCurrent && playerState.isPlaying
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayTrack(tracks.indexOf(track)) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            null,
                            tint = if (isPlaying) accentColor else textColor,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                track.title.orEmpty(),
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                track.artist.orEmpty(),
                                color = secondaryColor,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

// #MUSIC-RADIO: заглушка вкладки «Радио».
// Section-id радиовкладки пока не найден в снапшотах. Инфраструктура готова:
// AudioRadioStation (Models.kt:1642), audio.radioGetById/followRadioStation.
// Нужен живой section-id из m.vk.ru/audios<id>?section=radiostations после JS.
@Composable
private fun RadioStubTab(
    textColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    cardColor: Color,
) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                tint = secondaryColor,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text("Радио", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Раздел в разработке",
                color = secondaryColor,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Откройте m.vk.ru/feed в браузере чтобы получить section-id",
                color = secondaryColor.copy(alpha = 0.6f),
                fontSize = 12.sp,
            )
        }
    }
}

// ─── Вкладка «Обзор» (P0): каталог explore (catalog.getAudio section=explore) ─

@Composable
private fun DiscoverTab(
    textColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    cardColor: Color,
    playerState: vk.mint.data.model.PlayerState,
    downloads: Map<Long, DownloadState>,
    onPlaylistClick: (vk.mint.data.model.CatalogPlaylist) -> Unit = {},
    onShowAll: (sectionId: String, title: String) -> Unit = { _, _ -> },
    section: String = "explore",
) {
    val app = VkMintApp.get()
    var catalogBlocks by remember { mutableStateOf<List<vk.mint.data.model.CatalogBlock>>(emptyList()) }
    var catalogLoading by remember { mutableStateOf(true) }
    // Волна 40 #SECTIONS-HONEST: провал «Обзора»/«Обновлений» выглядел как
    // «Нет рекомендаций» без причины — теперь честный текст + «Повторить».
    var catalogError by remember { mutableStateOf<String?>(null) }
    var discoverReloadTick by remember { mutableStateOf(0) }

    LaunchedEffect(section, discoverReloadTick) {
        catalogLoading = true
        catalogError = null
        try {
            val blocks = app.apiClient.catalogGetAudio(section = section, count = 10)
            catalogBlocks = blocks
            AppLog.i("DiscoverTab", "Loaded ${blocks.size} blocks (section=$section)")
            // #SECTIONS-HONEST: различаем офлайн и ошибку API/парсинга.
            if (blocks.isEmpty()) {
                catalogError = if (app.apiClient.isOffline()) "Нет сети — раздел доступен онлайн"
                else (app.apiClient.lastApiErrorHuman() ?: "Раздел временно недоступен — попробуйте позже")
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Fix #252: корректная отмена (пользователь ушёл со экрана)
            throw e
        } catch (e: Exception) {
            AppLog.e("DiscoverTab", "catalog.getAudio($section) error", e)
            // #SECTIONS-HONEST: причина видна в UI (было — только AppLog).
            catalogError = if (app.apiClient.isOffline()) "Нет сети — раздел доступен онлайн"
            else "Ошибка: ${e.message}"
        } finally {
            catalogLoading = false
        }
    }

    val discoverListState = rememberLazyListState()
    LazyColumn(
        state = discoverListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = if (playerState.currentTrack != null) 100.dp else 16.dp),
    ) {
        if (catalogLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = accentColor) }
            }
        } else if (catalogError != null && catalogBlocks.isEmpty()) {
            // Волна 40 #SECTIONS-HONEST: честная причина сбоя + «Повторить».
            item {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.MusicNote, null, tint = secondaryColor, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            catalogError ?: "",
                            color = textColor,
                            fontSize = 15.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { discoverReloadTick++ }) { Text("Повторить") }
                    }
                }
            }
        } else if (catalogBlocks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.MusicNote, null, tint = secondaryColor, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Нет рекомендаций", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Text("Попробуйте обновить позже", color = secondaryColor, fontSize = 13.sp)
                    }
                }
            }
        } else {
            // #MUSIC-CATALOG-NO-DUP-HEADERS: HEADER/HEADER_EXTENDED не рендерим
            // отдельно — title дублируется в SectionHeader слайдера.
            catalogBlocks.forEach { block ->
                when (block.viewType) {
                    vk.mint.data.model.CatalogViewType.HEADER,
                    vk.mint.data.model.CatalogViewType.HEADER_EXTENDED -> Unit
                    vk.mint.data.model.CatalogViewType.SEPARATOR -> {
                        item { HorizontalDivider(color = secondaryColor.copy(alpha = 0.1f), thickness = 1.dp) }
                    }
                    vk.mint.data.model.CatalogViewType.TRIPLE_STACKED_SLIDER -> {
                        if (block.tracks.isNotEmpty()) {
                            item {
                                // #ARCH-CONTAINERS 3.7-1: модели в :core:data — smart cast
                                // свойства чужого модуля невозможен; захват в локальный val.
                                val blockTitle = block.title
                                if (blockTitle != null) {
                                    SectionHeader(
                                        blockTitle.uppercase(), textColor, secondaryColor,
                                        onShowAll = block.showAllId?.let { sid ->
                                            { onShowAll(sid, blockTitle) }
                                        },
                                    )
                                }
                            }
                            item {
                                TrackSliderRow(
                                    tracks = block.tracks,
                                    playerState = playerState,
                                    downloads = downloads,
                                    cardColor = cardColor,
                                    textColor = textColor,
                                    secondaryColor = secondaryColor,
                                    accentColor = accentColor,
                                    onPlayTrack = { idx ->
                                        PlayerConnection.playTrackList(block.tracks, idx)
                                    },
                                    onToggleTrack = {},
                                    onDownloadToggle = {},
                                )
                            }
                        }
                    }
                    vk.mint.data.model.CatalogViewType.RECOMMS_SLIDER -> {
                        if (block.playlists.isNotEmpty()) {
                            item {
                                // #ARCH-CONTAINERS 3.7-1: модели в :core:data — smart cast
                                // свойства чужого модуля невозможен; захват в локальный val.
                                val blockTitle = block.title
                                if (blockTitle != null) {
                                    SectionHeader(
                                        blockTitle.uppercase(), textColor, secondaryColor,
                                        onShowAll = block.showAllId?.let { sid ->
                                            { onShowAll(sid, blockTitle) }
                                        },
                                    )
                                }
                            }
                            item {
                                PlaylistSliderRow(
                                    playlists = block.playlists,
                                    textColor = textColor,
                                    secondaryColor = secondaryColor,
                                    accentColor = accentColor,
                                    onPlaylistClick = onPlaylistClick,
                                )
                            }
                        }
                    }
                    vk.mint.data.model.CatalogViewType.LARGE_SLIDER -> {
                        if (block.playlists.isNotEmpty()) {
                            item {
                                // #ARCH-CONTAINERS 3.7-1: модели в :core:data — smart cast
                                // свойства чужого модуля невозможен; захват в локальный val.
                                val blockTitle = block.title
                                if (blockTitle != null) {
                                    SectionHeader(
                                        blockTitle.uppercase(), textColor, secondaryColor,
                                        onShowAll = block.showAllId?.let { sid ->
                                            { onShowAll(sid, blockTitle) }
                                        },
                                    )
                                }
                            }
                            item {
                                PlaylistSliderRow(
                                    playlists = block.playlists,
                                    textColor = textColor,
                                    secondaryColor = secondaryColor,
                                    accentColor = accentColor,
                                    onPlaylistClick = onPlaylistClick,
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

// ─── Section Header ────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    textColor: Color,
    secondaryColor: Color,
    onShowAll: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (onShowAll != null) {
            Text(
                text = "Показать все",
                color = secondaryColor,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .clickable(onClick = onShowAll)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

// ─── VK-style Track Row ────────────────────────────────────────────────────

@Composable
private fun VKTrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    downloadState: DownloadState?,
    cardColor: Color,
    accentColor: Color,
    textColor: Color,
    secondaryColor: Color,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onMoreClick: () -> Unit = {},
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onPlayClick)
                .background(if (isCurrent) Color(0xFF382F37).copy(alpha = 0.5f) else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Обложка трека: сквиркл 44x44dp, 8dp (small) скругление
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
                } else if (isPlaying) {
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

            Spacer(modifier = Modifier.width(14.dp))

            // Название + артист
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = if (isCurrent) Color(0xFFF2DAEE) else Color.White,
                    fontSize = 15.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artist,
                    color = Color(0xFF9E96A0),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Длительность + кнопка скачивания (если активна) + меню ⋮
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (downloadState != null) {
                    VKDownloadButton(
                        state = downloadState,
                        onClick = onDownloadClick,
                        accentColor = accentColor,
                        secondaryColor = secondaryColor,
                    )
                }

                Text(
                    text = track.duration.toDurationString(),
                    color = Color(0xFF9E96A0),
                    fontSize = 13.sp,
                )

                IconButton(
                    onClick = onMoreClick,
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Ещё",
                        tint = Color(0xFF9E96A0),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // Прогресс-бар скачивания под строкой
        if (downloadState != null && downloadState.isInProgress) {
            LinearProgressIndicator(
                progress = {
                    if (downloadState.progress >= 0) downloadState.progress / 100f else 0f
                },
                modifier = Modifier.fillMaxWidth().height(2.dp).padding(horizontal = 16.dp),
                color = accentColor,
                trackColor = Color.Transparent,
            )
        }
    }
}

// ─── Вкладка «Загружено» (офлайн музыка) ──────────────────────────────────────

@Composable
private fun MusicDownloadedTab(
    downloads: Map<Long, DownloadState>,
    libraryTracks: List<Track>,
    playerState: vk.mint.data.model.PlayerState,
    cardColor: Color,
    textColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    onPlayTrack: (Track) -> Unit,
    onShuffleAll: () -> Unit,
    onMoreClick: (Track) -> Unit,
) {
    val downloadedList = remember(downloads, libraryTracks) {
        downloads.values
            .filter { it.isCompleted }
            .map { ds ->
                val lib = libraryTracks.find { it.id == ds.trackId }
                Track(
                    id = ds.trackId,
                    ownerId = ds.ownerId,
                    artist = ds.artist,
                    title = ds.title,
                    duration = lib?.duration ?: 0,
                    albumThumb = lib?.albumThumb,
                )
            }
    }

    var sortOrder by remember { mutableStateOf(TrackSortOrder.DEFAULT) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val displayedTracks = remember(downloadedList, sortOrder) {
        when (sortOrder) {
            TrackSortOrder.DEFAULT -> downloadedList
            TrackSortOrder.NEWEST_FIRST -> downloadedList.reversed()
            TrackSortOrder.OLDEST_FIRST -> downloadedList
            TrackSortOrder.BY_TITLE -> downloadedList.sortedBy { it.title.lowercase() }
            TrackSortOrder.BY_ARTIST -> downloadedList.sortedBy { it.artist.lowercase() }
        }
    }

    if (downloadedList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(Color(0xFF282329)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.DownloadDone,
                        contentDescription = null,
                        tint = Color(0xFFF2DAEE),
                        modifier = Modifier.size(32.dp),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Нет скачанной музыки",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Треки, сохранённые для офлайн-прослушивания, появятся здесь",
                    color = Color(0xFF9E96A0),
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (playerState.currentTrack != null) 100.dp else 16.dp),
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Скачанная музыка",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${downloadedList.size}",
                        color = Color(0xFF9E96A0),
                        fontSize = 14.sp,
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    Box {
                        Row(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable { sortMenuExpanded = true }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = sortOrder.title,
                                color = Color(0xFF9E96A0),
                                fontSize = 13.sp,
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Сортировка",
                                tint = Color(0xFF9E96A0),
                                modifier = Modifier.size(16.dp),
                            )
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            modifier = Modifier.background(Color(0xFF282329)),
                        ) {
                            TrackSortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = order.title,
                                            color = if (sortOrder == order) Color(0xFFF2DAEE) else Color.White,
                                            fontWeight = if (sortOrder == order) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    },
                                    onClick = {
                                        sortOrder = order
                                        sortMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Shuffle row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onShuffleAll)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(Color(0xFF282329)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = "Перемешать",
                            tint = Color(0xFFF2DAEE),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Перемешать",
                        color = Color.White,
                        fontSize = 15.sp,
                    )
                }
            }

            items(displayedTracks, key = { "dl_${it.ownerId}_${it.id}" }) { track ->
                val current = playerState.currentTrack
                val isCurrent = current != null &&
                    track.id == current.id &&
                    track.ownerId == current.ownerId
                val isPlaying = isCurrent && playerState.isPlaying
                val dl = downloads[track.id]
                VKTrackRow(
                    track = track,
                    isCurrent = isCurrent,
                    isPlaying = isPlaying,
                    downloadState = dl,
                    cardColor = cardColor,
                    accentColor = accentColor,
                    textColor = textColor,
                    secondaryColor = secondaryColor,
                    onPlayClick = { onPlayTrack(track) },
                    onDownloadClick = {
                        TrackDownloadManager.removeDownload(track.id)
                    },
                    onMoreClick = { onMoreClick(track) },
                )
            }
        }
    }
}

@Composable
private fun VKDownloadButton(
    state: DownloadState?,
    onClick: () -> Unit,
    accentColor: Color,
    secondaryColor: Color,
) {
    when {
        state == null -> {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Скачать",
                    tint = secondaryColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        // #OFFLINE-STATUS-1: дохлый трек (DEAD_URL — URL протух/удалён).
        // Отдельная иконка MusicOff, чтобы пользователь ВИДЕЛ что трек «скончался»,
        // а не просто «ошибка загрузки». Тап — повтор (вдруг VK вернул URL).
        state.isDead -> {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Filled.MusicOff,
                    contentDescription = "Недоступен (URL истёк) — тап для повтора",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        state.status == DownloadStatus.FAILED -> {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Повторить",
                    tint = secondaryColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        // Fix #266: QUEUED (в очереди) — показываем часики + badge с позицией.
        // Отличается от DOWNLOADING (active) — там крутится progress-circle.
        state.status == DownloadStatus.QUEUED -> {
            // Позиция в очереди (1-based). 0 = уже активен (но статус QUEUED —
            // маловероятно, но защитно).
            val queuePos = vk.mint.media.TrackDownloadManager.getQueuePosition(state.trackId)
            IconButton(onClick = onClick) {
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,  // часики — "ожидание"
                        contentDescription = if (queuePos > 0) "В очереди: $queuePos" else "В очереди",
                        tint = secondaryColor,
                        modifier = Modifier.size(20.dp),
                    )
                    if (queuePos > 1) {
                        // Badge с номером позиции в очереди
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-6).dp)
                                .background(accentColor, shape = MaterialTheme.shapes.small)
                                .padding(horizontal = 3.dp, vertical = 0.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = queuePos.toString(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
        state.status == DownloadStatus.DOWNLOADING -> {
            IconButton(onClick = onClick) {
                CircularProgressIndicator(
                    progress = { if (state.progress >= 0) state.progress / 100f else 0f },
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = accentColor,
                    trackColor = Color.Transparent,
                )
            }
        }
        // #OFFLINE-STATUS-1: siren-кэш (COMPLETED, codec=siren) — файл есть,
        // но офлайн не играется (стримится онлайн через HLS). DownloadDone +
        // маленький wifi-бейдж, чтобы пользователь видел: кеш есть, онлайн-only.
        state.isSirenCache -> {
            IconButton(onClick = onClick) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.DownloadDone,
                        contentDescription = "Скачано (онлайн-кеш, siren) — тап чтобы удалить",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Icon(
                        imageVector = Icons.Filled.Wifi,
                        contentDescription = null,
                        tint = Color(0xFF22C55E),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .size(10.dp),
                    )
                }
            }
        }
        state.isCompleted -> {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Filled.DownloadDone,
                    contentDescription = "Скачано (тап чтобы удалить)",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        else -> {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Скачивание",
                    tint = secondaryColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// Fix #71: VKMiniPlayerBar удалён — теперь используется GlobalMiniPlayer

// ─── Горизонтальная карусель треков (TrackSlider) ──────────────────

@Composable
private fun TrackSliderRow(
    tracks: List<Track>,
    playerState: vk.mint.data.model.PlayerState,
    downloads: Map<Long, DownloadState>,
    cardColor: Color,
    textColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    onPlayTrack: (Int) -> Unit,
    onToggleTrack: (Track) -> Unit,
    onDownloadToggle: (Track) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(tracks, key = { "${it.ownerId}_${it.id}" }) { track ->
            val isCurrent = playerState.currentTrack?.let {
                it.id == track.id && it.ownerId == track.ownerId
            } ?: false

            Column(
                modifier = Modifier
                    .width(140.dp)
                    .clickable { onToggleTrack(track) },
            ) {
                // Обложка с оверлеем play
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(cardColor),
                    contentAlignment = Alignment.Center,
                ) {
                    if (track.albumThumb != null) {
                        AsyncImage(
                            model = track.albumThumb,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            Icons.Filled.MusicNote, null,
                            tint = secondaryColor,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    // Оверлей play
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isCurrent && playerState.isPlaying) Color.Black.copy(alpha = 0.6f) else accentColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (isCurrent && playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    // Explicit badge
                    if (track.isExplicit) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .background(Color.Black.copy(alpha = 0.7f), MaterialTheme.shapes.extraSmall)
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        ) {
                            Text("E", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                // Название
                Text(
                    text = track.title,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                // Артист
                Text(
                    text = track.artist,
                    color = secondaryColor,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ─── Горизонтальная карусель плейлистов (PlaylistSlider) ─────────────

@Composable
private fun PlaylistSliderRow(
    playlists: List<vk.mint.data.model.CatalogPlaylist>,
    textColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    onPlaylistClick: (vk.mint.data.model.CatalogPlaylist) -> Unit = {},
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(playlists, key = { "${it.ownerId}_${it.id}" }) { pl ->
            Column(
                modifier = Modifier
                    .width(150.dp)
                    // #PLAYLIST-OPEN: открыть плейлист (audio.getPlaylistById → play).
                    .clickable { onPlaylistClick(pl) },
            ) {
                // Обложка
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (pl.coverUrl != null) {
                        AsyncImage(
                            model = pl.coverUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            Icons.Filled.MusicNote, null,
                            tint = accentColor,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                // Название
                Text(
                    text = pl.title,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                // Совпадение вкусов или кол-во треков
                val subtitle = when {
                    pl.matchPercent != null -> "${pl.matchPercent}% совпадение"
                    pl.count > 0 -> "${pl.count} треков"
                    else -> null
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = secondaryColor,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Fix #266: Компоненты для секций поиска (Артисты / Плейлисты / Треки)
// ════════════════════════════════════════════════════════════════════

/**
 * Заголовок секции в результатах поиска («Артисты», «Плейлисты», «Треки»).
 * Слева — название, справа — счётчик.
 */
@Composable
private fun SearchSectionHeader(
    title: String,
    count: Int,
    textColor: Color,
    secondaryColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = count.toString(),
            color = secondaryColor,
            fontSize = 13.sp,
        )
    }
}

/**
 * Карточка артиста для горизонтального слайдера в результатах поиска.
 * Круглая аватарка + имя + жанр (если есть).
 */
@Composable
private fun SearchArtistCard(
    artist: AudioArtist,
    cardColor: Color,
    textColor: Color,
    secondaryColor: Color,
) {
    Column(
        modifier = Modifier
            .width(88.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(cardColor)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(cardColor.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            val photo = artist.coverUrl
            if (!photo.isNullOrBlank()) {
                AsyncImage(
                    model = photo,
                    contentDescription = artist.name,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = artist.name,
                    tint = secondaryColor,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = artist.name,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        // #ARCH-CONTAINERS 3.7-1: genres в :core:data — захват ДО проверки.
        val artistGenres = artist.genres
        if (!artistGenres.isNullOrEmpty()) {
            Text(
                text = artistGenres.first(),
                color = secondaryColor,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Карточка плейлиста для горизонтального слайдера в результатах поиска.
 * Квадратная обложка + название + кол-во треков.
 */
@Composable
private fun SearchPlaylistCard(
    playlist: AudioPlaylist,
    cardColor: Color,
    textColor: Color,
    secondaryColor: Color,
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(cardColor)
            .padding(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(MaterialTheme.shapes.small)
                .background(cardColor.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            val cover = playlist.photo300 ?: playlist.photo200 ?: playlist.photo600 ?: playlist.photo
            if (!cover.isNullOrBlank()) {
                AsyncImage(
                    model = cover,
                    contentDescription = playlist.title,
                    modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    Icons.Outlined.Album,
                    contentDescription = playlist.title,
                    tint = secondaryColor,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = playlist.title,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // #ARCH-CONTAINERS 3.7-1: модели в :core:data — smart cast чужого модуля
        // невозможен; без захвата subtitle выводился бы String? и isNotBlank() падал.
        val playlistDescription = playlist.description
        val subtitle = when {
            !playlistDescription.isNullOrBlank() -> playlistDescription
            playlist.count > 0 -> "${playlist.count} треков"
            else -> ""
        }
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                color = secondaryColor,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
