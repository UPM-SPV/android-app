package gf.gab.upm.spv.ble

import gf.gab.upm.spv.ble.uuid.BleIdentifier
import gf.gab.upm.spv.ble.uuid.BleNamespace
import gf.gab.upm.spv.ble.uuid.BleUuid

/**
 * DSL for building BLE scan filters. For a filter to pass, all of its configured members must
 * individually pass. Currently three types of filter types are implemented:
 *
 * - **Name filters**: filter by the device's announced name. If multiple are configured, only
 * one must match for it to be considered valid.
 *
 * * - **MAC address filters**: filter by the device's MAC address. If multiple are configured, only
 * one must match for it to be considered valid.
 *
 * - **Service filters**: use the device's announced services as a filter. If multiple are configured
 * **ALL** must match for the announcement to pass the filter.
 *
 *
 * **NOTE**: Devices are not required to advertise their service list. If filtering by service, make
 * sure that your device does advertise it.
 *
 * ## Example
 * ```
 * // Only devices with the 5 services AND one of the names.
 * scanFilter {
 *  // Filter devices that contain ALL the following services.
 *  services {
 *      uuid(BleUuid("00001805-0000-1000-8000-00805F9B34FB"))
 *      uuid("0000181E-0000-1000-8000-00805F9B34FB") // You may also use a string directly.
 *      uuid(BleNamespace("00000000-0000-1000-8000-00805F9B34FB"), BleIdentifier(0x1820)) // Or use a namespace and an identifier.
 *
 *      uuidStd(BleIdentifier(0x180F))
 *      uuidStd(0x1826) // Same for 16-bit standard identifiers.
 *  }
 *
 *  // Only devices that are either named `Nordic nRF54L15DK` or start with `nRF*`.
 *  name {
 *      exact("Nordic nRF54L15DK")
 *      prefix("nRF")
 *      regex("nRF.*") // Same as previous line
 *  }
 *
 *  // Any device with one of these addresses will match.
 *  mac {
 *      address(MacAddress.fromString("00:11:22:AA:BB:CC"))
 *      address("00:11:22:AA:BB:CC")    // You may also pass a string.
 *  }
 * }
 * ```
 */
fun scanFilter(lambda: BleScanFilterBuilder.() -> Unit): BleScanFilter {
    return BleScanFilterBuilder().apply(lambda).build()
}

/**
 * A filter applied to received BLE advertisements. Only advertisements which match the filter will
 * be considered valid.
 *
 * See [scanFilter] for more details.
 */
data class BleScanFilter(
    val services: BleScanFilterServices,
    val names: BleScanFilterNames,
    val addresses: BleScanFilterMac,
) {
    /**
     * Checks if the given advertisement passes the configured filters.
     */
    fun matches(advertisement: BleAdvertisement): Boolean =
                services.matches(advertisement.services) &&
                names.matches(advertisement.name) &&
                addresses.matches(advertisement.macAddress)
}

/**
 * A set of services the device must advertise to be considered valid.
 *
 * See [scanFilter] for more details.
 */
data class BleScanFilterServices(val services: Set<BleUuid>) {
    /**
     * Checks if the given service list matches the required services.
     */
    fun matches(serviceList: Collection<BleUuid>): Boolean = serviceList.containsAll(services)
}

/**
 * A set of names to filter, the device must advertise at least one to be considered
 * valid.
 *
 * See [scanFilter] for more details.
 */
data class BleScanFilterNames(val patterns: Set<Regex>) {
    fun matches(name: String?): Boolean {
        if(patterns.isEmpty()) {
            return true
        }

        if(name == null) {
            return false
        }

        return patterns.any { it.matches(name)}
    }
}

/**
 * A set of MAC addresses to filter, if an advertisement comes from one of these, it will be
 * considered valid.
 *
 * See [scanFilter] for more details.
 */
data class BleScanFilterMac(val addresses: Set<MacAddress>) {
    fun matches(address: MacAddress): Boolean {
        if(addresses.isEmpty()) {
            return true
        }

        return addresses.any { it == address }
    }
}


@DslMarker annotation class BleScanFilterDsl

@BleScanFilterDsl
class BleScanFilterBuilder {
    private val services = BleScanFilterServicesBuilder()
    private val names = BleScanFilterNamesBuilder()
    private val addresses = BleScanFilterMacBuilder()

    /**
     * Filter by a set of services.
     */
    fun services(lambda: BleScanFilterServicesBuilder.() -> Unit) = services.apply(lambda)

    /**
     * Filter by the device's advertised name.
     */
    fun names(lambda: BleScanFilterNamesBuilder.() -> Unit) = names.apply(lambda)

    /**
     * Filter by the device's MAC address.
     */
    fun mac(lambda: BleScanFilterMacBuilder.() -> Unit) = addresses.apply(lambda)

    fun build(): BleScanFilter = BleScanFilter(services.build(), names.build(), addresses.build())
}

@BleScanFilterDsl
class BleScanFilterServicesBuilder {
    private val services: MutableSet<BleUuid> = mutableSetOf()

    /**
     * Filter by a full 128-bit UUID.
     */
    fun uuid(uuid: BleUuid) {
        services.add(uuid)
    }

    /**
     * Filter by a full 128-bit UUID.
     */
    fun uuid(uuidString: String) {
        uuid(BleUuid(uuidString))
    }

    /**
     * Filter by a UUID constructed from a [gf.gab.upm.spv.ble.uuid.BleNamespace] and a [gf.gab.upm.spv.ble.uuid.BleIdentifier].
     */
    fun uuid(namespace: BleNamespace, identifier: BleIdentifier) {
        uuid(namespace.applyIdentifier(identifier))
    }

    /**
     * Filter by a standard 16-bit UUID.
     */
    fun uuidStd(identifier: BleIdentifier) {
        val uuid = BleNamespace.BT_BASE_UUID.applyIdentifier(identifier)
        services.add(uuid)
    }

    /**
     * Filter by a standard 16-bit UUID.
     */
    fun uuidStd(identifier: Int) = uuidStd(BleIdentifier(identifier))


    fun build(): BleScanFilterServices = BleScanFilterServices(services)
}

@BleScanFilterDsl
class BleScanFilterNamesBuilder {
    private val regexPatterns: MutableSet<Regex> = mutableSetOf()

    /**
     * Filter by an exact name.
     */
    fun exact(name: String) {
        val pattern = Regex.escape(name)
        regex(pattern)
    }

    /**
     * Filter a name that starts with the given string.
     */
    fun prefix(prefix: String) {
        val escapedPrefix = Regex.escape(prefix)
        val pattern = "${escapedPrefix}.*"
        regex(pattern)
    }

    /**
     * Filter a custom regex.
     */
    fun regex(pattern: String) {
        val compiledPattern = Regex(pattern)
        regex(compiledPattern)
    }

    /**
     * Filter a custom regex.
     */
    fun regex(compiledPattern: Regex) = regexPatterns.add(compiledPattern)

    fun build(): BleScanFilterNames = BleScanFilterNames(regexPatterns)
}

@BleScanFilterDsl
class BleScanFilterMacBuilder {
    private val addresses: MutableSet<MacAddress> = mutableSetOf()

    /**
     * Filter by an exact MAC address.
     */
    fun address(macAddress: MacAddress) {
        addresses.add(macAddress)
    }

    /**
     * Filter by an exact MAC address.
     */
    fun address(addressString: String) {
        address(MacAddress.fromString(addressString))
    }


    fun build(): BleScanFilterMac = BleScanFilterMac(addresses)
}
