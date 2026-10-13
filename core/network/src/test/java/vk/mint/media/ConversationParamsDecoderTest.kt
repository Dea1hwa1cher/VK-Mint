package vk.mint.media

import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ConversationParamsDecoderTest {

    @Test
    fun testDecodeInvalidPayload() {
        assertNull(ConversationParamsDecoder.decode(""))
        assertNull(ConversationParamsDecoder.decode("invalid"))
        assertNull(ConversationParamsDecoder.decode("abc:def"))
        assertNull(ConversationParamsDecoder.decode("100:"))
    }

    @Test
    fun testDecodeParamsJson() {
        val json = JsonObject().apply {
            addProperty("endpoint", "wss://calls.vk.com/signaling")
            addProperty("token", "secret_call_token_123")
            addProperty("clientType", "android_vk_mint")
            addProperty("wtEndpoint", "https://wt.vk.com")
        }

        val params = ConversationParamsDecoder.decodeParamsJson(json)
        assertNotNull(params)
        assertEquals("wss://calls.vk.com/signaling", params?.endpoint)
        assertEquals("secret_call_token_123", params?.token)
        assertEquals("android_vk_mint", params?.clientType)
        assertEquals("https://wt.vk.com", params?.wtEndpoint)
        assertNull(params?.turnServer)
        assertNull(params?.stunServer)
    }

    @Test
    fun testLz4LiteralDecompress() {
        // LZ4 token for 5 literal bytes: (5 << 4) = 0x50 = 80
        val literals = "Hello".toByteArray(Charsets.UTF_8)
        val src = byteArrayOf(0x50.toByte()) + literals
        val out = ConversationParamsDecoder.lz4Decompress(src, 5)
        assertEquals("Hello", String(out, Charsets.UTF_8))
    }
}
