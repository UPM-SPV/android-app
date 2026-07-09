package gf.gab.upm.spv.ble.uuid

import androidx.compose.runtime.Immutable
import kotlin.uuid.Uuid

/**
 * Class representing an incomplete 128-bit UUID, able to be subnetted into other UUIDs via the use
 * of a [BleIdentifier].
 *
 * For example, the Bluetooth Base UUID namespace: 0000xxxx-0000-1000-8000-00805F9B34FB.
 */
@Immutable
class BleNamespace {

    companion object {
        /**
         * Specifies which bytes are to be used for the identifier part assuming **Big Endian**
         * ordering.
         *
         * For reference (using [BT_BASE_UUID]):
         *
         * ```
         * 00 00 xx xx - 00 00 - 10 00 - 80 00 - 00 80 5F 9B 34 FB
         * ^^ ^^ ^^ ^^   ^^ ^^   ^^ ^^   ^^ ^^   ^^ ^^ ^^ ^^ ^^ ^^
         * 0  1  2  3    4  5    6  7    8  9    10 11 12 13 14 15
         * ```
         */
        private val NAMESPACE_BYTES = intArrayOf(2, 3)

        /**
         * Bluetooth Base UUID.
         *
         * `0000xxxx-0000-1000-8000-00805F9B34FB`
         */
        val BT_BASE_UUID = BleNamespace(Uuid.parseHexDashOrNull("00000000-0000-1000-8000-00805F9B34FB")!!)
    }


    /**
     * Namespace base.
     */
    private val base: Uuid

    /**
     * Creates a new namespace from a base UUID.
     *
     * The UUID must be in the format xxxx0000-xxxx-xxxx-xxxx-xxxxxxxxxxxx, where `x` is any hex
     * digit, and `0` *must* be zero.
     *
     * @param base Namespace base, in the format: xxxx0000-xxxx-xxxx-xxxx-xxxxxxxxxxxx.
     *
     * @throws IllegalArgumentException If the [base] parameter does not contain the required `0`
     * values.
     */
    constructor(base: Uuid) {
        if(!verifyNamespaceZeros(base)) {
            throw IllegalArgumentException("UUID ${base.toHexDashString()} is not properly zeroed")
        }

        this.base = base
    }

    /**
     * Creates a new namespace from a UUID string.
     *
     * The UUID must be in a hex & dash format, and should match the following schema:
     * xxxx0000-xxxx-xxxx-xxxx-xxxxxxxxxxxx.
     *
     * @param base Namespace base, in the format: xxxx0000-xxxx-xxxx-xxxx-xxxxxxxxxxxx.
     *
     * @throws IllegalArgumentException If [base] is not a hex-dash UUID or is not properly zeroed.
     */
    constructor(base: String) {
        val uuid = Uuid.parseHexDash(base)
        if(!verifyNamespaceZeros(uuid)) {
            throw IllegalArgumentException("UUID ${uuid.toHexDashString()} is not properly zeroed")
        }

        this.base = uuid
    }

    /**
     * Obtain the namespace as a UUID object.
     */
    fun toUuid(): Uuid = base

    /**
     * Converts the namespace into a byte array.
     */
    fun toByteArray(): ByteArray = base.toByteArray()

    /**
     * Replace the 16-bit identifier part of the namespace with the given UUID, returning a full
     * 128-bit UUID.
     *
     * For example, take the two following values:
     *
     * | Name           | Value                                    |
     * | -------------- | ---------------------------------------- |
     * | **Namespace**  | 00000000-0000-1000-8000-00805F9B34FB     |
     * | **Identifier** | 0xF080                                   |
     * | **Result**     | 0000**F080**-0000-1000-8000-00805F9B34FB |
     */
    fun applyIdentifier(uuid16: BleIdentifier): BleUuid {
        val uuidBytes = this.toByteArray()
        val uuid16Bytes = uuid16.toBigEndianByteArray()

        NAMESPACE_BYTES.zip(uuid16Bytes.toList()).forEach { (index, byte) ->
            uuidBytes[index] = byte
        }

        val uuid = Uuid.fromByteArray(uuidBytes)
        return BleUuid(uuid)
    }

    /**
     * Same as [applyIdentifier(BleIdentifier)].
     */
    fun applyIdentifier(uuid16: UShort): BleUuid = applyIdentifier(BleIdentifier(uuid16))

    /**
     * Same as [applyIdentifier(BleIdentifier)].
     */
    fun applyIdentifier(uuid16: Int): BleUuid = applyIdentifier(BleIdentifier(uuid16))



    /**
     * Check that the given UUID contains zeroes in the required bytes.
     */
    private fun verifyNamespaceZeros(uuid: Uuid): Boolean {
        val bytes = uuid.toByteArray()
        return NAMESPACE_BYTES.all { bytes[it] == 0.toByte() }
    }


    override fun toString(): String = base.toHexDashString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleNamespace

        return base == other.base
    }

    override fun hashCode(): Int {
        return base.hashCode()
    }
}
