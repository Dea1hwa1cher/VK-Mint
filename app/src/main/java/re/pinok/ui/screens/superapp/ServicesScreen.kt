package re.pinok.ui.screens.superapp

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import re.pinok.ui.navigation.Screen

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
)

/**
 * Категория сервисов с заголовком и списком карточек.
 */
private data class ServiceCategory(
    val title: String,
    val items: List<ServiceItem>,
)

private val categories = listOf(
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
        title = "Возможности PinoK",
        items = listOf(
            ServiceItem(
                title = "Офлайн-менеджер",
                subtitle = "Скачанные треки и видео без сети",
                icon = Icons.Default.CloudOff,
                iconColor = Color(0xFF00897B),
                iconBgColor = Color(0x2600897B),
                screen = Screen.OfflineManager,
                badge = "Offline",
            ),
            ServiceItem(
                title = "Эквалайзер",
                subtitle = "5 полос, бас, виртуализатор и пресеты",
                icon = Icons.Default.Equalizer,
                iconColor = Color(0xFF8E24AA),
                iconBgColor = Color(0x268E24AA),
                screen = Screen.Equalizer,
                badge = "DSP",
            ),
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
        ),
    ),
)

/**
 * Экран «Сервисы» — главный SuperApp-хаб в оригинальном стиле VK с Material 3 дизайном.
 */
@Composable
fun ServicesScreen(onOpenSection: ((Screen) -> Unit)? = null) {
    val context = LocalContext.current

    val handleItemClick: (ServiceItem) -> Unit = { item ->
        val screen = item.screen
        if (screen != null) {
            onOpenSection?.invoke(screen)
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
        Triple("Офлайн", Icons.Default.CloudOff, Screen.OfflineManager),
        Triple("Друзья", Icons.Default.People, Screen.Friends),
        Triple("Эквалайзер", Icons.Default.Equalizer, Screen.Equalizer),
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                    text = "Каталог разделов, медиатеки и возможностей PinoK",
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
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable {
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
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
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
                        .background(item.iconBgColor, RoundedCornerShape(12.dp)),
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
                                shape = RoundedCornerShape(6.dp),
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
