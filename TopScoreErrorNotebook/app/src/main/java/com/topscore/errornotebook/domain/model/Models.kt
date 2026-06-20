package com.topscore.errornotebook.domain.model

/**
 * 错题实体
 */
data class Question(
    val id: Long = 0,
    val userId: Long = 0,
    val imageId: Long = 0,
    val stage: SubjectStage,
    val subject: String,
    val errorReason: ErrorReason,
    val source: String? = null,
    val questionType: QuestionType? = null,
    val errorDate: Long,
    val correctAnswer: String? = null,
    val wrongAnswer: String? = null,
    val notes: String? = null,
    val tags: List<String> = emptyList(),
    val status: QuestionStatus = QuestionStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

/**
 * 错题原始图片
 */
data class QuestionImage(
    val id: Long = 0,
    val userId: Long = 0,
    val originalImageUrl: String? = null,
    val originalImageLocal: String,
    val recognizedText: String? = null,
    val isComplete: Boolean = false,
    val hasHandwriting: Boolean = false,
    val handwritingRegions: List<Rect>? = null,
    val ocrConfidence: Float = 0f,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 矩形区域（用于手写区域标记）
 */
data class Rect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

/**
 * 用户信息
 */
data class User(
    val id: Long = 0,
    val phone: String,
    val nickname: String? = null,
    val avatarUrl: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

/**
 * 自定义标签
 */
data class Tag(
    val id: Long = 0,
    val userId: Long = 0,
    val name: String,
    val color: String? = null,
    val createdAt: Long = 0
)

/**
 * OCR 识别结果
 */
data class OcrResult(
    val text: String,
    val isComplete: Boolean,
    val hasHandwriting: Boolean,
    val handwritingRegions: List<Rect>? = null,
    val confidence: Float,
    val errorCode: String? = null,    // 错误码，如 "ClientError.413"
    val errorMessage: String? = null   // 错误信息，如 "Request Entity Too Large"
)

/**
 * 错题筛选条件
 */
data class QuestionFilter(
    val stage: SubjectStage? = null,
    val subject: String? = null,
    val errorReasons: List<ErrorReason>? = null,
    val tags: List<String>? = null,
    val dateStart: Long? = null,
    val dateEnd: Long? = null,
    val source: String? = null,
    val questionType: QuestionType? = null,
    val keyword: String? = null,
    val status: QuestionStatus = QuestionStatus.ACTIVE
)

/**
 * 分页结果
 */
data class QuestionPageResult(
    val questions: List<Question>,
    val total: Int,
    val page: Int,
    val hasMore: Boolean
)

/**
 * 登录结果
 */
data class LoginResult(
    val token: String,
    val refreshToken: String,
    val user: User
)