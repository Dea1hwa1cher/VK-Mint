package vk.mint.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import vk.mint.ui.icons.VkIcons

// ══════════════════════════════════════════════════════════════════════
//  Fix #PANELEDIT: «Редактор панелей» — единый канонический набор пунктов
//  боковой и нижней панелей по требованию пользователя.
//
//  Список (порядок = канонический):
//    Уведомления, Мессенджер, Сообщества, Фотографии, Друзья, Видео, Клипы,
//    Музыка, Закладки, Файлы, Поиск, Реакции, Сервисы, Логи.
//
//  Главное отличие от Screen.route-подхода (который не мог выразить):
//    - дубли одного экрана («Мессенджер» → Screen.Messages);
//    - «Сообщества» (Screen.Groups) рядом с «Мессенджером»;
//    - специальные действия («Реакции» → Лента с фильтром LIKES).
//  Поэтому каждый пункт имеет СТАБИЛЬНЫЙ key (не Screen.route), который и
//  хранится в VkMintPrefs (sidebar_items_order/hidden, bottombar_items_order/hidden).
// ══════════════════════════════════════════════════════════════════════

/**
 * Что делает пункт панели при тапе.
 * [Route] — обычная навигация на top-level экран.
 * [FeedFilter] — специальный пункт «Реакции»: открыть Ленту на разделе LIKES.
 */
sealed class PanelAction {
    data class Route(val screen: Screen) : PanelAction()

    /**
     * Открыть Ленту (Screen.Feed) и переключить её на указанный раздел.
     * [filterName] — это FeedFilter.name ("LIKES"), задаётся строкой, т.к.
     * enum FeedFilter приватный (в FeedScreen.kt) и не доступен из навигации.
     */
    data class FeedFilter(val filterName: String) : PanelAction()
}

/**
 * Один редактируемый пункт панели навигации.
 * @param key стабильный идентификатор для VkMintPrefs (не Screen.route —
 *            допускает дубли/соседние экраны и спец-действие «реакции»).
 * @param title подпись в панели/редакторе.
 * @param icon иконка в невыбранном состоянии (null → пункт рисуется без иконки).
 * @param action поведение при тапе.
 * @param selectedIcon иконка в выбранном состоянии (VKUI Fill28/Fill20; по умолчанию равна [icon]).
 */
data class PanelItem(
    val key: String,
    val title: String,
    val icon: ImageVector?,
    val action: PanelAction,
    val selectedIcon: ImageVector? = icon,
) {
    /** Конечный destination-route для подсветки выбранного состояния. */
    val destinationRoute: String
        get() = when (val a = action) {
            is PanelAction.Route -> a.screen.route
            is PanelAction.FeedFilter -> Screen.Feed.route
        }
}

/**
 * Канонические списки пунктов панелей. Порядок в этих списках = канонический
 * (для initial/сброса). Оба списка читаются динамически из VkMintPrefs
 * (задом `left_panel_items` / `bottom_panel_items` через order+hidden JSON).
 *
 * Боковая и нижняя панели используют ОДИН набор пунктов (требование
 * пользователя: нижняя панель показывает «тот же список»).
 */
object PanelItems {

    /** Канонический порядок + полный набор пунктов панелей (VKUI v8 `@vkontakte/icons` 28dp/24dp/20dp). */
    val all: List<PanelItem> = listOf(
        PanelItem(
            key = "feed",
            title = "Главная",
            icon = VkIcons.NewsfeedOutline28,
            action = PanelAction.Route(Screen.Feed),
            selectedIcon = VkIcons.NewsfeedFill28,
        ),
        PanelItem(
            key = "services",
            title = "Сервисы",
            icon = VkIcons.ServicesOutline28,
            action = PanelAction.Route(Screen.Services),
            selectedIcon = VkIcons.ServicesFill28,
        ),
        PanelItem(
            key = "messenger",
            title = "Мессенджер",
            icon = VkIcons.MessagesOutline28,
            action = PanelAction.Route(Screen.Messages),
            selectedIcon = VkIcons.MessagesFill28,
        ),
        PanelItem(
            key = "clips",
            title = "Клипы",
            icon = VkIcons.ClipsOutline28,
            action = PanelAction.Route(Screen.Clips),
            selectedIcon = VkIcons.ClipsFill28,
        ),
        PanelItem(
            key = "profile",
            title = "Профиль",
            icon = VkIcons.ProfileOutline28,
            action = PanelAction.Route(Screen.Profile),
            selectedIcon = VkIcons.ProfileFill28,
        ),
        PanelItem(
            key = "notifications",
            title = "Уведомления",
            icon = VkIcons.NotificationsOutline28,
            action = PanelAction.Route(Screen.Notifications),
            selectedIcon = VkIcons.NotificationsFill28,
        ),
        PanelItem(
            key = "music",
            title = "Музыка",
            icon = VkIcons.MusicOutline28,
            action = PanelAction.Route(Screen.Music),
            selectedIcon = VkIcons.MusicFill28,
        ),
        PanelItem(
            key = "video",
            title = "Видео",
            icon = VkIcons.VideoOutline28,
            action = PanelAction.Route(Screen.Video),
            selectedIcon = VkIcons.VideoFill28,
        ),
        PanelItem(
            key = "groups",
            title = "Сообщества",
            icon = VkIcons.UsersOutline28,
            action = PanelAction.Route(Screen.Groups),
            selectedIcon = VkIcons.UsersFill28,
        ),
        PanelItem(
            key = "photos",
            title = "Фотографии",
            icon = Screen.Photos.icon,
            action = PanelAction.Route(Screen.Photos),
        ),
        PanelItem(
            key = "friends",
            title = "Друзья",
            icon = VkIcons.UsersOutline28,
            action = PanelAction.Route(Screen.Friends),
            selectedIcon = VkIcons.UsersFill28,
        ),
        PanelItem(
            key = "bookmarks",
            title = "Закладки",
            icon = VkIcons.BookmarkOutline20,
            action = PanelAction.Route(Screen.Bookmarks),
            selectedIcon = VkIcons.BookmarkFill20,
        ),
        PanelItem(
            key = "files",
            title = "Файлы",
            icon = Screen.Documents.icon,
            action = PanelAction.Route(Screen.Documents),
        ),
        PanelItem(
            key = "search",
            title = "Поиск",
            icon = VkIcons.Search24,
            action = PanelAction.Route(Screen.Search),
            selectedIcon = VkIcons.Search24,
        ),
        PanelItem(
            key = "reactions",
            title = "Реакции",
            icon = VkIcons.LikeOutline20,
            action = PanelAction.FeedFilter("LIKES"),
            selectedIcon = VkIcons.LikeFill20,
        ),
        PanelItem(
            key = "logs",
            title = "Логи",
            icon = Screen.Logs.icon,
            action = PanelAction.Route(Screen.Logs),
        ),
    )

    /** По key → PanelItem (для маппинга из VkMintPrefs). */
    val byKey: Map<String, PanelItem> = all.associateBy { it.key }

    /** Те же keys в каноническом порядке. */
    val allKeys: List<String> = all.map { it.key }
}

/**
 * #PANELEDIT: запрос на открытие Ленты в конкретном разделе (для спец-пункта
 * «Реакции» → FeedFilter.LIKES). Маршрут Ленты не параметризован (Screen.Feed
 * "feed" — main-роут, параметризация ломает lastRoute/currentRoute restore,
 * см. #FEED-MENU-NOTIF в VkMintNavHost), поэтому seed передаётся через in-memory
 * holder (паттерн PostHolder/StoryHolder этого же пакета).
 *
 * FeedScreen читает желаемый раздел в LaunchedEffect(FeedOpenRequest.version):
 *  - на первой композиции применяет как initial filterName;
 *  - если Лента уже на экране — version++ заставляет перечитать (второй тап
 *    по «Реакции» переключает прямо на месте, без пересоздания screen).
 */
object FeedOpenRequest {
    /** Желаемое имя раздела ленты (FeedFilter.name) или null = без запроса. */
    @Volatile private var filterName: String? = null

    /** Монотонно растёт при каждом запросе — ключ LaunchedEffect-перечитывания. */
    @Volatile var version: Int = 0
        private set

    /** Запросить открытие Ленты на разделе [filter]. */
    fun request(filter: String) {
        filterName = filter
        version++
    }

    /** Забрать и сбросить текущий запрос (вызывается FeedScreen). */
    fun consume(): String? {
        val f = filterName
        filterName = null
        return f
    }
}