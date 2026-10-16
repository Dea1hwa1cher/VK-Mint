package vk.mint.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OAuthTokenUtilsTest {

    @Test
    fun testContainsOAuthToken() {
        assertTrue(OAuthTokenUtils.containsOAuthToken("https://oauth.vk.com/blank.html#access_token=vk1.a.1234567890&user_id=12345"))
        assertTrue(OAuthTokenUtils.containsOAuthToken("access_token=vk1.a.1234567890&user_id=12345"))
        assertTrue(OAuthTokenUtils.containsOAuthToken("https://oauth.vk.com/blank.html%23access_token%3Dvk1.a.1234567890%26user_id%3D12345"))
        assertTrue(OAuthTokenUtils.containsOAuthToken("Some text before https://oauth.vk.com/blank.html#access_token=vk1.a.1234567890&user_id=12345 and text after"))

        assertFalse(OAuthTokenUtils.containsOAuthToken("https://oauth.vk.com/blank.html#error=access_denied"))
        assertFalse(OAuthTokenUtils.containsOAuthToken("access_token=vk1.a.1234567890"))
        assertFalse(OAuthTokenUtils.containsOAuthToken("user_id=12345"))
        assertFalse(OAuthTokenUtils.containsOAuthToken(""))
    }

    @Test
    fun testParseStandardFragment() {
        val url = "https://oauth.vk.com/blank.html#access_token=vk1.a.ABCDEF1234567890_xyz&user_id=987654321&expires_in=0"
        val parsed = OAuthTokenUtils.parse(url)
        assertNotNull(parsed)
        assertEquals("vk1.a.ABCDEF1234567890_xyz", parsed!!.accessToken)
        assertEquals(987654321L, parsed.userId)
        assertEquals(0L, parsed.expiresIn)
    }

    @Test
    fun testParseWithTitleAndExtraText() {
        val shareText = "OAuth Blank\nhttps://oauth.vk.com/blank.html#access_token=vk1.a.TOKEN_VAL_12345&user_id=555123&expires_in=86400"
        val parsed = OAuthTokenUtils.parse(shareText)
        assertNotNull(parsed)
        assertEquals("vk1.a.TOKEN_VAL_12345", parsed!!.accessToken)
        assertEquals(555123L, parsed.userId)
        assertEquals(86400L, parsed.expiresIn)
    }

    @Test
    fun testParseUrlEncodedFragment() {
        val encodedUrl = "https://oauth.vk.com/blank.html%23access_token=vk1.a.TOKEN_VAL_12345&user_id=444&expires_in=0"
        val parsed = OAuthTokenUtils.parse(encodedUrl)
        assertNotNull(parsed)
        assertEquals("vk1.a.TOKEN_VAL_12345", parsed!!.accessToken)
        assertEquals(444L, parsed.userId)
    }

    @Test
    fun testParseUrlEncodedParameters() {
        val encodedParams = "https://oauth.vk.com/blank.html#access_token=vk1.a.TOKEN%2B12345%3D%3D&user_id=777&expires_in=0"
        val parsed = OAuthTokenUtils.parse(encodedParams)
        assertNotNull(parsed)
        assertEquals("vk1.a.TOKEN+12345==", parsed!!.accessToken)
        assertEquals(777L, parsed.userId)
    }

    @Test
    fun testParseRawParameters() {
        val raw = "access_token=vk1.a.RAW_TOKEN_99999&user_id=123"
        val parsed = OAuthTokenUtils.parse(raw)
        assertNotNull(parsed)
        assertEquals("vk1.a.RAW_TOKEN_99999", parsed!!.accessToken)
        assertEquals(123L, parsed.userId)
    }

    @Test
    fun testParseQueryString() {
        val queryUrl = "https://oauth.vk.com/blank.html?access_token=vk1.a.QUERY_TOKEN_888&user_id=8888"
        val parsed = OAuthTokenUtils.parse(queryUrl)
        assertNotNull(parsed)
        assertEquals("vk1.a.QUERY_TOKEN_888", parsed!!.accessToken)
        assertEquals(8888L, parsed.userId)
    }

    @Test
    fun testParseTrailingWhitespaceAndNewlines() {
        val text = "https://oauth.vk.com/blank.html#access_token=vk1.a.TRAILING_TOKEN&user_id=999\r\n\n  "
        val parsed = OAuthTokenUtils.parse(text)
        assertNotNull(parsed)
        assertEquals("vk1.a.TRAILING_TOKEN", parsed!!.accessToken)
        assertEquals(999L, parsed.userId)
    }

    @Test
    fun testParseInvalidInputs() {
        assertNull(OAuthTokenUtils.parse("https://oauth.vk.com/blank.html#error=access_denied&error_description=User+denied"))
        assertNull(OAuthTokenUtils.parse("access_token=short&user_id=123"))
        assertNull(OAuthTokenUtils.parse("access_token=vk1.a.VALID_LEN_BUT_NO_USER_ID"))
        assertNull(OAuthTokenUtils.parse(""))
    }

    @Test
    fun testMaskTokenPrefix() {
        assertEquals("vk1.a.AB…_xyz", OAuthTokenUtils.maskTokenPrefix("vk1.a.ABCDEF1234567890_xyz"))
        assertEquals("***", OAuthTokenUtils.maskTokenPrefix("short"))
    }
}
