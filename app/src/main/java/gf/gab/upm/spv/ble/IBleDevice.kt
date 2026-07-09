package gf.gab.upm.spv.ble

import kotlinx.coroutines.flow.StateFlow

/**
 * Represents a specific BLE device, providing operations to connect, disconnect, bond and
 * interact with its GATT server.
 */
interface IBleDevice {

    /**
     * Device's MAC address.
     */
    val macAddress: MacAddress

    /**
     * Attempts to connect to the device.
     * Suspends until the connection is completed or fails.
     *
     * Automatically discovers the device's services.
     *
     * Does nothing if already connected.
     *
     * @param autoBond Set to true to automatically bond with the device after connecting.
     *
     * @throws Exception On connection error, timeout or bonding if enabled.
     */
    suspend fun connect(autoBond: Boolean = false)

    /**
     * Disconnects from the device. Does nothing if not connected.
     */
    suspend fun disconnect()
}
