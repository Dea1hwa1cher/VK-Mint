package vk.mint.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserAliasResolverTest {

    @Before
    fun setUp() {
        UserAliasResolver.update(emptyMap())
    }

    @Test
    fun testEmptyAliases() {
        assertNull(UserAliasResolver.getAlias(123L))
        assertFalse(UserAliasResolver.hasAlias(123L))
        assertEquals("Павел Дуров", UserAliasResolver.resolveName(123L, "Павел Дуров"))
    }

    @Test
    fun testUpdateAndResolve() {
        UserAliasResolver.update(mapOf(123L to "Дуров", 456L to "Брат"))

        assertEquals("Дуров", UserAliasResolver.getAlias(123L))
        assertEquals("Брат", UserAliasResolver.getAlias(456L))
        assertTrue(UserAliasResolver.hasAlias(123L))
        assertTrue(UserAliasResolver.hasAlias(456L))
        assertFalse(UserAliasResolver.hasAlias(789L))

        assertEquals("Дуров", UserAliasResolver.resolveName(123L, "Павел"))
        assertEquals("Иван", UserAliasResolver.resolveName(789L, "Иван"))
    }

    @Test
    fun testBlankAliasFallback() {
        UserAliasResolver.update(mapOf(123L to "   "))
        assertEquals("Павел", UserAliasResolver.resolveName(123L, "Павел"))
    }
}
