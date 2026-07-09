package gf.gab.upm.spv.ble

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Class for storing MAC addresses.
 */
@Immutable
@Serializable
@Parcelize
class MacAddress(
    /**
     * MAC address stored as a Byte array.
     */
    private val macBytes: ByteArray
) : Parcelable {

    companion object {
        /**
         * How many bytes should a MAC address contain.
         */
        private const val MAC_LENGTH_BYTES = 6

        /**
         * Tries to parse a MAC address in the hex & colon format.
         *
         * @throws IllegalArgumentException If the given string does not match the required format.
         */
        fun fromString(mac: String): MacAddress {
            val bytes = try {
                mac
                    .split(":")
                    .map { it.toUByte(radix = 16).toByte() }
                    .toByteArray()

            } catch(_: NumberFormatException) {
                throw IllegalArgumentException("MAC string '$mac' is not valid")
            }

            return MacAddress(bytes)
        }

        /**
         * Creates a MAC address from a raw Byte array.
         *
         * @param bytes A byte array with a length of 6.
         *
         * @throws IllegalArgumentException
         */
        fun fromBeBytes(bytes: ByteArray): MacAddress {
            return MacAddress(bytes)
        }

        /**
         * Creates a MAC address from a raw Byte array.
         *
         * @param bytes A byte array with a length of 6.
         *
         * @throws IllegalArgumentException
         */
        fun fromLeBytes(bytes: ByteArray): MacAddress = fromBeBytes(bytes.reversed().toByteArray())
    }


    init {
        if(macBytes.size != MAC_LENGTH_BYTES) {
            throw IllegalArgumentException("MAC address must have $MAC_LENGTH_BYTES bytes")
        }
    }


    /**
     * Returns this MAC address as a Big-endian Byte array.
     */
    fun toBeBytes(): ByteArray = macBytes.clone()

    /**
     * Returns this MAC address as a Little-endian Byte array.
     */
    fun toLeBytes(): ByteArray = macBytes.reversed().toByteArray()


    override fun toString(): String =
        macBytes
            .map { "%02X".format(it) }
            .fold("") { address, byte -> if(address.isEmpty()) byte else "${address}:${byte}"}


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MacAddress

        return macBytes.contentEquals(other.macBytes)
    }

    override fun hashCode(): Int {
        return macBytes.contentHashCode()
    }
}
