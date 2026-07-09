package gf.gab.upm.spv.screens.ota

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import gf.gab.upm.spv.ble.MacAddress
import gf.gab.upm.spv.ble.SpvDevice
import gf.gab.upm.spv.ble.SpvDeviceRepository
import gf.gab.upm.spv.ota.OtaInfo
import gf.gab.upm.spv.ota.OtaServerManager
import gf.gab.upm.spv.ota.ZipPackage
import gf.gab.upm.spv.util.logDebug
import gf.gab.upm.spv.util.logError
import gf.gab.upm.spv.util.logInfo
import gf.gab.upm.spv.util.logVerbose
import io.runtime.mcumgr.ble.McuMgrBleTransport
import io.runtime.mcumgr.dfu.FirmwareUpgradeCallback
import io.runtime.mcumgr.dfu.FirmwareUpgradeController
import io.runtime.mcumgr.dfu.mcuboot.FirmwareUpgradeManager
import io.runtime.mcumgr.exception.McuMgrException
import io.runtime.mcumgr.managers.DefaultManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class OtaViewModel @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val sensorRepository: SpvDeviceRepository,
    private val otaServerManager: OtaServerManager
) : ViewModel() {

    companion object {
        private val CONNECT_TIMEOUT = 15.seconds
    }

    val otaStatus = MutableStateFlow<OtaStatus>(OtaStatus.Connecting)
    private var updateJob: Job? = null

    fun startOta(address: MacAddress, otaInfo: OtaInfo) {
        logInfo("Starting OTA update to ${otaInfo.version}")
        otaStatus.value = OtaStatus.Connecting

        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            logDebug("Connecting to $address...")
            val scanJob = launch {
                sensorRepository.startAutoReconnect()
            }

            val device = withTimeoutOrNull(CONNECT_TIMEOUT) {
                sensorRepository.devices
                    .mapNotNull { deviceList ->
                        deviceList
                            .filterIsInstance<SpvDevice.Connected>()
                            .firstOrNull { it.info.macAddress == address }
                    }
                    .first()
            }
            if(device == null) {
                logError("Connection timed out")
                otaStatus.value = OtaStatus.ConnectionError
                return@launch
            }
            logVerbose("    Connection successful")

            logDebug("Downloading OTA image")
            val dfuBytes = otaServerManager.downloadDfu(otaInfo) { downloadedBytes ->
                val progress = downloadedBytes.toFloat() / otaInfo.sizeBytes.toFloat()
                otaStatus.value = OtaStatus.Downloading(
                    progress,
                    otaInfo.sizeBytes,
                    downloadedBytes
                )
                logVerbose("    Progress:   ${progress * 100}%")
            }
            if(dfuBytes == null) {
                logError("OTA download error")
                otaStatus.value = OtaStatus.ConnectionError
                return@launch
            }
            logVerbose("    Download complete")
            val updatePackage = ZipPackage(dfuBytes).binaries
            otaStatus.value


            // MCUmgr expects to be the sole owner of the connection.
            scanJob.cancel()

            val transport = McuMgrBleTransport(context, device.manager.bluetoothDevice!!)
            val defaultManager = DefaultManager(transport)
            val dfuManager = FirmwareUpgradeManager(
                transport,
                object : FirmwareUpgradeCallback<FirmwareUpgradeManager.State> {
                    override fun onUpgradeStarted(controller: FirmwareUpgradeController?) {
                        otaStatus.value = OtaStatus.DfuUpdating(0f, otaInfo.sizeBytes, 0)
                        logVerbose("    DFU started")
                    }

                    override fun onStateChanged(
                        prevState: FirmwareUpgradeManager.State?,
                        newState: FirmwareUpgradeManager.State?
                    ) {
                        when(newState) {
                            FirmwareUpgradeManager.State.TEST -> otaStatus.value = OtaStatus.DfuValidating
                            else -> {}
                        }
                    }

                    override fun onUpgradeCompleted() {
                        otaStatus.value = OtaStatus.Done
                        logInfo("DFU update successful")
                    }

                    override fun onUpgradeFailed(
                        state: FirmwareUpgradeManager.State?,
                        error: McuMgrException?
                    ) {
                        otaStatus.value = OtaStatus.DfuError
                        logError("DFU update error: $error")
                    }

                    override fun onUpgradeCanceled(state: FirmwareUpgradeManager.State?) {
                        // Cancellation is not exposed to the user. Treat it as an error.
                        otaStatus.value = OtaStatus.DfuError
                        logError("DFU update canceled")
                    }

                    override fun onUploadProgressChanged(
                        bytesSent: Int,
                        imageSize: Int,
                        timestamp: Long
                    ) {
                        if(bytesSent == 0) {
                            return
                        }
                        val progress = bytesSent.toFloat() / imageSize.toFloat()
                        otaStatus.value = OtaStatus.DfuUpdating(
                            progress,
                            imageSize.toLong(),
                            bytesSent.toLong()
                        )
                        logVerbose("    DFU progress:   $progress")
                    }

                }
            )

            otaStatus.value = OtaStatus.DfuStarting
            logDebug("Starting DFU update")
            dfuManager.setMode(FirmwareUpgradeManager.Mode.TEST_ONLY)
            withContext(Dispatchers.IO) {
                dfuManager.start(
                    updatePackage,
                    FirmwareUpgradeManager.Settings.Builder()
                        .setEraseAppSettings(false)
                        .build()
                )
                try {
                    defaultManager.reset()
                } catch(_: Exception) {

                }
                try {
                    device.manager.disconnectAndAwait()
                } catch(_: Exception) {

                }
            }
        }
    }
}

sealed interface OtaStatus {
    object Connecting : OtaStatus
    object ConnectionError : OtaStatus

    data class Downloading(val progress: Float, val totalBytes: Long, val currentBytes: Long) : OtaStatus
    object DownloadError : OtaStatus

    object DfuStarting : OtaStatus
    data class DfuUpdating(val progress: Float, val totalBytes: Long, val currentBytes: Long) : OtaStatus
    object DfuValidating : OtaStatus
    object DfuError : OtaStatus

    object Done : OtaStatus
}
