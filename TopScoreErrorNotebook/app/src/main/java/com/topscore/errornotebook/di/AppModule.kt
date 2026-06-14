package com.topscore.errornotebook.di

import android.content.Context
import androidx.room.Room
import com.topscore.errornotebook.core.database.AppDatabase
import com.topscore.errornotebook.core.database.dao.ImageDao
import com.topscore.errornotebook.core.database.dao.QuestionDao
import com.topscore.errornotebook.core.database.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideQuestionDao(database: AppDatabase): QuestionDao {
        return database.questionDao()
    }

    @Provides
    @Singleton
    fun provideImageDao(database: AppDatabase): ImageDao {
        return database.imageDao()
    }

    @Provides
    @Singleton
    fun provideTagDao(database: AppDatabase): TagDao {
        return database.tagDao()
    }
}