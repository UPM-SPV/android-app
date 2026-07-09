package gf.gab.upm.spv.nfc

import kotlinx.coroutines.flow.Flow

/**
 * Interface for interacting with NFC scanners.
 */
interface INfcScanner {

    /**
     * Informs the device's NFC reader to scan for NTAG tags. Scanning will automatically stop
     * once the Flow is dropped.
     */
    fun startNtagScan(): Flow<INtag>

    /**
     * Returns whether the device currently supports NFC.
     */
    fun isSupported(): NfcSupport

    enum class NfcSupport {
        /**
         * Fully supported.
         */
        SUPPORTED,
        /**
         * Supported, permission granted, but disabled by the user.
         */
        DISABLED,
        /**
         * Permission denied or not granted by the user.
         */
        NO_PERMISSION,
        /**
         * The device has no NFC scanner.
         */
        NOT_SUPPORTED
    }

}

/**
 * Interface for interacting with NFC NTAG-family devices.
 */
interface INtag {
    companion object {
        const val PAGE_SIZE = 4
    }

    /**
     * Closes the connection to the tag.
     */
    suspend fun close()

    /**
     * Reads a 4-byte page from the tag.
     */
    suspend fun readPage(pageNumber: Int): ByteArray

    /**
     * Translates the given byte range into a page-compatible map, reads the tag
     * and returns a [ByteArray] with the requested bytes.
     */
    suspend fun dumpRange(byteStart: Int, byteEnd: Int): ByteArray {
        val startPage = byteStart / PAGE_SIZE
        val endPageInclusive = byteEnd / PAGE_SIZE

        val fullByteDump = (startPage..endPageInclusive)
            .map { this.readPage(it) }
            .fold(byteArrayOf()) { previous, next -> previous.plus(next) }

        val startOffset = byteStart % PAGE_SIZE
        val endOffset = (byteEnd - byteStart + startOffset)

        return fullByteDump.sliceArray(startOffset..endOffset)
    }

    /**
     * Same as [dumpRange] but specifying the start and length.
     */
    suspend fun dumpBytes(byteStart: Int, size: Int) = dumpRange(byteStart, byteStart + size - 1)

    /**
     * See [dumpRange(Int,Int)].
     */
    suspend fun dumpRange(byteRange: ClosedRange<Int>): ByteArray = dumpRange(byteRange.start, byteRange.endInclusive)
}
