package com.topscore.errornotebook.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.topscore.errornotebook.core.database.dao.ImageDao
import com.topscore.errornotebook.core.database.dao.QuestionDao
import com.topscore.errornotebook.core.database.dao.TagDao
import com.topscore.errornotebook.core.database.entity.QuestionEntity
import com.topscore.errornotebook.core.database.entity.QuestionImageEntity
import com.topscore.errornotebook.core.database.entity.TagEntity

@Database(
    entities = [
        QuestionEntity::class,
        QuestionImageEntity::class,
        TagEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun questionDao(): QuestionDao
    abstract fun imageDao(): ImageDao
    abstract fun tagDao(): TagDao

    companion object {
        const val DATABASE_NAME = "topscore_error_notebook.db"
    }
}