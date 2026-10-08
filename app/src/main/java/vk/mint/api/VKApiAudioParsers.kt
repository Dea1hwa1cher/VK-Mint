package vk.mint.api

import com.google.gson.JsonObject
import vk.mint.util.AppLog
import vk.mint.data.model.Track
import vk.mint.data.model.AudioSearchResult
import vk.mint.data.model.AudioPlaylist
import vk.mint.data.model.AudioArtist

/**
 * O2 #PERF-STRUCT (2026-09-16) шаг 2.1: вынос "листовых" audio-парсеров из VKApiClient.kt.
 *
 * Эти функции зависят только от JsonObject — не используют приватные поля VKApiClient
 * (httpClient, tokenStorage, prefs, call() и т.д.), поэтому могут быть top-level
 * в том же пакете vk.mint.api. Вызовы из VKApiClient.kt продолжают работать
 * без правок (Kotlin резолвит top-level functions в том же package).
 */

internal fun extractAlbumThumb(o: JsonObject): String? {
    // Прямая строка URL в album_thumb
    val direct = o.get("album_thumb")?.takeIf { !it.isJsonNull }?.asString
    if (!direct.isNullOrBlank()) return direct
    // Объект album.thumb.photo_XXX
    val thumbObj = o.getAsJsonObject("album")?.getAsJsonObject("thumb") ?: return null
    // Берём самый большой размер
    listOf("photo_270", "photo_300", "photo_135", "photo_68", "photo_34")
        .forEach { key ->
            val url = thumbObj.get(key)?.takeIf { !it.isJsonNull }?.asString
            if (!url.isNullOrBlank()) return url
        }
    return null
}

/** Извлекает обложку из разных форматов каталога. */
internal fun extractCatalogAlbumThumb(o: JsonObject): String? {
    // Прямая строка
    val direct = o.get("album_thumb")?.takeIf { !it.isJsonNull }?.asString
    if (!direct.isNullOrBlank()) return direct
    // Объект album.thumb
    val thumbObj = o.getAsJsonObject("album")?.getAsJsonObject("thumb") ?: return null
    for (key in listOf("photo_600", "photo_300", "photo_270", "photo_135", "photo_68", "photo_34")) {
        val url = thumbObj.get(key)?.takeIf { !it.isJsonNull }?.asString
        if (!url.isNullOrBlank()) return url
    }
    // Обложка thumb (новый формат): cover_url или photos[]
    val coverUrl = o.get("cover_url")?.takeIf { !it.isJsonNull }?.asString
    if (!coverUrl.isNullOrBlank()) return coverUrl
    return null
}


// ─── Шаг 2.2 (2026-09-16): audio playlist / artist / search finalizer ───

/**
 * Fix #281 (crash): финальная дедупликация результатов поиска.
 *
 * Артисты, спарсенные из links[] catalog.getAudioSearch, приходят с id=0 у
 * ВСЕХ (VK не отдаёт числовой id в links). Без дедупа два таких артиста в
 * результате давали два item с key="artist_0" в LazyRow →
 * IllegalArgumentException «Key was already used» → процесс падал (пользователь
 * видел «приложение закрылось при поиске»).
 *
 * Идентичность артиста: id>0 → по числовому id; id=0 → по имени (lowercase).
 * Плейлисты/треки — по (ownerId, id). Треки обрезаются до maxTracks.
 */
internal fun finalizeAudioSearchResult(
    tracks: MutableList<Track>,
    artists: MutableList<vk.mint.data.model.AudioArtist>,
    playlists: MutableList<AudioPlaylist>,
    maxTracks: Int,
    nextFrom: String? = null,
): vk.mint.data.model.AudioSearchResult {
    val uniqueTracks = tracks.distinctBy { "${it.ownerId}_${it.id}" }.take(maxTracks)
    val uniqueArtists = artists.distinctBy { a ->
        if (a.id > 0L) "id_${a.id}" else "name_${a.name.lowercase()}"
    }
    val uniquePlaylists = playlists.distinctBy { "${it.ownerId}_${it.id}" }
    return vk.mint.data.model.AudioSearchResult(
        uniqueTracks, uniqueArtists, uniquePlaylists, nextFrom,
    )
}

/** Универсальный парсер AudioPlaylist из JsonObject. */
internal fun parseAudioPlaylist(o: JsonObject): vk.mint.data.model.AudioPlaylist? {
    return try {
        val id = o.get("id")?.asLong ?: return null
        val ownerId = o.get("owner_id")?.asLong ?: 0L
        val photo = o.getAsJsonObject("photo")
        vk.mint.data.model.AudioPlaylist(
            id = id,
            ownerId = ownerId,
            title = o.get("title")?.takeIf { !it.isJsonNull }?.asString ?: "",
            description = o.get("description")?.takeIf { !it.isJsonNull }?.asString,
            photo = photo?.get("photo_1200")?.takeIf { !it.isJsonNull }?.asString
                ?: photo?.get("photo_600")?.takeIf { !it.isJsonNull }?.asString,
            photo200 = photo?.get("photo_200")?.takeIf { !it.isJsonNull }?.asString,
            photo300 = photo?.get("photo_300")?.takeIf { !it.isJsonNull }?.asString,
            photo600 = photo?.get("photo_600")?.takeIf { !it.isJsonNull }?.asString,
            count = o.get("count")?.takeIf { !it.isJsonNull }?.asInt ?: 0,
            genreId = o.get("genre_id")?.takeIf { !it.isJsonNull }?.asInt,
            type = o.get("type")?.takeIf { !it.isJsonNull }?.asString,
            accessKey = o.get("access_key")?.takeIf { !it.isJsonNull }?.asString,
            followers = o.get("followers")?.takeIf { !it.isJsonNull }?.asInt ?: 0,
            plays = o.get("plays")?.takeIf { !it.isJsonNull }?.asInt ?: 0,
            createTime = o.get("create_time")?.takeIf { !it.isJsonNull }?.asLong ?: 0L,
            updateTime = o.get("update_time")?.takeIf { !it.isJsonNull }?.asLong ?: 0L,
        )
    } catch (e: Exception) {
        AppLog.w("VKApiClient", "parseAudioPlaylist failed: ${e.message}")
        null
    }
}

/** Универсальный парсер AudioArtist из JsonObject. */
internal fun parseAudioArtist(o: JsonObject): vk.mint.data.model.AudioArtist? {
    return try {
        val id = o.get("id")?.asLong ?: return null
        vk.mint.data.model.AudioArtist(
            id = id,
            name = o.get("name")?.takeIf { !it.isJsonNull }?.asString ?: "",
            domain = o.get("domain")?.takeIf { !it.isJsonNull }?.asString,
            photo = o.get("photo")?.takeIf { !it.isJsonNull }?.asString,
            photo100 = o.get("photo_100")?.takeIf { !it.isJsonNull }?.asString,
            photo200 = o.get("photo_200")?.takeIf { !it.isJsonNull }?.asString,
            followers = o.get("followers")?.takeIf { !it.isJsonNull }?.asInt ?: 0,
            genres = o.getAsJsonArray("genres")?.mapNotNull {
                it.takeIf { !it.isJsonNull }?.asString
            },
            isFollowed = o.get("is_followed")?.takeIf { !it.isJsonNull }?.asBoolean ?: false,
        )
    } catch (e: Exception) {
        AppLog.w("VKApiClient", "parseAudioArtist failed: ${e.message}")
        null
    }
}

