package com.topscore.errornotebook.core.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response

/**
 * API 统一响应格式
 */
@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @Json(name = "code") val code: Int,
    @Json(name = "message") val message: String,
    @Json(name = "data") val data: T?
)

/**
 * 登录请求
 */
@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "phone") val phone: String,
    @Json(name = "code") val code: String
)

/**
 * 发送验证码请求
 */
@JsonClass(generateAdapter = true)
data class SendSmsCodeRequest(
    @Json(name = "phone") val phone: String
)

/**
 * 刷新 Token 请求
 */
@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    @Json(name = "refresh_token") val refreshToken: String
)

/**
 * 登录数据
 */
@JsonClass(generateAdapter = true)
data class LoginData(
    @Json(name = "token") val token: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "user") val user: UserDto
)

/**
 * 用户信息 DTO
 */
@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: Long,
    @Json(name = "phone") val phone: String,
    @Json(name = "nickname") val nickname: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String
)

/**
 * 创建错题请求
 */
@JsonClass(generateAdapter = true)
data class CreateQuestionRequest(
    @Json(name = "image_id") val imageId: Long,
    @Json(name = "stage") val stage: String,
    @Json(name = "subject") val subject: String,
    @Json(name = "error_reason") val errorReason: String,
    @Json(name = "source") val source: String? = null,
    @Json(name = "question_type") val questionType: String? = null,
    @Json(name = "error_date") val errorDate: String,
    @Json(name = "correct_answer") val correctAnswer: String? = null,
    @Json(name = "wrong_answer") val wrongAnswer: String? = null,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "tags") val tags: List<String>? = null
)

/**
 * 更新错题请求
 */
@JsonClass(generateAdapter = true)
data class UpdateQuestionRequest(
    @Json(name = "stage") val stage: String? = null,
    @Json(name = "subject") val subject: String? = null,
    @Json(name = "error_reason") val errorReason: String? = null,
    @Json(name = "source") val source: String? = null,
    @Json(name = "question_type") val questionType: String? = null,
    @Json(name = "error_date") val errorDate: String? = null,
    @Json(name = "correct_answer") val correctAnswer: String? = null,
    @Json(name = "wrong_answer") val wrongAnswer: String? = null,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "tags") val tags: List<String>? = null,
    @Json(name = "status") val status: String? = null
)

/**
 * 批量删除请求
 */
@JsonClass(generateAdapter = true)
data class BatchDeleteRequest(
    @Json(name = "ids") val ids: List<Long>
)

/**
 * 错题 DTO
 */
@JsonClass(generateAdapter = true)
data class QuestionDto(
    @Json(name = "id") val id: Long,
    @Json(name = "user_id") val userId: Long,
    @Json(name = "image_id") val imageId: Long,
    @Json(name = "stage") val stage: String,
    @Json(name = "subject") val subject: String,
    @Json(name = "error_reason") val errorReason: String,
    @Json(name = "source") val source: String? = null,
    @Json(name = "question_type") val questionType: String? = null,
    @Json(name = "error_date") val errorDate: String,
    @Json(name = "correct_answer") val correctAnswer: String? = null,
    @Json(name = "wrong_answer") val wrongAnswer: String? = null,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "tags") val tags: List<String>? = null,
    @Json(name = "status") val status: String,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String
)

/**
 * 错题分页数据
 */
@JsonClass(generateAdapter = true)
data class QuestionPageDto(
    @Json(name = "total") val total: Int,
    @Json(name = "page") val page: Int,
    @Json(name = "size") val size: Int,
    @Json(name = "items") val items: List<QuestionDto>
)

/**
 * OCR 识别请求
 */
@JsonClass(generateAdapter = true)
data class OcrRecognizeRequest(
    @Json(name = "image_url") val imageUrl: String
)

/**
 * OCR 识别结果 DTO
 */
@JsonClass(generateAdapter = true)
data class OcrResultDto(
    @Json(name = "text") val text: String,
    @Json(name = "is_complete") val isComplete: Boolean,
    @Json(name = "has_handwriting") val hasHandwriting: Boolean,
    @Json(name = "handwriting_regions") val handwritingRegions: List<RectDto>? = null,
    @Json(name = "confidence") val confidence: Float
)

/**
 * 矩形区域 DTO
 */
@JsonClass(generateAdapter = true)
data class RectDto(
    @Json(name = "x") val x: Float,
    @Json(name = "y") val y: Float,
    @Json(name = "width") val width: Float,
    @Json(name = "height") val height: Float
)

/**
 * 创建标签请求
 */
@JsonClass(generateAdapter = true)
data class CreateTagRequest(
    @Json(name = "name") val name: String,
    @Json(name = "color") val color: String? = null
)

/**
 * 标签 DTO
 */
@JsonClass(generateAdapter = true)
data class TagDto(
    @Json(name = "id") val id: Long,
    @Json(name = "user_id") val userId: Long,
    @Json(name = "name") val name: String,
    @Json(name = "color") val color: String? = null,
    @Json(name = "created_at") val createdAt: String
)

/**
 * 上传凭证 DTO
 */
@JsonClass(generateAdapter = true)
data class UploadTokenDto(
    @Json(name = "upload_url") val uploadUrl: String,
    @Json(name = "file_url") val fileUrl: String,
    @Json(name = "expires_in") val expiresIn: Long
)