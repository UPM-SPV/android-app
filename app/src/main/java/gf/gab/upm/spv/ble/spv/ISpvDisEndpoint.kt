package gf.gab.upm.spv.ble.spv

import gf.gab.upm.spv.ble.MacAddress

interface ISpvDisEndpoint {
    fun getMacAddress(): MacAddress?
    suspend fun readSerialNumber(): String?
}
