package gf.gab.upm.spv.nfc

import NfcTagOuterClass.NfcTag
import com.google.protobuf.InvalidProtocolBufferException
import gf.gab.upm.spv.ble.MacAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Stores an SPV tag's data.
 */
data class SpvTagData(
    /**
     * Device MAC address.
     */
    val macAddress: MacAddress,
    /**
     * Device serial number.
     */
    val serialNumber: String
) {
    companion object {
        private val LEN_TAG_LOCATION = 16..17
        private val DATA_START_LOCATION = 18
        private val MAGIC = 0x1A60DA1A


        /**
         * Try to read a connected tag's data and parse it.
         *
         * @throws com.google.protobuf.InvalidProtocolBufferException When parsing fails.
         */
        suspend fun tryDecodeFromNTag(tag: INtag): SpvTagData {
            val len = ByteBuffer
                .wrap(tag.dumpRange(LEN_TAG_LOCATION))
                .order(ByteOrder.LITTLE_ENDIAN)
                .short

            val protoBytes = tag.dumpBytes(DATA_START_LOCATION, len.toInt())
            val protoData = NfcTag.parseFrom(protoBytes)

            if(protoData.magic != MAGIC) {
                throw InvalidProtocolBufferException("Invalid magic number")
            }

            val macAddress = MacAddress.fromLeBytes(protoData.macAddress.toByteArray())
            val serialNumber = protoData.serialNumber

            return SpvTagData(macAddress, serialNumber)
        }
    }
}
