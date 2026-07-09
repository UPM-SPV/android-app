package gf.gab.upm.spv.nfc

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.MifareUltralight
import android.os.Bundle
import android.util.Log
import gf.gab.upm.spv.util.logDebug
import gf.gab.upm.spv.util.logError
import gf.gab.upm.spv.util.logInfo
import gf.gab.upm.spv.util.logVerbose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class AndroidNfcScanner : INfcScanner {

    private val activity: Activity

    /**
     * Builds an [AndroidNfcScanner], requires an [Activity] to interact with the system APIs.
     */
    constructor(activity: Activity) {
        this.activity = activity
    }

    override fun startNtagScan(): Flow<INtag> = callbackFlow {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        val callback = NfcAdapter.ReaderCallback { tag ->
            MifareUltralight.get(tag)?.let { ultralight ->
                ultralight.connect()
                trySend(AndroidNtag(ultralight))
            }
        }
        val options = Bundle().apply {
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
        }
        nfcAdapter.enableReaderMode(
            activity,
            callback,
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            options
        )

        logInfo("Starting NFC scan")

        awaitClose {
            logInfo("Stopping NFC scan")
            nfcAdapter.disableReaderMode(activity)
        }
    }

    override fun isSupported(): INfcScanner.NfcSupport {
        logVerbose("Checking NFC support")
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)

        // Android has no dedicated NFC permission, no need to check.

        if(nfcAdapter == null) {
            logVerbose("    NFC not supported")
            return INfcScanner.NfcSupport.NOT_SUPPORTED
        }

        if(!nfcAdapter.isEnabled) {
            logVerbose("    NFC disabled")
            return INfcScanner.NfcSupport.DISABLED
        }

        logVerbose("    NFC supported and enabled")
        return INfcScanner.NfcSupport.SUPPORTED
    }
}

class AndroidNtag : INtag {

    /**
     * NTAGs and Mifare Ultralights share the same binary interface.
     */
    private val ultralightTag: MifareUltralight

    /**
     * Creates an [AndroidNtag] from a [MifareUltralight] object.
     */
    constructor(ultralight: MifareUltralight) {
        this.ultralightTag = ultralight
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        logDebug("Closing NFC tag")
        ultralightTag.close()
    }

    override suspend fun readPage(pageNumber: Int): ByteArray = withContext(Dispatchers.IO) {
        logVerbose("Reading NFC page $pageNumber [${pageNumber * INtag.PAGE_SIZE}, ${(pageNumber + 1) * INtag.PAGE_SIZE}")
        try {
            return@withContext ultralightTag.readPages(pageNumber).sliceArray(0..<INtag.PAGE_SIZE)
        } catch (e: TagLostException) {
            logError("NFC tag lost during reading")
            if (!isActive) {
                throw CancellationException("Coroutine cancelled during NFC TagLost event", e)
            }

            throw IOException("Tag lost during read", e)

        } catch (e: Exception) {
            logError("NFC tag read failure")
            if (!isActive) {
                throw CancellationException("Coroutine cancelled during NFC read failure", e)
            }
            throw IOException("Failed to read NFC page", e)
        }
    }
}
