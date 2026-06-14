package com.topscore.errornotebook.core.database

import com.topscore.errornotebook.core.database.entity.QuestionEntity
import com.topscore.errornotebook.domain.model.*

/**
 * 将 QuestionEntity 转换为 domain Question 模型
 */
fun QuestionEntity.toDomain(): Question {
    return Question(
        id = id,
        userId = userId,
        imageId = imageId,
        stage = SubjectStage.valueOf(stage),
        subject = subject,
        errorReason = ErrorReason.valueOf(errorReason),
        source = source,
        questionType = questionType?.let { QuestionType.valueOf(it) },
        errorDate = errorDate,
        correctAnswer = correctAnswer,
        wrongAnswer = wrongAnswer,
        notes = notes,
        tags = tags?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        status = QuestionStatus.valueOf(status),
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = SyncStatus.valueOf(syncStatus)
    )
}

/**
 * 将 domain Question 模型转换为 QuestionEntity
 */
fun Question.toEntity(): QuestionEntity {
    return QuestionEntity(
        id = id,
        userId = userId,
        imageId = imageId,
        stage = stage.name,
        subject = subject,
        errorReason = errorReason.name,
        source = source,
        questionType = questionType?.name,
        errorDate = errorDate,
        correctAnswer = correctAnswer,
        wrongAnswer = wrongAnswer,
        notes = notes,
        tags = tags.joinToString(","),
        status = status.name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = syncStatus.name
    )
}

/**
 * 安全地将字符串转换为 SubjectStage，默认值 PRIMARY
 */
fun String.toSubjectStage(): SubjectStage {
    return try {
        SubjectStage.valueOf(this)
    } catch (e: Exception) {
        SubjectStage.PRIMARY
    }
}

/**
 * 安全地将字符串转换为 ErrorReason，默认值 OTHER
 */
fun String.toErrorReason(): ErrorReason {
    return try {
        ErrorReason.valueOf(this)
    } catch (e: Exception) {
        ErrorReason.OTHER
    }
}

/**
 * 安全地将字符串转换为 QuestionType，默认值 OTHER
 */
fun String?.toQuestionType(): QuestionType? {
    return this?.let {
        try {
            QuestionType.valueOf(it)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * 安全地将字符串转换为 QuestionStatus，默认值 ACTIVE
 */
fun String.toQuestionStatus(): QuestionStatus {
    return try {
        QuestionStatus.valueOf(this)
    } catch (e: Exception) {
        QuestionStatus.ACTIVE
    }
}

/**
 * 安全地将字符串转换为 SyncStatus，默认值 PENDING
 */
fun String.toSyncStatus(): SyncStatus {
    return try {
        SyncStatus.valueOf(this)
    } catch (e: Exception) {
        SyncStatus.PENDING
    }
}