package gf.gab.upm.spv.ble

import androidx.compose.runtime.Immutable

/**
 * Represents a device's received signal strength indicator.
 */
@Immutable
class BleRssi {

    companion object {
        /**
         * Valid RSSI values.
         */
        private val DBM_VALID_RANGE = -100..0
    }

    /**
     * Signal level in dBm.
     */
    val dBm: Int

    /**
     * Creates a [BleRssi] object from an integer.
     *
     * @param dBm Signal level in dBm. Must be between 0 and -100.
     *
     * @throws IllegalArgumentException If [dBm] is outside the valid range.
     */
    constructor(dBm: Int) {
        if(dBm !in DBM_VALID_RANGE) {
            throw IllegalArgumentException("RSSI value $dBm must be in the range $DBM_VALID_RANGE")
        }

        this.dBm = dBm
    }


    override fun toString(): String = dBm.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleRssi

        return dBm == other.dBm
    }

    override fun hashCode(): Int {
        return dBm
    }
}
