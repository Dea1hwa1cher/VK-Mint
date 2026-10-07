package vk.mint.ui.screens.superapp

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import vk.mint.VkMintApp
import vk.mint.data.local.VkMintPrefs
import vk.mint.ui.components.NotifyModeItem
import vk.mint.ui.navigation.Screen

/**
 * Модель отдельного сервиса/раздела в хабе.
 */
private data class ServiceItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val iconBgColor: Color,
    val screen: Screen? = null,
    val badge: String? = null,
    val action: (() -> Unit)? = null,
)

/**
 * Категория сервисов с заголовком и списком карточек.
 */
private data class ServiceCategory(
    val title: String,
    val items: List<ServiceItem>,
)

/**
 * Экран «Сервисы» — главный SuperApp-хаб в оригинальном стиле VK с Material 3 дизайном.
 * Включает в себя все возможности удалённого левого меню: каталог сервисов,
 * быстрый выбор режима уведомлений, настройки, логи, о приложении и управление сессией.
 */
@Composable
fun ServicesScreen(
    onOpenSection: ((Screen) -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    onExitApp: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val app = VkMintApp.get()
    val scope = rememberCoroutineScope()
    val prefsSnap by app.prefs.data.collectAsState(initial = null)

    val categories = remember(onLogout, onExitApp) {
        listOf(
            ServiceCategory(
                title = "Общение и люди",
                items = listOf(
                    ServiceItem(
                        title = "Сообщества",
                        subtitle = "Группы, паблики и встречи",
                        icon = Icons.Default.Group,
                        iconColor = Color(0xFF2688EB),
                        iconBgColor = Color(0x262688EB),
                        screen = Screen.Groups,
                    ),
                    ServiceItem(
                        title = "Друзья",
                        subtitle = "Список друзей, заявки и онлайн",
                        icon = Icons.Default.People,
                        iconColor = Color(0xFF4BB34B),
                        iconBgColor = Color(0x264BB34B),
                        screen = Screen.Friends,
                    ),
                    ServiceItem(
                        title = "Звонки",
                        subtitle = "История вызовов и звонки по ссылке",
                        icon = Icons.Default.Call,
                        iconColor = Color(0xFF009688),
                        iconBgColor = Color(0x26009688),
                        screen = Screen.CallsHistory,
                    ),
                ),
            ),
            ServiceCategory(
                title = "Медиатека и контент",
                items = listOf(
                    ServiceItem(
                        title = "Музыка",
                        subtitle = "Мои треки, плейлисты и каталог",
                        icon = Icons.Default.LibraryMusic,
                        iconColor = Color(0xFF904DF6),
                        iconBgColor = Color(0x26904DF6),
                        screen = Screen.Music,
                    ),
                    ServiceItem(
                        title = "Видеозаписи",
                        subtitle = "Фильмы, сериалы и трансляции",
                        icon = Icons.Default.PlayCircle,
                        iconColor = Color(0xFFFF334B),
                        iconBgColor = Color(0x26FF334B),
                        screen = Screen.Video,
                    ),
                    ServiceItem(
                        title = "Клипы",
                        subtitle = "Короткие вертикальные видео",
                        icon = Icons.Outlined.VideoLibrary,
                        iconColor = Color(0xFFE64646),
                        iconBgColor = Color(0x26E64646),
                        screen = Screen.Clips,
                    ),
                    ServiceItem(
                        title = "Фотографии",
                        subtitle = "Альбомы, сохранённые и отметки",
                        icon = Icons.Default.PhotoLibrary,
                        iconColor = Color(0xFFFFA000),
                        iconBgColor = Color(0x26FFA000),
                        screen = Screen.Photos,
                    ),
                ),
            ),
            ServiceCategory(
                title = "Инструменты и закладки",
                items = listOf(
                    ServiceItem(
                        title = "Закладки",
                        subtitle = "Сохранённые записи и статьи",
                        icon = Icons.Default.Bookmark,
                        iconColor = Color(0xFF00A2D3),
                        iconBgColor = Color(0x2600A2D3),
                        screen = Screen.Bookmarks,
                    ),
                    ServiceItem(
                        title = "Документы",
                        subtitle = "Файлы, архивы и документы",
                        icon = Icons.Default.Description,
                        iconColor = Color(0xFF5C6BC0),
                        iconBgColor = Color(0x265C6BC0),
                        screen = Screen.Documents,
                    ),
                    ServiceItem(
                        title = "Поиск",
                        subtitle = "Глобальный поиск по всей сети",
                        icon = Icons.Default.Search,
                        iconColor = Color(0xFF3F51B5),
                        iconBgColor = Color(0x263F51B5),
                        screen = Screen.Search,
                    ),
                    ServiceItem(
                        title = "Уведомления",
                        subtitle = "Все события, ответы и реакции",
                        icon = Icons.Default.Notifications,
                        iconColor = Color(0xFF7E57C2),
                        iconBgColor = Color(0x267E57C2),
                        screen = Screen.Notifications,
                    ),
                ),
            ),
            ServiceCategory(
                title = "Возможности VK Mint",
                items = listOf(
                    ServiceItem(
                        title = "Настройки",
                        subtitle = "Интерфейс, панели, кэш и приватность",
                        icon = Icons.Default.Settings,
                        iconColor = Color(0xFF546E7A),
                        iconBgColor = Color(0x26546E7A),
                        screen = Screen.Settings,
                    ),
                    ServiceItem(
                        title = "Логи системы",
                        subtitle = "Диагностика и системные события",
                        icon = Icons.Default.BugReport,
                        iconColor = Color(0xFF607D8B),
                        iconBgColor = Color(0x26607D8B),
                        screen = Screen.Logs,
                    ),
                    ServiceItem(
                        title = "О приложении",
                        subtitle = "Версия VK Mint, авторы и сборка",
                        icon = Icons.Default.Info,
                        iconColor = Color(0xFF29B6F6),
                        iconBgColor = Color(0x2629B6F6),
                        screen = Screen.About,
                        badge = "Инфо",
                    ),
                ),
            ),
            ServiceCategory(
                title = "Сессия и управление",
                items = listOf(
                    ServiceItem(
                        title = "Выйти из аккаунта",
                        subtitle = "Смена аккаунта и сброс сессии",
                        icon = Icons.AutoMirrored.Filled.Logout,
                        iconColor = Color(0xFFFF5252),
                        iconBgColor = Color(0x26FF5252),
                        action = onLogout,
                        badge = "Сессия",
                    ),
                    ServiceItem(
                        title = "Выйти из приложения",
                        subtitle = "Закрыть с сохранением входа",
                        icon = Icons.Outlined.PowerSettingsNew,
                        iconColor = Color(0xFFFF7043),
                        iconBgColor = Color(0x26FF7043),
                        action = onExitApp,
                    ),
                ),
            ),
        )
    }

    val handleItemClick: (ServiceItem) -> Unit = { item ->
        if (item.action != null) {
            item.action.invoke()
        } else if (item.screen != null) {
            onOpenSection?.invoke(item.screen)
        } else {
            Toast.makeText(context, "«${item.title}» — раздел в разработке", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Заголовок хаба и быстрые чипы
        item(key = "hub_header") {
            HubHeader(onOpenSection = onOpenSection)
        }

        // Быстрый выбор режима уведомлений (перенесён из шапки левого меню)
        item(key = "notify_mode_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                NotifyModeItem(
                    notifyMode = prefsSnap?.notifyMode ?: VkMintPrefs.NOTIFY_MODE_ALL,
                    onNotifyModeSelected = { mode ->
                        scope.launch { app.prefs.setNotifyMode(mode) }
                    },
                )
            }
        }

        // Категории сервисов
        categories.forEach { category ->
            item(key = "header_${category.title}") {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }

            // Разбиваем элементы категории по парам (2 карточки в строку)
            val chunkedItems = category.items.chunked(2)
            items(chunkedItems, key = { row -> row.joinToString { it.title } }) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { item ->
                        ServiceCard(
                            item = item,
                            onClick = { handleItemClick(item) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // Если нечётное число карточек в строке — пустой распорочный спейсер
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Верхний блок хаба: быстрые кнопки-ярлыки.
 */
@Composable
private fun HubHeader(onOpenSection: ((Screen) -> Unit)?) {
    val quickShortcuts = listOf(
        Triple("Музыка", Icons.Default.LibraryMusic, Screen.Music),
        Triple("Видео", Icons.Default.PlayCircle, Screen.Video),
        Triple("Сообщества", Icons.Default.Group, Screen.Groups),
        Triple("Закладки", Icons.Default.Bookmark, Screen.Bookmarks),
        Triple("Друзья", Icons.Default.People, Screen.Friends),
        Triple("Уведомления", Icons.Default.Notifications, Screen.Notifications),
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Сервисы ВКонтакте",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Каталог разделов, медиатеки и возможностей VK Mint",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Быстрые чипы
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(quickShortcuts) { (title, icon, screen) ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.clip(MaterialTheme.shapes.medium).clickable {
                        onOpenSection?.invoke(screen)
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Карточка отдельного сервиса в стиле Material 3.
 */
@Composable
private fun ServiceCard(
    item: ServiceItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Иконка в сквиркле с мягким цветным фоном
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(item.iconBgColor, MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = item.iconColor,
                        modifier = Modifier.size(22.dp),
                    )
                }

                // Дополнительный бейдж (если есть)
                if (item.badge != null) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.extraSmall,
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = item.badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp,
            )
        }
    }
}
