package com.example.template.core.database.di

import android.content.Context
import androidx.room.Room
import com.example.template.core.database.AppDatabase
import com.example.template.core.database.dao.GreetingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the app database and every DAO declared by [AppDatabase].
 *
 * @author WangZhiYao
 * @since 2026/11/11
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "template.db").build()

    @Provides
    fun provideGreetingDao(database: AppDatabase): GreetingDao = database.greetingDao()
}
