package com.example.binauralbeats

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.binauralbeats.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // --- Variables para el Servicio ---
    private var playbackService: PlaybackService? = null
    private var isBound = false
    private val activeAmbientSounds = mutableSetOf<Int>()


    private val appStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                PlaybackService.ACTION_TIMER_UPDATE -> {
                    val timeRemaining = intent.getLongExtra(PlaybackService.EXTRA_TIME_REMAINING, 0)
                    updateCountdownUI(timeRemaining)
                }
                PlaybackService.ACTION_WAVEFORM_UPDATE -> {
                    val waveform = intent.getShortArrayExtra(PlaybackService.EXTRA_WAVEFORM)
                    if (waveform != null) {
                        binding.visualizerView.updateVisualizer(waveform)
                    }
                }
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PlaybackService.LocalBinder
            playbackService = binder.getService()
            isBound = true
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            playbackService = null
        }
    }

    // --- Datos de la App ---
    private val frequencies = mapOf(
        "Ondas Binaurales" to mapOf("Delta (1-4 Hz)" to Pair(100.0, 2.0), "Theta (4-8 Hz)" to Pair(120.0, 6.0), "Alpha (8-13 Hz)" to Pair(150.0, 10.0), "Beta (13-30 Hz)" to Pair(180.0, 20.0), "Gamma (30-100 Hz)" to Pair(200.0, 40.0)),
        "Frecuencias Solfeggio" to mapOf("174 Hz" to Pair(174.0, 0.0), "285 Hz" to Pair(285.0, 0.0), "396 Hz" to Pair(396.0, 0.0), "417 Hz" to Pair(417.0, 0.0), "528 Hz" to Pair(528.0, 0.0), "639 Hz" to Pair(639.0, 0.0))
    )
    private val timerOptions = mapOf("Sin límite" to 0L, "5 minutos" to 5L, "10 minutos" to 10L, "30 minutos" to 30L, "1 hora" to 60L)

    // NUEVO: Mapa con las descripciones de cada frecuencia
    private val frequencyDescriptions = mapOf(
        "Delta (1-4 Hz)" to "Asociadas con el sueño profundo sin sueños y la sanación. Promueven la regeneración del cuerpo.",
        "Theta (4-8 Hz)" to "Vinculadas a la meditación profunda, la creatividad y el sueño REM. Ayudan a mejorar la intuición.",
        "Alpha (8-13 Hz)" to "Presentes en estados de relajación consciente y calma. Ideales para reducir el estrés y fomentar el aprendizaje.",
        "Beta (13-30 Hz)" to "Relacionadas con el estado de alerta, la concentración, la lógica y el pensamiento crítico. Útiles para trabajar o estudiar.",
        "Gamma (30-100 Hz)" to "Asociadas con un alto nivel de procesamiento cognitivo, la memoria y la percepción. Ayudan a la resolución de problemas.",
        "174 Hz" to "Considerada un anestésico natural. Ayuda a aliviar el dolor físico y energético, proporcionando una sensación de seguridad a los órganos.",
        "285 Hz" to "Frecuencia que ayuda a la sanación de tejidos y órganos, devolviéndolos a su estado original. Influye en los campos de energía.",
        "396 Hz" to "Libera del miedo y la culpa. Ayuda a eliminar bloqueos subconscientes, creencias negativas y traumas.",
        "417 Hz" to "Facilita el cambio y la transmutación. Limpia experiencias traumáticas y deshace situaciones negativas.",
        "528 Hz" to "Conocida como la '''frecuencia del amor''' o de los milagros. Se asocia con la reparación del ADN, la claridad mental y la paz interior.",
        "639 Hz" to "Mejora la conexión, las relaciones y la comunicación. Fomenta la comprensión, la tolerancia y el amor."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupMenus()
        setupControls()
    }

    override fun onStart() {
        super.onStart()
        Intent(this, PlaybackService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
        val intentFilter = IntentFilter().apply {
            addAction(PlaybackService.ACTION_TIMER_UPDATE)
            addAction(PlaybackService.ACTION_WAVEFORM_UPDATE)
        }

        ContextCompat.registerReceiver(this, appStateReceiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        unregisterReceiver(appStateReceiver)
    }

    private fun setupMenus() {
        binding.categoryAutoCompleteTextView.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, frequencies.keys.toTypedArray()))
        binding.categoryAutoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
            updateFrequencyMenu(parent.getItemAtPosition(position) as String)
            binding.frequencyAutoCompleteTextView.setText("", false)
        }
        binding.timerAutoCompleteTextView.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, timerOptions.keys.toTypedArray()))
        binding.timerAutoCompleteTextView.setText(timerOptions.keys.first(), false)
    }

    private fun updateFrequencyMenu(category: String) {
        val frequencyNames = frequencies[category]?.keys?.toTypedArray() ?: return
        binding.frequencyAutoCompleteTextView.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, frequencyNames))
    }

    private fun setupControls() {
        binding.playButton.setOnClickListener {
            val category = binding.categoryAutoCompleteTextView.text.toString()
            val frequencyName = binding.frequencyAutoCompleteTextView.text.toString()
            val timerSelection = binding.timerAutoCompleteTextView.text.toString()

            if (category.isNotEmpty() && frequencyName.isNotEmpty() && isBound) {
                val frequencyData = frequencies[category]?.get(frequencyName)
                val durationMinutes = timerOptions[timerSelection] ?: 0L
                val durationMillis = TimeUnit.MINUTES.toMillis(durationMinutes)

                if (frequencyData != null) {
                    val (baseFreq, binauralFreq) = frequencyData
                    startService(Intent(this, PlaybackService::class.java))
                    playbackService?.playFrequency(baseFreq, binauralFreq, frequencyName, durationMillis)
                    binding.statusTextView.text = "Reproduciendo: $frequencyName"
                }
            }
        }

        binding.stopButton.setOnClickListener {
            if (isBound) {
                playbackService?.stop()
            }
            binding.statusTextView.text = "Estado: Detenido"
            updateCountdownUI(0)
            activeAmbientSounds.clear()
            updateAllAmbientButtonUI()
        }

        binding.volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && isBound) {
                    val volume = progress / 100f
                    playbackService?.setFrequencyVolume(volume)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.rainSoundButton.setOnClickListener {
            handleAmbientButtonClick(R.raw.rainthunder, binding.rainSoundButton)
        }
        binding.fireSoundButton.setOnClickListener {
            handleAmbientButtonClick(R.raw.fogata, binding.fireSoundButton)
        }
        binding.forestSoundButton.setOnClickListener {
            handleAmbientButtonClick(R.raw.forest, binding.forestSoundButton)
        }

        binding.ambientVolumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && isBound) {
                    val volume = progress / 100f
                    playbackService?.setAmbientVolume(volume)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.infoButton.setOnClickListener {
            showFrequencyInfo()
        }
    }

    private fun showFrequencyInfo() {
        val selectedFrequency = binding.frequencyAutoCompleteTextView.text.toString()
        if (selectedFrequency.isEmpty()) {
            // No hacer nada si no hay una frecuencia seleccionada
            return
        }

        val description = frequencyDescriptions[selectedFrequency] ?: "No hay información disponible para esta frecuencia."

        AlertDialog.Builder(this)
            .setTitle(selectedFrequency)
            .setMessage(description)
            .setPositiveButton("Cerrar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun handleAmbientButtonClick(soundResId: Int, button: ImageView) {
        if (!isBound) return
        playbackService?.toggleAmbientSound(soundResId)

        if (activeAmbientSounds.contains(soundResId)) {
            activeAmbientSounds.remove(soundResId)
        } else {
            activeAmbientSounds.add(soundResId)
        }
        updateAmbientButtonUI(soundResId, button)
    }

    private fun updateAmbientButtonUI(soundResId: Int, button: ImageView) {
        if (activeAmbientSounds.contains(soundResId)) {
            button.setColorFilter(ContextCompat.getColor(this, R.color.button_activated))
        } else {
            button.setColorFilter(ContextCompat.getColor(this, R.color.white))
        }
    }

    private fun updateAllAmbientButtonUI() {
        updateAmbientButtonUI(R.raw.rainthunder, binding.rainSoundButton)
        updateAmbientButtonUI(R.raw.fogata, binding.fireSoundButton)
        updateAmbientButtonUI(R.raw.forest, binding.forestSoundButton)
    }

    private fun updateCountdownUI(timeRemainingMillis: Long) {
        if (timeRemainingMillis > 0) {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(timeRemainingMillis)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(timeRemainingMillis) - TimeUnit.MINUTES.toSeconds(minutes)
            binding.countdownTextView.text = String.format("%02d:%02d", minutes, seconds)
        } else {
            binding.countdownTextView.text = ""
        }
    }
}