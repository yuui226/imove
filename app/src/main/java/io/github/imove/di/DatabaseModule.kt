package io.github.imove.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.imove.data.local.database.IMoveDatabase
import io.github.imove.data.local.database.dao.DeviceDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // v2 drops the unused transferred_files_table (transfer state is tracked in-memory).
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS transferred_files_table")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): IMoveDatabase {
        return Room.databaseBuilder(
            context,
            IMoveDatabase::class.java,
            "imove_database"
        ).addMigrations(MIGRATION_1_2).build()
    }

    @Provides
    fun provideDeviceDao(database: IMoveDatabase): DeviceDao = database.deviceDao()
}
