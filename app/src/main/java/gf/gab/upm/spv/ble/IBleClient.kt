package gf.gab.upm.spv.ble

import kotlinx.coroutines.flow.Flow

/**
 * An [IBleClient] opens access to the device's native BLE stack.
 *
 * Scanning allows discovering scannable (advertising) devices. The scanner will return information
 * on found devices, which can be filtered depending on the contents of the advertisement, the
 * signal strength and device address.
 *
 * After receiving an advertisement, a connection or bond to the device may be performed. See
 * [BleScanResult] for more information.
 */
interface IBleClient {

    /**
     * Starts scanning for devices.
     *
     * Will return a flow of [BleScanResult], which may be tagged as [BleScanResult.New],
     * [BleScanResult.Update] or [BleScanResult.Lost].
     *
     * The scan will automatically stop when the flow is closed.
     *
     * @param scanFilters A list of BLE filters. Only devices which match at least one will be returned.
     * Multiple filters are effectively OR'd together. See [scanFilter] for information on how to
     * configure filters. May be required depending on implementation.
     *
     * @throws Exception If permissions are missing, or if an error was encountered when scanning.
     * Exceptions should be caught using the Flow catch method.
     */
    fun scan(vararg scanFilters: BleScanFilter): Flow<BleScanResult>

    /**
     * Returns all recently found devices retrieved by [scan].
     */
    fun scannedDevices(): Set<IBleDevice>

    /**
     * Returns if the device supports BLE.
     */
    fun isSupported(): BleSupport

    /**
     * This device's support status for BLE.
     */
    enum class BleSupport {
        /**
         * Bluetooth is fully supported.
         */
        SUPPORTED,

        /**
         * Bluetooth is supported, but currently disabled.
         */
        DISABLED,

        /**
         * The app does not have the required permissions to use Bluetooth.
         */
        NO_PERMISSION,

        /**
         * The device does not support Bluetooth.
         */
        NOT_SUPPORTED
    }
}
