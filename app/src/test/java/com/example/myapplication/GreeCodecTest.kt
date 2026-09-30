package com.example.myapplication

import com.example.myapplication.ir.GreeCodec
import com.example.myapplication.ir.GreeRemoteModel
import com.example.myapplication.ir.GreeState
import com.example.myapplication.ir.VerticalSwing
import com.example.myapplication.ir.YapofCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GreeCodecTest {
    @Test
    fun fixedSwingUsesPositionCodes() {
        val up = GreeCodec.encodeState(GreeState(power = true, verticalSwing = VerticalSwing.FixUp))
        val mid = GreeCodec.encodeState(GreeState(power = true, verticalSwing = VerticalSwing.FixMid))
        val down = GreeCodec.encodeState(GreeState(power = true, verticalSwing = VerticalSwing.FixDown))
        assertEquals(0b0010, up[4] and 0x0F)
        assertEquals(0b0100, mid[4] and 0x0F)
        assertEquals(0b0110, down[4] and 0x0F)
    }

    @Test
    fun checksumMatchesKelvinatorNibbleSum() {
        val bytes = GreeCodec.encodeState(
            GreeState(power = true, mode = GreeCodec.MODE_COOL, tempC = 26, light = true),
        )
        assertEquals(GreeCodec.checksum(bytes), (bytes[7] shr 4) and 0x0F)
        assertEquals(0x50, bytes[3])
        assertEquals(0x20, bytes[5])
        assertEquals(10, bytes[1] and 0x0F)
        assertEquals(0b00001001, bytes[0])
        assertEquals(0x20, bytes[2])
        assertEquals(0x20, bytes[6])
        assertEquals(0x01, (bytes[7] shr 4) and 0x0F)
    }

    @Test
    fun userCaptureMatchesYaw1fCool26MidSwing() {
        val bytes = GreeCodec.encodeState(
            GreeState(
                power = true,
                mode = GreeCodec.MODE_COOL,
                tempC = 26,
                verticalSwing = VerticalSwing.FixMid,
            ),
            GreeRemoteModel.Yaw1f,
        )
        assertEquals("09 0A 60 50 04 20 00 F0", bytes.joinToString(" ") { "%02X".format(it) })
    }

    @Test
    fun knownResetStateChecksumStartsAtTen() {
        val bytes = GreeCodec.encodeState(
            GreeState(power = false, mode = GreeCodec.MODE_AUTO, tempC = 25, light = true),
            GreeRemoteModel.Ybofb,
        )
        assertEquals("00 09 20 50 00 20 00 50", bytes.joinToString(" ") { "%02X".format(it) })
    }

    @Test
    fun pulsePatternHasEvenLengthAndHeader() {
        val pattern = GreeCodec.toPulsePattern(GreeState(power = true))
        assertTrue(pattern.size % 2 == 0)
        assertEquals(9000, pattern[0])
        assertEquals(4500, pattern[1])
        assertEquals(140, pattern.size)
    }

    @Test
    fun yapof16MatchesCool26FanLowSwingUp() {
        val bytes = YapofCodec.encodeState(
            GreeState(
                power = true,
                mode = GreeCodec.MODE_COOL,
                tempC = 26,
                fan = GreeCodec.FAN_LOW,
                verticalSwing = VerticalSwing.FixUp,
            ),
        )
        assertEquals(
            "19 0A 20 50 02 00 00 D0 19 0A 20 70 00 00 10 E0",
            bytes.joinToString(" ") { "%02X".format(it) },
        )
        val pattern = YapofCodec.toPulsePattern(
            GreeState(power = true, fan = GreeCodec.FAN_LOW, verticalSwing = VerticalSwing.FixUp),
        )
        assertEquals(280, pattern.size)
        assertEquals(9010, pattern[0])
    }
}
