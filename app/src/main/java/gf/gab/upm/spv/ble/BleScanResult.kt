package gf.gab.upm.spv.ble

/**
 * Contains the result of a BLE scan.
 */
sealed class BleScanResult {

    /**
     * A new device was found.
     */
    data class New(val advertisement: BleAdvertisement): BleScanResult()

    /**
     * Received an advertisement from a previously found device.
     */
    data class Update(val advertisement: BleAdvertisement): BleScanResult()

    /**
     * The device has fallen out of range or is no longer advertising.
     */
    data class Lost(val address: MacAddress): BleScanResult()
}
