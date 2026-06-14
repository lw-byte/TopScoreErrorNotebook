package com.topscore.errornotebook.core.database.entity

import androidx.room.*
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Entity
import androidx.room.ColumnInfo

/**
 * 错题数据库实体
 */
@Entity(
    tableName = "questions",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "stage"]),
        Index(value = ["userId", "subject"]),
        Index(value = ["userId", "errorReason"]),
        Index(value = ["errorDate"]),
        Index(value = ["status"]),
        Index(value = ["syncStatus"])
    ]
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "userId")
    val userId: Long,

    @ColumnInfo(name = "imageId")
    val imageId: Long,

    @ColumnInfo(name = "stage")
    val stage: String,

    @ColumnInfo(name = "subject")
    val subject: String,

    @ColumnInfo(name = "errorReason")
    val errorReason: String,

    @ColumnInfo(name = "source")
    val source: String? = null,

    @ColumnInfo(name = "questionType")
    val questionType: String? = null,

    @ColumnInfo(name = "errorDate")
    val errorDate: Long,

    @ColumnInfo(name = "correctAnswer")
    val correctAnswer: String? = null,

    @ColumnInfo(name = "wrongAnswer")
    val wrongAnswer: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "tags")
    val tags: String? = null,

    @ColumnInfo(name = "status")
    val status: String = "ACTIVE",

    @ColumnInfo(name = "createdAt")
    val createdAt: Long,

    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long,

    @ColumnInfo(name = "syncStatus")
    val syncStatus: String = "PENDING"
)

/**
 * 错题图片数据库实体
 */
@Entity(
    tableName = "question_images",
    indices = [Index(value = ["userId"])]
)
data class QuestionImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "userId")
    val userId: Long,

    @ColumnInfo(name = "originalImageUrl")
    val originalImageUrl: String? = null,

    @ColumnInfo(name = "originalImageLocal")
    val originalImageLocal: String,

    @ColumnInfo(name = "recognizedText")
    val recognizedText: String? = null,

    @ColumnInfo(name = "isComplete")
    val isComplete: Boolean = false,

    @ColumnInfo(name = "hasHandwriting")
    val hasHandwriting: Boolean = false,

    @ColumnInfo(name = "handwritingRegions")
    val handwritingRegions: String? = null,

    @ColumnInfo(name = "ocrConfidence")
    val ocrConfidence: Float = 0f,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long
)

/**
 * 标签数据库实体
 */
@Entity(
    tableName = "user_tags",
    indices = [Index(value = ["userId"])]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "userId")
    val userId: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "color")
    val color: String? = null,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long
)