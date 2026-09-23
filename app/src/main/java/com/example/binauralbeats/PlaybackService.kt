package com.example.binauralbeats

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlin.concurrent.thread
import kotlin.math.sin

class PlaybackService : Service() {

    interface VisualizerListener {
        fun onWaveformUpdate(waveform: ShortArray)
    }

    // --- Reproductores ---
    private var audioTrack: AudioTrack? = null
    private var isPlayingFrequency = false
    private var frequencyPlaybackThread: Thread? = null
    private var currentVolume: Float = 1.0f

    // Volúmenes independientes por cada pista ambiental
    private val ambientPlayers = mutableMapOf<Int, MediaPlayer>()
    private val ambientVolumes = mutableMapOf<Int, Float>()

    // Visualizer Listener directo (reemplaza IPC Broadcasts masivos)
    private var visualizerListener: VisualizerListener? = null

    // --- Variables de estado ---
    private var countDownTimer: CountDownTimer? = null
    private var timeRemainingInMillis: Long = 0
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wasPlayingBeforeFocusLoss = false
    private var lastBaseFreq: Double = 0.0
    private var lastBinauralFreq: Double = 0.0
    private var lastFrequencyName: String = ""

    // Temporizador para el fundido de salida
    private var fadeOutTimer: CountDownTimer? = null

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (isPlayingFrequency) {
                    wasPlayingBeforeFocusLoss = true
                    pausePlayback()
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (wasPlayingBeforeFocusLoss) {
                    resumePlayback()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                if (isPlayingFrequency) {
                    stop()
                }
            }
        }
    }

    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    override fun onBind(intent: Intent): IBinder = binder

    fun setVisualizerListener(listener: VisualizerListener?) {
        this.visualizerListener = listener
    }

    companion object {
        private const val CHANNEL_ID = "PlaybackServiceChannel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_TIMER_UPDATE = "com.example.binauralbeats.TIMER_UPDATE"
        const val EXTRA_TIME_REMAINING = "TIME_REMAINING"

        private const val FADE_OUT_DURATION = 5000L // 5 segundos
        private const val FADE_OUT_INTERVAL = 50L // Actualizar volumen cada 50ms
        private const val FRAME_INTERVAL_MS = 25L // ~40 FPS para el visualizer sin sobrecargar la UI
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("Selecciona una frecuencia")
        startForeground(NOTIFICATION_ID, notification)
        return START_NOT_STICKY
    }

    fun playFrequency(baseFreq: Double, binauralFreq: Double, frequencyName: String, durationMillis: Long) {
        if (!requestAudioFocus()) return
        if (isPlayingFrequency) stopFrequencyPlayback()

        fadeOutTimer?.cancel()

        lastBaseFreq = baseFreq
        lastBinauralFreq = binauralFreq
        lastFrequencyName = frequencyName
        timeRemainingInMillis = durationMillis

        isPlayingFrequency = true
        updateNotification("Reproduciendo: $frequencyName")

        if (timeRemainingInMillis > 0) startTimer(timeRemainingInMillis)
        startFrequencyPlaybackThread()
    }

    fun stop() {
        fadeOutTimer?.cancel()
        stopFrequencyPlayback()
        stopAllAmbientSounds()
        abandonAudioFocus()
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun setFrequencyVolume(volume: Float) {
        currentVolume = volume
        audioTrack?.setVolume(currentVolume)
    }

    fun toggleAmbientSound(soundResId: Int) {
        if (ambientPlayers.containsKey(soundResId)) {
            ambientPlayers[soundResId]?.stop()
            ambientPlayers[soundResId]?.release()
            ambientPlayers.remove(soundResId)
        } else {
            try {
                val volume = ambientVolumes[soundResId] ?: 0.5f
                val mediaPlayer = MediaPlayer.create(this, soundResId).apply {
                    isLooping = true
                    setVolume(volume, volume)
                    start()
                }
                ambientPlayers[soundResId] = mediaPlayer
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun isAmbientSoundPlaying(soundResId: Int): Boolean {
        return ambientPlayers.containsKey(soundResId)
    }

    fun setAmbientSoundVolume(soundResId: Int, volume: Float) {
        ambientVolumes[soundResId] = volume
        ambientPlayers[soundResId]?.setVolume(volume, volume)
    }

    fun setAllAmbientVolumes(volume: Float) {
        ambientPlayers.keys.forEach { soundResId ->
            setAmbientSoundVolume(soundResId, volume)
        }
    }

    private fun stopAllAmbientSounds() {
        ambientPlayers.values.forEach { player ->
            player.stop()
            player.release()
        }
        ambientPlayers.clear()
    }

    private fun requestAudioFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        if (ambientPlayers.isEmpty() && !isPlayingFrequency) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(audioFocusChangeListener)
            }
        }
    }

    private fun pausePlayback() {
        if (!isPlayingFrequency) return
        stopFrequencyPlayback()
        updateNotification("Pausado por interrupción")
    }

    private fun resumePlayback() {
        if (isPlayingFrequency) return
        if (!requestAudioFocus()) return
        isPlayingFrequency = true
        updateNotification("Reproduciendo: $lastFrequencyName")
        if (timeRemainingInMillis > 0) startTimer(timeRemainingInMillis)
        startFrequencyPlaybackThread()
    }

    private fun stopFrequencyPlayback() {
        isPlayingFrequency = false
        frequencyPlaybackThread?.join()
        frequencyPlaybackThread = null
        audioTrack = null
        cancelTimer()
        if (ambientPlayers.isEmpty()) {
            abandonAudioFocus()
        }
    }

    private fun startFrequencyPlaybackThread() {
        frequencyPlaybackThread = thread(start = true) {
            val sampleRate = 44100
            val bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                .setBufferSizeInBytes(bufferSize).build()

            audioTrack?.setVolume(currentVolume)
            val buffer = ShortArray(bufferSize)
            val leftFreq = lastBaseFreq
            val rightFreq = lastBaseFreq + lastBinauralFreq
            var angleLeft = 0.0
            var angleRight = 0.0
            val twoPi = 2.0 * Math.PI

            var lastFrameTime = 0L

            audioTrack?.play()
            while (isPlayingFrequency) {
                for (i in 0 until bufferSize step 2) {
                    buffer[i] = (sin(angleLeft) * Short.MAX_VALUE).toInt().toShort()
                    buffer[i + 1] = (sin(angleRight) * Short.MAX_VALUE).toInt().toShort()

                    angleLeft += twoPi * leftFreq / sampleRate
                    if (angleLeft >= twoPi) angleLeft %= twoPi

                    angleRight += twoPi * rightFreq / sampleRate
                    if (angleRight >= twoPi) angleRight %= twoPi
                }
                audioTrack?.write(buffer, 0, bufferSize)

                // Throttled UI notification directly to bound listener
                val now = System.currentTimeMillis()
                if (now - lastFrameTime >= FRAME_INTERVAL_MS) {
                    lastFrameTime = now
                    visualizerListener?.onWaveformUpdate(buffer)
                }
            }
            audioTrack?.stop()
            audioTrack?.release()
        }
    }

    private fun startTimer(durationMillis: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemainingInMillis = millisUntilFinished
                broadcastTimeUpdate(timeRemainingInMillis)
            }
            override fun onFinish() {
                startFadeOut()
            }
        }.start()
    }

    private fun cancelTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        broadcastTimeUpdate(0)
    }

    private fun startFadeOut() {
        val initialFrequencyVolume = currentVolume

        fadeOutTimer = object : CountDownTimer(FADE_OUT_DURATION, FADE_OUT_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                val volumeMultiplier = millisUntilFinished.toFloat() / FADE_OUT_DURATION
                setFrequencyVolume(initialFrequencyVolume * volumeMultiplier)
            }

            override fun onFinish() {
                stop()
            }
        }.start()
    }

    private fun broadcastTimeUpdate(timeRemaining: Long) {
        val intent = Intent(ACTION_TIMER_UPDATE)
        intent.putExtra(EXTRA_TIME_REMAINING, timeRemaining)
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Canal del Servicio de Reproducción", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Binaural Beats")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(contentText))
    }

    override fun onDestroy() {
        super.onDestroy()
        stopFrequencyPlayback()
        stopAllAmbientSounds()
        abandonAudioFocus()
        visualizerListener = null
    }
}
