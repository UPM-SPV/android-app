package gf.gab.upm.spv.ble.uuid

import androidx.compose.runtime.Immutable

/**
 * Represents a 16-bit BLE UUID which identifies a service or attribute inside a particular
 * [BleNamespace].
 */
@Immutable
class BleIdentifier {

    companion object {
        private val VALID_RANGE = 0x0000..0xFFFF
    }

    /**
     * 16-bit identifier
     */
    private val identifier: UShort


    /**
     * Create a new [BleIdentifier] from a 16-bit unsigned integer.
     */
    constructor(identifier: UShort) {
        this.identifier = identifier
    }

    /**
     * Create a new [BleIdentifier] from an integer, which must be **between 0 and 65535**
     * (`0x0000` to `0xFFFF`).
     *
     * @param identifier Integer value between `0x0000` and `0xFFFF` (65535).
     *
     * @throws IllegalArgumentException If `identifier` is not in the valid range.
     */
    constructor(identifier: Int) {
        if(identifier !in VALID_RANGE) {
            throw IllegalArgumentException(
                "Identifier ${"%04X".format(identifier)} is not in the valid range $VALID_RANGE"
            )
        }

        this.identifier = identifier.toUShort()
    }

    /**
     * Return this value as a [UShort].
     */
    fun toUShort(): UShort = identifier

    /**
     * Replace a namespace's identifier part with this value.
     *
     * Symmetrical to [BleNamespace.applyIdentifier], see that function for more details.
     */
    fun applyNamespace(namespace: BleNamespace): BleUuid = namespace.applyIdentifier(this)

    /**
     * Convert the identifier into a 2-Byte array, where the most significant byte comes first.
     *
     * For example, the value `0x11FF` becomes `[0x11, 0xFF]`.
     */
    fun toBigEndianByteArray(): ByteArray {
        val bytes = ByteArray(2)
        val intIdentifier = identifier.toInt()
        bytes[0] = (intIdentifier shr 8).toByte()
        bytes[1] = (intIdentifier and 0x00FF).toByte()

        return bytes
    }

    /**
     * Convert the identifier into a 2-Byte array, where the least significant byte comes first.
     *
     * For example, the value `0x11FF` becomes `[0xFF, 0x11]`.
     */
    fun toLittleEndianByteArray(): ByteArray {
        val bytes = this.toBigEndianByteArray()
        bytes.reverse()

        return bytes
    }


    override fun toString(): String = "%04X".format(identifier)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleIdentifier

        return identifier == other.identifier
    }

    override fun hashCode(): Int {
        return identifier.hashCode()
    }
}
