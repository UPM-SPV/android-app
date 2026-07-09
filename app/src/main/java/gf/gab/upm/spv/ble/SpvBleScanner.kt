package gf.gab.upm.spv.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.getSystemService
import gf.gab.upm.spv.ble.uuid.BleUuid
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanFilter
import no.nordicsemi.android.support.v18.scanner.ScanResult
import no.nordicsemi.android.support.v18.scanner.ScanSettings

class SpvBleScanner(private val context: Context) {

    companion object {
        private val TAG = this::class.simpleName!!

        /**
         * List of permissions required for scanning.
         */
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    }

    fun scan(vararg scanFilters: BleScanFilter): Flow<BluetoothDevice> = callbackFlow {
        val scanner = BluetoothLeScannerCompat.getScanner()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .setUseHardwareBatchingIfSupported(false)
            .build()

        val filters = scanFilters.flatMap { it.toAndroidFilter() }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                trySend(result.device)
            }

            override fun onBatchScanResults(results: List<ScanResult>) {
                results.forEach { trySend(it.device) }
            }

            override fun onScanFailed(errorCode: Int) {
                close(ScanFailedException(errorCode))
            }
        }

        scanner.startScan(filters, settings, callback)
        awaitClose { scanner.stopScan(callback) }
    }

    fun isSupported(): IBleClient.BleSupport {
        if(!checkBlePermissions()) {
            return IBleClient.BleSupport.NO_PERMISSION
        }

        val bluetoothManager = context.getSystemService<BluetoothManager>() ?: return IBleClient.BleSupport.NOT_SUPPORTED
        val bluetoothAdapter = bluetoothManager.adapter

        if(!bluetoothAdapter.isEnabled) {
            return IBleClient.BleSupport.DISABLED
        }

        return IBleClient.BleSupport.SUPPORTED
    }

    /**
     * Returns all devices which are already connected.
     */
    @SuppressLint("MissingPermission")
    fun connectedDevices(vararg scanFilters: BleScanFilter): List<BluetoothDevice> {
        if(!checkBlePermissions()) {
            return emptyList()
        }

        val bluetoothManager = context.getSystemService<BluetoothManager>() ?: return emptyList()
        val connectedDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)

        val scanFilters = scanFilters.toList()
        return connectedDevices
            .filter { device -> scanFilters.isEmpty() || scanFilters.any { filter -> filter.matchesDevice(device) }}
            .toList()
    }

    /**
     * Check whether the required permissions have been granted.
     */
    private fun checkBlePermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}


/**
 * Converts a [BleScanFilter] to an Android filter.
 *
 * Only converts the services part.
 */
private fun BleScanFilter.toAndroidFilter(): List<ScanFilter> {

    if(!this.addresses.addresses.isEmpty()) {
        // Each Android ScanFilter only accepts a single MAC address.
        return this.addresses.addresses.map { address ->
            val builder = ScanFilter.Builder()

            builder.setDeviceAddress(address.toString())
            this.services.services.forEach { service ->
                builder.setServiceUuid(
                    ParcelUuid.fromString(service.toUuid().toHexDashString())
                )
            }

            builder.build()
        }.toList()
    }

    val builder = ScanFilter.Builder()

    this.services.services.forEach { service ->
        builder.setServiceUuid(
            ParcelUuid.fromString(service.toUuid().toHexDashString())
        )
    }

    return listOf(builder.build())
}

/**
 * Converts an Android [ScanResult] into a [BleAdvertisement].
 */
private fun bleAdvertisementFromAndroid(context: Context, scanResult: ScanResult): BleAdvertisement =
    BleAdvertisement(
        macAddress = MacAddress.fromString(scanResult.device.address),
        name = scanResult.scanRecord?.deviceName,
        services = scanResult.scanRecord?.serviceUuids?.map { BleUuid(it.toString()) }?.toSet()
            ?: setOf(),
        rssi = BleRssi(scanResult.rssi),

        device = scanResult.device,
    )


@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
private fun BleScanFilter.matchesDevice(device: BluetoothDevice): Boolean {
    val advertisement = BleAdvertisement(
        macAddress = MacAddress.fromString(device.macAddress.toString()),
        name = device.name,
        services = device.uuids.map { BleUuid(it.toString()) }.toSet(),
        rssi = BleRssi(0),
        device = device,
    )

    return this.matches(advertisement)
}

class ScanFailedException(errorCode: Int) :
    Exception("BLE scan failed with error code $errorCode")
