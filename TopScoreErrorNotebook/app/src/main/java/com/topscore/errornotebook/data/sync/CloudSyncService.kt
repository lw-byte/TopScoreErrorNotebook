package com.topscore.errornotebook.data.sync

import android.content.Context
import android.util.Base64
import com.topscore.errornotebook.core.database.dao.ImageDao
import com.topscore.errornotebook.core.database.dao.QuestionDao
import com.topscore.errornotebook.core.database.entity.QuestionEntity
import com.topscore.errornotebook.core.database.entity.QuestionImageEntity
import com.topscore.errornotebook.core.database.toDomain
import com.topscore.errornotebook.domain.model.Question
import com.topscore.errornotebook.domain.model.SyncStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud Sync Service
 *
 * Handles synchronization of local data with Alibaba Cloud server.
 * Implements incremental sync with conflict resolution.
 *
 * Sync flow:
 * 1. Get local pending changes
 * 2. Send changes to server
 * 3. Receive server changes
 * 4. Apply changes to local database
 * 5. Update sync status
 */
@Singleton
class CloudSyncService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val questionDao: QuestionDao,
    private val imageDao: ImageDao
) {
    companion object {
        private const val HMAC_SHA256 = "HmacSHA256"
        private const val ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    }

    /**
     * Sync result
     */
    data class SyncResult(
        val success: Boolean,
        val syncedQuestions: Int = 0,
        val syncedImages: Int = 0,
        val conflicts: Int = 0,
        val errorMessage: String? = null
    )

    /**
     * Sync mode
     */
    enum class SyncMode {
        INCREMENTAL,  // Only sync changes since last sync
        FULL          // Full sync of all data
    }

    /**
     * Perform cloud sync
     *
     * @param authToken User authentication token
     * @param mode Sync mode (incremental or full)
     * @return SyncResult with sync statistics
     */
    suspend fun sync(authToken: String, mode: SyncMode = SyncMode.INCREMENTAL): SyncResult =
        withContext(Dispatchers.IO) {
            try {
                // Step 1: Get pending local changes
                val pendingQuestions = getPendingQuestions()
                val pendingImages = getPendingImages()

                // Step 2: Prepare sync payload
                val payload = buildSyncPayload(pendingQuestions, pendingImages, mode)

                // Step 3: Send sync request to server
                val response = sendSyncRequest(authToken, payload, mode)

                // Step 4: Process server response
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "{}"
                    val syncResult = parseSyncResponse(responseBody)

                    // Step 5: Update local sync status
                    updateLocalSyncStatus(pendingQuestions, pendingImages, syncResult)

                    SyncResult(
                        success = true,
                        syncedQuestions = pendingQuestions.size,
                        syncedImages = pendingImages.size,
                        conflicts = syncResult.conflicts
                    )
                } else {
                    SyncResult(
                        success = false,
                        errorMessage = "Sync failed: ${response.code} - ${response.message}"
                    )
                }
            } catch (e: Exception) {
                SyncResult(
                    success = false,
                    errorMessage = e.message ?: "Unknown sync error"
                )
            }
        }

    /**
     * Get questions that need to be synced (pending status)
     */
    private suspend fun getPendingQuestions(): List<QuestionEntity> {
        return questionDao.getPendingSyncQuestions()
    }

    /**
     * Get images that need to be synced (pending status)
     */
    private suspend fun getPendingImages(): List<QuestionImageEntity> {
        return imageDao.getImagesBySyncStatus(SyncStatus.PENDING.name)
    }

    /**
     * Build sync payload for API request
     */
    private fun buildSyncPayload(
        questions: List<QuestionEntity>,
        images: List<QuestionImageEntity>,
        mode: SyncMode
    ): JSONObject {
        val payload = JSONObject()

        payload.put("syncMode", if (mode == SyncMode.FULL) CloudSyncConfig.MODE_FULL else CloudSyncConfig.MODE_INCREMENTAL)
        payload.put("clientId", getClientId())
        payload.put("timestamp", getTimestamp())

        // Add questions
        val questionsArray = JSONArray()
        questions.forEach { entity ->
            val questionJson = JSONObject().apply {
                put("id", entity.id)
                put("userId", entity.userId)
                put("imageId", entity.imageId)
                put("stage", entity.stage)
                put("subject", entity.subject)
                put("errorReason", entity.errorReason)
                put("source", entity.source)
                put("questionType", entity.questionType)
                put("errorDate", entity.errorDate)
                put("correctAnswer", entity.correctAnswer)
                put("wrongAnswer", entity.wrongAnswer)
                put("notes", entity.notes)
                put("tags", JSONArray(entity.tags?.split(",")?.filter { it.isNotBlank() } ?: listOf<String>()))
                put("status", entity.status)
                put("createdAt", entity.createdAt)
                put("updatedAt", entity.updatedAt)
            }
            questionsArray.put(questionJson)
        }
        payload.put("questions", questionsArray)

        // Add images (only metadata, actual image upload is handled separately)
        val imagesArray = JSONArray()
        images.forEach { entity ->
            val imageJson = JSONObject().apply {
                put("id", entity.id)
                put("userId", entity.userId)
                put("originalImageUrl", entity.originalImageUrl)
                put("originalImageLocal", entity.originalImageLocal)
                put("recognizedText", entity.recognizedText)
                put("isComplete", entity.isComplete)
                put("hasHandwriting", entity.hasHandwriting)
                put("ocrConfidence", entity.ocrConfidence)
                put("createdAt", entity.createdAt)
            }
            imagesArray.put(imageJson)
        }
        payload.put("images", imagesArray)

        return payload
    }

    /**
     * Send sync request to server
     */
    private suspend fun sendSyncRequest(
        authToken: String,
        payload: JSONObject,
        mode: SyncMode
    ): okhttp3.Response = withContext(Dispatchers.IO) {
        val url = "${CloudSyncConfig.SYNC_SERVER_URL}${CloudSyncConfig.ENDPOINT_SYNC}?mode=${mode.name.lowercase()}"

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("X-Client-Id", getClientId())
            .addHeader("X-Timestamp", getTimestamp())
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute()
    }

    /**
     * Parse sync response from server
     */
    private fun parseSyncResponse(responseBody: String): ServerSyncResult {
        val json = JSONObject(responseBody)

        // Check for error
        if (json.has("error")) {
            throw IOException("Server sync error: ${json.getString("error")}")
        }

        val data = json.optJSONObject("data") ?: JSONObject()

        val syncedQuestions = data.optInt("syncedQuestions", 0)
        val syncedImages = data.optInt("syncedImages", 0)
        val conflicts = data.optInt("conflicts", 0)

        // Process server updates if any
        val serverQuestions = data.optJSONArray("questions")
        val serverImages = data.optJSONArray("images")

        // Apply server updates to local database (in coroutine context)
        serverQuestions?.let {
            kotlinx.coroutines.runBlocking {
                applyServerQuestions(it)
            }
        }
        serverImages?.let {
            kotlinx.coroutines.runBlocking {
                applyServerImages(it)
            }
        }

        return ServerSyncResult(syncedQuestions, syncedImages, conflicts)
    }

    /**
     * Update local sync status after successful sync
     */
    private suspend fun updateLocalSyncStatus(
        questions: List<QuestionEntity>,
        images: List<QuestionImageEntity>,
        result: ServerSyncResult
    ) {
        questions.forEach { entity ->
            questionDao.updateSyncStatus(entity.id, SyncStatus.SYNCED.name)
        }

        images.forEach { entity ->
            imageDao.updateSyncStatus(entity.id, SyncStatus.SYNCED.name)
        }
    }

    /**
     * Apply questions received from server
     */
    private suspend fun applyServerQuestions(questionsArray: JSONArray) {
        for (i in 0 until questionsArray.length()) {
            val questionJson = questionsArray.getJSONObject(i)
            val question = QuestionEntity(
                id = questionJson.getLong("id"),
                userId = questionJson.getLong("userId"),
                imageId = questionJson.getLong("imageId"),
                stage = questionJson.getString("stage"),
                subject = questionJson.getString("subject"),
                errorReason = questionJson.getString("errorReason"),
                source = questionJson.optString("source"),
                questionType = questionJson.optString("questionType"),
                errorDate = questionJson.getLong("errorDate"),
                correctAnswer = questionJson.optString("correctAnswer"),
                wrongAnswer = questionJson.optString("wrongAnswer"),
                notes = questionJson.optString("notes"),
                tags = questionJson.optJSONArray("tags")?.let { arr ->
                    (0 until arr.length()).joinToString(",") { arr.getString(it) }
                } ?: "",
                status = questionJson.getString("status"),
                syncStatus = SyncStatus.SYNCED.name,
                createdAt = questionJson.getLong("createdAt"),
                updatedAt = questionJson.getLong("updatedAt")
            )
            questionDao.insertQuestion(question)
        }
    }

    /**
     * Apply images received from server
     */
    private suspend fun applyServerImages(imagesArray: JSONArray) {
        for (i in 0 until imagesArray.length()) {
            val imageJson = imagesArray.getJSONObject(i)
            val image = QuestionImageEntity(
                id = imageJson.getLong("id"),
                userId = imageJson.getLong("userId"),
                originalImageUrl = imageJson.optString("originalImageUrl"),
                originalImageLocal = imageJson.optString("originalImageLocal"),
                recognizedText = imageJson.optString("recognizedText"),
                isComplete = imageJson.optBoolean("isComplete"),
                hasHandwriting = imageJson.optBoolean("hasHandwriting"),
                ocrConfidence = imageJson.optDouble("ocrConfidence").toFloat(),
                syncStatus = SyncStatus.SYNCED.name,
                createdAt = imageJson.getLong("createdAt")
            )
            imageDao.insertImage(image)
        }
    }

    /**
     * Get unique client ID for this device
     */
    private fun getClientId(): String {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        var clientId = prefs.getString("client_id", null)
        if (clientId == null) {
            clientId = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("client_id", clientId).apply()
        }
        return clientId
    }

    /**
     * Get current timestamp for API requests
     */
    private fun getTimestamp(): String {
        val dateFormat = SimpleDateFormat(ISO8601_FORMAT, Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(Date())
    }

    /**
     * Internal data class for parsing server response
     */
    private data class ServerSyncResult(
        val syncedQuestions: Int,
        val syncedImages: Int,
        val conflicts: Int
    )
}