package com.example.myapplication

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.ir.GreeCodec
import com.example.myapplication.ir.GreeState
import com.example.myapplication.ir.IrBlaster
import com.example.myapplication.ir.VerticalSwing
import com.example.myapplication.ui.ClickSound
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GreeRemoteViewModel(application: Application) : AndroidViewModel(application) {
    private val blaster = IrBlaster(application)
    private val prefs = application.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)

    var state by mutableStateOf(loadState())
        private set

    var clickSound by mutableStateOf(loadSound())
        private set

    var showSwingPicker by mutableStateOf(false)
        private set

    fun togglePower() = update { copy(power = !power) }

    fun setMode(mode: Int) = update {
        val nextFan = if (mode == GreeCodec.MODE_DRY) GreeCodec.FAN_LOW else fan
        copy(mode = mode, fan = nextFan, power = true)
    }

    fun changeTemp(delta: Int) = update {
        copy(tempC = (tempC + delta).coerceIn(GreeCodec.MIN_TEMP, GreeCodec.MAX_TEMP), power = true)
    }

    fun cycleFan() = update {
        if (mode == GreeCodec.MODE_DRY) this else {
            copy(fan = (fan + 1) % 4, power = true)
        }
    }

    fun openSwingPicker() {
        showSwingPicker = true
    }

    fun dismissSwingPicker() {
        showSwingPicker = false
    }

    fun lockVane(level: Int) {
        showSwingPicker = false
        update {
            val swing = when (level) {
                0 -> VerticalSwing.FixUp
                1 -> VerticalSwing.FixMid
                else -> VerticalSwing.FixDown
            }
            copy(verticalSwing = swing, power = true)
        }
    }

    fun selectClickSound(sound: ClickSound) {
        clickSound = sound
        prefs.edit().putString(KEY_SOUND, sound.name).apply()
    }

    private fun update(block: GreeState.() -> GreeState) {
        val next = state.block()
        state = next
        saveState(next)
        viewModelScope.launch {
            withContext(Dispatchers.IO) { blaster.transmit(next) }
        }
    }

    private fun loadState(): GreeState {
        val swing = prefs.getString(KEY_SWING, VerticalSwing.Off.name)
            ?.let { name -> VerticalSwing.entries.find { it.name == name } }
            ?: VerticalSwing.Off
        return GreeState(
            power = prefs.getBoolean(KEY_POWER, false),
            mode = prefs.getInt(KEY_MODE, GreeCodec.MODE_COOL),
            tempC = prefs.getInt(KEY_TEMP, 26).coerceIn(GreeCodec.MIN_TEMP, GreeCodec.MAX_TEMP),
            fan = prefs.getInt(KEY_FAN, GreeCodec.FAN_AUTO).coerceIn(0, 3),
            verticalSwing = swing,
            light = prefs.getBoolean(KEY_LIGHT, true),
        )
    }

    private fun saveState(s: GreeState) {
        prefs.edit()
            .putBoolean(KEY_POWER, s.power)
            .putInt(KEY_MODE, s.mode)
            .putInt(KEY_TEMP, s.tempC)
            .putInt(KEY_FAN, s.fan)
            .putString(KEY_SWING, s.verticalSwing.name)
            .putBoolean(KEY_LIGHT, s.light)
            .apply()
    }

    private fun loadSound(): ClickSound {
        val name = prefs.getString(KEY_SOUND, ClickSound.Beep.name)
        return ClickSound.entries.find { it.name == name } ?: ClickSound.Beep
    }

    companion object {
        private const val PREFS = "gree_remote"
        private const val KEY_POWER = "power"
        private const val KEY_MODE = "mode"
        private const val KEY_TEMP = "temp"
        private const val KEY_FAN = "fan"
        private const val KEY_SWING = "swing"
        private const val KEY_LIGHT = "light"
        private const val KEY_SOUND = "sound"
    }
}

fun describe(s: GreeState): String {
    val power = if (s.power) "开" else "关"
    return "$power ${modeLabel(s.mode)} ${s.tempC}℃ 风向${s.verticalSwing.label}"
}

fun modeLabel(mode: Int) = when (mode) {
    GreeCodec.MODE_AUTO -> "自动"
    GreeCodec.MODE_COOL -> "制冷"
    GreeCodec.MODE_DRY -> "除湿"
    GreeCodec.MODE_FAN -> "送风"
    GreeCodec.MODE_HEAT -> "制热"
    else -> "未知"
}

fun fanLabel(fan: Int) = when (fan) {
    GreeCodec.FAN_AUTO -> "0"
    GreeCodec.FAN_LOW -> "1"
    GreeCodec.FAN_MID -> "2"
    GreeCodec.FAN_HIGH -> "3"
    else -> "-"
}

fun swingSummary(swing: VerticalSwing) = swing.label
