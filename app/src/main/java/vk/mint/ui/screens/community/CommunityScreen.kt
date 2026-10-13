package vk.mint.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import vk.mint.ui.theme.avatarShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vk.mint.ui.navigation.PostHolder
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import vk.mint.VkMintApp
import vk.mint.api.VKApiClient
import vk.mint.data.model.Post
import vk.mint.data.model.Video
import vk.mint.ui.components.CreatePostDialog
import vk.mint.ui.components.PhotoViewer
// #POST-CAROUSEL-EVERYWHERE (22-B): общий компонент карусели фото поста.
import vk.mint.ui.components.PostPhotoGrid
// Fix #366: общий компонент карусели видео поста.
import vk.mint.ui.components.PostVideoCarousel
import vk.mint.ui.components.ScrollToTopFab
import vk.mint.ui.components.ShareSheet
import vk.mint.ui.navigation.CommunityRestoreHolder
import vk.mint.util.AppLog
import vk.mint.util.toAbsoluteTime
import vk.mint.util.toCountString
import vk.mint.util.toMemberCountString
import java.util.Locale
import android.widget.Toast
import vk.mint.ui.components.VkGroup
import vk.mint.ui.components.VkGroupMode
import vk.mint.ui.components.VkPanelHeader
import vk.mint.ui.components.VkReactionPill
import vk.mint.ui.components.PostCard
import vk.mint.ui.components.PostMenuItem
import vk.mint.ui.components.VideoThumbnail
import vk.mint.ui.components.ClipThumbnail
import vk.mint.ui.icons.VkIcons
import vk.mint.ui.theme.VkTypography

/**
 * Fix #67: Экран сообщества — header (ава + имя + описание) + стена (wall.get).
 *
 * Открывается тапом по header'у поста в ленте (FeedScreen).
 * Использует `groups.getById` для метаданных и `wall.get` с `owner_id = -groupId`
 * для постов. Парсинг постов — через общий `parsePostMini`, который после Fix #70
 * корректно парсит attachments и copy_history.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    groupId: Long,
    onBack: () -> Unit,
    // P0.18 (Task 30): начальная вкладка при открытии CommunityScreen из stories
    // overlay (Фото автора → tab=1, Видео автора → tab=2). Default 0 (Записи).
    initialTab: Int = 0,
    onVideoClick: (Video) -> Unit = {},
    onPostClick: (Post) -> Unit = {},
    onUserClick: (Long) -> Unit = {},
    // Шаг 4 (#32d): тап по теме обсуждения → BoardTopicScreen.
    onTopicClick: (groupId: Long, topicId: Long, title: String) -> Unit = { _, _, _ -> },
    // #OPVK-EXTRACT (Task 3-c): тап по счётчику подписчиков в шапке →
    // GroupMembersScreen (список участников, groups.getMembers).
    onMembersClick: (groupId: Long) -> Unit = {},
    // W35-b (волна 35): блок «Управление» — маршрутизация в админ-экраны
    // (CommunityAdminBlock рендерится при admin_level >= 1 — сверка §5.1).
    onAdminSettingsClick: (groupId: Long) -> Unit = {},
    onAdminStatsClick: (groupId: Long) -> Unit = {},
    // W38 (C2/C5/C4): приглашения / отложенные+предложения / адреса.
    // W39 (C9): журнал действий сообщества (web-only al-эндпоинт, HAR §12.2).
    // W41 (C3): кнопка действия сообщества.
    // W41 (C6): чаты сообщества + раздел «Сообщения».
    onMessagesClick: (groupId: Long) -> Unit = {},
    // C7 (#ADMIN-SECTIONS): разделы сообщества.
    // C8 (#ADMIN-COMMENTS): комментарии сообщества (фильтры + лента).
    onAdminCommentsClick: (groupId: Long) -> Unit = {},
    // ADMIN-EVENTS: «События» сообщества (notifications.getRedesign).
    onAdminEventsClick: (groupId: Long) -> Unit = {},
    // ADMIN-MENU-STRIKES: «Меню» (owners.*) и «Страйки» (strikeSystem.*).
    onAdminStrikesClick: (groupId: Long) -> Unit = {},
    // #ADMIN-WIRE-FIX: недостающие 10 колбэков из VkMintNavHost → CommunityAdminBlock.
    onAdminPeopleClick: (groupId: Long, tab: String) -> Unit = { _, _ -> },
    onAdminLinksClick: (groupId: Long) -> Unit = {},
    onAdminInvitesClick: (groupId: Long) -> Unit = {},
    onAdminQueueClick: (groupId: Long) -> Unit = {},
    onAdminAddressesClick: (groupId: Long) -> Unit = {},
    onAdminEventLogClick: (groupId: Long) -> Unit = {},
    onCtaClick: (groupId: Long) -> Unit = {},
    onChatsClick: (groupId: Long) -> Unit = {},
    onSectionsClick: (groupId: Long) -> Unit = {},
    onAdminMenuClick: (groupId: Long) -> Unit = {},
) {
    val app = VkMintApp.get()
    val scope = rememberCoroutineScope()
    // #POST-CAROUSEL-EVERYWHERE (22-B): флаг карусели фото — один общий
    // VkMintPrefs.feedCarouselEnabled (управляет лентой/сообществами/профилями/
    // пост-детейлом; настройка «Настройки → Лента → „Карусель фото в постах"»).
    // СКОУП-РЕШЕНИЕ: читается ОДИН раз на уровне экрана (здесь, где уже есть
    // app) и пробрасывается параметром в CommunityPostCard (два места рендера
    // фото — пост и репост-карточка — внутри неё), а не читается на каждый кадр.
    val prefsSnap by app.prefs.data.collectAsState(initial = null)
    // NULL-ЯВНО: Snapshot-initial до первого эмита; дефолт true = VkMintPrefs default.
    val carouselEnabled: Boolean = prefsSnap?.feedCarouselEnabled ?: true
    var groupInfo by remember { mutableStateOf<VKApiClient.GroupInfo?>(null) }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    // #POST-SIGNER (волна 39): имена профилей из wall.get extended=1 —
    // подпись автора под постом сообщества (запрос пользователя: имя автора
    // под постом не писалось в приложении, а на веб-странице ВК писалось).
    // Копится между страницами пагинации (wallGetExtended.profileNames).
    var signerNames by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    // Fix #85: pull-to-refresh + infinite scroll стены группы.
    var isRefreshing by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var endReached by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    // Sprint 2, P1-1 (#88): полноэкранный просмотр фото.
    val photoViewerState = remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    // Sprint 2, P1-3 (#90): диалог репоста.
    val repostPost = remember { mutableStateOf<Post?>(null) }
    // #ADMIN-POST-ACTIONS: пост, для которого открыто меню действий админа.
    var menuPost by remember { mutableStateOf<Post?>(null) }
    // #ADMIN-POST-ACTIONS-EDIT: пост в режиме редактирования.
    var editingPost by remember { mutableStateOf<Post?>(null) }
    // #ADMIN-SOFT-DELETE: отложенное удаление поста (стрип + таймер 10с).
    var softDeletedPost by remember { mutableStateOf<Post?>(null) }
    var softDeleteSeconds by remember { mutableStateOf(0) }
    // W36 #COMMUNITY-COMPOSER (C0 волны 35): композер постинга на стену
    // сообщества (референс web: group_publish_block, сверка §4.1).
    var showComposer by remember { mutableStateOf(false) }
    // S6-4: подписка на сообщество.
    var isMember by remember { mutableStateOf(false) }
    // Fix #350: блокируем кнопку подписки на время API-запроса.
    var subscribePending by remember { mutableStateOf(false) }
    var descExpanded by remember { mutableStateOf(false) }
    // S6-4: оптимистичное состояние лайков.
    val likesState = remember { mutableStateMapOf<String, Pair<Boolean, Int>>() }
    // Оптимистичное состояние закладок.
    val bookmarkStates = remember { mutableStateMapOf<String, Boolean>() }
    // #30j (community tabs): активная вкладка контента сообщества.
    var selectedTab by remember { mutableStateOf(initialTab) }
    // P0.8 (Task 16): динамические вкладки на основе GroupSections.
    // GroupSections НЕ приходит в GroupInfo (поле отсутствует — VK API отдаёт
    // его только в groups.getSettings, который требует админ-прав). Грузим
    // отдельно; для не-админов → null → fallback на полный набор + «Файлы».
    // Логический индекс (0..6) ПОСТОЯНЕН — когда админ выключил секцию
    // (например photos=0), соответствующая пара исключается из списка, но
    // остальные индексы сохраняются → `when (selectedTab) 0/1/2/…` не нужно
    // перенумеровывать, существующие LaunchedEffect'ы по selectedTab==N не
    // ломаются (если вкладка не показана, по ней никогда не кликают).
    var groupSections by remember { mutableStateOf<VKApiClient.GroupSections?>(null) }
    var sectionsLoaded by remember { mutableStateOf(false) }

    // Шаг 1 (#32a): state для вкладки «Фото».
    // Ленивая загрузка при первом открытии вкладки; без пагинации (photosGet возвращает
    // до 50 за раз — достаточно для первого приближения, пагинацию добавим отдельным шагом).
    var photos by remember { mutableStateOf<List<vk.mint.data.model.PhotoItem>>(emptyList()) }
    var photosLoading by remember { mutableStateOf(false) }
    var photosError by remember { mutableStateOf<String?>(null) }
    var photosLoaded by remember { mutableStateOf(false) }

    // Шаг 2 (#32b): state для вкладки «Видео».
    // С пагинацией (видео «тяжелее», 30 за раз мало для активного сообщества).
    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var videosLoading by remember { mutableStateOf(false) }
    var videosLoadingMore by remember { mutableStateOf(false) }
    var videosError by remember { mutableStateOf<String?>(null) }
    var videosLoaded by remember { mutableStateOf(false) }
    var videosEndReached by remember { mutableStateOf(false) }

    // Шаг 3 (#32c): state для вкладки «Музыка».
    // #AUDIO-PAGING-ALL (волна 38): была капнута на 50 (одна страница audioGet) —
    // до конца списка у сообществ с музыкой >50 треков было не доскроллить.
    // Теперь — серверная пагинация по offset (audioGetWithCount), автодогрузка
    // по скроллу до конца, стоп по неполной странице (VK total НЕ доверяем —
    // занижает, прецедент AudioLibraryPager).
    var tracks by remember { mutableStateOf<List<vk.mint.data.model.Track>>(emptyList()) }
    var tracksLoading by remember { mutableStateOf(false) }
    var tracksError by remember { mutableStateOf<String?>(null) }
    var tracksLoaded by remember { mutableStateOf(false) }
    var tracksServerOffset by remember { mutableStateOf(0) }
    var tracksHasMore by remember { mutableStateOf(false) }
    var tracksLoadingMore by remember { mutableStateOf(false) }

    // #GROUP-CLIPS: state для вкладки «Клипы» (shortVideo.getOwnerVideos).
    // Без пагинации (30 за раз достаточно для первого приближения).
    var clips by remember { mutableStateOf<List<Video>>(emptyList()) }
    var clipsLoading by remember { mutableStateOf(false) }
    var clipsError by remember { mutableStateOf<String?>(null) }
    var clipsLoaded by remember { mutableStateOf(false) }

    // Шаг 4 (#32d): state для вкладки «Обсуждения».
    // Без пагинации (30 тем за раз — обычно достаточно;TopicsTab простой список).
    var topics by remember { mutableStateOf<List<vk.mint.data.model.BoardTopic>>(emptyList()) }
    var topicsLoading by remember { mutableStateOf(false) }
    var topicsError by remember { mutableStateOf<String?>(null) }
    var topicsLoaded by remember { mutableStateOf(false) }

    // P0.8 (Task 16): state для вкладки «Файлы» (docs.get с ownerId=-groupId).
    // С пагинацией (как у видео): 30 за раз, догрузка по скроллу до конца.
    // docs.get работает для любого сообщества (в отличие от groups.getSettings,
    // который требует админ-прав) → вкладка «Файлы» показывается ВСЕГДА.
    var docs by remember { mutableStateOf<List<vk.mint.data.model.DocFile>>(emptyList()) }
    var docsLoading by remember { mutableStateOf(false) }
    var docsLoadingMore by remember { mutableStateOf(false) }
    var docsError by remember { mutableStateOf<String?>(null) }
    var docsLoaded by remember { mutableStateOf(false) }
    var docsEndReached by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) {
        loading = true
        errorText = null
        try {
            val groups = app.apiClient.groupsGetById(listOf(groupId))
            groupInfo = groups.firstOrNull()
            isMember = groupInfo?.isMember == 1
            if (groupInfo == null) {
                errorText = app.apiClient.lastApiError ?: "Сообщество не найдено (id=$groupId)"
            } else {
                // wall.get с owner_id = -groupId (VK convention: группы — отрицательные).
                // #POST-SIGNER: extended=1 — вместе с постами приходят профили
                // (имена авторов подписи).
                val wallPage = app.apiClient.wallGetExtended(ownerId = -groupId, count = 30)
                posts = wallPage.posts
                    .filter { it.id > 0 && it.ownerId != 0L }
                    .distinctBy { "${it.ownerId}_${it.id}" }
                signerNames = wallPage.profileNames
                endReached = wallPage.posts.size < 30
                AppLog.i("CommunityScreen", "Loaded group ${groupInfo?.name} + ${posts.size} posts")
            }
        } catch (e: Exception) {
            AppLog.e("CommunityScreen", "Failed to load community", e)
            errorText = "Ошибка: ${e.message}"
        } finally {
            loading = false
        }
    }

    // #NAV-GROUP-VIDEO-RESTORE (Fix #344): сообщество загружено — пишем контекст
    // восстановления (id). Стирание — в VkMintNavHost при уходе с экрана Community.
    LaunchedEffect(groupInfo) {
        if (groupInfo != null) {
            app.prefs.setLastCommunityId(groupId)
        }
    }
    // P0.8 (Task 16): подгрузка GroupSections (groups.getSettings) для
    // динамических вкладок. Метод требует админ-прав — для не-менеджеров
    // возвращает null → fallback на полный набор вкладок (+ «Файлы»).
    // Гейтинг по groupInfo.isManager — чтобы не делать обречённый запрос
    // для обычного участника. sectionsLoaded флаг защищает от повторов.
    LaunchedEffect(groupId, groupInfo) {
        if (sectionsLoaded) return@LaunchedEffect
        val g0 = groupInfo
        if (g0 == null) return@LaunchedEffect
        if (!g0.isManager) {
            // Не-админ → группы.getSettings вернёт access-denied; не делаем
            // лишний сетевой запрос. Fallback: tabs без фильтрации.
            sectionsLoaded = true
            return@LaunchedEffect
        }
        scope.launch {
            try {
                val s = app.apiClient.groupsGetSettings(groupId)
                groupSections = s
                AppLog.i("CommunityScreen", "GroupSections loaded: " +
                    "wall=${s?.wall} photos=${s?.photos} video=${s?.video} " +
                    "audio=${s?.audio} docs=${s?.docs} topics=${s?.topics}")
            } catch (e: Exception) {
                AppLog.w("CommunityScreen", "GroupSections load failed: ${e.message}")
            } finally {
                sectionsLoaded = true
            }
        }
    }
    // P0.8 (Task 16): вычисление динамического списка вкладок.
    // Pair(логическийИндекс, заголовок): индекс 0=Записи, 1=Фото, 2=Видео,
    // 3=Клипы, 4=Музыка, 5=Обсуждения, 6=Файлы. Когда GroupSections явно
    // выключает секцию (значение 0 — disabled), исключаем пару из списка;
    // остальные индексы сохраняются. «Клипы» и «Файлы» — всегда (в VK API
    // нет групповой настройки clips, а docs.get работает для любого сообщества).
    val tabs: List<Pair<Int, String>> = remember(groupSections, sectionsLoaded) {
        val s = groupSections
        val has = s != null
        buildList {
            // «Записи» (wall): 0=выкл, 1=открытая, 2=ограниченная, 3=закрытая.
            // Всегда показываем вкладку: даже выключенная стена у сообщества
            // без записей — это базовая информация (пустой state внутри).
            // Явная non-null локальная переменная (sn) — smart-cast на var из
            // remember через lambda-захват не работает, делаем явную копию.
            if (!has) {
                add(0 to "Записи")
                add(1 to "Фото")
                add(2 to "Видео")
                add(3 to "Клипы")
                add(4 to "Музыка")
                add(5 to "Обсуждения")
            } else {
                val sn = s
                if (sn.wall != 0) add(0 to "Записи")
                if (sn.photos > 0) add(1 to "Фото")
                if (sn.video > 0) add(2 to "Видео")
                add(3 to "Клипы")
                if (sn.audio > 0) add(4 to "Музыка")
                if (sn.topics > 0) add(5 to "Обсуждения")
            }
            // «Файлы» (docs) — всегда: docs.get работает для любого сообщества
            // (не требует админ-прав), даже если админ выключил секцию docs=0.
            add(6 to "Файлы")
        }
    }
    // P0.8 (Task 16): если текущий selectedTab больше не в списке вкладок
    // (админ выключил секцию в другой сессии) — переключаемся на первую.
    LaunchedEffect(tabs) {
        if (tabs.isNotEmpty() && tabs.none { it.first == selectedTab }) {
            selectedTab = tabs.first().first
        }
    }
    // #NAV-GROUP-VIDEO-RESTORE (Fix #344): однократная прокрутка стены к позиции
    // до смерти процесса. Индекс сохраняется/восстанавливается в одних координатах
    // LazyColumn (хедер + табы + посты) — пространство индексов идентично.
    LaunchedEffect(posts) {
        val target = CommunityRestoreHolder.pendingScroll
        if (posts.isNotEmpty() && target >= 0 && selectedTab == 0) {
            CommunityRestoreHolder.pendingScroll = -1
            listState.scrollToItem(target)
        }
    }
    // #NAV-GROUP-VIDEO-RESTORE (Fix #344): позиция стены пишется на каждой
    // остановке скролла и после каждой загрузки постов (posts в ключах —
    // иначе после загрузки без скролла в prefs остался бы протухший индекс).
    // Process death dispose не вызывает, поэтому пишем постоянно, а не в onDispose.
    LaunchedEffect(listState.isScrollInProgress, selectedTab, posts) {
        if (selectedTab == 0 && !listState.isScrollInProgress && posts.isNotEmpty()) {
            app.prefs.setLastCommunityScroll(listState.firstVisibleItemIndex)
        }
    }

    // Fix #85: pull-to-refresh стены группы.
    fun refreshWall() {
        if (isRefreshing) return
        scope.launch {
            isRefreshing = true
            try {
                // #POST-SIGNER: extended=1 (имена авторов подписи).
                val wallPage = app.apiClient.wallGetExtended(ownerId = -groupId, count = 30)
                posts = wallPage.posts
                    .filter { it.id > 0 && it.ownerId != 0L }
                    .distinctBy { "${it.ownerId}_${it.id}" }
                signerNames = wallPage.profileNames
                endReached = wallPage.posts.size < 30
                AppLog.i("CommunityScreen", "refreshed wall: ${wallPage.posts.size} posts")
            } catch (e: Exception) {
                AppLog.e("CommunityScreen", "refreshWall failed", e)
            } finally {
                isRefreshing = false
            }
        }
    }

    // Fix #85: infinite scroll стены группы.
    fun loadMoreWall() {
        if (loadingMore || endReached || posts.isEmpty()) return
        scope.launch {
            loadingMore = true
            try {
                // #POST-SIGNER: extended=1; имена профилей копим между страницами.
                val wallPage = app.apiClient.wallGetExtended(
                    ownerId = -groupId, count = 30, offset = posts.size)
                val wall = wallPage.posts
                signerNames = signerNames + wallPage.profileNames
                val newPosts = wall
                    .filter { it.id > 0 && it.ownerId != 0L }
                    .filter { np -> posts.none { it.ownerId == np.ownerId && it.id == np.id } }
                    .distinctBy { "${it.ownerId}_${it.id}" }
                if (newPosts.isEmpty()) {
                    endReached = true
                } else {
                    posts = (posts + newPosts).distinctBy { "${it.ownerId}_${it.id}" }
                    if (wall.size < 30) endReached = true
                }
            } catch (e: Exception) {
                AppLog.e("CommunityScreen", "loadMoreWall failed", e)
            } finally {
                loadingMore = false
            }
        }
    }

    // Шаг 1 (#32a): загрузка фото сообщества при первом переходе на вкладку «Фото».
    // photosLoaded предотвращает повторные запросы при переключении туда-обратно.
    LaunchedEffect(selectedTab, groupId) {
        if (selectedTab != 1 || photosLoaded || photosLoading) return@LaunchedEffect
        photosLoading = true
        photosError = null
        try {
            // VK convention: owner_id группы — отрицательный.
            photos = app.apiClient.photosGet(ownerId = -groupId, albumId = "wall", count = 50)
            photosLoaded = true
            AppLog.i("CommunityScreen", "Loaded ${photos.size} photos for group $groupId")
        } catch (e: Exception) {
            AppLog.e("CommunityScreen", "photosGet failed", e)
            photosError = "Ошибка: ${e.message}"
        } finally {
            photosLoading = false
        }
    }

    // Шаг 2 (#32b): первая загрузка видео сообщества при переходе на вкладку 2.
    LaunchedEffect(selectedTab, groupId) {
        if (selectedTab != 2 || videosLoaded || videosLoading) return@LaunchedEffect
        videosLoading = true
        videosError = null
        try {
            videos = app.apiClient.videoGet(ownerId = -groupId, count = 30, offset = 0)
            videosLoaded = true
            videosEndReached = videos.size < 30
            AppLog.i("CommunityScreen", "Loaded ${videos.size} videos for group $groupId")
        } catch (e: Exception) {
            AppLog.e("CommunityScreen", "videoGet failed", e)
            videosError = "Ошибка: ${e.message}"
        } finally {
            videosLoading = false
        }
    }

    // Шаг 2 (#32b): пагинация видео.
    fun loadMoreVideos() {
        if (videosLoadingMore || videosEndReached || videos.isEmpty()) return
        scope.launch {
            videosLoadingMore = true
            try {
                val more = app.apiClient.videoGet(ownerId = -groupId, count = 30, offset = videos.size)
                val newOnes = more.filter { nv -> videos.none { it.id == nv.id && it.ownerId == nv.ownerId } }
                if (newOnes.isEmpty()) {
                    videosEndReached = true
                } else {
                    videos = (videos + newOnes).distinctBy { "${it.ownerId}_${it.id}" }
                    if (more.size < 30) videosEndReached = true
                }
            } catch (e: Exception) {
                AppLog.e("CommunityScreen", "loadMoreVideos failed", e)
            } finally {
                videosLoadingMore = false
            }
        }
    }

    // Шаг 2 (#32b): триггер пагинации видео (только когда активна вкладка 2).
    LaunchedEffect(listState, videos.size, selectedTab) {
        if (selectedTab != 2) return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = info.totalItemsCount
            total > 0 && lastVisible >= total - 3
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { loadMoreVideos() }
    }

    // #GROUP-CLIPS: загрузка клипов сообщества при переходе на вкладку 3.
    LaunchedEffect(selectedTab, groupId) {
        if (selectedTab != 3 || clipsLoaded || clipsLoading) return@LaunchedEffect
        clipsLoading = true
        clipsError = null
        try {
            clips = app.apiClient.shortVideoGetOwnerVideos(ownerId = -groupId, count = 30)
            clipsLoaded = true
            AppLog.i("CommunityScreen", "Loaded ${clips.size} clips for group $groupId")
        } catch (e: Exception) {
            AppLog.e("CommunityScreen", "shortVideoGetOwnerVideos failed", e)
            clipsError = "Ошибка: ${e.message}"
        } finally {
            clipsLoading = false
        }
    }

    // Шаг 3 (#32c): загрузка музыки сообщества при переходе на вкладку 4.
    // #AUDIO-PAGING-ALL: первая страница (100 треков) — дальше догрузка по скроллу.
    LaunchedEffect(selectedTab, groupId) {
        if (selectedTab != 4 || tracksLoaded || tracksLoading) return@LaunchedEffect
        tracksLoading = true
        tracksError = null
        try {
            val (total, page) = app.apiClient.audioGetWithCount(
                count = 100, offset = 0, ownerId = -groupId)
            tracks = page.filter { it.id > 0L && it.ownerId != 0L }
                .distinctBy { "${it.ownerId}_${it.id}" }
            tracksServerOffset = page.size
            // #AUDIO-PAGING-HOLE (волна 41): НЕпустая страница (даже короткая)
            // ≠ конец списка — VK режет листинг на «битой» записи (прецедент
            // стопа 149/3239). Сбой (total=-1) — hasMore сохраняется для повтора.
            tracksHasMore = page.isNotEmpty() || total < 0
            tracksLoaded = true
            AppLog.i("CommunityScreen", "Loaded ${tracks.size}/${page.size} tracks for group $groupId (total=$total hasMore=$tracksHasMore)")
        } catch (e: Exception) {
            AppLog.e("CommunityScreen", "audioGet failed", e)
            tracksError = "Ошибка: ${e.message}"
        } finally {
            tracksLoading = false
        }
    }

    // #AUDIO-PAGING-ALL: догрузка следующей страницы музыки сообщества.
    fun loadMoreCommunityTracks() {
        if (tracksLoadingMore || tracksLoading || !tracksHasMore) return
        scope.launch {
            tracksLoadingMore = true
            try {
                val (total, page) = app.apiClient.audioGetWithCount(
                    count = 100, offset = tracksServerOffset, ownerId = -groupId)
                tracksServerOffset += page.size
                val fresh = page.filter { it.id > 0L && it.ownerId != 0L }
                    .filter { nv -> tracks.none { it.ownerId == nv.ownerId && it.id == nv.id } }
                tracks = (tracks + fresh).distinctBy { "${it.ownerId}_${it.id}" }
                // #AUDIO-PAGING-HOLE (волна 41): непустая страница ≠ конец;
                // сбой (total=-1) — сохраняем hasMore для повтора по скроллу.
                tracksHasMore = page.isNotEmpty() || total < 0
                AppLog.i("CommunityScreen", "Community music loadMore: +${fresh.size} (offset=$tracksServerOffset total=$total)")
            } catch (e: Exception) {
                AppLog.e("CommunityScreen", "community music loadMore failed", e)
            } finally {
                tracksLoadingMore = false
            }
        }
    }

    // #AUDIO-PAGING-ALL: триггер автодогрузки музыки (вкладка 4).
    LaunchedEffect(listState, selectedTab) {
        if (selectedTab != 4) return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = info.totalItemsCount
            if (total > 0 && lastVisible >= total - 3) total else -1
        }
            .distinctUntilChanged()
            .filter { it > 0 }
            .collect { loadMoreCommunityTracks() }
    }

    // Шаг 4 (#32d): загрузка тем обсуждений при переходе на вкладку 5.
    LaunchedEffect(selectedTab, groupId) {
        if (selectedTab != 5 || topicsLoaded || topicsLoading) return@LaunchedEffect
        topicsLoading = true
        topicsError = null
        try {
            topics = app.apiClient.boardGetTopics(groupId = groupId, count = 30, offset = 0)
            topicsLoaded = true
            AppLog.i("CommunityScreen", "Loaded ${topics.size} topics for group $groupId")
        } catch (e: Exception) {
            AppLog.e("CommunityScreen", "boardGetTopics failed", e)
            topicsError = "Ошибка: ${e.message}"
        } finally {
            topicsLoading = false
        }
    }

    // P0.8 (Task 16): загрузка документов сообщества при переходе на вкладку 6.
    // docs.get с ownerId=-groupId (VK convention для групп — отрицательный id).
    // 30 за раз, догрузка по скроллу — см. loadMoreDocs ниже.
    LaunchedEffect(selectedTab, groupId) {
        if (selectedTab != 6 || docsLoaded || docsLoading) return@LaunchedEffect
        docsLoading = true
        docsError = null
        try {
            docs = app.apiClient.docsGet(ownerId = -groupId, count = 30, offset = 0)
            docsLoaded = true
            docsEndReached = docs.size < 30
            AppLog.i("CommunityScreen", "Loaded ${docs.size} docs for group $groupId")
        } catch (e: Exception) {
            AppLog.e("CommunityScreen", "docsGet failed", e)
            docsError = "Ошибка: ${e.message}"
        } finally {
            docsLoading = false
        }
    }

    // P0.8 (Task 16): пагинация документов (как у видео — по скроллу).
    fun loadMoreDocs() {
        if (docsLoadingMore || docsEndReached || docs.isEmpty()) return
        scope.launch {
            docsLoadingMore = true
            try {
                val more = app.apiClient.docsGet(
                    ownerId = -groupId, count = 30, offset = docs.size)
                val newOnes = more.filter { nd ->
                    docs.none { it.id == nd.id && it.ownerId == nd.ownerId }
                }
                if (newOnes.isEmpty()) {
                    docsEndReached = true
                } else {
                    docs = (docs + newOnes).distinctBy { "${it.ownerId}_${it.id}" }
                    if (more.size < 30) docsEndReached = true
                }
                AppLog.i("CommunityScreen", "loadMoreDocs: +${newOnes.size} (total=${docs.size})")
            } catch (e: Exception) {
                AppLog.e("CommunityScreen", "loadMoreDocs failed", e)
            } finally {
                docsLoadingMore = false
            }
        }
    }

    // P0.8 (Task 16): триггер пагинации документов (только когда активна вкладка 6).
    LaunchedEffect(listState, docs.size, selectedTab) {
        if (selectedTab != 6) return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = info.totalItemsCount
            total > 0 && lastVisible >= total - 3
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { loadMoreDocs() }
    }

    // Fix #85: триггер пагинации.
    LaunchedEffect(listState, posts.size) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = info.totalItemsCount
            total > 0 && lastVisible >= total - 3
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { loadMoreWall() }
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val g = groupInfo
    if (g == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = errorText ?: "Сообщество не загружено",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.Button(onClick = onBack) { Text("Назад") }
                androidx.compose.material3.OutlinedButton(onClick = {
                    loading = true
                    errorText = null
                    scope.launch {
                        try {
                            val grps = app.apiClient.groupsGetById(listOf(groupId))
                            groupInfo = grps.firstOrNull()
                            isMember = groupInfo?.isMember == 1
                            if (groupInfo == null) {
                                errorText = app.apiClient.lastApiError ?: "Сообщество не найдено (id=$groupId)"
                            } else {
                                val wallPage = app.apiClient.wallGetExtended(ownerId = -groupId, count = 30)
                                posts = wallPage.posts
                                    .filter { it.id > 0 && it.ownerId != 0L }
                                    .distinctBy { "${it.ownerId}_${it.id}" }
                                signerNames = wallPage.profileNames
                                endReached = wallPage.posts.size < 30
                            }
                        } catch (e: Exception) {
                            AppLog.e("CommunityScreen", "Retry failed", e)
                            errorText = "Ошибка: ${e.message}"
                        } finally {
                            loading = false
                        }
                    }
                }) { Text("Повторить") }
            }
        }
        return
    }

    // Fix #43: statusBarsPadding — контент не уходит под системную панель.
    // CommunityScreen в hasOwnTopBar списке VkMintNavHost → глобальный TopAppBar
    // не рисуется, insets нужно применять самому. Аналогично ProfileScreen.
    // Fix #389 #SCROLL-TOP-PARITY: Box-обёртка — оверлей для FAB «наверх»
    // (один общий listState на стену/видео/обсуждения — FAB общий для всех табов).
    // #ADMIN-SOFT-DELETE: soft-delete tick — 1 секунда за шаг; на 0 — реальный wall.delete.
    LaunchedEffect(softDeletedPost, softDeleteSeconds) {
        val sd = softDeletedPost
        if (sd != null) {
            if (softDeleteSeconds <= 0) {
                app.apiClient.wallDelete(-groupId, sd.id)
                softDeletedPost = null
                refreshWall()
            } else {
                kotlinx.coroutines.delay(1000)
                softDeleteSeconds -= 1
            }
        }
    }
    androidx.compose.runtime.DisposableEffect(groupId) {
        onDispose {
            val pending = softDeletedPost
            if (pending != null) {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO).launch {
                    app.apiClient.wallDelete(-groupId, pending.id)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        VkPanelHeader(
            modifier = Modifier.statusBarsPadding(),
            title = g.name,
            subtitle = if (g.membersCount > 0) "${formatMemberCount(g.membersCount)} подписчиков" else null,
            before = {
                IconButton(onClick = onBack) {
                    Icon(
                        VkIcons.ArrowBack24,
                        contentDescription = "Назад",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            },
            showSeparator = true,
        )
        Box(modifier = Modifier.weight(1f)) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { refreshWall() },
            modifier = Modifier.fillMaxSize(),
        ) {
        LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
            item {
                VkGroup(
                    mode = VkGroupMode.Card,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    // Header — avatar + name
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (g.photo100 != null || g.photo200 != null) {
                            AsyncImage(
                                model = g.photo200 ?: g.photo100,
                                contentDescription = g.name,
                                modifier = Modifier.size(56.dp).clip(avatarShape),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(56.dp).clip(avatarShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = g.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            // Name row with optional verified badge
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = g.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (g.verified == 1) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = "Подтверждённое сообщество",
                                        modifier = Modifier.size(18.dp),
                                        tint = Color(0xFF1976D2),
                                    )
                                }
                            }
                            // Type badge
                            val typeLabel = when (g.type) {
                                "page" -> "Публичная страница"
                                "event" -> "Мероприятие"
                                else -> "Группа"
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = typeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                                if (g.isClosed == 1) {
                                    Text(
                                        text = " · Закрытое сообщество",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            }
                            // Subscriber count — #OPVK-EXTRACT (Task 3-c): кликабельная
                            // строка-вход «Участники»: тап по счётчику подписчиков
                            // (members_count из GroupInfo) открывает список участников
                            // (GroupMembersScreen, groups.getMembers). Шеврон — аффорданс;
                            // тач-таргет ≥ 44dp (defaultMinSize).
                            if (g.membersCount > 0) {
                                val countStr = formatMemberCount(g.membersCount)
                                Row(
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 44.dp)
                                        .clickable { onMembersClick(groupId) },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "$countStr подписчиков",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Участники",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                    // Status text
                    if (!g.status.isNullOrBlank()) {
                        Text(
                            text = g.status,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    // Description
                    if (!g.description.isNullOrBlank()) {
                        val maxLines = if (descExpanded) Int.MAX_VALUE else 3
                        val descColor = MaterialTheme.colorScheme.onSurfaceVariant
                        val linkColor = Color(0xFF1976D2)
                        Text(
                            text = buildAnnotatedString {
                                append(g.description)
                                if (!descExpanded) {
                                    withStyle(SpanStyle(color = linkColor)) {
                                        append(" Показать ещё")
                                    }
                                }
                            },
                            style = MaterialTheme.typography.bodySmall.copy(color = descColor),
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .clickable { if (!descExpanded) descExpanded = true },
                            maxLines = maxLines,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        // S6-4: кнопка подписки / отписки.
        item {
            val buttonShape = MaterialTheme.shapes.medium
            val context = LocalContext.current
            if (isMember) {
                OutlinedButton(
                    onClick = {
                        if (subscribePending) return@OutlinedButton
                        scope.launch {
                            subscribePending = true
                            try {
                                val ok = app.apiClient.groupsLeave(groupId)
                                if (ok) {
                                    isMember = false
                                    Toast.makeText(context, "Вы отписались от «${groupInfo?.name ?: "сообщества"}»", Toast.LENGTH_SHORT).show()
                                    // Fix #52-B: после отписки состав историй мог
                                    // измениться (stories.get включает истории от
                                    // подписанных сообществ) — markDirty триггерит
                                    // перезагрузку в StoriesRow при возврате на Feed.
                                    vk.mint.ui.navigation.StoriesHolder.markDirty()
                                } else {
                                    val err = app.apiClient.lastApiError
                                    Toast.makeText(
                                        context,
                                        if (err.isNullOrBlank()) "Не удалось отписаться" else "Ошибка: $err",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            } catch (e: Exception) {
                                AppLog.e("CommunityScreen", "groupsLeave failed", e)
                                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                subscribePending = false
                            }
                        }
                    },
                    enabled = !subscribePending,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = buttonShape,
                ) {
                    if (subscribePending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Отписаться")
                    }
                }
            } else {
                Button(
                    onClick = {
                        if (subscribePending) return@Button
                        scope.launch {
                            subscribePending = true
                            try {
                                val ok = app.apiClient.groupsJoin(groupId)
                                if (ok) {
                                    isMember = true
                                    Toast.makeText(context, "Вы подписались на «${groupInfo?.name ?: "сообщество"}»", Toast.LENGTH_SHORT).show()
                                    // Fix #52-B: после подписки состав историй мог
                                    // измениться (новое сообщество может иметь активные
                                    // истории) — markDirty триггерит перезагрузку.
                                    vk.mint.ui.navigation.StoriesHolder.markDirty()
                                } else {
                                    val err = app.apiClient.lastApiError
                                    Toast.makeText(
                                        context,
                                        if (err.isNullOrBlank()) "Не удалось подписаться" else "Ошибка: $err",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            } catch (e: Exception) {
                                AppLog.e("CommunityScreen", "groupsJoin failed", e)
                                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                subscribePending = false
                            }
                        }
                    },
                    enabled = !subscribePending,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = buttonShape,
                ) {
                    if (subscribePending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Подписаться")
                    }
                }
            }
        }
        // W35-b (волна 35): карточка «Управление» — админ-блок над вкладками
        // (референс: правое меню 11 пунктов, сверка «Группа_админ» §4.1;
        // внутренний гейт isManager внутри блока — у обычного участника невидим).
        item {
            CommunityAdminBlock(
                groupInfo = groupInfo,
                onSettingsClick = onAdminSettingsClick,
                onStatsClick = onAdminStatsClick,
                onPeopleClick = onAdminPeopleClick,
                onLinksClick = onAdminLinksClick,
                onInvitesClick = onAdminInvitesClick,
                onQueueClick = onAdminQueueClick,
                onAddressesClick = onAdminAddressesClick,
                onEventLogClick = onAdminEventLogClick,
                onCtaClick = onCtaClick,
                onChatsClick = onChatsClick,
                onSectionsClick = onSectionsClick,
                onMenuClick = onAdminMenuClick,
                onMessagesClick = onMessagesClick,
                onCommentsClick = onAdminCommentsClick,
                onEventsClick = onAdminEventsClick,
                onStrikesClick = onAdminStrikesClick,
            )
        }
        item {
            // #30j (community tabs): единый VK-ряд капсульных вкладок контента сообщества
            // (Записи, Обсуждения, Фото, Видео, Клипы, Музыка, Файлы).
            // P0.8 (Task 16): tabs — List<Pair<Int, String>> (логический индекс → заголовок).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tabs.forEach { (idx, title) ->
                    val isSelected = selectedTab == idx
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                            .clickable { selectedTab = idx }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
        // #30j: условный рендеринг контента по выбранной вкладке.
        when (selectedTab) {
            0 -> {
                // Wall — записи сообщества (существующая реализация)
                // W36 #COMMUNITY-COMPOSER (C0 волны 35): строка композера над
                // записями (как в web — group_publish_block). Гейт can_post=1 —
                // право постить на стене (приходит в groups.getById из волны 35;
                // у обычного участника без права композер не рисуется).
                if (g.canPost == 1) {
                    item(key = "community_composer") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { showComposer = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Ава сообщества (место публикации); у руководителя
                            // дефолтный автор записи — само сообщество (сверка §3.5).
                            AsyncImage(
                                model = g.photo200 ?: g.photo100,
                                contentDescription = "Аватар сообщества",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(avatarShape),
                                contentScale = ContentScale.Crop,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Написать запись…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                val stopWords = if (app.prefsSnapshot?.newsStopWordsEnabled == true) app.prefsSnapshot?.newsStopWords.orEmpty() else emptySet()
                val filteredPosts = posts.filter { it.id != softDeletedPost?.id }
                    .filterNot { stopWords.isNotEmpty() && it.containsAnyWord(stopWords) }
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "Записи (${filteredPosts.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
        // #ADMIN-SOFT-DELETE: стрип восстановления над списком.
        item {
            if (softDeletedPost != null) {
                SoftDeleteStrip(
                    secondsLeft = softDeleteSeconds,
                    onRestore = { softDeletedPost = null },
                )
            }
        }
        items(filteredPosts, key = { "${it.ownerId}_${it.id}" }) { post ->
            val ctx = LocalContext.current
            val postKey = "${post.ownerId}_${post.id}"
            val likeOverride = likesState[postKey]
            val isLiked = likeOverride?.first ?: (post.likes?.userLikes == 1)
            val likeCount = likeOverride?.second ?: (post.likes?.count ?: 0)
            val isBookmarked = bookmarkStates[postKey] ?: post.isFavoriteBool

            val toggleBookmarkAction = {
                val newB = !isBookmarked
                bookmarkStates[postKey] = newB
                scope.launch {
                    try {
                        if (newB) {
                            app.apiClient.faveAdd("post", post.ownerId, post.id)
                            Toast.makeText(ctx, "Добавлено в закладки", Toast.LENGTH_SHORT).show()
                        } else {
                            app.apiClient.faveRemove("post", post.ownerId, post.id)
                            Toast.makeText(ctx, "Удалено из закладок", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        bookmarkStates[postKey] = !newB
                        Toast.makeText(ctx, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            val menuItems = buildList {
                add(
                    PostMenuItem(
                        text = if (isBookmarked) "Убрать из закладок" else "В закладки",
                        icon = if (isBookmarked) VkIcons.BookmarkFill20 else VkIcons.BookmarkOutline20,
                        onClick = { toggleBookmarkAction() },
                    )
                )
                if (g.isManager) {
                    add(
                        PostMenuItem(
                            text = "Действия над постом",
                            icon = VkIcons.MoreHorizontal20,
                            onClick = { menuPost = post },
                        )
                    )
                }
            }

            PostCard(
                post = post,
                authorName = g.name,
                authorPhoto = g.photo200 ?: g.photo100,
                authorIsGroup = true,
                authorIsVerified = g.verified == 1,
                signerName = post.signerId?.let { sid -> signerNames[sid] },
                showFullDate = true,
                onAuthorClick = { /* уже в сообществе */ },
                onPostClick = { clickedPost ->
                    PostHolder.lastGroups = mapOf(groupId to g)
                    onPostClick(clickedPost)
                },
                onVideoClick = onVideoClick,
                onPhotoClick = { urls, idx -> photoViewerState.value = urls to idx },
                isLiked = isLiked,
                likeCount = likeCount,
                onLikeToggle = { p ->
                    val key = "${p.ownerId}_${p.id}"
                    val base = p.likes?.count ?: 0
                    val prev = likesState[key] ?: (p.likes?.userLikes == 1) to base
                    val newLiked = !prev.first
                    val newCount = if (prev.first) (prev.second - 1).coerceAtLeast(0) else prev.second + 1
                    likesState[key] = newLiked to newCount
                    scope.launch {
                        try {
                            val serverCount = if (prev.first) {
                                app.apiClient.likesDelete("post", p.ownerId, p.id)
                            } else {
                                app.apiClient.likesAdd("post", p.ownerId, p.id)
                            }
                            likesState[key] = newLiked to (if (serverCount >= 0) serverCount else newCount)
                        } catch (e: Exception) {
                            AppLog.e("CommunityScreen", "like toggle failed for $key", e)
                            likesState[key] = prev
                        }
                    }
                },
                onCommentClick = { cPost ->
                    PostHolder.last = cPost
                    PostHolder.lastGroups = mapOf(groupId to g)
                    onPostClick(cPost)
                },
                onRepostClick = { repostPost.value = it },
                menuItems = menuItems,
                isBookmarked = isBookmarked,
                onToggleBookmark = { toggleBookmarkAction() },
                carouselEnabled = carouselEnabled,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        // Fix #85: футер пагинации.
        item {
            when {
                loadingMore -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
                endReached && filteredPosts.isNotEmpty() -> {
                    Text(
                        text = "Это все записи",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    )
                }
            }
        }
        if (filteredPosts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "На стене нет записей",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
            } // when (selectedTab) 0 — Wall
            1 -> {
                // Шаг 1 (#32a): Фото сообщества — photosGet(ownerId=-groupId, albumId="wall").
                // Сетка 3 колонки (FlowRow), тап → PhotoViewer (уже подключён ниже).
                item {
                    CommunityPhotosTab(
                        photos = photos,
                        loading = photosLoading,
                        error = photosError,
                        onPhotoClick = { urls, idx -> photoViewerState.value = urls to idx },
                    )
                }
            }
            2 -> {
                // Шаг 2 (#32b): Видео сообщества — videoGet(ownerId=-groupId).
                // Список VideoThumbnail (существующий composable), тап → onVideoClick.
                // Пагинация по скроллу (loadMoreVideos триггерится выше).
                if (videosLoading && videos.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    }
                } else if (videosError != null && videos.isEmpty()) {
                    item {
                        val errMsg = videosError
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = errMsg ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                } else if (videos.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "В сообществе нет видео",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "Видео (${videos.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    items(videos, key = { "${it.ownerId}_${it.id}" }) { video ->
                        VideoThumbnail(video = video, onClick = onVideoClick)
                    }
                    // Футер пагинации видео.
                    item {
                        when {
                            videosLoadingMore -> {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                            videosEndReached -> {
                                Text(
                                    text = "Это все видео",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                )
                            }
                        }
                    }
                }
            }
            3 -> {
                // #GROUP-CLIPS: Клипы сообщества — shortVideo.getOwnerVideos.
                // Сетка/список клипов (вертикальные карточки), тап → onVideoClick.
                if (clipsLoading && clips.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    }
                } else if (clipsError != null && clips.isEmpty()) {
                    item {
                        val errMsg = clipsError
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = errMsg ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                } else if (clips.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "В сообществе нет клипов",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "Клипы (${clips.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    items(clips, key = { "clip_${it.ownerId}_${it.id}" }) { clip ->
                        ClipThumbnail(video = clip, onClick = onVideoClick)
                    }
                }
            }
            4 -> {
                // Шаг 3 (#32c): Музыка сообщества — audioGet(ownerId=-groupId).
                // AudioAttachmentList сам управляет play через PlayerConnection.playTrackList.
                item {
                    when {
                        tracksLoading && tracks.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            }
                        }
                        tracksError != null && tracks.isEmpty() -> {
                            val errMsg = tracksError
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = errMsg ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                            }
                        }
                        tracks.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "В сообществе нет музыки",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = "Музыка (${tracks.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            // AudioAttachmentList — общий компонент (FeedScreen/ProfileScreen).
                            // play/pause через PlayerConnection.playTrackList(tracks, startIndex).
                            vk.mint.ui.components.AudioAttachmentList(tracks = tracks)
                        }
                    }
                }
                // #AUDIO-PAGING-ALL: футер догрузки музыки сообщества.
                if (tracksHasMore) {
                    item(key = "community_music_more") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
            5 -> {
                // Шаг 4 (#32d): Обсуждения — boardGetTopics(groupId).
                // Список тем; тап → onTopicClick → BoardTopicScreen.
                if (topicsLoading && topics.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    }
                } else if (topicsError != null && topics.isEmpty()) {
                    item {
                        val errMsg = topicsError
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = errMsg ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                } else if (topics.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "В сообществе нет обсуждений",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "Обсуждения (${topics.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    items(topics, key = { it.id }) { topic ->
                        BoardTopicRow(
                            topic = topic,
                            onClick = { onTopicClick(groupId, topic.id, topic.title) },
                        )
                    }
                }
            }
            6 -> {
                // P0.8 (Task 16): Файлы сообщества — docs.get(ownerId=-groupId).
                // Список документов (иконка по типу / название / размер / дата),
                // тап → DownloadManager (как ChannelFilesDialog в ChatDetailScreen).
                // Пагинация по скроллу (loadMoreDocs триггерится выше).
                if (docsLoading && docs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    }
                } else if (docsError != null && docs.isEmpty()) {
                    item {
                        val errMsg = docsError
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = errMsg ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                } else if (docs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "В сообществе нет файлов",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "Файлы (${docs.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    items(docs, key = { "doc_${it.ownerId}_${it.id}" }) { doc ->
                        CommunityDocRow(doc = doc)
                    }
                    // Футер пагинации документов.
                    item {
                        when {
                            docsLoadingMore -> {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                            docsEndReached -> {
                                Text(
                                    text = "Это все файлы",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        } // when (selectedTab)
    }
    } // PullToRefreshBox (Fix #85)

    // Fix #389 #SCROLL-TOP-PARITY: единая FAB-стрелка «наверх» — работает на
    // всех табах сообщества (стена/видео/обсуждения — один общий LazyList).
    ScrollToTopFab(
        listState = listState,
        modifier = Modifier.align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 16.dp),
    )
    } // closes Box (Fix #389 #SCROLL-TOP-PARITY)
    } // closes Column

    // Sprint 2, P1-1 (#88): полноэкранный просмотр фото.
    // #ADMIN-POST-ACTIONS: меню действий над постом.
    menuPost?.let { mp ->
        PostActionsMenu(
            groupId = groupId,
            postId = mp.id,
            isPinnedInMain = mp.isPinned == 1,
            isCommentsClosed = (mp.comments?.canPost ?: 1) == 0,
            onEdit = { editingPost = mp; showComposer = true; menuPost = null },
            onSoftDelete = {
                val prev = softDeletedPost
                if (prev != null && prev.id != mp.id) {
                    scope.launch { app.apiClient.wallDelete(-groupId, prev.id) }
                }
                softDeletedPost = mp
                softDeleteSeconds = 10
                menuPost = null
            },
            onDismiss = { menuPost = null },
            onChanged = { refreshWall() },
        )
    }

    val viewer = photoViewerState.value
    if (viewer != null) {
        PhotoViewer(
            photos = viewer.first,
            initial = viewer.second,
            onDismiss = { photoViewerState.value = null },
        )
    }

    // ShareSheet: расширенный диалог «Поделиться».
    // #SHARE-18B: onSuccess → refreshWall — после репоста на стену сообщества
    // (или публикации вложения) стена сразу показывает новый пост,
    // как в PostDetailScreen (doRefresh).
    val sharing = repostPost.value
    if (sharing != null) {
        ShareSheet(
            post = sharing,
            onDismiss = { repostPost.value = null },
            onSuccess = { refreshWall() },
        )
    }

    // W36 #COMMUNITY-COMPOSER: диалог создания записи на стене сообщества.
    // Публикация: wall.post owner_id=-groupId + from_group/signed (переключатель
    // автора внутри диалога; «сообщество» гейтится canPostAsGroup=isAuthor —
    // admin_level>=2, волна 35). После публикации — refreshWall() (паттерн
    // #SHARE-18B): новый пост виден сразу, как в вебе.
    if (showComposer) {
        val composerContext = LocalContext.current
        CreatePostDialog(
            onDismiss = { showComposer = false; editingPost = null },
            onSubmit = { _, _ -> },
            initialText = editingPost?.text ?: "",
            submitLabel = if (editingPost != null) "Сохранить" else "Опубликовать",
            targetGroupId = groupId,
            canPostAsGroup = g.isAuthor,
            onSubmitGroup = { message, fromGroup, signed ->
                scope.launch {
                    try {
                        val postId = if (editingPost != null) {
                            val ep = editingPost!!
                            if (app.apiClient.wallEdit(-groupId, ep.id, message)) ep.id else 0L
                        } else {
                            app.apiClient.wallPost(
                                message,
                                ownerId = -groupId,
                                fromGroup = fromGroup,
                                signed = signed,
                            )
                        }
                        if (postId > 0) {
                            AppLog.i(
                                "CommunityScreen",
                                "posted to wall: id=$postId, groupId=$groupId, fromGroup=$fromGroup, signed=$signed",
                            )
                            Toast.makeText(composerContext, "Запись опубликована", Toast.LENGTH_SHORT).show()
                            showComposer = false
                            editingPost = null
                            refreshWall()
                        } else {
                            Toast.makeText(composerContext, "Не удалось опубликовать запись", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        AppLog.e("CommunityScreen", "wallPost(group) failed", e)
                        Toast.makeText(composerContext, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onSubmitGroupWithAttachments = { message, fromGroup, signed, attachments ->
                scope.launch {
                    try {
                        val attachStr = attachments.joinToString(",")
                        val postId = if (editingPost != null) {
                            val ep = editingPost!!
                            if (app.apiClient.wallEdit(-groupId, ep.id, message, attachStr)) ep.id else 0L
                        } else {
                            app.apiClient.wallPostWithAttachments(
                                message = message,
                                attachments = attachStr,
                                ownerId = -groupId,
                                fromGroup = fromGroup,
                                signed = signed,
                            )
                        }
                        if (postId > 0) {
                            AppLog.i(
                                "CommunityScreen",
                                "posted to wall with ${attachments.size} attachments: id=$postId, groupId=$groupId, fromGroup=$fromGroup",
                            )
                            Toast.makeText(composerContext, "Запись опубликована", Toast.LENGTH_SHORT).show()
                            showComposer = false
                            editingPost = null
                            refreshWall()
                        } else {
                            Toast.makeText(composerContext, "Не удалось опубликовать запись", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        AppLog.e("CommunityScreen", "wallPostWithAttachments(group) failed", e)
                        Toast.makeText(composerContext, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
    }
}




/** Formats member count in VK style: "1,2 млн", "345 тыс", "42". */
private fun formatMemberCount(n: Int): String = n.toMemberCountString()

/**
 * Шаг 4 (#32d): строка темы обсуждения в списке вкладки «Обсуждения».
 *
 * Card с заголовком темы, счётчиком комментариев, датой создания и иконкой-стрелкой.
 * Закрытые темы помечаются badge "Закрыта".
 *
 * @param topic   Тема (BoardTopic)
 * @param onClick Callback тапа → onTopicClick(groupId, topicId, title)
 */
@Composable
private fun BoardTopicRow(
    topic: vk.mint.data.model.BoardTopic,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${topic.comments} комм.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    if (topic.created > 0) {
                        Text(
                            text = "• " + topic.created.toAbsoluteTime(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    if (topic.isClosed == 1) {
                        Text(
                            text = "• Закрыта",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Шаг 1 (#32a): вкладка «Фото» сообщества.
 *
 * Сетка 3 колонки (FlowRow) из PhotoItem.largestUrl. Тап → PhotoViewer
 * через onPhotoClick(listOf(urls), index).
 *
 * Без пагинации: photosGet возвращает до 50 за раз. Если фото > 50, добавим
 * пагинацию отдельным шагом (как loadMoreWall).
 *
 * @param photos      Список PhotoItem (из photosGet)
 * @param loading     Состояние загрузки
 * @param error       Текст ошибки (null = OK)
 * @param onPhotoClick Callback (urls, index) → открытие PhotoViewer
 */
@Composable
private fun CommunityPhotosTab(
    photos: List<vk.mint.data.model.PhotoItem>,
    loading: Boolean,
    error: String?,
    onPhotoClick: (List<String>, Int) -> Unit,
) {
    when {
        loading -> {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
        }
        error != null -> {
            Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        photos.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "В сообществе нет фото",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        else -> {
            // Фильтруем только фото с валидным URL; largestUrl берёт самый большой size.
            val withUrl = photos.mapNotNull { p ->
                val url = p.largestUrl ?: return@mapNotNull null
                p to url
            }
            val allUrls = withUrl.map { it.second }
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                maxItemsInEachRow = 3,
            ) {
                withUrl.forEachIndexed { index, (_, url) ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onPhotoClick(allUrls, index) },
                        elevation = CardDefaults.cardElevation(0.dp),
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
        }
    }
}

// P0.8 (Task 16): строка документа во вкладке «Файлы» сообщества.
// Иконка по типу (image/gif/pdf/video/text) + название + ext/размер/тип/дата +
// кнопка «скачать» (DownloadManager, как ChannelFilesDialog в ChatDetailScreen).
// Тап по строке тоже запускает загрузку. Файлы с ext=gif/image показываются
// иконкой Image (preview не отрисован — отдельный viewer не заведён, только
// скачивание — паттерн ChannelFileRow).
@Composable
private fun CommunityDocRow(doc: vk.mint.data.model.DocFile) {
    val context = LocalContext.current

    fun downloadDoc() {
        val url = doc.url
        if (url.isBlank()) {
            Toast.makeText(context, "Ссылка на файл недоступна — обновите список", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val safeTitle = doc.title.ifBlank { "vk_doc_${doc.id}" }
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val fileName = "$safeTitle.${doc.ext}"
            val request = android.app.DownloadManager.Request(android.net.Uri.parse(url))
                .setTitle(fileName)
                .setDescription("Документы VK")
                .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, "VK/$fileName")
            val dm = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as? android.app.DownloadManager
            if (dm == null) {
                Toast.makeText(context, "Загрузка недоступна на этом устройстве", Toast.LENGTH_SHORT).show()
                return
            }
            dm.enqueue(request)
            Toast.makeText(context, "Загрузка началась: $fileName", Toast.LENGTH_SHORT).show()
            AppLog.i("CommunityScreen", "Doc download enqueued: $fileName (${doc.sizeLabel})")
        } catch (e: Exception) {
            AppLog.e("CommunityScreen", "Doc download failed", e)
            Toast.makeText(context, "Не удалось скачать: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().clickable { downloadDoc() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when {
                    doc.isImage -> Icons.Outlined.Image
                    doc.isGif -> Icons.Outlined.Image
                    doc.ext.lowercase() == "pdf" -> Icons.Outlined.PictureAsPdf
                    doc.type == 6 -> Icons.Outlined.Movie
                    else -> Icons.Outlined.Description
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = doc.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // EXT • размер • тип • дата (как в ChannelFilesDialog, + дата).
            val dateStr = if (doc.date > 0) " • " + doc.date.toAbsoluteTime() else ""
            Text(
                text = "${doc.ext.uppercase()} • ${doc.sizeLabel} • ${doc.typeLabel}$dateStr",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        IconButton(onClick = { downloadDoc() }) {
            Icon(Icons.Outlined.Download, contentDescription = "Скачать")
        }
    }
}
