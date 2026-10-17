package vk.mint.auth

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import vk.mint.auth.exchange.AuthCodeParser
import vk.mint.auth.exchange.VkAuthApiException

/**
 * Разбор ответов VK в auth-code флоу (шаги 0..5).
 *
 * Ключевой случай, который закрываем регрессией: VK на отказ отвечает
 * HTTP 200 с конвертом `{"error":{"error_code":…, …}}`. Разборщик обязан
 * такой ответ считать ошибкой. До появления [AuthCodeParser] ошибка глоталась:
 * приложение рапортовало «код принят», уводило на пароль и получало
 * `9;Flood control` на сессии, которую никто не подтверждал.
 */
class AuthCodeParserTest {

    private fun json(raw: String) = JsonParser.parseString(raw).asJsonObject

    // ── checkOtp: конверт ошибок ───────────────────────────────────────────

    @Test
    fun `checkOtp бросает ошибку на конверте VK, а не возвращает пустой успех`() {
        val body = json(
            """{"error":{"error_code":1110,"error_subcode":1122,
               "error_msg":"Incorrect code: invalid code for phone"}}""",
        )

        val e = runCatching { AuthCodeParser.otpCheck(body) }.exceptionOrNull()

        assertTrue("ожидалась VkAuthApiException, получено: $e", e is VkAuthApiException)
        e as VkAuthApiException
        assertEquals(1110, e.code)
        assertEquals(1122, e.subcode)
        assertTrue(e.message.orEmpty().contains("Incorrect code"))
    }

    @Test
    fun `checkOpt без sid считается отказом, а не принятым кодом`() {
        // Так выглядит «успех» без sid — раньше это проходило как валидный код
        // с пустым sid, и следующий вызов уходил с мусорным sid.
        val body = json("""{"response":{"can_skip_password":false}}""")

        val e = runCatching { AuthCodeParser.otpCheck(body) }.exceptionOrNull()

        assertTrue(e is VkAuthApiException)
    }

    @Test
    fun `checkOtp разбирает настоящий успех`() {
        val body = json(
            """{"response":{"sid":"81dcfb12ab","can_skip_password":false,
               "profile_exist":true,"auth_hash":"abc123",
               "profile":{"photo_200":"https://vk.com/photo.jpg"}}}""",
        )

        val r = AuthCodeParser.otpCheck(body)

        assertEquals("81dcfb12ab", r.sid)
        assertEquals("abc123", r.authHash)
        assertTrue(r.profileExist)
        assertFalse(r.canSkipPassword)
        assertEquals("https://vk.com/photo.jpg", r.photo200)
    }

    // ── validateAccount ─────────────────────────────────────────────────────

    @Test
    fun `validateAccount разбирает next_step`() {
        val body = json(
            """{"response":{"is_phone":true,"flow_name":"need_password_and_validation",
               "flow_names":["password"],"sid":"loginvalidate_6cf2a16",
               "next_step":{"verification_method":"email",
                            "has_another_verification_methods":true},
               "can_skip_password":false}}""",
        )

        val r = AuthCodeParser.validateAccount(body)!!

        assertEquals("need_password_and_validation", r.flowName)
        assertEquals("loginvalidate_6cf2a16", r.sid)
        assertEquals("email", r.nextStepMethod)
        assertTrue(r.hasAnotherMethods)
        // Маршрут решает AuthActivity.routeOf: next=email значит «код», а не пароль.
        assertFalse(r.nextStepMethod == null || r.nextStepMethod == "password")
    }

    @Test
    fun `validateAccount бросает на конверте ошибки`() {
        val body = json("""{"error":{"error_code":5,"error_msg":"User authorization failed"}}""")

        val e = runCatching { AuthCodeParser.validateAccount(body) }.exceptionOrNull()

        assertTrue(e is VkAuthApiException)
        assertEquals(5, (e as VkAuthApiException).code)
    }

    // ── шаг 5: ошибку НЕ бросаем, а возвращаем данными ────────────────────

    @Test
    fun `authCodeToken отдаёт отказ данными, а не исключением`() {
        val body = json("""{"error":"9;Flood control","error_description":"Пароль указан неверно"}""")

        val r = AuthCodeParser.authCodeToken(body)

        assertFalse(r.isSuccess)
        assertEquals("9;Flood control", r.error)
        assertEquals("Пароль указан неверно", r.errorDescription)
    }

    @Test
    fun `authCodeToken разбирает успешный токен`() {
        val body = json("""{"access_token":"vk1.a.TOKEN","user_id":12345,"expires_in":0}""")

        val r = AuthCodeParser.authCodeToken(body)

        assertTrue(r.isSuccess)
        assertEquals("vk1.a.TOKEN", r.accessToken)
        assertEquals(12345L, r.userId)
    }

    @Test
    fun `authCodeToken не принимает конверт error за успех`() {
        val body = json("""{"error":{"error_code":9,"error_msg":"Flood control"}}""")

        val r = AuthCodeParser.authCodeToken(body)

        assertFalse(r.isSuccess)
        assertEquals("Flood control", r.errorDescription)
    }

    // ── getVerificationMethods ──────────────────────────────────────────────

    @Test
    fun `verificationMethods разбирает список способов по приоритету`() {
        val body = json(
            """{"response":{"methods":[
                 {"name":"email","priority":5,"info":"a***@mail.ru"},
                 {"name":"sms","priority":1,"info":"7***06"},
                 {"name":"push","priority":3}]}}""",
        )

        val r = AuthCodeParser.verificationMethods(body)

        // Порядок — по возрастанию приоритета, как его ждёт UI.
        assertEquals(listOf("sms", "push", "email"), r.map { it.name })
        assertEquals("7***06", r.first().info)
    }

    /**
     * Регрессия: `unwrap` уже отдаёт содержимое `response`. Повторный поиск
     * конверта возвращал пустой список, флоу зависал на «Проверяем аккаунт»
     * (воспроизведено на устройстве 18.10 00:55).
     */
    @Test
    fun `verificationMethods не теряет список из-за двойного unwrap`() {
        val body = json("""{"response":{"methods":[{"name":"push","priority":1}]}}""")

        assertEquals(1, AuthCodeParser.verificationMethods(body).size)
    }

    @Test
    fun `verificationMethods бросает на конверте ошибки, а не возвращает пусто`() {
        val body = json("""{"error":{"error_code":5,"error_msg":"User authorization failed"}}""")

        val e = runCatching { AuthCodeParser.verificationMethods(body) }.exceptionOrNull()

        assertTrue("ошибка VK не должна выглядеть как «способов нет»", e is VkAuthApiException)
    }

    @Test
    fun `verificationMethods возвращает пусто если methods нет в ответе`() {
        assertTrue(AuthCodeParser.verificationMethods(json("""{"response":{}}""")).isEmpty())
    }

    @Test
    fun `authCodeToken распознаёт password_bruteforce_attempt`() {
        val body = json(
            """{"error":"9;Flood control",
               "error_description":"Произведено слишком много попыток входа по паролю",
               "error_type":"password_bruteforce_attempt","view":"alert"}""",
        )

        val r = AuthCodeParser.authCodeToken(body)

        assertFalse(r.isSuccess)
        assertTrue(r.isPasswordBruteforce)
    }

    @Test
    fun `обычный flood без error_type не считается парольным`() {
        val body = json("""{"error":"9;Flood control","error_description":"Подождите"}""")

        assertFalse(AuthCodeParser.authCodeToken(body).isPasswordBruteforce)
    }

    // ── session: две формы sid ──────────────────────────────────────────────

    @Test
    fun `после checkOtp используется короткая форма sid`() {
        val s = vk.mint.auth.exchange.AuthCodeSession(
            sidRaw = "loginvalidate_6cf2a16",
            sidAfterCheck = "6cf2a16",
        )

        assertEquals("6cf2a16", s.sidForNextCall())
    }

    @Test
    fun `до checkOtp используется полная форма sid`() {
        val s = vk.mint.auth.exchange.AuthCodeSession(sidRaw = "loginvalidate_6cf2a16")

        assertEquals("loginvalidate_6cf2a16", s.sidForNextCall())
    }

    @Test
    fun `normalizeFull добавляет префикс к короткому sid`() {
        assertEquals(
            "loginvalidate_6cf2a16",
            vk.mint.auth.exchange.AuthCodeSession.normalizeFull("6cf2a16"),
        )
        assertEquals(
            "loginvalidate_6cf2a16",
            vk.mint.auth.exchange.AuthCodeSession.normalizeFull("loginvalidate_6cf2a16"),
        )
    }
}