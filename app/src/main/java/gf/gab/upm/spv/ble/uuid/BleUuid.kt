package gf.gab.upm.spv.ble.uuid

import androidx.compose.runtime.Immutable
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Class representing a full 128-bit BLE UUID. Usually derived from a combination of a [BleIdentifier]
 * and a [BleNamespace].
 */
@Immutable
class BleUuid {

    /**
     * UUID representation.
     */
    private val uuid: Uuid

    /**
     * Creates a new [BleUuid] from a 128-bit [Uuid].
     */
    constructor(uuid: Uuid) {
        this.uuid = uuid
    }

    /**
     * Creates a new [BleUuid] from a raw 128-bit hex & dash UUID.
     *
     * For example: 0000180F-0000-1000-8000-00805F9B34FB
     *
     * @param uuid A hex & dash UUID string.
     *
     * @throws IllegalArgumentException If [uuid] is not properly formatted.
     */
    constructor(uuid: String) {
        val uuid = Uuid.parseHexDash(uuid)
        this.uuid = uuid
    }

    /**
     * Merges [namespace] and [identifier], creating a new [BleUuid].
     */
    constructor(namespace: BleNamespace, identifier: BleIdentifier) {
        this.uuid = namespace.applyIdentifier(identifier).uuid
    }

    /**
     * Returns this value as a Kotlin [Uuid].
     */
    fun toUuid() = uuid

    /**
     * Returns this value as a Java [UUID].
     */
    fun toUUID() = UUID.fromString(this.toString())


    override fun toString(): String = uuid.toHexDashString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleUuid

        return uuid == other.uuid
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }
}
