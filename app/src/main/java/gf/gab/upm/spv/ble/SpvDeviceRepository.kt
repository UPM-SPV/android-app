package gf.gab.upm.spv.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.update
import android.content.Context
import androidx.compose.ui.graphics.Path.Companion.combine
import androidx.compose.ui.text.style.TextDecoration.Companion.combine
import dagger.hilt.android.qualifiers.ApplicationContext
import gf.gab.upm.spv.ble.spv.SpvAttestationProvider
import gf.gab.upm.spv.config.SpvConfig
import gf.gab.upm.spv.db.SpvDeviceInfo
import gf.gab.upm.spv.db.SpvDeviceInfoDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

@Singleton
class SpvDeviceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bleScanner: SpvBleScanner,
    private val deviceDao: SpvDeviceInfoDao

) {
    data class DeviceConnection(val state: ConnectionState, val manager: SpvBleManager)
    enum class ConnectionState {
        Connecting, Connected
    }
    private val _connections = MutableStateFlow<Map<MacAddress, DeviceConnection>>(mapOf())

    val devices: Flow<List<SpvDevice>> = combine(
        _connections,
        deviceDao.getAllFlow()
    ) { connections, knownDevices ->
        knownDevices.map { deviceInfo ->
            val connection = connections[deviceInfo.macAddress]
            when(connection?.state) {
                null -> SpvDevice.Disconnected(deviceInfo)
                ConnectionState.Connected -> SpvDevice.Connected(deviceInfo, connection.manager)
                ConnectionState.Connecting -> SpvDevice.Connecting(deviceInfo, connection.manager)
            }
        }
    }

    fun bleStatus(): IBleClient.BleSupport = bleScanner.isSupported()

    suspend fun startAutoReconnect() {
        val normalFilter = scanFilter { services { uuid(SpvConfig.ADVERTISE_UUID) } }
        // Also scan for known devices that are still in pairing mode.
        val pairFilter = scanFilter { services { uuid(SpvConfig.ADVERTISE_PAIRING_UUID) } }

        // Handle devices that are already connected:
        bleScanner
            .connectedDevices()
            .forEach { device ->
                if (isConnected(device.macAddress)) return@forEach
                if (deviceDao.getAll().any { it.macAddress == device.macAddress }) {
                    connect(device, autoConnect = true)
                }
            }

        bleScanner
            .scan(normalFilter, pairFilter)
            .collect { device ->
                if (isConnected(device.macAddress)) return@collect
                if (deviceDao.getAll().any { it.macAddress == device.macAddress }) {
                    connect(device, autoConnect = true)
                }
            }
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(device: BluetoothDevice, autoConnect: Boolean = false): Result<Unit> {
        val macAddress = device.macAddress
        val existingConnection = _connections.value[macAddress]
        if(existingConnection != null) {
            return Result.success(Unit)
        }

        val manager = SpvBleManager(context, onDeviceDisconnected = {
            _connections.update { it - device.macAddress }
        })
        val connection = DeviceConnection(ConnectionState.Connecting, manager)
        _connections.update { map ->
            map + (macAddress to connection)
        }

        return runCatching {
            manager.connectAndAwait(device, autoConnect)
            SpvAttestationProvider(manager).attestDevice()
            val connection = DeviceConnection(ConnectionState.Connected, manager)
            _connections.update { map ->
                map + (macAddress to connection)
            }
            manager.setTime(Clock.System.now().epochSeconds)
        }.onFailure { e ->
            if (e is CancellationException) throw e
            _connections.update { map -> map - device.macAddress }
            manager.disconnectAndAwait()
        }
    }

    suspend fun disconnect(address: MacAddress) {
        val connection = _connections.value[address] ?: return
        _connections.update { map ->
            map - address
        }
        connection.manager.disconnectAndAwait()
    }

    suspend fun forget(address: MacAddress) {
        deviceDao.delete(address)
        val connection = _connections.value[address]
        _connections.update { map ->
            map - address
        }
        connection?.manager?.unpairDevice()
        connection?.manager?.disconnectAndAwait()
    }

    fun isConnected(address: MacAddress): Boolean {
        val connection = _connections.value[address] ?: return false
        return connection.state == ConnectionState.Connected
    }
}

val BluetoothDevice.macAddress: MacAddress
    get() = MacAddress.fromString(address)

sealed interface SpvDevice {
    data class Disconnected(val info: SpvDeviceInfo) : SpvDevice { override fun info(): SpvDeviceInfo = info }
    data class Connecting(val info: SpvDeviceInfo, val manager: SpvBleManager) : SpvDevice { override fun info(): SpvDeviceInfo = info }
    data class Connected(val info: SpvDeviceInfo, val manager: SpvBleManager) : SpvDevice { override fun info(): SpvDeviceInfo = info }

    fun info(): SpvDeviceInfo
}
