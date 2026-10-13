package vk.mint.ui.components

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import vk.mint.data.model.Poll
import vk.mint.data.model.Post
import vk.mint.data.model.Video
import vk.mint.ui.icons.VkIcons
import vk.mint.ui.screens.feed.ReactionEntry
import vk.mint.ui.screens.feed.ReactionPicker
import vk.mint.ui.theme.vkFootnote
import vk.mint.ui.theme.vkHeadline2
import vk.mint.ui.theme.vkParagraph
import vk.mint.ui.theme.vkSubhead
import vk.mint.util.linkifyVkText
import vk.mint.util.openUrlExternal
import vk.mint.util.toAbsoluteTime
import vk.mint.util.toCountString
import vk.mint.util.toRelativeTime

/**
 * Элемент контекстного меню поста (PostMenuItem).
 */
data class PostMenuItem(
    val text: String,
    val icon: ImageVector? = null,
    val isDestructive: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * Единая карточка поста (PostCard) для всех экранов приложения:
 * лента, стена профиля, стена сообщества, детальный экран поста, закладки.
 */
@Composable
fun PostCard(
    post: Post,
    authorName: String,
    authorPhoto: String?,
    authorIsGroup: Boolean = post.fromId < 0,
    authorIsVerified: Boolean = false,
    signerName: String? = null,
    // Дата: если true -> post.date.toAbsoluteTime() ("31 дек 2026, 14:05"), иначе post.date.toRelativeTime()
    showFullDate: Boolean = false,
    // Навигация
    onAuthorClick: () -> Unit = {},
    onPostClick: (Post) -> Unit = {},
    onVideoClick: (Video) -> Unit = {},
    onPhotoClick: (List<String>, Int) -> Unit = { _, _ -> },
    // Лайки и интерактив
    isLiked: Boolean = post.likes?.userLikes == 1,
    likeCount: Int = post.likes?.count ?: 0,
    onLikeToggle: (Post) -> Unit = {},
    onCommentClick: (Post) -> Unit = {},
    onRepostClick: (Post) -> Unit = {},
    onReaction: ((Post, ReactionEntry) -> Unit)? = null,
    // Подписка в хедере
    isSubscribed: Boolean? = null,
    onSubscribe: (() -> Unit)? = null,
    // ⋮ контекстное меню
    menuItems: List<PostMenuItem> = emptyList(),
    // Значок закладки на карточке (null = скрыть, true/false = показать заполненный/контурный)
    isBookmarked: Boolean? = null,
    onToggleBookmark: (() -> Unit)? = null,
    // Репост автор
    repostAuthorName: String? = null,
    repostAuthorPhoto: String? = null,
    onRepostAuthorClick: (() -> Unit)? = null,
    // Опросы
    onVote: ((Poll, List<Long>) -> Unit)? = null,
    // Настройки отрисовки
    textMaxLines: Int = 10,
    carouselEnabled: Boolean = true,
    showViews: Boolean = true,
    showViewsInHeader: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val avatarShape = CircleShape

    VkGroup(
        mode = VkGroupMode.Card,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Метка «Реклама» для рекламных постов
            if (post.isAd) {
                Text(
                    text = "Реклама",
                    style = MaterialTheme.typography.vkFootnote,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                )
            }
            // Метка для рекомендованных постов
            if (post.postType in listOf("suggest", "suggested")) {
                Text(
                    text = "Рекомендуемое",
                    style = MaterialTheme.typography.vkFootnote,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = if (post.isAd) 0.dp else 8.dp),
                )
            }

            // ─── Хедер поста (Аватар, автор, верификация, время, подписка, ⋮ меню) ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Аватар автора (кликабельный)
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(40.dp)
                        .clip(avatarShape)
                        .clickable { onAuthorClick() },
                ) {
                    if (authorPhoto != null) {
                        AsyncImage(
                            model = authorPhoto,
                            contentDescription = authorName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.outlineVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = authorName.take(1).uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // Имя автора и подзаголовок (дата/просмотры/подпись)
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = authorName,
                            style = MaterialTheme.typography.vkHeadline2,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.clickable { onAuthorClick() },
                        )
                        if (authorIsVerified) {
                            Icon(
                                VkIcons.Verified16,
                                contentDescription = "Верифицировано",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    val dateStr = if (showFullDate) post.date.toAbsoluteTime() else post.date.toRelativeTime()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (showViewsInHeader && (post.views?.count ?: 0) > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Icon(
                                    VkIcons.View16,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = (post.views?.count ?: 0).toCountString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                        val subtitleText = if (signerName != null) "$dateStr · $signerName" else dateStr
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.vkFootnote,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Кнопка «Подписаться» в хедере
                if (isSubscribed == false && onSubscribe != null) {
                    Text(
                        text = "Подписаться",
                        style = MaterialTheme.typography.vkFootnote,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .clickable { onSubscribe() }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }

                // Кнопка контекстного меню (⋮)
                if (menuItems.isNotEmpty()) {
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                VkIcons.MoreHorizontal20,
                                contentDescription = "Действия",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            menuItems.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = item.text,
                                            color = if (item.isDestructive) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface,
                                        )
                                    },
                                    leadingIcon = item.icon?.let { icon ->
                                        {
                                            Icon(
                                                icon,
                                                contentDescription = null,
                                                tint = if (item.isDestructive) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        item.onClick()
                                    },
                                    enabled = item.enabled,
                                )
                            }
                        }
                    }
                }
            }

            // Индикатор закреплённого поста
            if (post.isPinnedBool) {
                Text(
                    text = "Закреплённый пост",
                    style = MaterialTheme.typography.vkFootnote,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp),
                )
            }

            // Индикатор копирайта-источника
            val copyrightName = post.copyright?.name
            if (copyrightName != null) {
                Text(
                    text = copyrightName,
                    style = MaterialTheme.typography.vkFootnote,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Индикатор Donut-заглушки
            val donutPlaceholder = post.donut?.placeholder
            if (post.isDonut && donutPlaceholder != null) {
                Text(
                    text = donutPlaceholder,
                    style = MaterialTheme.typography.vkSubhead,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ─── Текст поста (сворачивание/разворачивание + linkify) ───
            if (post.text.isNotBlank()) {
                var expanded by remember { mutableStateOf(false) }
                val textLines = post.text.lines().size
                val needsExpand = textLines > 6
                val fullText = linkifyVkText(
                    text = post.text,
                    linkColor = MaterialTheme.colorScheme.primary,
                    onUrlClick = { url -> openUrlExternal(ctx, url) },
                )
                val collapsedText = if (needsExpand) {
                    linkifyVkText(
                        text = post.text.lines().take(6).joinToString("\n"),
                        linkColor = MaterialTheme.colorScheme.primary,
                        onUrlClick = { url -> openUrlExternal(ctx, url) },
                    )
                } else fullText

                Text(
                    text = if (needsExpand && !expanded) collapsedText else fullText,
                    style = MaterialTheme.typography.vkParagraph,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPostClick(post) }
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 6.dp),
                )
                if (needsExpand && !expanded) {
                    Text(
                        text = "Показать ещё",
                        style = MaterialTheme.typography.vkSubhead,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
                            .clickable { expanded = true },
                    )
                }
            }

            // ─── Вложения поста (AttachmentRenderer) ───
            AttachmentRenderer(
                post = post,
                onPhotoClick = onPhotoClick,
                onVideoClick = onVideoClick,
                onPostClick = onPostClick,
                carouselEnabled = carouselEnabled,
                repostAuthorName = repostAuthorName,
                repostAuthorPhoto = repostAuthorPhoto,
                onRepostAuthorClick = onRepostAuthorClick,
                onVote = onVote,
            )

            // ─── Футер поста (Лайк, Комментарий, Репост, Просмотры, Закладка) ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Левая группа: Лайк (+пикер реакций), Комментарий, Репост
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    var showReactions by remember { mutableStateOf(false) }
                    Box {
                        VkReactionPill(
                            icon = if (isLiked) VkIcons.LikeFill20 else VkIcons.LikeOutline20,
                            count = likeCount,
                            active = isLiked,
                            onClick = { onLikeToggle(post) },
                            onLongClick = if (onReaction != null) { { showReactions = true } } else null,
                        )
                        if (showReactions && onReaction != null) {
                            Box(
                                modifier = Modifier
                                    .offset(y = (-48).dp)
                                    .zIndex(10f),
                            ) {
                                ReactionPicker(
                                    onDismiss = { showReactions = false },
                                    onSelect = { reaction ->
                                        onReaction(post, reaction)
                                    },
                                )
                            }
                        }
                    }
                    VkReactionPill(
                        icon = VkIcons.CommentOutline20,
                        count = post.comments?.count ?: 0,
                        onClick = { onCommentClick(post) },
                    )
                    VkReactionPill(
                        icon = VkIcons.RepostOutline20,
                        count = post.reposts?.count ?: 0,
                        onClick = { onRepostClick(post) },
                    )
                }

                // Правая группа: Просмотры и Иконка-закладка
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val viewsCount = post.views?.count ?: 0
                    if (showViews && !showViewsInHeader && viewsCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = VkIcons.View16,
                                contentDescription = "Просмотры",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = formatVkCount(viewsCount),
                                style = MaterialTheme.typography.vkFootnote,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Иконка-закладка на карточке:
                    // Если задан явный isBookmarked (или onToggleBookmark не null) — показываем
                    val effectiveBookmark = isBookmarked ?: if (onToggleBookmark != null) post.isFavoriteBool else null
                    if (effectiveBookmark != null && onToggleBookmark != null) {
                        Icon(
                            imageVector = if (effectiveBookmark) VkIcons.BookmarkFill20 else VkIcons.BookmarkOutline20,
                            contentDescription = if (effectiveBookmark) "Убрать из закладок" else "В закладки",
                            tint = if (effectiveBookmark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { onToggleBookmark() },
                        )
                    }
                }
            }
        }
    }
}
