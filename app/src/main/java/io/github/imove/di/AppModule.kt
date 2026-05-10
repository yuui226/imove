package io.github.imove.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.imove.service.TransferNotificationManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTransferNotificationManager(
        @ApplicationContext context: Context
    ): TransferNotificationManager {
        return TransferNotificationManager(context)
    }
}
