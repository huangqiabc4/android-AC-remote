package com.example.myapplication.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import android.util.Log

class IrBlaster(context: Context) {
    private val manager = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    val hasEmitter: Boolean get() = manager?.hasIrEmitter() == true

    fun transmit(state: GreeState): IrResult {
        val ir = manager ?: return IrResult.NoHardware("系统未提供红外服务，没有发出信号")
        if (!ir.hasIrEmitter()) return IrResult.NoHardware("系统报告没有红外灯，没有发出信号")
        val freq = pickFrequency(ir)
        return try {
            val pattern = YapofCodec.toPulsePattern(state)
            ir.transmit(freq, pattern)
            Log.i(TAG, "IR sent YAP16 ${YapofCodec.toHex(state)}")
            IrResult.Sent
        } catch (e: Exception) {
            Log.e(TAG, "IR transmit failed", e)
            IrResult.Failed(e.message ?: "红外发送异常")
        }
    }

    private fun pickFrequency(ir: ConsumerIrManager): Int {
        val ranges = ir.carrierFrequencies ?: return GreeCodec.CARRIER_HZ
        if (ranges.isEmpty()) return GreeCodec.CARRIER_HZ
        val wanted = GreeCodec.CARRIER_HZ
        val hit = ranges.firstOrNull { wanted in it.minFrequency..it.maxFrequency }
        if (hit != null) return wanted
        val first = ranges.first()
        return ((first.minFrequency.toLong() + first.maxFrequency) / 2).toInt()
    }

    companion object {
        private const val TAG = "GreeIr"
    }
}

sealed interface IrResult {
    data object Sent : IrResult
    data class NoHardware(val reason: String) : IrResult
    data class Failed(val reason: String) : IrResult
}
