package com.topscore.errornotebook.core.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.topscore.errornotebook.domain.model.OcrResult
import com.topscore.errornotebook.domain.model.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
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
 * Alibaba Cloud OCR Service
 *
 * Provides OCR functionality using Alibaba Cloud's OCR API.
 * Supports general text recognition and handwritten text recognition.
 *
 * @see <a href="https://help.aliyun.com/zh/ocr/developer-reference/sdk-overview">Alibaba OCR SDK Documentation</a>
 */
@Singleton
class AlibabaOcrService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val HMAC_SHA256 = "HmacSHA256"
        private const val ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    }

    /**
     * Recognize text from an image file path
     */
    suspend fun recognizeText(imagePath: String): Result<OcrResult> = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
                ?: return@withContext Result.failure(IOException("Failed to decode image"))

            val base64Image = bitmapToBase64(bitmap)
            return@withContext callAlibabaOcrApi(base64Image)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recognize text from a bitmap
     */
    suspend fun recognizeText(bitmap: Bitmap): Result<OcrResult> = withContext(Dispatchers.IO) {
        try {
            val base64Image = bitmapToBase64(bitmap)
            return@withContext callAlibabaOcrApi(base64Image)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recognize text from base64 encoded image
     */
    suspend fun recognizeTextFromBase64(base64Image: String): Result<OcrResult> = withContext(Dispatchers.IO) {
        try {
            return@withContext callAlibabaOcrApi(base64Image)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Call Alibaba OCR API with the given base64 image
     */
    private fun callAlibabaOcrApi(base64Image: String): Result<OcrResult> {
        // Build the request body for Alibaba OCR API
        // Note: This uses the Alibaba Cloud general text OCR API
        // For production, you may need to use their SDK or different API endpoint

        val requestBody = buildString {
            append("{")
            append("\"image\":\"${base64Image}\",")
            append("\"prob\":\"true\",")
            append("\"rotate\":\"true\",")
            append("\"characterType\":\"mixed\"")
            append("}")
        }

        val request = Request.Builder()
            .url("${AlibabaOcrConfig.OCR_API_ENDPOINT}/?Action=${AlibabaOcrConfig.ACTION_RECOGNIZE_TEXT}&Format=JSON&Version=${AlibabaOcrConfig.SERVICE_VERSION}")
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Accept-Language", "zh")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(request).execute()

        return if (response.isSuccessful) {
            val responseBody = response.body?.string() ?: ""
            try {
                val ocrResult = parseAlibabaOcrResponse(responseBody)
                Result.success(ocrResult)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.failure(IOException("OCR API error: ${response.code} - ${response.message}"))
        }
    }

    /**
     * Parse Alibaba OCR API response to OcrResult
     */
    private fun parseAlibabaOcrResponse(jsonString: String): OcrResult {
        // Parse Alibaba OCR API response
        // The response format varies based on the API version and type
        // This is a generic parser for the general text OCR API

        val json = JSONObject(jsonString)

        // Check for error response
        if (json.has("Message")) {
            throw IOException("OCR API error: ${json.getString("Message")}")
        }

        // Parse the result based on Alibaba OCR response format
        val text = StringBuilder()
        val handwritingRegions = mutableListOf<Rect>()
        var hasHandwriting = false
        var totalConfidence = 0f
        var count = 0

        // Alibaba OCR response typically contains "data" with "text" or "result" array
        val data = json.optJSONObject("data") ?: json
        val result = data.optJSONArray("result")
            ?: data.optJSONArray("TextDetectionResults")
            ?: data.optJSONArray("data")

        if (result != null) {
            for (i in 0 until result.length()) {
                val item = result.getJSONObject(i)
                val detectedText = item.optString("text")
                    ?: item.optString("Text")
                    ?: item.optString("content", "")

                if (detectedText.isNotBlank()) {
                    if (text.isNotEmpty()) {
                        text.append("\n")
                    }
                    text.append(detectedText)
                }

                // Check for handwriting detection if available
                val isHandwriting = item.optBoolean("handwriting", false)
                    || item.optBoolean("isHandwriting", false)
                if (isHandwriting) {
                    hasHandwriting = true
                    // Try to extract region if available
                    val box = item.optJSONObject("box") ?: item.optJSONObject("rectangle")
                    if (box != null) {
                        handwritingRegions.add(
                            Rect(
                                x = box.optDouble("left", box.optDouble("x", 0.0)).toFloat(),
                                y = box.optDouble("top", box.optDouble("y", 0.0)).toFloat(),
                                width = box.optDouble("width", 0.0).toFloat(),
                                height = box.optDouble("height", 0.0).toFloat()
                            )
                        )
                    }
                }

                // Get confidence if available
                val confidence = item.optDouble("confidence", item.optDouble("score", 0.0))
                if (confidence > 0) {
                    totalConfidence += confidence.toFloat()
                    count++
                }
            }
        } else {
            // Fallback: try to get text directly
            val directText = data.optString("text")
                ?: data.optString("content")
                ?: data.optString("Text", "")
            text.append(directText)
        }

        val avgConfidence = if (count > 0) totalConfidence / count else 0.85f

        return OcrResult(
            text = text.toString(),
            isComplete = text.isNotEmpty(),
            hasHandwriting = hasHandwriting,
            handwritingRegions = if (handwritingRegions.isNotEmpty()) handwritingRegions else null,
            confidence = avgConfidence
        )
    }

    /**
     * Convert bitmap to base64 string
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Compress to reduce size while maintaining readability
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Build Alibaba Cloud API signature
     * This is used for authenticated API calls
     *
     * Note: For production use with Alibaba Cloud APIs,
     * you should use their official SDK which handles signature calculation
     */
    @Deprecated("Use official Alibaba SDK for production")
    private fun buildSignature(params: String, secret: String): String {
        val mac = Mac.getInstance(HMAC_SHA256)
        val secretKeySpec = SecretKeySpec(secret.toByteArray(), HMAC_SHA256)
        mac.init(secretKeySpec)
        val hash = mac.doFinal(params.toByteArray())
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    /**
     * Get ISO8601 timestamp for API requests
     */
    private fun getTimestamp(): String {
        val dateFormat = SimpleDateFormat(ISO8601_FORMAT, Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(Date())
    }
}