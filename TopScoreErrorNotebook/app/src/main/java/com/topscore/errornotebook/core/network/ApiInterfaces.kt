package com.topscore.errornotebook.core.network

import retrofit2.Response
import retrofit2.http.*

/**
 * 认证 API 服务
 */
interface AuthApiService {

    @POST("v1/auth/sms/send")
    suspend fun sendSmsCode(
        @Body request: SendSmsCodeRequest
    ): Response<ApiResponse<Unit>>

    @POST("v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<LoginData>>

    @POST("v1/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<ApiResponse<LoginData>>
}

/**
 * 用户 API 服务
 */
interface UserApiService {

    @GET("v1/user/profile")
    suspend fun getProfile(): Response<ApiResponse<UserDto>>

    @PUT("v1/user/profile")
    suspend fun updateProfile(
        @Body request: Map<String, String>
    ): Response<ApiResponse<UserDto>>
}

/**
 * 错题 API 服务
 */
interface QuestionApiService {

    @GET("v1/questions")
    suspend fun getQuestions(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
        @Query("stage") stage: String? = null,
        @Query("subject") subject: String? = null,
        @Query("error_reason") errorReason: String? = null,
        @Query("tags") tags: String? = null,
        @Query("date_start") dateStart: String? = null,
        @Query("date_end") dateEnd: String? = null,
        @Query("source") source: String? = null,
        @Query("question_type") questionType: String? = null,
        @Query("keyword") keyword: String? = null,
        @Query("status") status: String = "ACTIVE",
        @Query("sort_by") sortBy: String = "error_date",
        @Query("sort_order") sortOrder: String = "DESC"
    ): Response<ApiResponse<QuestionPageDto>>

    @GET("v1/questions/{id}")
    suspend fun getQuestionById(
        @Path("id") id: Long
    ): Response<ApiResponse<QuestionDto>>

    @POST("v1/questions")
    suspend fun createQuestion(
        @Body request: CreateQuestionRequest
    ): Response<ApiResponse<QuestionDto>>

    @PUT("v1/questions/{id}")
    suspend fun updateQuestion(
        @Path("id") id: Long,
        @Body request: UpdateQuestionRequest
    ): Response<ApiResponse<QuestionDto>>

    @DELETE("v1/questions/{id}")
    suspend fun deleteQuestion(
        @Path("id") id: Long
    ): Response<ApiResponse<Unit>>

    @POST("v1/questions/batch/delete")
    suspend fun batchDeleteQuestions(
        @Body request: BatchDeleteRequest
    ): Response<ApiResponse<Unit>>

    @PUT("v1/questions/{id}/archive")
    suspend fun archiveQuestion(
        @Path("id") id: Long
    ): Response<ApiResponse<Unit>>
}

/**
 * OCR API 服务
 */
interface OcrApiService {

    @POST("v1/ocr/recognize")
    suspend fun recognize(
        @Body request: OcrRecognizeRequest
    ): Response<ApiResponse<OcrResultDto>>
}

/**
 * 标签 API 服务
 */
interface TagApiService {

    @GET("v1/tags")
    suspend fun getTags(): Response<ApiResponse<List<TagDto>>>

    @POST("v1/tags")
    suspend fun createTag(
        @Body request: CreateTagRequest
    ): Response<ApiResponse<TagDto>>

    @DELETE("v1/tags/{id}")
    suspend fun deleteTag(
        @Path("id") id: Long
    ): Response<ApiResponse<Unit>>
}

/**
 * OSS API 服务
 */
interface OssApiService {

    @POST("v1/oss/upload-token")
    suspend fun getUploadToken(): Response<ApiResponse<UploadTokenDto>>
}