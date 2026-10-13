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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import vk.mint.VkMintApp
import vk.mint.data.local.VkMintPrefs
import vk.mint.data.model.UserProfile
import vk.mint.ui.components.NotifyModeItem
import vk.mint.ui.navigation.FeedOpenRequest
import vk.mint.ui.navigation.Screen
import vk.mint.ui.navigation.ScreenTopBar

/**
 * In-memory кэш текущего профиля для мгновенной отрисовки шапки в «Сервисах».
 */
internal object ServicesProfileCache {
    @Volatile
    var cachedUser: UserProfile? = null

    fun clear() {
        cachedUser = null
    }
}

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
 * Экран «Сервисы» — главный SuperApp-хаб в стиле оригинального мобильного приложения VK.
 * Содержит мини-шапку профиля, компактную 4-колоночную сетку основных разделов ВКонтакте,
 * быстрый переключатель режима уведомлений и сгруппированный список инструментов и сессии.
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
    var currentUser by remember { mutableStateOf(ServicesProfileCache.cachedUser) }

    LaunchedEffect(Unit) {
        try {
            val loaded = app.apiClient.usersGetFullExtended(null)
            if (loaded != null) {
                ServicesProfileCache.cachedUser = loaded
                currentUser = loaded
            }
        } catch (_: Exception) {
        }
    }

    // Иконки быстрого доступа в шапке «Сервисов» (как в мобильном VK: Поиск, Уведомления, Настройки)
    DisposableEffect(onOpenSection) {
        val token = ScreenTopBar.configure(
            actions = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onOpenSection?.invoke(Screen.Search) }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Поиск",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onOpenSection?.invoke(Screen.Notifications) }) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = "Уведомления",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onOpenSection?.invoke(Screen.Settings) }) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "Настройки",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
        )
        onDispose { ScreenTopBar.clear(token) }
    }

    // Основные разделы VK SuperApp (компактная сетка 4 в ряд, как в оригинальном VK)
    val mainGridItems = remember(onOpenSection) {
        listOf(
            ServiceItem(
                title = "Друзья",
                subtitle = "Список друзей и заявки",
                icon = Icons.Default.People,
                iconColor = Color(0xFF4BB34B),
                iconBgColor = Color(0x264BB34B),
                screen = Screen.Friends,
            ),
            ServiceItem(
                title = "Сообщества",
                subtitle = "Группы и паблики",
                icon = Icons.Default.Group,
                iconColor = Color(0xFF2688EB),
                iconBgColor = Color(0x262688EB),
                screen = Screen.Groups,
            ),
            ServiceItem(
                title = "Музыка",
                subtitle = "Треки и плейлисты",
                icon = Icons.Default.LibraryMusic,
                iconColor = Color(0xFF904DF6),
                iconBgColor = Color(0x26904DF6),
                screen = Screen.Music,
            ),
            ServiceItem(
                title = "Видео",
                subtitle = "Видеозаписи и эфиры",
                icon = Icons.Default.PlayCircle,
                iconColor = Color(0xFFFF334B),
                iconBgColor = Color(0x26FF334B),
                screen = Screen.Video,
            ),
            ServiceItem(
                title = "Клипы",
                subtitle = "Короткие видео",
                icon = Icons.Outlined.VideoLibrary,
                iconColor = Color(0xFFE64646),
                iconBgColor = Color(0x26E64646),
                screen = Screen.Clips,
            ),
            ServiceItem(
                title = "Фото",
                subtitle = "Альбомы и снимки",
                icon = Icons.Default.PhotoLibrary,
                iconColor = Color(0xFFFFA000),
                iconBgColor = Color(0x26FFA000),
                screen = Screen.Photos,
            ),
            ServiceItem(
                title = "Закладки",
                subtitle = "Сохранённое",
                icon = Icons.Default.Bookmark,
                iconColor = Color(0xFF00A2D3),
                iconBgColor = Color(0x2600A2D3),
                screen = Screen.Bookmarks,
            ),
            ServiceItem(
                title = "Файлы",
                subtitle = "Документы и архивы",
                icon = Icons.Default.Description,
                iconColor = Color(0xFF5C6BC0),
                iconBgColor = Color(0x265C6BC0),
                screen = Screen.Documents,
            ),
            ServiceItem(
                title = "Звонки",
                subtitle = "История вызовов",
                icon = Icons.Default.Call,
                iconColor = Color(0xFF009688),
                iconBgColor = Color(0x26009688),
                screen = Screen.CallsHistory,
            ),
            ServiceItem(
                title = "Реакции",
                subtitle = "Понравившиеся записи",
                icon = Icons.Default.ThumbUp,
                iconColor = Color(0xFFE91E63),
                iconBgColor = Color(0x26E91E63),
                action = {
                    FeedOpenRequest.request("LIKES")
                    onOpenSection?.invoke(Screen.Feed)
                },
            ),
            ServiceItem(
                title = "Уведомления",
                subtitle = "Ответы и события",
                icon = Icons.Default.Notifications,
                iconColor = Color(0xFF7E57C2),
                iconBgColor = Color(0x267E57C2),
                screen = Screen.Notifications,
            ),
            ServiceItem(
                title = "Поиск",
                subtitle = "Глобальный поиск",
                icon = Icons.Default.Search,
                iconColor = Color(0xFF3F51B5),
                iconBgColor = Color(0x263F51B5),
                screen = Screen.Search,
            ),
        )
    }

    val toolMenuItems = remember(onLogout, onExitApp) {
        listOf(
            ServiceItem(
                title = "Настройки",
                subtitle = "Интерфейс, нижняя панель, кэш и приватность",
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
            ServiceItem(
                title = "Выйти из аккаунта",
                subtitle = "Смена аккаунта и сброс сессии",
                icon = Icons.AutoMirrored.Filled.Logout,
                iconColor = Color(0xFFFF5252),
                iconBgColor = Color(0x26FF5252),
                action = onLogout,
            ),
            ServiceItem(
                title = "Выйти из приложения",
                subtitle = "Закрыть с сохранением входа",
                icon = Icons.Outlined.PowerSettingsNew,
                iconColor = Color(0xFFFF7043),
                iconBgColor = Color(0x26FF7043),
                action = onExitApp,
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1. Мини-карточка профиля (как в шапке вкладки «Сервисы» / «Меню» мобильного VK)
        item(key = "mini_profile_card") {
            ServicesMiniProfileCard(
                user = currentUser,
                onClick = { onOpenSection?.invoke(Screen.Profile) },
            )
        }

        // 2. Компактная 4-колоночная сетка разделов ВКонтакте (VK SuperApp style)
        item(key = "superapp_grid_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        .padding(horizontal = 8.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = "Все разделы",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp),
                    )
                    mainGridItems.chunked(4).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            rowItems.forEach { item ->
                                SuperAppGridTile(
                                    item = item,
                                    onClick = { handleItemClick(item) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(4 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // 3. Быстрый выбор режима уведомлений
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

        // 4. Компактный список настроек, инструментов и управления сессией
        item(key = "tools_menu_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    toolMenuItems.forEachIndexed { index, item ->
                        ServiceMenuRow(
                            item = item,
                            onClick = { handleItemClick(item) },
                        )
                        if (index < toolMenuItems.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 64.dp, end = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Мини-карточка текущего пользователя в верхней части экрана «Сервисы» (как в мобильном VK).
 */
@Composable
private fun ServicesMiniProfileCard(
    user: UserProfile?,
    onClick: () -> Unit,
) {
    val fullName = remember(user) {
        if (user != null) "${user.firstName} ${user.lastName}".trim() else "Мой профиль"
    }
    val subtitle: String = remember(user) {
        val statusText = user?.status
        val domainText = user?.domain
        when {
            !statusText.isNullOrBlank() -> statusText
            !domainText.isNullOrBlank() -> "@$domainText"
            else -> "Перейти на страницу профиля"
        }
    }
    val avatarUrl = user?.photo200 ?: user?.photo100

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = fullName,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = fullName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Компактная плитка сервиса для 4-колоночной сетки в стиле мобильного VK SuperApp.
 */
@Composable
private fun SuperAppGridTile(
    item: ServiceItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(item.iconBgColor, MaterialTheme.shapes.large),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = item.iconColor,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Компактная строка меню в сгруппированной карточке инструментов/сессии.
 */
@Composable
private fun ServiceMenuRow(
    item: ServiceItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(item.iconBgColor, MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = item.iconColor,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.badge != null) {
                    Spacer(modifier = Modifier.width(8.dp))
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
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

