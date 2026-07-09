package gf.gab.upm.spv.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothSocket
import android.content.Context
import gf.gab.upm.spv.ble.spv.ISpvAttestationEndpoint
import gf.gab.upm.spv.ble.spv.ISpvDisEndpoint
import gf.gab.upm.spv.ble.spv.ISpvEnvironmentalEndpoint
import gf.gab.upm.spv.ble.spv.SpvBleAttestationService
import gf.gab.upm.spv.ble.spv.SpvBleSensorService
import gf.gab.upm.spv.ble.spv.SpvBleSettingsService
import gf.gab.upm.spv.ble.spv.SpvBleTimeService
import gf.gab.upm.spv.ble.spv.SpvBleEnvironmentalService
import gf.gab.upm.spv.ble.spv.SpvBleBatteryService
import gf.gab.upm.spv.ble.spv.SpvBleDeviceInformationService
import gf.gab.upm.spv.ble.spv.SpvDataRequest
import gf.gab.upm.spv.ble.spv.SpvDeviceSettings
import gf.gab.upm.spv.ble.spv.SpvEnvironmentalMeasurements
import gf.gab.upm.spv.ble.spv.SpvFsSummary
import gf.gab.upm.spv.ble.spv.SpvSdsMetadata
import gf.gab.upm.spv.ota.OtaVersion
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.ktx.suspend
import no.nordicsemi.android.ble.observer.ConnectionObserver
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Represents a connection to an SPV device.
 *
 * The workflow for making use of this class is:
 *  - Wait for connection.
 *  - Bond to device. (if not previously done)
 *  - Perform attestation.
 *  - Use services.
 *
 * @note User code *must* call [gf.gab.upm.spv.ble.spv.SpvAttestationProvider.attestDevice] before using any other service. Not doing so will
 * result in service methods throwing [SecurityException].
 */
class SpvBleManager(context: Context, onDeviceDisconnected: () -> Unit = {}) :
    BleManager(context), ISpvAttestationEndpoint, ISpvDisEndpoint, ISpvEnvironmentalEndpoint {

    companion object {
        private const val DEFAULT_MTU = 517
        private const val CONNECTION_RETRIES = 3
        private const val CONNECTION_RETRY_DELAY_MS = 5_000
        private const val CONNECTION_TIMEOUT_MS: Long = 15_000
    }

    /**
     * Stores whether the device passed attestation.
     */
    private var isAuthentic: Boolean = false

    private var sensorService: SpvBleSensorService? = null
    private var attestationService: SpvBleAttestationService? = null
    private var timeService: SpvBleTimeService? = null
    private var settingsService: SpvBleSettingsService? = null
    private var deviceInformationService: SpvBleDeviceInformationService? = null
    private var batteryService: SpvBleBatteryService? = null
    private var environmentalService: SpvBleEnvironmentalService? = null

    init {
        setConnectionObserver(object : ConnectionObserver {
            override fun onDeviceConnecting(p0: BluetoothDevice) {}
            override fun onDeviceConnected(p0: BluetoothDevice) {}
            override fun onDeviceFailedToConnect(p0: BluetoothDevice, p1: Int) {}
            override fun onDeviceReady(p0: BluetoothDevice) {}
            override fun onDeviceDisconnecting(p0: BluetoothDevice) {}
            override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
                onDeviceDisconnected()
            }
        })
    }

    /**
     * Attempts to connect to the device.
     */
    suspend fun connectAndAwait(device: BluetoothDevice, autoConnect: Boolean = true) {
        connect(device)
            .retry(CONNECTION_RETRIES, CONNECTION_RETRY_DELAY_MS)
            .timeout(CONNECTION_TIMEOUT_MS)
            .useAutoConnect(autoConnect)
            .suspend()
    }

    suspend fun disconnectAndAwait() {
        runCatching {
            disconnect().then { close() }.suspend()
        }
    }

    fun isPaired(): Boolean = isBonded

    suspend fun pairDevice() {
        ensureBond().suspend()
    }

    suspend fun unpairDevice() {
        removeBond().suspend()
    }

    /**
     * In charge of verifying (and initializing) if the device supports the required GATT services.
     */
    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
        sensorService = SpvBleSensorService.tryFromGatt(gatt) ?: return false
        attestationService = SpvBleAttestationService.tryFromGatt(gatt) ?: return false
        timeService = SpvBleTimeService.tryFromGatt(gatt) ?: return false
        settingsService = SpvBleSettingsService.tryFromGatt(gatt) ?: return false
        deviceInformationService = SpvBleDeviceInformationService.tryFromGatt(gatt) ?: return false
        environmentalService = SpvBleEnvironmentalService.tryFromGatt(gatt) ?: return false
        batteryService = SpvBleBatteryService.tryFromGatt(gatt) ?: return false

        return true
    }

    override fun initialize() {
        requestMtu(DEFAULT_MTU).enqueue()
    }

    override fun onServicesInvalidated() {
        sensorService = null
        attestationService = null
        timeService = null
        settingsService = null
        deviceInformationService = null
        environmentalService = null
        batteryService = null

        isAuthentic = false
    }

    /*
     *
     * Service methods.
     *
     */

    /*
     * Device information service.
     */
    suspend fun getSerialNumber(): String {
        val serialNumberChr = deviceInformationService!!.serialNumber
        val serialNumber = readCharacteristic(
            serialNumberChr,
        ).suspend()

        return String(serialNumber.value!!, Charsets.UTF_8)
    }

    suspend fun getFirmwareVersion(): OtaVersion? {
        val fwVersionChr = deviceInformationService?.fwVersion
        return readCharacteristic(fwVersionChr)
            .suspend()
            .value?.let { String(it, Charsets.UTF_8) }
            ?.let { OtaVersion.tryParseSemver(it) }
    }

    /*
     * Attestation service.
     */
    override suspend fun readCertificate(): X509Certificate? {
        val attestationService = attestationService ?: return null
        val certRaw = readCharacteristic(attestationService.certificate)
            .suspend()
            .value ?: return null
        val certFactory = CertificateFactory.getInstance("X.509")
        return certFactory.generateCertificate(certRaw.inputStream()) as X509Certificate
    }

    override suspend fun enableChallengeResponseIndication() {
        enableIndications(attestationService?.response).suspend()
    }

    override suspend fun waitForChallengeResponse(timeoutMs: Long): ByteArray? {
        val attestationService = attestationService ?: return null

        return waitForIndication(attestationService.response)
            .timeout(timeoutMs)
            .suspend()
            .value
    }

    override suspend fun writeChallenge(challenge: ByteArray) {
        val attestationService = attestationService ?: return
        writeCharacteristic(
            attestationService.challenge,
            challenge,
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        ).suspend()
    }

    override fun getMacAddress(): MacAddress? {
        val macString = bluetoothDevice?.address ?: return null
        return MacAddress.fromString(macString)
    }

    override fun markAuthenticated(isAuthenticated: Boolean) {
        this.isAuthentic = isAuthenticated
    }

    /*
     * Device Information Service
     */
    override suspend fun readSerialNumber(): String? {
        return readCharacteristic(deviceInformationService?.serialNumber)
            .suspend()
            .value
            ?.toString(Charsets.UTF_8)
    }

    /*
     * Environmental Measurement Service.
     */
    override suspend fun readEnvironmentalMeasurements(): SpvEnvironmentalMeasurements? {
        ensureAttested()
        val temperature = environmentalService?.temperature
        val humidity = environmentalService?.humidity
        val luminosity = environmentalService?.luminosity
        val noise = environmentalService?.noise
        val vocRaw = environmentalService?.vocRaw

        return SpvEnvironmentalMeasurements(
            temperatureCelsius = readCharacteristicOrNull(temperature)
                ?.getIntValue(Data.FORMAT_SINT16_LE, 0)
                ?.let { it.toDouble() / 100 },
            humidityRh = readCharacteristicOrNull(humidity)
                ?.getIntValue(Data.FORMAT_UINT16_LE, 0)
                ?.let { it.toDouble() / 100 },
            lux = readCharacteristicOrNull(luminosity)
                ?.getIntValue(Data.FORMAT_UINT16_LE, 0)
                ?.toDouble(),
            decibels = readCharacteristicOrNull(noise)
                ?.getIntValue(Data.FORMAT_UINT32_LE, 0)
                ?.let { it.toDouble() / 10 },
            vocRaw = readCharacteristicOrNull(vocRaw)
                ?.getIntValue(Data.FORMAT_UINT32_LE, 0)
                ?.toLong(),
        )
    }

    /*
     * Battery level service.
     */
    suspend fun readBattery(): Int? {
        ensureAttested()
        val batteryLevel = batteryService?.batteryLevel ?: return null

        return readCharacteristicOrNull(batteryLevel)
            ?.getIntValue(Data.FORMAT_UINT8, 0)
    }

    /*
     * Time service.
     */
    suspend fun setTime(unixTime: Long) {
        ensureAttested()
        val timeChr = timeService?.unixTime ?: return
        val timeBytes = ByteBuffer.allocate(8)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putLong(unixTime)
            .array()

        writeCharacteristic(timeChr, timeBytes, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
            .suspend()
    }

    /*
     * Settings service
     */
    suspend fun readSettings(): SpvDeviceSettings? {
        ensureAttested()
        val settingsChr = settingsService?.settings ?: return null
        return readCharacteristicOrNull(settingsChr)
            ?.value
            ?.let { SpvDeviceSettings.tryDecode(it) }
    }

    suspend fun writeSettings(settings: SpvDeviceSettings) {
        ensureAttested()
        val settingsChr = settingsService?.settings
        writeCharacteristic(settingsChr, settings.encode(), BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
            .suspend()
    }

    /*
     * Sensor Data Service
     */
    suspend fun readSensorMetadata(): SpvSdsMetadata? {
        ensureAttested()

        val metadata = sensorService?.control ?: return null
        return readCharacteristicOrNull(metadata)
            ?.value
            ?.let { SpvSdsMetadata.tryDecode(it) }
    }

    suspend fun writeSensorControl(data: ByteArray) {
        ensureAttested()

        val control = sensorService?.control ?: return
        writeCharacteristic(control, data, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
            .suspend()
    }

    fun openL2CapChannel(psm: Int): BluetoothSocket? {
        ensureAttested()

        // Android considers Just-Works pairing "insecure".
        val socket = bluetoothDevice?.createInsecureL2capChannel(psm)
        socket?.connect()
        return socket
    }



    private fun ensureAttested() {
        if(!isAuthentic) {
            throw SecurityException("Device has not passed attestation")
        }
    }

    private suspend fun readCharacteristicOrNull(characteristic: BluetoothGattCharacteristic?): Data? {
        return try {
            readCharacteristic(characteristic).suspend()
        } catch(_: Exception) {
            null
        }
    }
}
