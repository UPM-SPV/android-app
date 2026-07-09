package gf.gab.upm.spv.screens.historical

import android.content.Context
import android.content.Intent
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import gf.gab.upm.spv.R
import gf.gab.upm.spv.ble.MacAddress
import gf.gab.upm.spv.ble.SpvDevice
import gf.gab.upm.spv.ble.SpvDeviceRepository
import gf.gab.upm.spv.ble.spv.SpvDataFetcher
import gf.gab.upm.spv.db.SpvSensorData
import gf.gab.upm.spv.db.SpvSensorDataDao
import gf.gab.upm.spv.ota.OtaInfo
import gf.gab.upm.spv.ota.OtaServerManager
import gf.gab.upm.spv.ota.OtaVersion
import gf.gab.upm.spv.util.logError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.toJavaInstant

@HiltViewModel
class HistoricalDataViewModel @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val sensorRepository: SpvDeviceRepository,
    private val dataDao: SpvSensorDataDao,
    private val otaManager: OtaServerManager
) : ViewModel() {

    private val currentDevice = MutableStateFlow<SpvDevice.Connected?>(null)
    val sensorData = MutableStateFlow<List<SpvSensorData>>(emptyList())
    val updateStatus = MutableStateFlow<HistoricalDataUpdateStatus>(HistoricalDataUpdateStatus.None)
    private var dbJob: Job? = null
    private var dataFetchJob: Job? = null

    fun startConnection(address: MacAddress) {
        viewModelScope.launch {
            launch {
                sensorRepository.startAutoReconnect()
            }

            launch {

                sensorRepository.devices
                    .map { list -> list.filter { it.info().macAddress == address } }
                    .collect { deviceList ->
                        if (deviceList.isEmpty()) {
                            return@collect
                        }
                        when (val device = deviceList.first()) {
                            is SpvDevice.Connected -> {
                                if (currentDevice.value == null) {
                                    currentDevice.value = device
                                    startDataFetch(
                                        device,
                                        dataDao.getLatest(address)?.date
                                    )
                                    checkUpdates(device)
                                }
                            }

                            is SpvDevice.Connecting -> {}
                            is SpvDevice.Disconnected -> currentDevice.value = null
                        }
                    }
            }
        }
    }

    fun startDbFetch(address: MacAddress, startTime: Instant? = null, endTime: Instant? = null) {
        dbJob?.cancel()

        dbJob = viewModelScope.launch {
            startTime?.let {
                val endTime = endTime ?: Clock.System.now()
                dataDao.getAllTimeFrameFlow(address, startTime, endTime)
                    .collect { sensorData.value = it }
            } ?: run {
                dataDao.getAllFlow(address).collect { sensorData.value = it }
            }
        }
    }

    fun exportCsv(address: MacAddress, start: Instant, end: Instant) {
        viewModelScope.launch(Dispatchers.IO) {
            val csvFile = File(context.cacheDir, "export.csv")

            try {
                val dbData = dataDao.getTimeFrame(address, start, end)
                FileWriter(csvFile).use { writer ->
                    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    writer.append(
                        "${context.getString(R.string.data_export_date_header)}," +
                        "${context.getString(R.string.data_export_date_temperature)}," +
                        "${context.getString(R.string.data_export_date_humidity)}," +
                        "${context.getString(R.string.data_export_date_lux)}," +
                        "${context.getString(R.string.data_export_date_decibels)}," +
                        context.getString(R.string.data_export_date_voc) +
                        "\n"
                    )
                    dbData.forEach { reading ->
                        val localDate = reading.date.toJavaInstant().atZone(ZoneId.systemDefault())
                        writer.append(
                            "${dateFormatter.format(localDate)}," +
                            "${reading.data.temperatureCelsius ?: ""}," +
                            "${reading.data.humidityRh ?: ""}," +
                            "${reading.data.lux ?: ""}," +
                            "${reading.data.decibels ?: ""}," +
                            "${reading.data.vocRaw ?: ""}" +
                            "\n"
                        )
                    }
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    csvFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.data_export_intent_subject))
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(shareIntent, context.getString(R.string.data_export_intent_title))
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch(e: Exception) {
                logError(e.toString())
            }
        }
    }

    private fun startDataFetch(device: SpvDevice.Connected, latestData: Instant?) {
        dataFetchJob?.cancel()
        dataFetchJob = viewModelScope.launch {
            SpvDataFetcher(
                device.manager,
                latest = latestData ?: Instant.fromEpochSeconds(0),
                onDataIngested = { dataPoint ->
                    launch(Dispatchers.IO) {
                        dataDao.insert(dataPoint)
                    }
                }
            ).start()
        }
    }

    private fun checkUpdates(device: SpvDevice.Connected) {
        viewModelScope.launch {
            val currentFuture = async { device.manager.getFirmwareVersion() }
            val newFuture = async { otaManager.getLatestRelease() }

            val current = currentFuture.await() ?: return@launch
            val new = newFuture.await() ?: return@launch

            if(new.version > current) {
                updateStatus.value = HistoricalDataUpdateStatus.Available(current, new)
            }
        }
    }
}

sealed interface HistoricalDataUpdateStatus {
    object None : HistoricalDataUpdateStatus { override fun isAvailable(): Boolean = false }
    data class Available(val current: OtaVersion, val new: OtaInfo) : HistoricalDataUpdateStatus {
        override fun isAvailable(): Boolean = true
    }

    fun isAvailable(): Boolean
}
