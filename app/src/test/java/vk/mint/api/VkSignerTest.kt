package vk.mint.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class VkSignerTest {

    @Test
    fun testRequiresSig() {
        assertTrue(VkSigner.requiresSig("messages.getConversations"))
        assertTrue(VkSigner.requiresSig("messages.send"))
        assertTrue(VkSigner.requiresSig("messages.customAction"))
        assertTrue(VkSigner.requiresSig("audio.get"))
        assertTrue(VkSigner.requiresSig("execute"))

        assertFalse(VkSigner.requiresSig("users.get"))
        assertFalse(VkSigner.requiresSig("wall.get"))
        assertFalse(VkSigner.requiresSig("friends.get"))
        assertFalse(VkSigner.requiresSig("newsfeed.get"))
    }

    @Test
    fun testIsWebToken() {
        assertTrue(VkSigner.isWebToken("vk1.a.AbCdEf1234567890_xyz"))
        assertFalse(VkSigner.isWebToken("9876543210fedcba"))
        assertFalse(VkSigner.isWebToken(""))
        assertFalse(VkSigner.isWebToken(null))
    }

    @Test
    fun testShouldSign() {
        val webToken = "vk1.a.some_long_web_token"
        val oldToken = "old_direct_auth_token"
        val secret = "user_secret_abc"

        // Web tokens never require signature
        assertFalse(VkSigner.shouldSign("messages.send", webToken, secret))

        // Normal token without secret cannot be signed
        assertFalse(VkSigner.shouldSign("messages.send", oldToken, null))
        assertFalse(VkSigner.shouldSign("messages.send", oldToken, ""))

        // Normal token with secret on signed method -> true
        assertTrue(VkSigner.shouldSign("messages.send", oldToken, secret))

        // Public method does not require signature even with secret
        assertFalse(VkSigner.shouldSign("users.get", oldToken, secret))
    }

    @Test
    fun testSignFormula() {
        val method = "messages.getHistory"
        val params = linkedMapOf(
            "peer_id" to "12345",
            "count" to "20",
            "v" to "5.199"
        )
        val secret = "test_secret_key"

        val sig = VkSigner.sign(method, params, secret)
        assertEquals(32, sig.length)
        assertTrue(sig.all { it in "0123456789abcdef" })

        // Check against direct MD5 calculation of query
        val expectedSource = "/method/messages.getHistory?peer_id=12345&count=20&v=5.199test_secret_key"
        val md5Bytes = MessageDigest.getInstance("MD5").digest(expectedSource.toByteArray(Charsets.UTF_8))
        val expectedHex = md5Bytes.joinToString("") { "%02x".format(it) }

        assertEquals(expectedHex, sig)
    }
}
