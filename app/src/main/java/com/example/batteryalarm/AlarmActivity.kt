package com.example.batteryalarm

import android.content.Context
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.roundToInt

class AlarmActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var originalAlarmVolume: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val currentPercentage = intent.getIntExtra("CURRENT_PERCENTAGE", 100)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#B71C1C"))
            setPadding(64, 64, 64, 64)
        }

        val tvTitle = TextView(this).apply {
            text = "PERINGATAN BATERAI!"
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val tvMessage = TextView(this).apply {
            text = "Baterai telah mencapai $currentPercentage%.\nSilakan cabut kabel charger sekarang."
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 64)
        }

        val btnDismiss = Button(this).apply {
            text = "MATIKAN ALARM"
            textSize = 18f
            setOnClickListener {
                stopAlarmAndFinish()
            }
        }

        layout.addView(tvTitle)
        layout.addView(tvMessage)
        layout.addView(btnDismiss)
        setContentView(layout)

        playAlarmSound()
    }

    private fun playAlarmSound() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try {
            // Simpan volume alarm awal, lalu atur ke 85% dari volume maksimal
            originalAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val targetVolume = (maxVolume * 0.85).roundToInt().coerceAtLeast(1)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0)
        } catch (e: SecurityException) {
            // Abaikan jika sistem membatasi perubahan volume saat mode Jangan Ganggu
        }

        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(this@AlarmActivity, alarmUri)
            setVolume(1.0f, 1.0f)
            isLooping = true
            prepare()
            start()

            // Penguat audio moderat (+10 dB) agar suara tetap jernih di level 85%
            try {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                    setTargetGain(1000)
                    enabled = true
                }
            } catch (e: Exception) {
                // Tetap lanjutkan pemutaran normal jika perangkat tidak mendukung efek gain
            }
        }
    }

    private fun stopAlarmAndFinish() {
        cleanupAudio()
        finish()
    }

    private fun cleanupAudio() {
        try {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
        } catch (e: Exception) {
            // Abaikan error saat melepas resource
        }
        loudnessEnhancer = null

        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.release()
            } catch (e: Exception) {
                // Abaikan error saat melepas resource
            }
        }
        mediaPlayer = null

        // Kembalikan volume alarm ke pengaturan awal pengguna
        if (originalAlarmVolume != -1) {
            try {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalAlarmVolume, 0)
            } catch (e: SecurityException) {
                // Abaikan jika dibatasi sistem
            }
            originalAlarmVolume = -1
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupAudio()
    }
}
