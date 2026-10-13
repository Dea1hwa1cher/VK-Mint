package vk.mint.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import vk.mint.data.model.Post
import vk.mint.data.model.Video
import vk.mint.util.linkifyVkText
import vk.mint.util.openUrlExternal

/**
 * Единый компонент карточки репоста (RepostBlock).
 * Отображает автора репоста, текст с linkify и вложения (фото, видео, аудио, плейлисты).
 */
@Composable
fun RepostBlock(
    repost: Post,
    repostAuthorName: String? = null,
    repostAuthorPhoto: String? = null,
    onAuthorClick: (() -> Unit)? = null,
    onPhotoClick: (List<String>, Int) -> Unit = { _, _ -> },
    onVideoClick: (Video) -> Unit = {},
    onPostClick: (Post) -> Unit = {},
    carouselEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable { onPostClick(repost) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Outlined.Repeat,
                    contentDescription = "Репост",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (repostAuthorPhoto != null) {
                    AsyncImage(
                        model = repostAuthorPhoto,
                        contentDescription = repostAuthorName,
                        modifier = Modifier.size(24.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else if (repostAuthorName != null) {
                    Box(
                        modifier = Modifier.size(24.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = repostAuthorName.take(1).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = repostAuthorName ?: buildString {
                        append("Запись")
                        if (repost.ownerId < 0) append(" клуба")
                        append(' ')
                        append(repost.ownerId)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .then(if (onAuthorClick != null) Modifier.clickable { onAuthorClick() } else Modifier),
                )
            }
            if (repost.text.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                var repostTextExpanded by remember { mutableStateOf(false) }
                var repostTextOverflowed by remember { mutableStateOf(false) }
                Text(
                    text = linkifyVkText(
                        text = repost.text,
                        linkColor = MaterialTheme.colorScheme.primary,
                        onUrlClick = { url -> openUrlExternal(ctx, url) },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (repostTextOverflowed && !repostTextExpanded) {
                                repostTextExpanded = true
                            } else {
                                onPostClick(repost)
                            }
                        },
                    onTextLayout = { result ->
                        if (!repostTextExpanded && result.hasVisualOverflow) repostTextOverflowed = true
                    },
                    maxLines = if (repostTextExpanded) Int.MAX_VALUE else 5,
                    overflow = TextOverflow.Ellipsis,
                )
                if (repostTextOverflowed && !repostTextExpanded) {
                    Text(
                        text = "Показать ещё",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                            .clickable { repostTextExpanded = true },
                    )
                }
            }
            // Фото из репоста
            val repostPhotos = repost.attachments
                ?.filter { it.type == "photo" && it.photo != null }
                .orEmpty()
            if (repostPhotos.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                PostPhotoGrid(
                    photos = repostPhotos.mapNotNull { it.photo },
                    onPhotoClick = onPhotoClick,
                    carouselEnabled = carouselEnabled,
                )
            }
            // Видео в репосте
            val repostVideos = repost.attachments
                ?.filter { it.type == "video" && it.video != null }
                .orEmpty()
            if (carouselEnabled && repostVideos.size > 1) {
                Spacer(Modifier.height(6.dp))
                PostVideoCarousel(
                    videos = repostVideos.mapNotNull { it.video },
                    carouselEnabled = carouselEnabled,
                    onVideoClick = onVideoClick,
                )
            } else {
                repostVideos.forEach { att ->
                    val v = att.video
                    if (v != null) {
                        Spacer(Modifier.height(6.dp))
                        VideoThumbnail(video = v, onClick = onVideoClick)
                    }
                }
            }
            // Аудио в репосте
            val repostAudios = repost.attachments
                ?.filter { it.type == "audio" && it.audio != null }
                .orEmpty()
            if (repostAudios.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                AudioAttachmentList(tracks = repostAudios.mapNotNull { it.audio })
            }
            // Плейлисты в репосте
            repost.attachments
                ?.filter { it.type == "audio_playlist" && it.audioPlaylist != null }
                ?.forEach { att ->
                    att.audioPlaylist?.let {
                        Spacer(Modifier.height(6.dp))
                        PlaylistAttachmentCard(playlist = it)
                    }
                }
        }
    }
}
