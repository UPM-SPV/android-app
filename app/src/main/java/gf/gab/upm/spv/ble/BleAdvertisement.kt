package gf.gab.upm.spv.ble

import android.bluetooth.BluetoothDevice
import gf.gab.upm.spv.ble.uuid.BleUuid

/**
 * Class containing advertisement information from a device.
 */
data class BleAdvertisement(
    /**
     * The device's MAC address.
     */
    val macAddress: MacAddress,

    /**
     * Advertised device name.
     */
    val name: String?,

    /**
     * Services advertised by the device.
     */
    val services: Set<BleUuid>,

    /**
     * Device's signal level.
     */
    val rssi: BleRssi,

    /**
     * Handle to the device itself. Used for connecting.
     */
    val device: BluetoothDevice
)
