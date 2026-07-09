package gf.gab.upm.spv.ble.spv

import gf.gab.upm.spv.ble.SpvBleManager
import gf.gab.upm.spv.db.SpvSensorData
import gf.gab.upm.spv.util.logDebug
import gf.gab.upm.spv.util.logVerbose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedInputStream
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class SpvDataFetcher(
    private val manager: SpvBleManager,
    private val latest: Instant,
    private val onDataIngested: suspend (SpvSensorData) -> Unit
) {
    companion object {
        private const val READ_TIMEOUT_MS = 2000L
    }

    suspend fun start() {
        logDebug("Starting SDS data fetch")
        val address = manager.getMacAddress() ?: return
        logVerbose("Obtaining service metadata")
        val serviceMetadata = manager.readSensorMetadata() ?: return
        logVerbose("Obtained metadata:")
        logVerbose("    L2CAP PSM:  ${serviceMetadata.psm}")

        logDebug("Connecting via L2CAP")
        val channel = manager.openL2CapChannel(serviceMetadata.psm) ?: return
        channel.use {
            val stream = channel.inputStream.buffered()

            logVerbose("Requesting summaries")
            manager.writeSensorControl(SpvSummaryRequest.encode())
            logVerbose("Reading summaries")
            val summaryHeaderBytes = stream
                .readNBytesTimeout(SpvSummaryHeader.SIZE, READ_TIMEOUT_MS) ?: return
            val summaryHeader = SpvSummaryHeader.tryDecode(summaryHeaderBytes) ?: return

            logVerbose("Found ${summaryHeader.count} summary blocks, reading...")
            val rawSummaries = (0..<summaryHeader.count).mapIndexed { index, _ ->
                stream
                    .readNBytesTimeout(16, READ_TIMEOUT_MS)
                    ?.let { SpvFsSummary.tryDecode(it, index) }
            }.filterNotNull()
            logDebug("Successfully read ${rawSummaries.size} summary blocks")
            val filteredSummaries = rawSummaries.filter {
                logVerbose("    Summary block:")
                logVerbose("        Index:  ${it.index}")
                logVerbose("        Date:   ${it.startDate}")
                logVerbose("        Sync:   ${it.timesyncStatus}")
                logVerbose("        Length: ${it.measurementCount}")
                it.measurementCount > 0 && it.getLatestReadingDate() > latest
            }
            logVerbose("Filtered ${rawSummaries.size - filteredSummaries.size} summary blocks")

            logDebug("Reading ${filteredSummaries.size} data blocks")
            filteredSummaries.forEach { summary ->
                val offset = summary.getDateOffset(latest)
                val request = summary.toRequest(offset)
                logDebug("Reading data ${request.index}")
                logVerbose("        Index:  ${request.index}")
                logVerbose("        Date:  ${Instant.fromEpochSeconds(request.unixDate)}")
                logVerbose("        Offset: ${request.readingOffset}")

                manager.writeSensorControl(request.encode())

                val dataHeaderBytes = stream
                    .readNBytesTimeout(SpvDataHeader.SIZE, READ_TIMEOUT_MS) ?: return@forEach
                val dataHeader = SpvDataHeader.tryDecode(dataHeaderBytes) ?: return@forEach

                logVerbose("Found ${dataHeader.count} data blocks, reading...")
                (0..<dataHeader.count).forEachIndexed { index, _ ->
                    val dataBytes = stream
                        .readNBytesTimeout(16, READ_TIMEOUT_MS) ?: return@forEachIndexed
                    val data = SpvEnvironmentalMeasurements.tryDecode(dataBytes) ?: return@forEachIndexed
                    val date = summary.getReadingDate(index + request.readingOffset)
                    onDataIngested(SpvSensorData(address, date, data))
                }
            }
        }
    }
}

private fun SpvFsSummary.getLatestReadingDate(): Instant {
    val startTimestamp = this.startDate.toEpochMilliseconds()
    val endOffset = this.settings.samplingRateMinutes * (this.measurementCount - 1) * 60 * 1000

    return Instant.fromEpochMilliseconds(startTimestamp + endOffset)
}

private fun SpvFsSummary.getDateOffset(instant: Instant): Int {
    val startTimestamp = this.startDate.toEpochMilliseconds()
    val requestedTimestamp = instant.toEpochMilliseconds()

    val timeBetweenMeasurements = (this.settings.samplingRateMinutes * 60 * 1000).coerceAtLeast(1)

    return ((requestedTimestamp - startTimestamp) / timeBetweenMeasurements)
        .coerceIn(0L..<this.measurementCount)
        .toInt()
}

private fun SpvFsSummary.getReadingDate(index: Int): Instant {
    val startTimestamp = this.startDate.toEpochMilliseconds()
    val endOffset = this.settings.samplingRateMinutes * index * 60 * 1000

    return Instant.fromEpochMilliseconds(startTimestamp + endOffset)
}

private suspend fun BufferedInputStream.readNBytesTimeout(size: Int, timeoutMilliseconds: Long): ByteArray? = withContext(Dispatchers.IO) {
    withTimeoutOrNull(timeoutMilliseconds.milliseconds) {
        readNBytes(size)
    }
}
