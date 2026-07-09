package gf.gab.upm.spv.ble.spv

import java.nio.ByteBuffer
import java.nio.ByteOrder

interface ISpvEnvironmentalEndpoint {

    suspend fun readEnvironmentalMeasurements(): SpvEnvironmentalMeasurements?
}

data class SpvEnvironmentalMeasurements(
    val temperatureCelsius: Double?,
    val humidityRh: Double?,
    val lux: Double?,
    val decibels: Double?,
    val vocRaw: Long?,
) {
    companion object {
        fun tryDecode(bytes: ByteArray): SpvEnvironmentalMeasurements? {
            if(bytes.size != 16) {
                return null
            }
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

            return SpvEnvironmentalMeasurements(
                decibels = buffer.getSensorInt()?.toDouble()?.let { it / 10 },
                vocRaw = buffer.getSensorInt()?.toLong(),
                temperatureCelsius = buffer.getSensorInt()?.toDouble()?.let { it / 1000 },
                humidityRh = buffer.getSensorShort()?.toDouble()?.let { it / 10},
                lux = buffer.getSensorShort()?.toDouble(),
            )
        }

        fun tryDecodeMany(bytes: ByteArray): List<SpvEnvironmentalMeasurements>? {
            if(bytes.size % 16 != 0) {
                return null
            }

            val dataBlocks = bytes.asList()
                .chunked(16)
                .map { bytes -> tryDecode(bytes.toByteArray()) }

            if(dataBlocks.any { it == null }) {
                return null
            }

            // Pleases the compiler, does nothing.
            return dataBlocks.filterNotNull()
        }
    }
}

private fun ByteBuffer.getSensorInt(): Int? {
    val value = getInt().toUInt()
    return if(value == 0xFFFFFFFEU) null else value.toInt()
}

private fun ByteBuffer.getSensorShort(): Int? {
    val value = getShort().toUShort()
    return if(value == 0xFFFE.toUShort()) null else value.toInt()
}
