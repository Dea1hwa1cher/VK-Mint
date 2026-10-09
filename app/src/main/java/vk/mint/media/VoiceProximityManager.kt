package vk.mint.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import vk.mint.util.AppLog
import vk.mint.VkMintApp

/**
 * #VOICE-PROXIMITY: Управление датчиком приближения при воспроизведении голосовых/аудиосообщений.
 *
 * Логика:
 * 1. При старте воспроизведения регистрируется SensorEventListener для Sensor.TYPE_PROXIMITY
 *    (если в настройках не включено отключение датчика: voiceDisableProximity == false).
 * 2. При поднесении устройства к уху (isNear == true):
 *    - Экран блокируется/гасится через PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK.
 *    - ExoPlayer переключается на C.AUDIO_CONTENT_TYPE_SPEECH / C.USAGE_VOICE_COMMUNICATION.
 *    - AudioManager переключается на разговорный динамик (earpiece).
 * 3. При отдалении от уха (isNear == false):
 *    - WakeLock отпускается (экран включается).
 *    - Аудио возвращается на основной медиа-динамик (C.AUDIO_CONTENT_TYPE_MUSIC / C.USAGE_MEDIA).
 *    - AudioManager возвращается в AudioManager.MODE_NORMAL.
 * 4. Если подключена гарнитура (проводная или Bluetooth), переключение на разговорный динамик
 *    и гашение экрана блокируются (звук остаётся в наушниках).
 * 5. При включённой настройке «Отключить датчик приближения» (voiceDisableProximity == true)
 *    датчик не регистрируется вовсе, и звук всегда идёт в обычный динамик.
 */
class VoiceProximityManager(context: Context) : SensorEventListener {

    private val appContext: Context = context.applicationContext
    private val sensorManager: SensorManager? = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val proximitySensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val powerManager: PowerManager? = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val audioManager: AudioManager? = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var proximityWakeLock: PowerManager.WakeLock? = null
    private var headsetReceiver: BroadcastReceiver? = null
    private var activePlayer: ExoPlayer? = null

    private var isListening: Boolean = false
    private var isNear: Boolean = false
    private var isHeadsetConnected: Boolean = false

    init {
        try {
            val pm = powerManager
            if (pm != null && pm.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                proximityWakeLock = pm.newWakeLock(
                    PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                    "vkmint:voice_proximity"
                ).apply {
                    setReferenceCounted(false)
                }
            }
        } catch (e: Exception) {
            AppLog.w(TAG, "Init proximity wake lock error: ${e.message}")
        }
    }

    /**
     * Запуск отслеживания приближения для текущего [player].
     */
    fun start(player: ExoPlayer) {
        val disabled = isProximityDisabledInPrefs()
        if (disabled) {
            stop()
            return
        }

        if (proximitySensor == null) {
            return
        }

        activePlayer = player

        if (!isListening) {
            isHeadsetConnected = isHeadsetOrBtOutputActive()
            isNear = false

            registerHeadsetReceiver()

            try {
                sensorManager?.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
                isListening = true
                AppLog.d(TAG, "Proximity listener registered")
            } catch (e: Exception) {
                AppLog.w(TAG, "registerListener failed: ${e.message}")
            }
        }
    }

    /**
     * Остановка отслеживания и сброс аудио-роутинга на громкую связь.
     */
    fun stop() {
        if (isNear) {
            disengageNearMode()
        }

        if (isListening) {
            try {
                sensorManager?.unregisterListener(this)
            } catch (e: Exception) {
                AppLog.w(TAG, "unregisterListener failed: ${e.message}")
            }
            unregisterHeadsetReceiver()
            isListening = false
            AppLog.d(TAG, "Proximity listener unregistered")
        }

        // Страховочный сброс локов и AudioManager
        releaseWakeLock()
        resetAudioRouting()

        activePlayer = null
        isNear = false
        isHeadsetConnected = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_PROXIMITY) return

        if (isProximityDisabledInPrefs()) {
            stop()
            return
        }

        if (isHeadsetOrBtOutputActive()) {
            if (isNear) {
                disengageNearMode()
            }
            return
        }

        val maxRange = event.sensor.maximumRange
        val distance = event.values.getOrNull(0) ?: maxRange
        val newNear = if (maxRange <= 1f) {
            distance < 0.5f || distance == 0f
        } else {
            distance < minOf(maxRange, 5.0f)
        }

        if (newNear != isNear) {
            isNear = newNear
            if (newNear) {
                engageNearMode()
            } else {
                disengageNearMode()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    private fun engageNearMode() {
        AppLog.d(TAG, "Proximity NEAR -> switching to earpiece & screen off")

        // 1. Экран гасится
        acquireWakeLock()

        // 2. Аудио-роутинг ExoPlayer
        try {
            activePlayer?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_VOICE_COMMUNICATION)
                    .build(),
                false
            )
        } catch (e: Exception) {
            AppLog.w(TAG, "setAudioAttributes SPEECH failed: ${e.message}")
        }

        // 3. Маршрутизация на разговорный динамик (earpiece)
        try {
            val am = audioManager
            if (am != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val earpiece = am.availableCommunicationDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                    }
                    if (earpiece != null) {
                        am.setCommunicationDevice(earpiece)
                    } else {
                        am.mode = AudioManager.MODE_IN_COMMUNICATION
                    }
                } else {
                    @Suppress("DEPRECATION")
                    am.mode = AudioManager.MODE_IN_COMMUNICATION
                    @Suppress("DEPRECATION")
                    am.isSpeakerphoneOn = false
                }
            }
        } catch (e: Exception) {
            AppLog.w(TAG, "AudioManager earpiece route failed: ${e.message}")
        }
    }

    private fun disengageNearMode() {
        AppLog.d(TAG, "Proximity FAR -> switching back to loudspeaker & screen on")

        // 1. Экран включается
        releaseWakeLock()

        // 2. Сброс маршрутизации AudioManager
        resetAudioRouting()

        // 3. Аудио-роутинг ExoPlayer на стандартный медиа
        try {
            activePlayer?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
        } catch (e: Exception) {
            AppLog.w(TAG, "setAudioAttributes MUSIC failed: ${e.message}")
        }
    }

    private fun acquireWakeLock() {
        try {
            if (proximityWakeLock?.isHeld == false) {
                proximityWakeLock?.acquire(10 * 60 * 1000L) // 10 минут макс
            }
        } catch (e: Exception) {
            AppLog.w(TAG, "acquireWakeLock failed: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (proximityWakeLock?.isHeld == true) {
                proximityWakeLock?.release()
            }
        } catch (e: Exception) {
            AppLog.w(TAG, "releaseWakeLock failed: ${e.message}")
        }
    }

    private fun resetAudioRouting() {
        try {
            val am = audioManager
            if (am != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    am.clearCommunicationDevice()
                }
                am.mode = AudioManager.MODE_NORMAL
                @Suppress("DEPRECATION")
                am.isSpeakerphoneOn = true
            }
        } catch (e: Exception) {
            AppLog.w(TAG, "resetAudioRouting failed: ${e.message}")
        }
    }

    private fun registerHeadsetReceiver() {
        if (headsetReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_HEADSET_PLUG) {
                    val state = intent.getIntExtra("state", -1)
                    if (state == 1) {
                        isHeadsetConnected = true
                        if (isNear) disengageNearMode()
                    } else if (state == 0) {
                        isHeadsetConnected = isHeadsetOrBtOutputActive()
                    }
                }
            }
        }
        headsetReceiver = receiver
        val filter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                appContext.registerReceiver(receiver, filter)
            }
        } catch (e: Exception) {
            AppLog.w(TAG, "registerHeadsetReceiver failed: ${e.message}")
        }
    }

    private fun unregisterHeadsetReceiver() {
        val r = headsetReceiver ?: return
        headsetReceiver = null
        try {
            appContext.unregisterReceiver(r)
        } catch (e: Exception) {
            AppLog.w(TAG, "unregisterHeadsetReceiver failed: ${e.message}")
        }
    }

    private fun isHeadsetOrBtOutputActive(): Boolean {
        val am = audioManager ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                devices.any { device ->
                    when (device.type) {
                        AudioDeviceInfo.TYPE_WIRED_HEADSET,
                        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                        AudioDeviceInfo.TYPE_USB_HEADSET,
                        AudioDeviceInfo.TYPE_USB_DEVICE,
                        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                        AudioDeviceInfo.TYPE_HEARING_AID,
                        AudioDeviceInfo.TYPE_BLE_HEADSET,
                        AudioDeviceInfo.TYPE_BLE_SPEAKER -> true
                        else -> false
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                am.isWiredHeadsetOn || am.isBluetoothA2dpOn || am.isBluetoothScoOn
            }
        } catch (e: Exception) {
            AppLog.w(TAG, "isHeadsetOrBtOutputActive error: ${e.message}")
            false
        }
    }

    private fun isProximityDisabledInPrefs(): Boolean {
        return VkMintApp.getOrNull()?.prefsSnapshot?.voiceDisableProximity ?: false
    }

    companion object {
        private const val TAG = "VoiceProximityManager"
    }
}
