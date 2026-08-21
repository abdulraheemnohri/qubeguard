package com.qubeguard.app.di

import android.content.Context
import androidx.room.Room
import com.qubeguard.app.browser.QubeDao
import com.qubeguard.app.browser.QubeDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the Room database used by the isolated Qube browser profiles.
 *
 * Keeping the database and DAO in the SingletonComponent makes them available
 * to QubeManager and Hilt ViewModels without requiring manual service-locator
 * wiring.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideQubeDatabase(
        @ApplicationContext context: Context
    ): QubeDatabase = Room.databaseBuilder(
        context,
        QubeDatabase::class.java,
        "qubeguard_qubes.db"
    ).fallbackToDestructiveMigration().build()

    @Provides
    fun provideQubeDao(database: QubeDatabase): QubeDao = database.qubeDao()
}
