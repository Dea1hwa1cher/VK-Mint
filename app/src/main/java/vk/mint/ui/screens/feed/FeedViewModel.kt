package vk.mint.ui.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vk.mint.api.VKApiClient
import vk.mint.data.local.VkMintPrefs
import vk.mint.data.model.Post
import vk.mint.data.model.UserProfile
import vk.mint.ui.navigation.FeedDataHolder
import vk.mint.ui.navigation.FeedScrollHolder
import vk.mint.ui.navigation.ScrollPosition
import vk.mint.util.AppLog

/**
 * Настройки фильтрации новостной ленты, извлечённые из [VkMintPrefs.Snapshot].
 */
data class FeedFilterConfig(
    val adsBlocked: Boolean = true,
    val promoHidden: Boolean = true,
    val repostsHidden: Boolean = false,
    val stripReposts: Boolean = false,
    val stopWordsEnabled: Boolean = false,
    val stopWords: Set<String> = emptySet(),
) {
    companion object {
        fun from(snapshot: VkMintPrefs.Snapshot): FeedFilterConfig {
            return FeedFilterConfig(
                adsBlocked = snapshot.newsAdsBlocked,
                promoHidden = snapshot.newsPromoHidden,
                repostsHidden = snapshot.newsRepostsHidden,
                stripReposts = snapshot.newsStripReposts,
                stopWordsEnabled = snapshot.newsStopWordsEnabled,
                stopWords = snapshot.newsStopWords,
            )
        }
    }
}

/**
 * P3: FeedViewModel — централизованное управление состоянием ленты новостей.
 *
 * Переносит логику пагинации, кэширования, фильтрации и вызовов VK API
 * из Composable-функции в Android Architecture Components ViewModel.
 * Переживает смену конфигурации (поворот экрана), исключает гонки корутин
 * в Composable и обеспечивает полную независимость бизнес-логики от UI.
 */
class FeedViewModel(
    private val apiClient: VKApiClient,
) : ViewModel() {

    data class FeedUiState(
        val allPosts: List<Post> = emptyList(),
        val posts: List<Post> = emptyList(),
        val profiles: Map<Long, UserProfile> = emptyMap(),
        val groups: Map<Long, VKApiClient.GroupInfo> = emptyMap(),
        val loading: Boolean = false,
        val isRefreshing: Boolean = false,
        val loadingMore: Boolean = false,
        val endReached: Boolean = false,
        val nextFrom: String? = null,
        val errorText: String? = null,
        val feedFilter: FeedFilter = FeedFilter.ALL,
        val feedSearchQuery: String = "",
    )

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    @Volatile
    private var jobRunning = false

    /** Инициализация из кэша (если данные уже сохранены в FeedDataHolder). */
    fun initFromCache(
        cachedPosts: List<Post>?,
        cachedProfiles: Map<Long, UserProfile>?,
        cachedGroups: Map<Long, VKApiClient.GroupInfo>?,
        cachedNextFrom: String?,
        cachedEndReached: Boolean,
        filterConfig: FeedFilterConfig,
    ) {
        if (cachedPosts != null && _uiState.value.allPosts.isEmpty()) {
            val filtered = filterPosts(cachedPosts, filterConfig, _uiState.value.feedFilter)
            _uiState.update {
                it.copy(
                    allPosts = cachedPosts,
                    posts = filtered,
                    profiles = cachedProfiles ?: emptyMap(),
                    groups = cachedGroups ?: emptyMap(),
                    nextFrom = cachedNextFrom,
                    endReached = cachedEndReached,
                    loading = false,
                )
            }
        }
    }

    /** Первичная или принудительная загрузка ленты. */
    fun loadFeed(filterConfig: FeedFilterConfig, force: Boolean = false) {
        if (jobRunning && !force) return
        jobRunning = true

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, endReached = false, errorText = null) }
            try {
                val result = fetchPage(30, null)
                val cleanPosts = result.posts
                    .filter { it.id > 0 && it.ownerId != 0L }
                    .distinctBy { "${it.ownerId}_${it.id}" }
                val filtered = filterPosts(cleanPosts, filterConfig, _uiState.value.feedFilter)

                val nextCursor = result.nextFrom
                val isEnd = nextCursor == null

                _uiState.update {
                    it.copy(
                        allPosts = cleanPosts,
                        posts = filtered,
                        profiles = result.profiles,
                        groups = result.groups,
                        nextFrom = nextCursor,
                        endReached = isEnd,
                        loading = false,
                        errorText = if (cleanPosts.isEmpty() &&
                            it.feedFilter != FeedFilter.LIKES &&
                            it.feedFilter != FeedFilter.SEARCH
                        ) {
                            val err = apiClient.lastApiError
                            if (err != null) "Ошибка API: $err" else "Лента пуста"
                        } else null,
                    )
                }

                FeedDataHolder.snapshot(cleanPosts, result.profiles, result.groups, nextCursor, isEnd)
            } catch (e: Exception) {
                AppLog.e("FeedViewModel", "loadFeed failed", e)
                _uiState.update { it.copy(loading = false, errorText = e.message ?: "Ошибка загрузки") }
            } finally {
                jobRunning = false
            }
        }
    }

    /** Pull-to-refresh: перезагрузка со сбросом кэша и спиннером. */
    fun refreshFeed(filterConfig: FeedFilterConfig) {
        if (jobRunning) return
        jobRunning = true

        FeedDataHolder.clear()
        FeedScrollHolder.position = ScrollPosition(0, 0)
        vk.mint.ui.navigation.StoriesHolder.clear()

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorText = null) }
            try {
                val result = fetchPage(30, null)
                val cleanPosts = result.posts
                    .filter { it.id > 0 && it.ownerId != 0L }
                    .distinctBy { "${it.ownerId}_${it.id}" }
                val filtered = filterPosts(cleanPosts, filterConfig, _uiState.value.feedFilter)
                val nextCursor = result.nextFrom
                val isEnd = nextCursor == null

                _uiState.update {
                    it.copy(
                        allPosts = cleanPosts,
                        posts = filtered,
                        profiles = result.profiles,
                        groups = result.groups,
                        nextFrom = nextCursor,
                        endReached = isEnd,
                        isRefreshing = false,
                        errorText = null,
                    )
                }

                FeedDataHolder.snapshot(cleanPosts, result.profiles, result.groups, nextCursor, isEnd)
            } catch (e: Exception) {
                AppLog.w("FeedViewModel", "refreshFeed failed: ${e.message}")
                _uiState.update { it.copy(isRefreshing = false) }
            } finally {
                jobRunning = false
            }
        }
    }

    /** Пагинация: дозагрузка следующей страницы. */
    fun loadMore(filterConfig: FeedFilterConfig) {
        val s = _uiState.value
        if (s.loadingMore || s.endReached || s.nextFrom == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true) }
            try {
                val result = fetchPage(30, s.nextFrom)
                val currentAll = _uiState.value.allPosts
                val newPosts = result.posts
                    .filter { it.id > 0 && it.ownerId != 0L }
                    .filter { np -> currentAll.none { it.ownerId == np.ownerId && it.id == np.id } }

                val updatedAll = if (newPosts.isNotEmpty()) {
                    (currentAll + newPosts).distinctBy { "${it.ownerId}_${it.id}" }
                } else currentAll

                val updatedFiltered = filterPosts(updatedAll, filterConfig, _uiState.value.feedFilter)
                val updatedProfiles = _uiState.value.profiles + result.profiles
                val updatedGroups = _uiState.value.groups + result.groups
                val nextCursor = result.nextFrom
                val isEnd = nextCursor == null || newPosts.isEmpty()

                _uiState.update {
                    it.copy(
                        allPosts = updatedAll,
                        posts = updatedFiltered,
                        profiles = updatedProfiles,
                        groups = updatedGroups,
                        nextFrom = nextCursor,
                        endReached = isEnd,
                        loadingMore = false,
                    )
                }

                FeedDataHolder.snapshot(updatedAll, updatedProfiles, updatedGroups, nextCursor, isEnd)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.w("FeedViewModel", "loadMore failed: ${e.message}")
                _uiState.update { it.copy(loadingMore = false) }
            }
        }
    }

    /** Переключение раздела ленты. */
    fun setFeedFilter(filter: FeedFilter, filterConfig: FeedFilterConfig) {
        if (_uiState.value.feedFilter == filter) return
        _uiState.update { it.copy(feedFilter = filter) }
        loadFeed(filterConfig, force = true)
    }

    /** Обновление поискового запроса. */
    fun setSearchQuery(query: String, filterConfig: FeedFilterConfig) {
        _uiState.update { it.copy(feedSearchQuery = query) }
        if (_uiState.value.feedFilter == FeedFilter.SEARCH) {
            loadFeed(filterConfig, force = true)
        }
    }

    /** Применение настроек фильтрации (без повторного запроса к сети). */
    fun applyPreferences(filterConfig: FeedFilterConfig) {
        val s = _uiState.value
        val filtered = filterPosts(s.allPosts, filterConfig, s.feedFilter)
        _uiState.update { it.copy(posts = filtered) }
    }

    /** Локальное обновление поста (закрепление, закладка, редактирование). */
    fun updatePost(ownerId: Long, postId: Long, transform: (Post) -> Post) {
        _uiState.update { state ->
            val updatedAll = state.allPosts.map {
                if (it.ownerId == ownerId && it.id == postId) transform(it) else it
            }
            val updatedFiltered = state.posts.map {
                if (it.ownerId == ownerId && it.id == postId) transform(it) else it
            }
            state.copy(allPosts = updatedAll, posts = updatedFiltered)
        }
        FeedDataHolder.allPosts = _uiState.value.allPosts
    }

    /** Локальное удаление поста из ленты. */
    fun removePost(ownerId: Long, postId: Long) {
        _uiState.update { state ->
            val updatedAll = state.allPosts.filter { it.ownerId != ownerId || it.id != postId }
            val updatedFiltered = state.posts.filter { it.ownerId != ownerId || it.id != postId }
            state.copy(allPosts = updatedAll, posts = updatedFiltered)
        }
        FeedDataHolder.allPosts = _uiState.value.allPosts
    }

    /** Локальное удаление всех постов автора при блокировке источника. */
    fun removePostsFromAuthor(fromId: Long) {
        _uiState.update { state ->
            val updatedAll = state.allPosts.filter { it.fromId != fromId }
            val updatedFiltered = state.posts.filter { it.fromId != fromId }
            state.copy(allPosts = updatedAll, posts = updatedFiltered)
        }
        FeedDataHolder.allPosts = _uiState.value.allPosts
    }

    private suspend fun fetchPage(count: Int, startFrom: String?): VKApiClient.NewsfeedResult {
        val filter = _uiState.value.feedFilter
        val query = _uiState.value.feedSearchQuery
        return when {
            filter.recommended -> apiClient.newsfeedGetRecommended(count, startFrom)
            filter == FeedFilter.LIKES -> {
                VKApiClient.NewsfeedResult(emptyList(), emptyMap(), emptyMap(), null)
            }
            filter == FeedFilter.FRIENDS -> {
                val friendIds = try {
                    apiClient.friendsGet(count = 1000).map { it.id }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    AppLog.e("FeedViewModel", "friendsGet for friends feed failed", e)
                    emptyList()
                }
                if (friendIds.isEmpty()) {
                    VKApiClient.NewsfeedResult(emptyList(), emptyMap(), emptyMap(), null)
                } else {
                    apiClient.newsfeedGet(
                        count = count,
                        startFrom = startFrom,
                        filters = "post,photo,video",
                        sourceIds = friendIds.distinct().take(100),
                    )
                }
            }
            filter == FeedFilter.SEARCH -> {
                if (query.isBlank()) {
                    VKApiClient.NewsfeedResult(emptyList(), emptyMap(), emptyMap(), null)
                } else {
                    val (_, searchPosts) = apiClient.newsfeedSearch(query, count)
                    VKApiClient.NewsfeedResult(searchPosts, emptyMap(), emptyMap(), null)
                }
            }
            else -> apiClient.newsfeedGet(count, startFrom, filter.apiFilters ?: "post,photo,video")
        }
    }

    companion object {
        /** Чистая функция клиентской фильтрации постов по настройкам и фильтру. */
        fun filterPosts(
            allPosts: List<Post>,
            config: FeedFilterConfig,
            filter: FeedFilter,
        ): List<Post> {
            return allPosts.mapNotNull { post ->
                if (config.adsBlocked && post.isAd) return@mapNotNull null
                if (config.promoHidden && (post.postType == "promo" || post.postType == "ad_promo")) return@mapNotNull null

                if (config.stopWordsEnabled && config.stopWords.isNotEmpty() && post.containsAnyWord(config.stopWords)) {
                    return@mapNotNull null
                }

                if (config.repostsHidden && post.hasCopyHistory) {
                    return@mapNotNull null
                }

                val processedPost = if (config.stripReposts && post.hasCopyHistory) {
                    if (post.isOnlyRepost) {
                        return@mapNotNull null
                    } else {
                        post.strippedRepost()
                    }
                } else {
                    post
                }

                when (filter) {
                    FeedFilter.PHOTOS -> if (processedPost.attachments?.any { it.type == "photo" } == true) processedPost else null
                    else -> processedPost
                }
            }
        }

        fun provideFactory(apiClient: VKApiClient): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                FeedViewModel(apiClient)
            }
        }
    }
}
