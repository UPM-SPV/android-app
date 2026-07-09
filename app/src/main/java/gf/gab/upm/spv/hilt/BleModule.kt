package gf.gab.upm.spv.hilt

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import gf.gab.upm.spv.ble.SpvBleManager
import gf.gab.upm.spv.ble.SpvBleScanner
import gf.gab.upm.spv.ble.SpvDeviceRepository
import gf.gab.upm.spv.db.SpvDeviceInfoDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BleModule {

    @Singleton
    @Provides
    fun provideBleScanner(@ApplicationContext context: Context): SpvBleScanner {
        return SpvBleScanner(context)
    }

    @Singleton
    @Provides
    fun provideBleRepository(@ApplicationContext context: Context, scanner: SpvBleScanner, deviceDao: SpvDeviceInfoDao): SpvDeviceRepository =
        SpvDeviceRepository(context, scanner, deviceDao)

    @Provides
    fun provideSpvBleManager(@ApplicationContext context: Context): SpvBleManager {
        return SpvBleManager(context)
    }
}
