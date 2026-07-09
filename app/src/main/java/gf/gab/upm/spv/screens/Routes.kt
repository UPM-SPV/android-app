package gf.gab.upm.spv.screens

import android.os.Bundle
import androidx.navigation.NavType
import gf.gab.upm.spv.ble.MacAddress
import gf.gab.upm.spv.ota.OtaInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
@Serializable
object PermissionRoute

@Serializable
object DashboardRoute

@Serializable
object PairingRoute

@Serializable
data class DeviceDetailsRoute(val macAddress: String)

@Serializable
data class OtaRoute(val macAddress: String, val otaSize: Long, val otaUrl: String, val otaVersion: String)
