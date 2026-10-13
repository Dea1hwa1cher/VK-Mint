package vk.mint.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import vk.mint.VkMintApp
import vk.mint.util.AppLog
import vk.mint.data.model.Attachment
import vk.mint.data.model.Poll
import vk.mint.data.model.Post
import vk.mint.data.model.Video
import vk.mint.util.toFileSizeString
import kotlin.math.roundToInt

/**
 * Единый рендерер вложений поста (AttachmentRenderer).
 * Поддерживает: фото (PostPhotoGrid), видео (PostVideoCarousel / VideoThumbnail / ClipThumbnail),
 * аудио (AudioAttachmentList), плейлисты (PlaylistAttachmentCard),
 * ссылки (LinkCard), опросы (PollCard), документы (DocAttachmentCard) и репосты (RepostBlock).
 */
@Composable
fun AttachmentRenderer(
    post: Post,
    onPhotoClick: (List<String>, Int) -> Unit = { _, _ -> },
    onVideoClick: (Video) -> Unit = {},
    onPostClick: (Post) -> Unit = {},
    carouselEnabled: Boolean = true,
    repostAuthorName: String? = null,
    repostAuthorPhoto: String? = null,
    onRepostAuthorClick: (() -> Unit)? = null,
    onVote: ((poll: Poll, answerIds: List<Long>) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val attachments = post.attachments.orEmpty()

    Column(modifier = modifier.fillMaxWidth()) {
        // 1. Фото вложения
        val photoAttachments = attachments.filter { it.type == "photo" && it.photo != null }
        if (photoAttachments.isNotEmpty()) {
            PostPhotoGrid(
                photos = photoAttachments.mapNotNull { it.photo },
                onPhotoClick = onPhotoClick,
                carouselEnabled = carouselEnabled,
            )
        }

        // 2. Видео и клипы
        val videoAttachments = attachments.filter { it.type == "video" && it.video != null }
        if (carouselEnabled && videoAttachments.size > 1) {
            PostVideoCarousel(
                videos = videoAttachments.mapNotNull { it.video },
                carouselEnabled = carouselEnabled,
                onVideoClick = onVideoClick,
            )
        } else {
            videoAttachments.forEach { attach ->
                val v = attach.video
                if (v != null) {
                    if (v.isClip) {
                        ClipThumbnail(video = v, onClick = onVideoClick)
                    } else {
                        VideoThumbnail(video = v, onClick = onVideoClick)
                    }
                }
            }
        }

        // 3. Аудио вложения
        val audioAttachments = attachments.filter { it.type == "audio" && it.audio != null }
        if (audioAttachments.isNotEmpty()) {
            AudioAttachmentList(tracks = audioAttachments.mapNotNull { it.audio })
        }

        // 4. Плейлисты
        val playlistAttachments = attachments.filter { it.type == "audio_playlist" && it.audioPlaylist != null }
        playlistAttachments.forEach { att ->
            att.audioPlaylist?.let { PlaylistAttachmentCard(playlist = it) }
        }

        // 5. Ссылки и wiki-страницы
        val linkAttachments = attachments.filter { (it.type == "link" || it.type == "page") && it.link != null }
        linkAttachments.forEach { attach ->
            val link = attach.link
            if (link != null) {
                LinkCard(link = link, onClick = { openSafeUrl(ctx, link.url) })
            }
        }

        // 6. Опросы
        val pollAtt = attachments.firstOrNull { it.type == "poll" && it.poll != null }
        pollAtt?.poll?.let { poll ->
            val pollScope = rememberCoroutineScope()
            PollCard(poll = poll, onVote = { answerIds ->
                if (onVote != null) {
                    onVote(poll, answerIds)
                } else {
                    pollScope.launch {
                        try {
                            VkMintApp.get().apiClient.pollsAddVote(poll.id, poll.ownerId, answerIds)
                        } catch (e: Exception) {
                            AppLog.w("AttachmentRenderer", "pollsAddVote failed: ${e.message}")
                        }
                    }
                }
            })
        }

        // 7. Документы
        val docAttachments = attachments.filter { it.type == "doc" && it.doc != null }
        docAttachments.forEach { attach ->
            val doc = attach.doc
            if (doc != null) {
                DocAttachmentCard(doc = doc, onOpen = { openSafeUrl(ctx, doc.url) })
            }
        }

        // 8. Репост (copy_history)
        val copyHistory = post.copyHistory
        if (!copyHistory.isNullOrEmpty()) {
            val original = copyHistory.first()
            RepostBlock(
                repost = original,
                repostAuthorName = repostAuthorName,
                repostAuthorPhoto = repostAuthorPhoto,
                onAuthorClick = onRepostAuthorClick,
                onPhotoClick = onPhotoClick,
                onVideoClick = onVideoClick,
                onPostClick = onPostClick,
                carouselEnabled = carouselEnabled,
            )
        }
    }
}

/**
 * Единая карточка ссылки / сниппета (LinkCard).
 */
@Composable
fun LinkCard(
    link: Attachment.Link,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column {
            val linkPhotoUrl = link.photo?.largestUrl
            if (linkPhotoUrl != null) {
                AsyncImage(
                    model = linkPhotoUrl,
                    contentDescription = link.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                val linkTitle = link.title
                if (!linkTitle.isNullOrBlank()) {
                    Text(
                        text = linkTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val linkDescription = link.description
                if (!linkDescription.isNullOrBlank()) {
                    Text(
                        text = linkDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Text(
                    text = extractDomain(link.url.orEmpty()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/**
 * Единая карточка документа (DocAttachmentCard).
 */
@Composable
fun DocAttachmentCard(
    doc: Attachment.Doc,
    onOpen: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onOpen() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Description,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = doc.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${doc.ext.uppercase()} · ${doc.size.toFileSizeString()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Единая карточка опроса (PollCard).
 */
@Composable
fun PollCard(
    poll: Poll,
    onVote: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var voted by remember { mutableStateOf(poll.isVoted) }
    val totalVotes = poll.votes.coerceAtLeast(1)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp),
    ) {
        Text(
            text = poll.question,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(8.dp))

        poll.answers.forEach { answer ->
            val isSelected = selectedIds.contains(answer.id) || (voted && poll.answerId == answer.id)
            val pct = if (voted) (answer.votes.toFloat() / totalVotes * 100).roundToInt() else 0

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                    .then(
                        if (!voted) Modifier.clickable {
                            if (poll.multiple == 1) {
                                selectedIds = if (isSelected) selectedIds - answer.id else selectedIds + answer.id
                            } else {
                                selectedIds = listOf(answer.id)
                                voted = true
                                onVote(listOf(answer.id))
                            }
                        } else Modifier
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (poll.multiple == 1 && !voted) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = answer.text,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    if (voted) {
                        Text(
                            text = "${pct}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (voted) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(pct / 100f)
                            .height(3.dp)
                            .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall),
                    )
                }
            }
        }
    }
}

private fun extractDomain(url: String): String {
    return try {
        val withScheme = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        val host = Uri.parse(withScheme).host ?: return url
        if (host.startsWith("www.")) host.substring(4) else host
    } catch (_: Exception) {
        url
    }
}

private fun openSafeUrl(ctx: Context, rawUrl: String?) {
    val cleanUrl = rawUrl.orEmpty().trim()
    if (cleanUrl.isEmpty()) {
        Toast.makeText(ctx, "Ссылка недоступна", Toast.LENGTH_SHORT).show()
        return
    }
    val normalized = if (cleanUrl.contains("://")) cleanUrl else "https://$cleanUrl"
    val uri = try {
        Uri.parse(normalized)
    } catch (e: Exception) {
        AppLog.w("AttachmentRenderer", "invalid url: $cleanUrl", e)
        Toast.makeText(ctx, "Некорректная ссылка", Toast.LENGTH_SHORT).show()
        return
    }
    if (uri.scheme == null || uri.host == null) {
        Toast.makeText(ctx, "Некорректная ссылка", Toast.LENGTH_SHORT).show()
        return
    }
    val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (intent.resolveActivity(ctx.packageManager) == null) {
        Toast.makeText(ctx, "Нет приложения для открытия ссылки", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        ctx.startActivity(intent)
    } catch (e: Exception) {
        AppLog.w("AttachmentRenderer", "failed to open: $normalized", e)
        Toast.makeText(ctx, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show()
    }
}
