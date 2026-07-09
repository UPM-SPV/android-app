package gf.gab.upm.spv.hilt

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import gf.gab.upm.spv.db.LocalDb
import gf.gab.upm.spv.db.SpvDeviceInfoConverter
import gf.gab.upm.spv.db.SpvDeviceInfoDao
import gf.gab.upm.spv.db.SpvSensorData
import gf.gab.upm.spv.db.SpvSensorDataDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class LocalDbModule {

    @Provides
    @Singleton
    fun provideLocalDb(@ApplicationContext context: Context): LocalDb =
        Room.databaseBuilder(
            context,
            LocalDb::class.java, "spv-database"
        )
        .addTypeConverter(SpvDeviceInfoConverter())
        .build()

    @Provides
    @Singleton
    fun provideDeviceInfo(db: LocalDb): SpvDeviceInfoDao = db.deviceDao()

    @Provides
    @Singleton
    fun provideSensorData(db: LocalDb): SpvSensorDataDao = db.sensorData()
}
