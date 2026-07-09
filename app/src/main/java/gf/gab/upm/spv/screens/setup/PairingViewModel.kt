package gf.gab.upm.spv.screens.setup

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.protobuf.InvalidProtocolBufferException
import dagger.hilt.android.lifecycle.HiltViewModel
import gf.gab.upm.spv.ble.IBleClient
import gf.gab.upm.spv.ble.SpvBleManager
import gf.gab.upm.spv.ble.SpvBleScanner
import gf.gab.upm.spv.ble.macAddress
import gf.gab.upm.spv.ble.scanFilter
import gf.gab.upm.spv.ble.spv.SpvAttestationProvider
import gf.gab.upm.spv.config.SpvConfig
import gf.gab.upm.spv.db.SpvDeviceInfo
import gf.gab.upm.spv.db.SpvDeviceInfoDao
import gf.gab.upm.spv.nfc.INfcScanner
import gf.gab.upm.spv.nfc.INtag
import gf.gab.upm.spv.nfc.SpvTagData
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import no.nordicsemi.android.ble.ktx.suspend
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val scanner: SpvBleScanner,
    private val deviceDao: SpvDeviceInfoDao,
    private val manager: SpvBleManager
) : ViewModel() {

    companion object {
        private const val CONNECT_RETRY = 3
    }

    private val _uiState = MutableStateFlow<PairingUiState>(PairingUiState.NfcWaiting())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()
    var nfcJob: Job? = null

    /*
     * User interactions.
     */
    fun scanNfc(scanner: INfcScanner) {
        if(_uiState.value !is PairingUiState.NfcWaiting &&
            _uiState.value !is PairingUiState.NfcSupportError) return

        val scanSupported = scanner.isSupported()
        if(scanSupported != INfcScanner.NfcSupport.SUPPORTED) {
            _uiState.value = PairingUiState.NfcSupportError(scanSupported)
            return
        }

        _uiState.value = PairingUiState.NfcWaiting()

        nfcJob?.cancel()
        nfcJob = viewModelScope.launch {
            scanner.startNtagScan()
                .catch { _uiState.value = PairingUiState.NfcWaiting(PairingUiState.NfcTagError.TagReadingError) }
                .collect { tag ->
                    _uiState.value = PairingUiState.NfcReading
                    processTag(tag)
                }
        }
    }

    fun startBleScan() {
        val tagData = when(val state = _uiState.value) {
            is PairingUiState.BleAttestationError -> state.nfcData
            is PairingUiState.BleConnectionError -> state.nfcData
            is PairingUiState.BleSupportError -> state.nfcData
            else -> return
        }
        viewModelScope.launch {
            startScanning(tagData)
        }
    }


    /*
     * Internal methods.
     */

    private suspend fun processTag(tag: INtag) {
        val tagData = try {
            SpvTagData.tryDecodeFromNTag(tag).also { tag.close() }
        } catch(cancellationException: CancellationException) {
                throw cancellationException
        } catch(_: InvalidProtocolBufferException) {
            _uiState.value = PairingUiState.NfcWaiting(PairingUiState.NfcTagError.UnknownTag)
            return
        } catch(_: Exception) {
            _uiState.value = PairingUiState.NfcWaiting(PairingUiState.NfcTagError.TagReadingError)
            return
        }
        nfcJob?.cancel()
        viewModelScope.launch { startScanning(tagData) }
    }

    private suspend fun startScanning(tagData: SpvTagData) {
        val support = scanner.isSupported()
        if(support != IBleClient.BleSupport.SUPPORTED) {
            _uiState.value = PairingUiState.BleSupportError(tagData, support)
        }

        _uiState.value = PairingUiState.BleScanning(tagData)

        val device = try {
            scanner.scan(scanFilter {
                services { uuid(SpvConfig.ADVERTISE_PAIRING_UUID) }
               mac { address(tagData.macAddress) }
            })
            .first()
        } catch(cancellationException: CancellationException) {
            throw cancellationException
        } catch(_: Exception) {
            _uiState.value = PairingUiState.BleConnectionError(tagData)
            return
        }
        connectDevice(tagData, device)
    }

    private suspend fun connectDevice(tagData: SpvTagData, device: BluetoothDevice) {
        _uiState.value = PairingUiState.BleConnecting(tagData)

        var retry = 0
        while(true) {
            try {
                manager.connectAndAwait(device, false)
                break
            } catch(cancellationException: CancellationException) {
                throw cancellationException
            } catch(_: Exception) {
                if(retry >= CONNECT_RETRY) {
                    _uiState.value = PairingUiState.BleConnectionError(tagData)
                }
            }
            retry++
        }

        pairDevice(tagData)
    }

    private suspend fun pairDevice(tagData: SpvTagData) {
        _uiState.value = PairingUiState.BlePairing(tagData)
        try {
            if(!manager.isPaired()) {
                manager.pairDevice()
            }
        } catch(cancellationException: CancellationException) {
            throw cancellationException
        } catch(_: Exception) {
            ensureBleDisconnected()
            _uiState.value = PairingUiState.BleConnectionError(tagData)
            return
        }
        attestDevice(tagData)
    }

    private suspend fun attestDevice(tagData: SpvTagData) {
        _uiState.value = PairingUiState.BleAttesting(tagData)

        try {
            SpvAttestationProvider(manager).attestDevice()
        } catch(cancellationException: CancellationException) {
            throw cancellationException
        } catch(_: Exception) {
            ensureBleDisconnected()
            _uiState.value = PairingUiState.BleAttestationError(tagData)
            return
        }

        try {
            val device = manager.bluetoothDevice!!
            val serialNumber = manager.getSerialNumber()
            deviceDao.insert(
                SpvDeviceInfo(
                    macAddress = device.macAddress,
                    friendlyName = serialNumber,
                    serialNumber = serialNumber
                )
            )
        } catch(cancellationException: CancellationException) {
            throw cancellationException
        } catch(_: Exception) {
            ensureBleDisconnected()
            _uiState.value = PairingUiState.BleConnectionError(tagData)
            return
        }

        manager.disconnect().suspend()
        _uiState.value = PairingUiState.Done
    }

    private fun ensureBleDisconnected() {
        runCatching { manager.disconnect().enqueue() }
    }
}

sealed interface PairingUiState {

    sealed interface NfcTagError {
        data object UnknownTag : NfcTagError
        data object TagReadingError : NfcTagError
    }
    data class NfcWaiting(val error: NfcTagError? = null) : PairingUiState
    data object NfcReading : PairingUiState
    data class NfcSupportError(val errorReason: INfcScanner.NfcSupport) : PairingUiState


    data class BleSupportError(val nfcData: SpvTagData, val errorReason: IBleClient.BleSupport) : PairingUiState
    data class BleScanning(val nfcData: SpvTagData) : PairingUiState
    data class BleConnecting(val nfcData: SpvTagData) : PairingUiState
    data class BlePairing(val nfcData: SpvTagData) : PairingUiState
    data class BleConnectionError(val nfcData: SpvTagData) : PairingUiState

    data class BleAttesting(val nfcData: SpvTagData) : PairingUiState
    data class BleAttestationError(val nfcData: SpvTagData) : PairingUiState

    // Done States
    data object Done : PairingUiState
}
