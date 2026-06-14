package com.topscore.errornotebook.core.database.dao

import androidx.room.*
import com.topscore.errornotebook.core.database.entity.QuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {

    @Query("SELECT * FROM questions WHERE userId = :userId AND status = :status ORDER BY errorDate DESC LIMIT :limit OFFSET :offset")
    suspend fun getQuestions(userId: Long, status: String = "ACTIVE", limit: Int = 20, offset: Int = 0): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE userId = :userId AND status = :status ORDER BY errorDate DESC")
    fun observeQuestions(userId: Long, status: String = "ACTIVE"): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getQuestionById(id: Long): QuestionEntity?

    @Query("SELECT * FROM questions WHERE userId = :userId AND stage = :stage AND status = 'ACTIVE' ORDER BY errorDate DESC")
    suspend fun getQuestionsByStage(userId: Long, stage: String): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE userId = :userId AND subject = :subject AND status = 'ACTIVE' ORDER BY errorDate DESC")
    suspend fun getQuestionsBySubject(userId: Long, subject: String): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE userId = :userId AND errorReason IN (:reasons) AND status = 'ACTIVE' ORDER BY errorDate DESC")
    suspend fun getQuestionsByErrorReasons(userId: Long, reasons: List<String>): List<QuestionEntity>

    @Query("SELECT COUNT(*) FROM questions WHERE userId = :userId AND status = 'ACTIVE'")
    suspend fun getActiveQuestionCount(userId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Update
    suspend fun updateQuestion(question: QuestionEntity)

    @Query("DELETE FROM questions WHERE id = :id")
    suspend fun deleteQuestion(id: Long)

    @Query("DELETE FROM questions WHERE id IN (:ids)")
    suspend fun deleteQuestions(ids: List<Long>)

    @Query("UPDATE questions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE questions SET syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, syncStatus: String)

    @Query("SELECT * FROM questions WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncQuestions(): List<QuestionEntity>
}