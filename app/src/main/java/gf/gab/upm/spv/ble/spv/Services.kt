package gf.gab.upm.spv.ble.spv

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import gf.gab.upm.spv.config.SpvConfig

data class SpvBleSensorService(
    val service: BluetoothGattService,
    val control: BluetoothGattCharacteristic,
) {
    companion object {
        /**
         * Tries to construct an [SpvBleSensorService] from a [BluetoothGatt] server connection.
         *
         * Returns `null` if the device does not support the required services.
         */
        fun tryFromGatt(gatt: BluetoothGatt): SpvBleSensorService? {
            val service = gatt.getService(SpvConfig.SDS_UUID.toUUID()) ?: return null
            val control = service.getCharacteristic(SpvConfig.SDS_CONTROL_CHR.toUUID()) ?: return null

            return SpvBleSensorService(
                service = service,
                control = control,
            )
        }
    }
}

data class SpvBleAttestationService(
    val service: BluetoothGattService,
    val challenge: BluetoothGattCharacteristic,
    val response: BluetoothGattCharacteristic,
    val certificate: BluetoothGattCharacteristic
) {
    companion object {
        /**
         * Tries to construct an [SpvBleAttestationService] from a [BluetoothGatt] server connection.
         *
         * Returns `null` if the device does not support the required services.
         */
        fun tryFromGatt(gatt: BluetoothGatt): SpvBleAttestationService? {
            val service = gatt.getService(SpvConfig.ATTEST_SERVICE_UUID.toUUID()) ?: return null
            val challenge = service.getCharacteristic(SpvConfig.ATTEST_CHALLENGE_CHR.toUUID()) ?: return null
            val response = service.getCharacteristic(SpvConfig.ATTEST_RESPONSE_CHR.toUUID()) ?: return null
            val certificate = service.getCharacteristic(SpvConfig.ATTEST_CERT_CHR.toUUID()) ?: return null

            return SpvBleAttestationService(
                service = service,
                challenge = challenge,
                response = response,
                certificate = certificate
            )
        }
    }
}

data class SpvBleTimeService(
    val service: BluetoothGattService,
    val unixTime: BluetoothGattCharacteristic
) {
    companion object {
        /**
         * Tries to construct an [SpvBleTimeService] from a [BluetoothGatt] server connection.
         *
         * Returns `null` if the device does not support the required services.
         */
        fun tryFromGatt(gatt: BluetoothGatt): SpvBleTimeService? {
            val service = gatt.getService(SpvConfig.TIME_SERVICE_UUID.toUUID()) ?: return null
            val time = service.getCharacteristic(SpvConfig.TIME_SERVICE_UNIX_CHR.toUUID()) ?: return null

            return SpvBleTimeService(
                service = service,
                unixTime = time
            )
        }
    }
}

data class SpvBleBatteryService(
    val service: BluetoothGattService,
    val batteryLevel: BluetoothGattCharacteristic,
) {
    companion object {
        /**
         * Tries to construct an [SpvBleBatteryService] from a [BluetoothGatt] server connection.
         *
         * Returns `null` if the device does not support the required services.
         */
        fun tryFromGatt(gatt: BluetoothGatt): SpvBleBatteryService? {
            val service = gatt.getService(SpvConfig.BATTERY_SERVICE_UUID.toUUID()) ?: return null
            val batteryLevel = service.getCharacteristic(SpvConfig.BATTERY_LEVEL_CHR.toUUID()) ?: return null

            return SpvBleBatteryService(
                service = service,
                batteryLevel = batteryLevel
            )
        }
    }
}

data class SpvBleSettingsService(
    val service: BluetoothGattService,
    val settings: BluetoothGattCharacteristic,
) {
    companion object {
        /**
         * Tries to construct an [SpvBleSettingsService] from a [BluetoothGatt] server connection.
         *
         * Returns `null` if the device does not support the required services.
         */
        fun tryFromGatt(gatt: BluetoothGatt): SpvBleSettingsService? {
            val service = gatt.getService(SpvConfig.SETTINGS_SERVICE_UUID.toUUID()) ?: return null
            val settings = service.getCharacteristic(SpvConfig.SETTINGS_CHR.toUUID()) ?: return null

            return SpvBleSettingsService(
                service = service,
                settings = settings,
            )
        }
    }
}

data class SpvBleEnvironmentalService(
    val service: BluetoothGattService,
    val temperature: BluetoothGattCharacteristic,
    val humidity: BluetoothGattCharacteristic,
    val luminosity: BluetoothGattCharacteristic,
    val noise: BluetoothGattCharacteristic,
    val vocRaw: BluetoothGattCharacteristic,
) {
    companion object {
        /**
         * Tries to construct an [SpvBleEnvironmentalService] from a [BluetoothGatt] server connection.
         *
         * Returns `null` if the device does not support the required services.
         */
        fun tryFromGatt(gatt: BluetoothGatt): SpvBleEnvironmentalService? {
            val service = gatt.getService(SpvConfig.ENVIRON_SERVICE_UUID.toUUID()) ?: return null
            val temperature = service.getCharacteristic(SpvConfig.ENVIRON_TEMPERATURE_CHR.toUUID()) ?: return null
            val humidity = service.getCharacteristic(SpvConfig.ENVIRON_HUMIDITY_CHR.toUUID()) ?: return null
            val luminosity = service.getCharacteristic(SpvConfig.ENVIRON_LUMINOSITY_CHR.toUUID()) ?: return null
            val noise = service.getCharacteristic(SpvConfig.ENVIRON_NOISE_CHR.toUUID()) ?: return null
            val vocRaw = service.getCharacteristic(SpvConfig.ENVIRON_VOC_RAW_CHR.toUUID()) ?: return null


            return SpvBleEnvironmentalService(
                service = service,
                temperature = temperature,
                humidity = humidity,
                luminosity = luminosity,
                noise = noise,
                vocRaw = vocRaw
            )
        }
    }
}



data class SpvBleDeviceInformationService(
    val service: BluetoothGattService,
    val serialNumber: BluetoothGattCharacteristic,
    val fwVersion: BluetoothGattCharacteristic,
    val hwRevision: BluetoothGattCharacteristic,
) {
    companion object {
        /**
         * Tries to construct an [SpvBleDeviceInformationService] from a [BluetoothGatt] server connection.
         *
         * Returns `null` if the device does not support the required services.
         */
        fun tryFromGatt(gatt: BluetoothGatt): SpvBleDeviceInformationService? {
            val service = gatt.getService(SpvConfig.DIS_UUID.toUUID()) ?: return null
            val serialNumber = service.getCharacteristic(SpvConfig.DIS_SERIAL_NUMBER_CHR.toUUID()) ?: return null
            val fwVersion = service.getCharacteristic(SpvConfig.DIS_FW_REVISION_CHR.toUUID()) ?: return null
            val hwRevision = service.getCharacteristic(SpvConfig.DIS_HW_REVISION_CHR.toUUID()) ?: return null


            return SpvBleDeviceInformationService(
                service = service,
                serialNumber = serialNumber,
                fwVersion = fwVersion,
                hwRevision = hwRevision
            )
        }
    }
}
