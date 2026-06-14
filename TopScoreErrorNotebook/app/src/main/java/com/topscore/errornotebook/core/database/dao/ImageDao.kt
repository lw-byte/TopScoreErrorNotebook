package com.topscore.errornotebook.core.database.dao

import androidx.room.*
import com.topscore.errornotebook.core.database.entity.QuestionImageEntity

@Dao
interface ImageDao {

    @Query("SELECT * FROM question_images WHERE id = :id")
    suspend fun getImageById(id: Long): QuestionImageEntity?

    @Query("SELECT * FROM question_images WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getImagesByUser(userId: Long): List<QuestionImageEntity>

    @Query("SELECT * FROM question_images WHERE syncStatus = :syncStatus")
    suspend fun getImagesBySyncStatus(syncStatus: String): List<QuestionImageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: QuestionImageEntity): Long

    @Update
    suspend fun updateImage(image: QuestionImageEntity)

    @Query("DELETE FROM question_images WHERE id = :id")
    suspend fun deleteImage(id: Long)

    @Query("UPDATE question_images SET syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, syncStatus: String)
}