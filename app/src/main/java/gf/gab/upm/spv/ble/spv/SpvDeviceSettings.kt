package gf.gab.upm.spv.ble.spv

import DeviceSettingsOuterClass.DeviceSettings
import com.google.protobuf.InvalidProtocolBufferException
import kotlin.Boolean

private enum class SensorFlags(val bits: Int) {
    VOC(0b0001),
    NOISE(0b0010),
    TEMPERATURE_HUMIDITY(0b0100),
    LUMINOSITY(0b1000)
}

data class SpvDeviceSettings(
    var samplingRateMinutes: Int,
    var sensorSettings: SpvDeviceSettingsSensors
) {
    companion object {
        fun tryDecode(bytes: ByteArray): SpvDeviceSettings? {
            val protoData = try {
                 DeviceSettings.parseFrom(bytes)
            } catch(_: InvalidProtocolBufferException) {
                return null
            }
            if(protoData.schemaVersion != 1) {
                return null
            }

            return SpvDeviceSettings(
                samplingRateMinutes = (protoData.samplingRateSeconds / 60).coerceAtLeast(1),
                sensorSettings = SpvDeviceSettingsSensors.decode(protoData.sensorFlags)
            )
        }
    }

    fun encode(): ByteArray =
        DeviceSettings.newBuilder()
            .setSchemaVersion(1)
            .setSensorFlags(sensorSettings.encode())
            .setSamplingRateSeconds(samplingRateMinutes * 60)
            .build()
            .toByteArray()
}

data class SpvDeviceSettingsSensors(
    var temperature: Boolean,
    var humidity: Boolean,
    var luminosity: Boolean,
    var noise: Boolean,
    var voc: Boolean,
) {
    companion object {
        fun decode(flags: Int): SpvDeviceSettingsSensors = SpvDeviceSettingsSensors(
            voc = flags and SensorFlags.VOC.bits != 0,
            noise = flags and SensorFlags.NOISE.bits != 0,
            temperature = flags and SensorFlags.TEMPERATURE_HUMIDITY.bits != 0,
            humidity = flags and SensorFlags.TEMPERATURE_HUMIDITY.bits != 0,
            luminosity = flags and SensorFlags.LUMINOSITY.bits != 0,
        )
    }

    fun encode(): Int {
        var flags = 0
        if(voc) flags = flags or SensorFlags.VOC.bits
        if(noise) flags = flags or SensorFlags.NOISE.bits
        if(temperature) flags = flags or SensorFlags.TEMPERATURE_HUMIDITY.bits
        if(humidity) flags = flags or SensorFlags.TEMPERATURE_HUMIDITY.bits
        if(luminosity) flags = flags or SensorFlags.LUMINOSITY.bits

        return flags
    }
}
