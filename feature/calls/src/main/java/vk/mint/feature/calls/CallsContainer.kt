package vk.mint.feature.calls

import android.app.Application
import vk.mint.contracts.AppContainer
import vk.mint.contracts.CallStarter
import vk.mint.contracts.Capability
import vk.mint.contracts.NavEntry
import vk.mint.contracts.PermissionNeeds
import vk.mint.contracts.SettingsSection
import vk.mint.util.AppLog

/**
 * #ARCH-CONTAINERS (Этап 1.3): контейнер-пионер «Звонки» — первый :feature-контейнер.
 *
 * Публикует 4 capability (реестр → хост):
 *  1. [CallsNavEntry]         — кнопка «Звонки» в боковой панели (route "calls_history" —
 *     СОВПАДАЕТ с текущим хардкодом drawer: Screen.CallsHistory в ui/navigation/Screen.kt);
 *  2. [CallsSettingsSection]  — вкладка настроек «Звонки» (route "settings_calls" — НОВЫЙ
 *     стабильный маршрут: текущий SettingsScreen переключает вкладки приватным enum'ом,
 *     маршрутов у него нет; хост замапит route→контент на Этапе 1.4);
 *  3. [CallsPermissions]      — RECORD_AUDIO (голос), CAMERA (фаза 2, §2.2 плана),
 *     BLUETOOTH_CONNECT (BT-гарнитура, API 31+);
 *  4. [CallsStarterImpl]      — запуск звонка из ядра; хук хоста приходит через
 *     конструктор контейнера при регистрации в VkMintApp (ноль :app-типов в контракте).
 *
 * Движок звонков (WebRtcEngine/VideoTextureRenderer, пакет vk.mint.media) и модели
 * (vk.mint.data.model.CallModels) живут в этом же модуле — пакеты сохранены, :app
 * продолжает видеть их через зависимость (поведение не изменено).
 *
 * На Этапе 1.3 хост ещё рендерит панель/настройки хардкодом: capability присутствуют
 * в реестре БЕЗ видимого эффекта (потребление реестра — Этап 1.4). Это осознанно:
 * добавление контейнера не должно менять поведение приложения.
 */
class CallsContainer(
    /**
     * Хост-хук запуска звонка: (peerId, video) -> успех. Хост передаёт его при
     * регистрации (VkMintApp), т.к. реальный старт = навигация на экран звонка
     * (NavController живёт в :app). Контейнер и контракт :app-типов не знают.
     */
    private val startCallHook: (peerId: Long, video: Boolean) -> Boolean,
) : AppContainer {

    override val id: String = "calls"

    override fun capabilities(): List<Capability> = listOf(
        CallsNavEntry,
        CallsSettingsSection,
        CallsPermissions,
        CallsStarterImpl(startCallHook),
    )

    override fun init(app: Application) {
        // Минимум на Этапе 1.3: движок/сигналинг создаются лениво CallScreen'ом
        // (:app) как раньше — контейнер ничего не стартует (поведение прежнее).
        AppLog.i("CallsContainer", "init: контейнер звонков зарегистрирован (id=$id)")
    }

    override fun release() {
        // Идемпотентный no-op: ресурсов контейнер на 1.3 не держит
        // (WebRtcEngine/signaling принадлежат экрану звонка и живут своим циклом).
        AppLog.i("CallsContainer", "release: no-op (идемпотентно)")
    }
}

/** Кнопка «Звонки» в drawer. route = текущий хардкод хоста (Screen.CallsHistory). */
private object CallsNavEntry : NavEntry {
    override val title: String = "Звонки"
    override val iconKey: String = "calls"
    /** После ядерных социальных разделов хоста; хост сортирует по order (1.4). */
    override val order: Int = 10
    override val route: String = "calls_history"
}

/** Вкладка настроек «Звонки». route — новый стабильный ключ (см. KDoc контейнера). */
private object CallsSettingsSection : SettingsSection {
    override val title: String = "Звонки"
    /** Ядерные вкладки хоста — без order; 90 ставит секцию в конец списка. */
    override val order: Int = 90
    override val route: String = "settings_calls"
}

/** Права звонков: голос — сразу, камера — фаза 2 (план §2.2), BT — гарнитура. */
private object CallsPermissions : PermissionNeeds {
    override val permissions: List<String> = listOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.BLUETOOTH_CONNECT,
    )
}

/**
 * Реализация [CallStarter]: делегирует в хук хоста, полученный через конструктор
 * контейнера (VkMintApp ставит pending-событие, VkMintNavHost открывает экран звонка —
 * тот же паттерн, что pendingIncomingCallPayload).
 */
private class CallsStarterImpl(
    private val startCallHook: (peerId: Long, video: Boolean) -> Boolean,
) : CallStarter {
    override fun startCall(peerId: Long, video: Boolean): Boolean = try {
        startCallHook(peerId, video)
    } catch (e: Throwable) {
        AppLog.e("CallsStarter", "startCall failed: ${e.message}")
        false
    }
}
