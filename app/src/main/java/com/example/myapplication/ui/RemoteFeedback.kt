package com.example.myapplication.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class ClickSound(val label: String, val tone: Int, val durationMs: Int) {
    Off("无", -1, 0),
    Beep("短嘀", ToneGenerator.TONE_PROP_BEEP, 50),
    Key("按键", ToneGenerator.TONE_DTMF_5, 40),
    Crisp("清脆", ToneGenerator.TONE_PROP_ACK, 80),
    Soft("轻柔", ToneGenerator.TONE_PROP_PROMPT, 90),
}

object RemoteFeedback {
    private var tone: ToneGenerator? = null

    fun play(context: Context, sound: ClickSound = ClickSound.Beep) {
        vibrate(context)
        beep(sound)
    }

    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator == null || !vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
    }

    private fun beep(sound: ClickSound) {
        if (sound.tone < 0) return
        try {
            val generator = tone ?: ToneGenerator(AudioManager.STREAM_MUSIC, 70).also { tone = it }
            generator.startTone(sound.tone, sound.durationMs)
        } catch (_: Exception) {
            tone = null
        }
    }
}
