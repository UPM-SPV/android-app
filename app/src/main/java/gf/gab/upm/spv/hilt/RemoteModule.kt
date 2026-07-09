package gf.gab.upm.spv.hilt

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import gf.gab.upm.spv.config.SpvConfig
import gf.gab.upm.spv.db.LocalDb
import gf.gab.upm.spv.db.SpvDeviceInfoConverter
import gf.gab.upm.spv.db.SpvDeviceInfoDao
import gf.gab.upm.spv.db.SpvSensorData
import gf.gab.upm.spv.db.SpvSensorDataDao
import gf.gab.upm.spv.ota.OtaServer
import gf.gab.upm.spv.ota.OtaServerManager
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RemoteModule {

    @Provides
    @Singleton
    fun provideOtaServer(): OtaServer {
        return Retrofit.Builder()
            .baseUrl(SpvConfig.OTA_ENDPOINT_URL)
            .build()
            .create()
    }

    @Provides
    @Singleton
    fun provideOtaServerManager(server: OtaServer): OtaServerManager = OtaServerManager(server)
}
