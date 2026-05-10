package io.github.imove.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.imove.data.local.database.IMoveDatabase
import io.github.imove.data.local.database.dao.DeviceDao
import io.github.imove.data.local.database.dao.TransferredFileDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): IMoveDatabase {
        return Room.databaseBuilder(
            context,
            IMoveDatabase::class.java,
            "imove_database"
        ).build()
    }

    @Provides
    fun provideDeviceDao(database: IMoveDatabase): DeviceDao = database.deviceDao()

    @Provides
    fun provideTransferredFileDao(database: IMoveDatabase): TransferredFileDao =
        database.transferredFileDao()
}
