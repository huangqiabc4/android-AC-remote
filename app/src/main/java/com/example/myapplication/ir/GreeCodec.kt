package com.example.myapplication.ir

/**
 * 格力壁挂机 8 字节状态。国内常见 YB1F2/YAA 不置 ModelA，且 Byte6 高半字节为 0x2。
 */
object GreeCodec {
    const val CARRIER_HZ = 38_000

    const val MIN_TEMP = 16
    const val MAX_TEMP = 30

    const val MODE_AUTO = 0
    const val MODE_COOL = 1
    const val MODE_DRY = 2
    const val MODE_FAN = 3
    const val MODE_HEAT = 4

    const val FAN_AUTO = 0
    const val FAN_LOW = 1
    const val FAN_MID = 2
    const val FAN_HIGH = 3

    const val SWING_V_LAST = 0b0000
    const val SWING_V_FULL_AUTO = 0b0001
    const val SWING_V_UP = 0b0010
    const val SWING_V_MIDDLE = 0b0100
    const val SWING_V_DOWN = 0b0110

    private const val HDR_MARK = 9000
    private const val HDR_SPACE = 4500
    private const val BIT_MARK = 620
    private const val ONE_SPACE = 1600
    private const val ZERO_SPACE = 540
    private const val MSG_SPACE = 19_980
    private const val FOOTER_BITS = 0b010
    private const val STATE_LEN = 8

    fun encodeState(state: GreeState, model: GreeRemoteModel = GreeRemoteModel.Yaa): IntArray {
        val bytes = IntArray(STATE_LEN)
        val swingAuto = state.verticalSwing.usesAutoBit
        bytes[0] = (state.mode and 0b111) or
            ((if (state.power) 1 else 0) shl 3) or
            ((state.fan and 0b11) shl 4) or
            ((if (swingAuto) 1 else 0) shl 6) or
            ((if (state.sleep) 1 else 0) shl 7)
        bytes[1] = (state.tempC - MIN_TEMP).coerceIn(0, 14) and 0x0F
        val modelA = if (state.power && model == GreeRemoteModel.Yaw1f) 1 else 0
        bytes[2] = ((if (state.turbo) 1 else 0) shl 4) or
            ((if (state.light) 1 else 0) shl 5) or
            (modelA shl 6)
        bytes[3] = 0x50
        bytes[4] = (state.verticalSwing.code and 0x0F) or
            ((state.horizontalSwing and 0b111) shl 4)
        bytes[5] = 0x20
        bytes[6] = if (model == GreeRemoteModel.Yaa) 0x20 else 0x00
        bytes[7] = checksum(bytes) shl 4
        return bytes
    }

    fun toHex(state: GreeState, model: GreeRemoteModel = GreeRemoteModel.Yaa): String =
        encodeState(state, model).joinToString(" ") { "%02X".format(it) }

    fun toPulsePattern(state: GreeState, model: GreeRemoteModel = GreeRemoteModel.Yaa): IntArray {
        val bytes = encodeState(state, model)
        val out = ArrayList<Int>(140)
        out += HDR_MARK
        out += HDR_SPACE
        for (i in 0 until 4) appendByteLsb(out, bytes[i])
        appendBitsLsb(out, FOOTER_BITS, 3)
        out += BIT_MARK
        out += MSG_SPACE
        for (i in 4 until STATE_LEN) appendByteLsb(out, bytes[i])
        out += BIT_MARK
        out += MSG_SPACE
        return out.toIntArray()
    }

    fun checksum(state: IntArray): Int {
        // 与 Kelvinator/IRremoteESP8266 相同：起始值 10，否则室内机丢弃整帧。
        var sum = 10
        for (i in 0..3) sum += state[i] and 0x0F
        for (i in 4..6) sum += (state[i] shr 4) and 0x0F
        return sum and 0x0F
    }

    private fun appendByteLsb(out: MutableList<Int>, value: Int) {
        appendBitsLsb(out, value, 8)
    }

    private fun appendBitsLsb(out: MutableList<Int>, value: Int, bits: Int) {
        var v = value
        repeat(bits) {
            out += BIT_MARK
            out += if (v and 1 == 1) ONE_SPACE else ZERO_SPACE
            v = v shr 1
        }
    }
}

enum class GreeRemoteModel {
    Yaa,
    Ybofb,
    Yaw1f,
}

enum class VerticalSwing(
    val code: Int,
    val usesAutoBit: Boolean,
    val label: String,
) {
    Off(GreeCodec.SWING_V_LAST, false, "关"),
    FullAuto(GreeCodec.SWING_V_FULL_AUTO, true, "自动"),
    FixUp(GreeCodec.SWING_V_UP, false, "上"),
    FixMid(GreeCodec.SWING_V_MIDDLE, false, "中"),
    FixDown(GreeCodec.SWING_V_DOWN, false, "下"),
}

data class GreeState(
    val power: Boolean = false,
    val mode: Int = GreeCodec.MODE_COOL,
    val tempC: Int = 26,
    val fan: Int = GreeCodec.FAN_AUTO,
    val verticalSwing: VerticalSwing = VerticalSwing.Off,
    val horizontalSwing: Int = 0,
    val light: Boolean = true,
    val turbo: Boolean = false,
    val sleep: Boolean = false,
)
