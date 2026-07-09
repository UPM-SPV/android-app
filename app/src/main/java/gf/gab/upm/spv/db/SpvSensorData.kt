package gf.gab.upm.spv.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.ProvidedTypeConverter
import androidx.room.Query
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import gf.gab.upm.spv.ble.MacAddress
import gf.gab.upm.spv.ble.spv.SpvEnvironmentalMeasurements
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/**
 * Stores persistent sensor data.
 */
@Entity(
    tableName = "sensor_data",
    foreignKeys = [ForeignKey(
        entity = SpvDeviceInfo::class,
        parentColumns = arrayOf("macAddress"),
        childColumns = arrayOf("macAddress"),
        onDelete = ForeignKey.CASCADE
    )],
    primaryKeys = [
        "macAddress", "date"
    ]
)
data class SpvSensorData(
    @TypeConverters(SpvDeviceInfoConverter::class)
    val macAddress: MacAddress,
    @TypeConverters(SpvSensorDataConverter::class)
    val date: Instant,
    @Embedded val data: SpvEnvironmentalMeasurements
)

class SpvSensorDataConverter {
    // Convert to Long, otherwise DB will treat them as BLOBs, making comparisons inefficient.
    @TypeConverter
    fun instantToTimestamp(instant: Instant): Long = instant.toEpochMilliseconds()

    @TypeConverter
    fun timestampToInstant(timestamp: Long): Instant = Instant.fromEpochMilliseconds(timestamp)
}

@Dao
interface SpvSensorDataDao {
    @Query("SELECT * from sensor_data WHERE macAddress = :macAddress")
    suspend fun getAll(macAddress: MacAddress): List<SpvSensorData>

    @Query("SELECT * from sensor_data WHERE macAddress = :macAddress")
    fun getAllFlow(macAddress: MacAddress): Flow<List<SpvSensorData>>

    @Query("SELECT * from sensor_data WHERE macAddress = :macAddress AND date BETWEEN :start AND :end")
    suspend fun getTimeFrame(macAddress: MacAddress, start: Instant, end: Instant): List<SpvSensorData>

    @Query("SELECT * from sensor_data WHERE macAddress = :macAddress AND date BETWEEN :start AND :end")
    fun getAllTimeFrameFlow(macAddress: MacAddress, start: Instant, end: Instant): Flow<List<SpvSensorData>>

    @Query("SELECT * from sensor_data WHERE macAddress = :macAddress ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(macAddress: MacAddress): SpvSensorData?

    @Query("SELECT * from sensor_data WHERE macAddress = :macAddress ORDER BY date DESC LIMIT 1")
    fun getLatestFlow(macAddress: MacAddress): Flow<SpvSensorData?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg devices: SpvSensorData)
}
