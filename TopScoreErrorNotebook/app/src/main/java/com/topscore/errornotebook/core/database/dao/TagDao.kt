package com.topscore.errornotebook.core.database.dao

import androidx.room.*
import com.topscore.errornotebook.core.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM user_tags WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeTags(userId: Long): Flow<List<TagEntity>>

    @Query("SELECT * FROM user_tags WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getTagsByUser(userId: Long): List<TagEntity>

    @Query("SELECT * FROM user_tags WHERE id = :id")
    suspend fun getTagById(id: Long): TagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Query("DELETE FROM user_tags WHERE id = :id")
    suspend fun deleteTag(id: Long)

    @Query("DELETE FROM user_tags WHERE userId = :userId")
    suspend fun deleteAllTagsForUser(userId: Long)
}