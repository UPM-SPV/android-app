package gf.gab.upm.spv.ble.spv

import SensorDataRequest
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.time.Instant

/*
 * Filesystem structures straight from the firmware.
 *
 * Stored as raw C structs for efficiency and directly streamed, notably *not* protobuf.
 * All integers are Little-Endian.
 */

data class SpvFsSummary(
    val startDate: Instant,
    val settings: SpvDeviceSettings,
    val measurementCount: Int,
    val index: Int,
    val timesyncStatus: SpvTimesyncStatus,
) {
    companion object {
        fun tryDecode(bytes: ByteArray, index: Int): SpvFsSummary? {
            if(bytes.size != 16) {
                return null
            }
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

            val date = Instant.fromEpochSeconds(buffer.getInt().toLong())
            val samplingRate = buffer.getInt() / 60
            val count = buffer.getInt()
            val sensorFlags = SpvDeviceSettingsSensors.decode(buffer.get().toInt())
            val timesyncStatus = SpvTimesyncStatus.tryDecode(buffer.get().toInt()) ?: return null

            return SpvFsSummary(
                startDate = date,
                settings = SpvDeviceSettings(
                    samplingRateMinutes = samplingRate,
                    sensorSettings = sensorFlags
                ),
                measurementCount = count,
                index = index,
                timesyncStatus = timesyncStatus
            )
        }

        fun tryDecodeMany(offset: Int, bytes: ByteArray): List<SpvFsSummary>? {
            if(bytes.size % 16 != 0) {
                return null
            }

            val summaries = bytes.asList()
                .chunked(16)
                .mapIndexed { index, bytes -> tryDecode(bytes.toByteArray(), index + offset) }

            if(summaries.any { it == null }) {
                return null
            }

            // Pleases the compiler, does nothing.
            return summaries.filterNotNull()
        }
    }

    fun toRequest(readingOffset: Int = 0): SpvDataRequest = SpvDataRequest(
        index = index,
        unixDate = startDate.epochSeconds,
        readingOffset = readingOffset
    )


}

enum class SpvTimesyncStatus(val value: Int) {
    NONE(0),
    DEGRADED(1),
    PEER(2),
    AUTHORITATIVE(3);

    companion object {
        fun tryDecode(value: Int): SpvTimesyncStatus? = when(value) {
            0 -> NONE
            1 -> DEGRADED
            2 -> PEER
            3 -> AUTHORITATIVE

            else -> null
        }
    }
}

data class SpvPeriodData(
    val summary: SpvFsSummary,
    val data: List<SpvEnvironmentalMeasurements>
)

data class SpvSdsMetadata(val psm: Int) {
    companion object {
        fun tryDecode(bytes: ByteArray): SpvSdsMetadata? {

            val decoded = SensorDataRequest.SdsServiceMetadata
                .parseFrom(bytes)

            if(decoded.schemaVersion != 1) {
                return null
            }

            return SpvSdsMetadata(
                psm = decoded.l2CapPsm
            )
        }
    }
}

object SpvSummaryRequest {
    fun encode(): ByteArray =
        SensorDataRequest.SdsRequest.newBuilder()
            .setSchemaVersion(1)
            .setSummaryRequest(
                SensorDataRequest.SdsSummaryRequest.newBuilder()
            )
            .build()
            .toByteArray()
}

data class SpvDataRequest(
    val index: Int,
    val unixDate: Long,
    val readingOffset: Int,
) {
    fun encode(): ByteArray =
        SensorDataRequest.SdsRequest.newBuilder()
            .setSchemaVersion(1)
            .setDataRequest(
                SensorDataRequest.SdsDataRequest.newBuilder()
                    .setPeriodIndex(index)
                    .setPeriodUnixDate(unixDate)
                    .setReadingOffsetStart(readingOffset)
            )
            .build()
            .toByteArray()
}

data class SpvSummaryHeader(
    val count: Int,
) {
    companion object {
        const val SIZE = 10
        const val MAGIC = 0x3DB74FB8

        fun tryDecode(bytes: ByteArray): SpvSummaryHeader? {

            val decoded = SensorDataRequest.SdsSummaryResponseHeader
                .parseFrom(bytes)

            if(decoded.magic != MAGIC) {
                return null
            }

            return SpvSummaryHeader(
                count = decoded.summaryCount
            )
        }
    }
}

data class SpvDataHeader(
    val count: Int,
) {
    companion object {
        const val SIZE = 10
        const val MAGIC = 0xADAEEDD7

        fun tryDecode(bytes: ByteArray): SpvDataHeader? {

            val decoded = SensorDataRequest.SdsDataResponseHeader
                .parseFrom(bytes)

            if(decoded.magic != MAGIC.toInt()) {
                return null
            }

            return SpvDataHeader(
                count = decoded.dataBlockCount
            )
        }
    }
}
