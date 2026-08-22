package com.qubeguard.app.data.blocklist

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing BlocklistDatabase and BlocklistDao.
 */
@Module
@InstallIn(SingletonComponent::class)
object BlocklistModule {
    @Provides
    @Singleton
    fun provideBlocklistDatabase(@ApplicationContext context: Context): BlocklistDatabase {
        return Room.databaseBuilder(
            context,
            BlocklistDatabase::class.java,
            "blocklist_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideBlocklistDao(database: BlocklistDatabase): BlocklistDao {
        return database.blocklistDao()
    }
}
