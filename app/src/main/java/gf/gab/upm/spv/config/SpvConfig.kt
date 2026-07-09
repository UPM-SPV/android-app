package gf.gab.upm.spv.config

import gf.gab.upm.spv.BuildConfig
import gf.gab.upm.spv.ble.uuid.BleNamespace

object SpvConfig {
    val CA_CRT_PEM              = BuildConfig.CA_CRT_PEM
    val OTA_ENDPOINT_URL        = BuildConfig.OTA_ENDPOINT_URL

    const val ATTEST_CHALLENGE_LENGTH  = 32

    val SPV_NAMESPACE           = BleNamespace("3c150000-9311-4a07-8520-a817462a9f16")

    val ADVERTISE_UUID          = SPV_NAMESPACE.applyIdentifier(0x0001)
    val ADVERTISE_PAIRING_UUID  = SPV_NAMESPACE.applyIdentifier(0x0002)

    val SDS_UUID                = SPV_NAMESPACE.applyIdentifier(0x8010)
    val SDS_CONTROL_CHR         = SPV_NAMESPACE.applyIdentifier(0x8011)

    val ATTEST_SERVICE_UUID     = SPV_NAMESPACE.applyIdentifier(0x8020)
    val ATTEST_CHALLENGE_CHR    = SPV_NAMESPACE.applyIdentifier(0x8021)
    val ATTEST_RESPONSE_CHR     = SPV_NAMESPACE.applyIdentifier(0x8022)
    val ATTEST_CERT_CHR         = SPV_NAMESPACE.applyIdentifier(0x8023)

    val TIME_SERVICE_UUID       = BleNamespace.BT_BASE_UUID.applyIdentifier(0x1805)
    val TIME_SERVICE_UNIX_CHR   = SPV_NAMESPACE.applyIdentifier(0x8031)

    val SETTINGS_SERVICE_UUID   = SPV_NAMESPACE.applyIdentifier(0x8040)
    val SETTINGS_CHR            = SPV_NAMESPACE.applyIdentifier(0x8041)

    val ENVIRON_SERVICE_UUID    = BleNamespace.BT_BASE_UUID.applyIdentifier(0x181A)
    val ENVIRON_TEMPERATURE_CHR = BleNamespace.BT_BASE_UUID.applyIdentifier(0x2A6E)
    val ENVIRON_HUMIDITY_CHR    = BleNamespace.BT_BASE_UUID.applyIdentifier(0x2A6F)
    val ENVIRON_LUMINOSITY_CHR  = BleNamespace.BT_BASE_UUID.applyIdentifier(0x2AFB)
    val ENVIRON_NOISE_CHR       = SPV_NAMESPACE.applyIdentifier(0x8051)
    val ENVIRON_VOC_RAW_CHR     = SPV_NAMESPACE.applyIdentifier(0x8052)


    val BATTERY_SERVICE_UUID    = BleNamespace.BT_BASE_UUID.applyIdentifier(0x180F)
    val BATTERY_LEVEL_CHR       = BleNamespace.BT_BASE_UUID.applyIdentifier(0x2A19)

    val DIS_UUID                = BleNamespace.BT_BASE_UUID.applyIdentifier(0x180A)
    val DIS_SERIAL_NUMBER_CHR   = BleNamespace.BT_BASE_UUID.applyIdentifier(0x2A25)
    val DIS_FW_REVISION_CHR     = BleNamespace.BT_BASE_UUID.applyIdentifier(0x2A26)
    val DIS_HW_REVISION_CHR     = BleNamespace.BT_BASE_UUID.applyIdentifier(0x2A27)
}
