package vk.mint.ui.screens.feed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import vk.mint.data.model.Post

class FeedViewModelTest {

    private fun createPost(
        id: Long,
        ownerId: Long,
        fromId: Long = ownerId,
        text: String = "Test post",
        markedAsAds: Int = 0,
        postType: String = "post",
        copyHistory: List<Post>? = null,
    ): Post {
        return Post(
            id = id,
            ownerId = ownerId,
            fromId = fromId,
            date = System.currentTimeMillis() / 1000,
            text = text,
            markedAsAds = markedAsAds,
            postType = postType,
            copyHistory = copyHistory,
        )
    }

    @Test
    fun testFilterPosts_adsBlocked() {
        val normalPost = createPost(1, 100, markedAsAds = 0)
        val adPost = createPost(2, 100, markedAsAds = 1)
        val posts = listOf(normalPost, adPost)

        val unblocked = FeedViewModel.filterPosts(posts, FeedFilterConfig(adsBlocked = false), FeedFilter.ALL)
        assertEquals(2, unblocked.size)

        val blocked = FeedViewModel.filterPosts(posts, FeedFilterConfig(adsBlocked = true), FeedFilter.ALL)
        assertEquals(1, blocked.size)
        assertEquals(1L, blocked[0].id)
    }

    @Test
    fun testFilterPosts_repostsHidden() {
        val original = createPost(1, 100, text = "Original")
        val repost = createPost(2, 100, text = "Check this", copyHistory = listOf(original))
        val posts = listOf(original, repost)

        val allowed = FeedViewModel.filterPosts(posts, FeedFilterConfig(repostsHidden = false), FeedFilter.ALL)
        assertEquals(2, allowed.size)

        val hidden = FeedViewModel.filterPosts(posts, FeedFilterConfig(repostsHidden = true), FeedFilter.ALL)
        assertEquals(1, hidden.size)
        assertEquals(1L, hidden[0].id)
    }

    @Test
    fun testFilterPosts_stopWords() {
        val cleanPost = createPost(1, 100, text = "Hello world")
        val spamPost = createPost(2, 100, text = "Buy crypto now!")
        val posts = listOf(cleanPost, spamPost)

        val filtered = FeedViewModel.filterPosts(
            posts,
            FeedFilterConfig(stopWordsEnabled = true, stopWords = setOf("crypto")),
            FeedFilter.ALL,
        )
        assertEquals(1, filtered.size)
        assertEquals(1L, filtered[0].id)
    }

    @Test
    fun testFilterPosts_stripReposts() {
        val inner = createPost(10, 200, text = "Inside repost")
        val onlyRepost = createPost(1, 100, text = "", copyHistory = listOf(inner))
        val postWithComment = createPost(2, 100, text = "My comment on this", copyHistory = listOf(inner))
        val posts = listOf(onlyRepost, postWithComment)

        val stripped = FeedViewModel.filterPosts(
            posts,
            FeedFilterConfig(stripReposts = true),
            FeedFilter.ALL,
        )
        assertEquals(1, stripped.size)
        assertEquals(2L, stripped[0].id)
        assertTrue(stripped[0].copyHistory == null)
    }
}
