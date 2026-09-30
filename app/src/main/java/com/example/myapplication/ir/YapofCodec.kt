package com.example.myapplication.ir

/**
 * 格力新款遥控 YAPOF3 / YAP0F8 / YAP2F 使用 Kelvinator 16 字节协议，
 * 不是网上常见的 8 字节 YB0F2 短码。
 */
object YapofCodec {
    const val CARRIER_HZ = 38_000

    private const val HDR_MARK = 9010
    private const val HDR_SPACE = 4505
    private const val BIT_MARK = 680
    private const val ONE_SPACE = 1530
    private const val ZERO_SPACE = 510
    private const val GAP = 19_975
    private const val GAP_LONG = 39_950

    fun encodeState(state: GreeState): IntArray {
        val bytes = IntArray(16)
        val swingAuto = if (state.verticalSwing.usesAutoBit) 1 else 0
        val basicFan = state.fan.coerceIn(0, 3)
        bytes[0] = (state.mode and 0b111) or
            ((if (state.power) 1 else 0) shl 3) or
            ((basicFan and 0b11) shl 4) or
            (swingAuto shl 6)
        bytes[1] = (state.tempC - GreeCodec.MIN_TEMP).coerceIn(0, 14) and 0x0F
        bytes[2] = ((if (state.turbo) 1 else 0) shl 4) or
            ((if (state.light) 1 else 0) shl 5)
        bytes[3] = 0x50
        bytes[4] = state.verticalSwing.code and 0x0F
        bytes[7] = GreeCodec.checksum(bytes.copyOfRange(0, 8)) shl 4

        bytes[8] = bytes[0]
        bytes[9] = bytes[1]
        bytes[10] = bytes[2]
        bytes[11] = 0x70
        bytes[14] = (state.fan and 0b111) shl 4
        bytes[15] = GreeCodec.checksum(bytes.copyOfRange(8, 16)) shl 4
        return bytes
    }

    fun toHex(state: GreeState): String =
        encodeState(state).joinToString(" ") { "%02X".format(it) }

    fun toPulsePattern(state: GreeState): IntArray {
        val bytes = encodeState(state)
        val out = ArrayList<Int>(280)
        appendBlock(out, bytes, 0)
        appendBlock(out, bytes, 8)
        return out.toIntArray()
    }

    private fun appendBlock(out: MutableList<Int>, bytes: IntArray, offset: Int) {
        out += HDR_MARK
        out += HDR_SPACE
        for (i in 0 until 4) appendByteLsb(out, bytes[offset + i])
        appendBitsLsb(out, 0b010, 3)
        out += BIT_MARK
        out += GAP
        for (i in 4 until 8) appendByteLsb(out, bytes[offset + i])
        out += BIT_MARK
        out += GAP_LONG
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
