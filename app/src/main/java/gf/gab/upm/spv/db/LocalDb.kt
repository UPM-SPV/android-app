package gf.gab.upm.spv.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [SpvDeviceInfo::class, SpvSensorData::class],
    version = 1
)
@TypeConverters(SpvSensorDataConverter::class, SpvDeviceInfoConverter::class)
abstract class LocalDb : RoomDatabase() {
    abstract fun deviceDao(): SpvDeviceInfoDao
    abstract fun sensorData(): SpvSensorDataDao
}
