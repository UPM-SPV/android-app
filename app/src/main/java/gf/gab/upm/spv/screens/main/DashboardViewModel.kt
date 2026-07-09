package gf.gab.upm.spv.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gf.gab.upm.spv.ble.IBleClient
import gf.gab.upm.spv.ble.MacAddress
import gf.gab.upm.spv.ble.SpvDevice
import gf.gab.upm.spv.ble.SpvDeviceRepository
import gf.gab.upm.spv.ble.spv.SpvDeviceSettings
import gf.gab.upm.spv.ble.spv.SpvEnvironmentalMeasurements
import gf.gab.upm.spv.db.SpvDeviceInfo
import gf.gab.upm.spv.db.SpvDeviceInfoDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sensorRepository: SpvDeviceRepository,
    private val sensorDao: SpvDeviceInfoDao
) : ViewModel() {

    private val currentMeasurements = MutableStateFlow<Map<MacAddress, SpvEnvironmentalMeasurements?>>(mapOf())
    private val currentBatteryLevels = MutableStateFlow<Map<MacAddress, Int?>>(mapOf())
    private var scanJob: Job? = null

    val currentDeviceSettings = MutableStateFlow<DashboardSpvDeviceSettings>(DashboardSpvDeviceSettings.None)
    val currentDeviceInfo = MutableStateFlow<SpvDeviceInfo?>(null)

    val devices: StateFlow<List<DashboardSpvDevice>> = combine(
        sensorRepository.devices,
        currentMeasurements,
        currentBatteryLevels
    ) { devices, measurements, batteryLevels ->
        devices
            .sortedByDescending {
                when (it) {
                    is SpvDevice.Connected -> 2
                    is SpvDevice.Connecting -> 1
                    is SpvDevice.Disconnected -> 0
                }
            }.map { device ->
                when (device) {
                    is SpvDevice.Disconnected -> DashboardSpvDevice.Disconnected(device)
                    is SpvDevice.Connecting -> DashboardSpvDevice.Connecting(device)
                    is SpvDevice.Connected -> DashboardSpvDevice.Connected(
                        device,
                        measurements = measurements[device.info.macAddress],
                        batteryLevel = batteryLevels[device.info.macAddress]
                    )
                }
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(500),
        initialValue = emptyList()
    )

    fun bleStatus(): IBleClient.BleSupport = sensorRepository.bleStatus()

    fun startScan() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            sensorRepository.startAutoReconnect()
        }
    }

    fun stopScan() {
        scanJob?.cancel()
    }

    fun updateDeviceName(macAddress: MacAddress, name: String) {
        viewModelScope.launch {
            sensorDao.updateName(macAddress, name)
        }
    }

    fun updateDeviceSettings(macAddress: MacAddress, settings: SpvDeviceSettings) {
        viewModelScope.launch {
            devices.value.filterIsInstance<DashboardSpvDevice.Connected>()
                .find { macAddress == it.spvDevice.manager.getMacAddress() }
                ?.let { device ->
                    device.spvDevice.manager.writeSettings(settings)
                }
        }
    }

    fun deleteDevice(macAddress: MacAddress) {
        viewModelScope.launch {
            sensorRepository.forget(macAddress)
        }
    }

    fun fetchDeviceInfo(device: DashboardSpvDevice) {
        val macAddress = when(device) {
            is DashboardSpvDevice.Connected -> device.spvDevice.info.macAddress
            is DashboardSpvDevice.Connecting -> device.spvDevice.info.macAddress
            is DashboardSpvDevice.Disconnected -> device.spvDevice.info.macAddress
        }
        viewModelScope.launch {
            currentDeviceInfo.value = sensorDao.get(macAddress)

            devices.value.filterIsInstance<DashboardSpvDevice.Connected>()
                .find { macAddress == it.spvDevice.manager.getMacAddress() }
                ?.let { connectedDevice ->
                    currentDeviceSettings.value = DashboardSpvDeviceSettings.Loading
                    currentDeviceSettings.value =
                        when (val settings = connectedDevice.spvDevice.manager.readSettings()) {
                            is SpvDeviceSettings -> DashboardSpvDeviceSettings.Settings(settings)
                            null -> DashboardSpvDeviceSettings.None
                        }
                }
        }
    }

    fun removeDeviceInfo() {
        currentDeviceInfo.value = null
        currentDeviceSettings.value = DashboardSpvDeviceSettings.None
    }

    init {
        viewModelScope.launch {
            sensorRepository.devices.collect { devices ->
                val connected = devices.filterIsInstance<SpvDevice.Connected>()

                val connectedAddresses = connected.map { it.info.macAddress }.toSet()
                currentMeasurements.update { it.filter { (addr, _) -> addr in connectedAddresses } }

                connected
                    .filter { !currentMeasurements.value.containsKey(it.info.macAddress) }
                    .forEach { device ->
                        currentMeasurements.update { it + (device.info.macAddress to null) }
                        launch {
                            runCatching { device.manager.readEnvironmentalMeasurements() }
                                .onSuccess { measurements ->
                                    currentMeasurements.update { it + (device.info.macAddress to measurements) }
                                }
                        }
                    }
            }
        }

        viewModelScope.launch {
            sensorRepository.devices.collect { devices ->
                val connected = devices.filterIsInstance<SpvDevice.Connected>()

                val connectedAddresses = connected.map { it.info.macAddress }.toSet()
                currentBatteryLevels.update { it.filter { (addr, _) -> addr in connectedAddresses } }

                connected
                    .filter { !currentBatteryLevels.value.containsKey(it.info.macAddress) }
                    .forEach { device ->
                        currentBatteryLevels.update { it + (device.info.macAddress to null) }
                        launch {
                            runCatching { device.manager.readBattery() }
                                .onSuccess { battery ->
                                    currentBatteryLevels.update { it + (device.info.macAddress to battery) }
                                }
                        }
                    }
            }
        }

        viewModelScope.launch {
            while(isActive) {
                try {
                    val devices = sensorRepository.devices.first()
                    devices.filterIsInstance<SpvDevice.Connected>()
                        .forEach { device ->
                            currentBatteryLevels.update { it + (device.info.macAddress to null) }
                            launch {
                                runCatching { device.manager.readBattery() }
                                    .onSuccess { battery ->
                                        if(currentBatteryLevels.value[device.info.macAddress] != battery ) {
                                            currentBatteryLevels.update { it + (device.info.macAddress to battery) }
                                        }
                                    }
                            }
                            launch {
                                runCatching { device.manager.readEnvironmentalMeasurements() }
                                    .onSuccess { measurements ->
                                        if(currentMeasurements.value[device.info.macAddress] != measurements ) {
                                            currentMeasurements.update { it + (device.info.macAddress to measurements) }
                                        }
                                    }
                            }
                        }
                } catch(e: Exception) {
                    if(e is CancellationException) throw e
                }

                delay(60 * 1000)
            }
        }
    }

}

sealed interface DashboardSpvDevice {
    data class Disconnected(val spvDevice: SpvDevice.Disconnected) : DashboardSpvDevice {
        override fun device(): SpvDevice = spvDevice
    }
    data class Connecting(val spvDevice: SpvDevice.Connecting) : DashboardSpvDevice {
        override fun device(): SpvDevice = spvDevice
    }

    data class Connected(
        val spvDevice: SpvDevice.Connected,
        val measurements: SpvEnvironmentalMeasurements?,
        val batteryLevel: Int?
    ) : DashboardSpvDevice {
        override fun device(): SpvDevice = spvDevice
    }

    fun device(): SpvDevice
}

sealed interface DashboardSpvDeviceSettings {
    object None : DashboardSpvDeviceSettings
    object Loading : DashboardSpvDeviceSettings
    data class Settings(val settings: SpvDeviceSettings) : DashboardSpvDeviceSettings
}
