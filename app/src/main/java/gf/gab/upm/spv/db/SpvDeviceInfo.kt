package gf.gab.upm.spv.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.ProvidedTypeConverter
import androidx.room.Query
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import gf.gab.upm.spv.ble.MacAddress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Stores persistent information about known devices.
 */
@Entity(tableName = "device_info")
data class SpvDeviceInfo(
    @TypeConverters(SpvDeviceInfoConverter::class)
    @PrimaryKey val macAddress: MacAddress,
    val friendlyName: String,
    val serialNumber: String,
)

@ProvidedTypeConverter
class SpvDeviceInfoConverter {
    @TypeConverter
    fun macAddressToString(macAddress: MacAddress): String = macAddress.toString()

    @TypeConverter
    fun stringToMacAddress(string: String): MacAddress = MacAddress.fromString(string)
}

@Dao
interface SpvDeviceInfoDao {
    @Query("SELECT * from device_info")
    suspend fun getAll(): List<SpvDeviceInfo>

    @Query("SELECT * from device_info")
    fun getAllFlow(): Flow<List<SpvDeviceInfo>>

    @Query("SELECT * from device_info WHERE macAddress = :address LIMIT 1")
    @TypeConverters(SpvDeviceInfoConverter::class)
    suspend fun get(address: MacAddress): SpvDeviceInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg devices: SpvDeviceInfo)

    @Query("DELETE FROM device_info WHERE macAddress = :address")
    @TypeConverters(SpvDeviceInfoConverter::class)
    suspend fun delete(address: MacAddress)

    @Query("UPDATE device_info SET friendlyName = :name WHERE macAddress = :address")
    @TypeConverters(SpvDeviceInfoConverter::class)
    suspend fun updateName(address: MacAddress, name: String)
}
