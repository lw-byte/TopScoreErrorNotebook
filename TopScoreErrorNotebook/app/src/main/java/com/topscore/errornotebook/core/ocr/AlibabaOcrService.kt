package com.topscore.errornotebook.core.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.aliyun.ocr_api20210707.Client
import com.aliyun.ocr_api20210707.models.RecognizeEduQuestionOcrRequest
import com.aliyun.ocr_api20210707.models.RecognizeEduQuestionOcrResponse
import com.aliyun.tea.TeaException
import com.aliyun.teaopenapi.models.Config
import com.aliyun.teautil.models.RuntimeOptions
import com.topscore.errornotebook.domain.model.OcrResult
import com.topscore.errornotebook.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Alibaba Cloud OCR Service
 *
 * Provides OCR functionality using Alibaba Cloud's official OCR SDK (com.aliyun:ocr_api20210707).
 * Uses RecognizeEduQuestionOcr for single question recognition in educational papers.
 *
 * @see <a href="https://help.aliyun.com/zh/ocr/developer-reference/api-ocr-api-2021-07-07-recognizeeduquestionocr">教育试卷OCR单题识别 API文档</a>
 */
@Singleton
class AlibabaOcrService @Inject constructor() {

    private val client: Client by lazy {
        Logger.OCR.d("Initializing Alibaba OCR Client")
        Logger.OCR.d("Endpoint: ${AlibabaOcrConfig.OCR_API_ENDPOINT}")
        Logger.OCR.d("AccessKeyId: ${AlibabaOcrConfig.ACCESS_KEY_ID.take(4)}***")

        val config = Config().apply {
            accessKeyId = AlibabaOcrConfig.ACCESS_KEY_ID
            accessKeySecret = AlibabaOcrConfig.ACCESS_KEY_SECRET
            endpoint = AlibabaOcrConfig.OCR_API_ENDPOINT
        }

        Logger.OCR.d("Client created successfully")
        Client(config)
    }

    /**
     * Recognize text from an image file path
     */
    suspend fun recognizeText(imagePath: String): Result<OcrResult> = withContext(Dispatchers.IO) {
        Logger.OCR.d("recognizeText called with path: $imagePath")
        try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
                ?: run {
                    Logger.OCR.e("Failed to decode image at path: $imagePath")
                    return@withContext Result.failure(IOException("Failed to decode image"))
                }

            Logger.OCR.d("Image decoded successfully, size: ${bitmap.width}x${bitmap.height}")
            return@withContext recognizeText(bitmap)
        } catch (e: Exception) {
            Logger.OCR.e("Exception in recognizeText", e)
            Result.failure(e)
        }
    }

    /**
     * Recognize text from a bitmap
     */
    suspend fun recognizeText(bitmap: Bitmap): Result<OcrResult> = withContext(Dispatchers.IO) {
        Logger.OCR.d("recognizeText called with bitmap: ${bitmap.width}x${bitmap.height}")
        try {
            val imageBytes = bitmapToByteArray(bitmap)
            Logger.OCR.d("Bitmap converted to bytes, size: ${imageBytes.size}")
            return@withContext callEduQuestionOcrApi(imageBytes)
        } catch (e: Exception) {
            Logger.OCR.e("Exception in recognizeText bitmap", e)
            Result.failure(e)
        }
    }

    /**
     * Recognize text from base64 encoded image
     */
    suspend fun recognizeTextFromBase64(base64Image: String): Result<OcrResult> = withContext(Dispatchers.IO) {
        Logger.OCR.d("recognizeTextFromBase64 called, length: ${base64Image.length}")
        try {
            val imageBytes = Base64.decode(base64Image, Base64.NO_WRAP)
            Logger.OCR.d("Base64 decoded to bytes, size: ${imageBytes.size}")
            return@withContext callEduQuestionOcrApi(imageBytes)
        } catch (e: Exception) {
            Logger.OCR.e("Exception in recognizeTextFromBase64", e)
            Result.failure(e)
        }
    }

    /**
     * Call Alibaba OCR EduQuestionOcr API with image bytes
     */
    private fun callEduQuestionOcrApi(imageBytes: ByteArray): Result<OcrResult> {
        Logger.OCR.d("callEduQuestionOcrApi started")
        Logger.OCR.d("Building RecognizeEduQuestionOcrRequest")
        Logger.OCR.d("Image bytes size: ${imageBytes.size} (${imageBytes.size / 1024}KB)")

        // Create InputStream from image bytes
        val imageInputStream = ByteArrayInputStream(imageBytes)

        val request = RecognizeEduQuestionOcrRequest().apply {
            body = imageInputStream
            needRotate = true  // Enable auto-rotation for better recognition
        }
        Logger.OCR.d("RecognizeEduQuestionOcrRequest built with body InputStream, needRotate=true")

        val runtime = RuntimeOptions().apply {
            connectTimeout = 30000
            readTimeout = 30000
        }
        Logger.OCR.d("RuntimeOptions: connectTimeout=30000, readTimeout=30000")

        return try {
            Logger.OCR.i("Calling OCR API: client.recognizeEduQuestionOcrWithOptions()")
            val startTime = System.currentTimeMillis()

            val response: RecognizeEduQuestionOcrResponse = client.recognizeEduQuestionOcrWithOptions(request, runtime)

            val elapsed = System.currentTimeMillis() - startTime
            Logger.OCR.i("OCR API call succeeded in ${elapsed}ms")

            val ocrResult = parseOcrResponse(response)
            Logger.OCR.d("Parsed OCR result: textLength=${ocrResult.text.length}, confidence=${ocrResult.confidence}")

            Result.success(ocrResult)
        } catch (e: UnknownHostException) {
            Logger.OCR.e("Network error: Unable to resolve host - ${e.message}")
            Logger.OCR.e("DNS resolution failed for endpoint: ${AlibabaOcrConfig.OCR_API_ENDPOINT}")
            Logger.OCR.w("Falling back to mock data due to network error")
            Result.success(getMockOcrResult(errorCode = "NetworkError", errorMessage = "无法连接到OCR服务器: ${e.message}"))
        } catch (e: TeaException) {
            val errorCode = e.code ?: "Unknown"
            val errorMsg = e.message ?: "Unknown error"
            val requestId = e.data?.get("requestId")?.toString() ?: ""
            val fullErrorMsg = if (requestId.isNotEmpty()) "$errorMsg ($requestId)" else errorMsg

            Logger.OCR.e("TeaException from OCR API: code=$errorCode, message=$errorMsg")
            Logger.OCR.e("RequestId: $requestId")

            // Return OcrResult with error info instead of failure, so UI can display it
            val errorOcrResult = OcrResult(
                text = "",
                isComplete = false,
                hasHandwriting = false,
                handwritingRegions = null,
                confidence = 0f,
                errorCode = errorCode,
                errorMessage = fullErrorMsg
            )
            Result.success(errorOcrResult)
        } catch (e: Exception) {
            Logger.OCR.e("Unexpected exception in callEduQuestionOcrApi: ${e::class.java.name}")
            Logger.OCR.e("Exception message: ${e.message}", e)
            Logger.OCR.w("Falling back to mock data due to unexpected error")
            Result.success(getMockOcrResult(errorCode = "Exception", errorMessage = e.message ?: "Unknown error"))
        }
    }

    /**
     * Word block data class for structured text parsing
     */
    private data class WordBlock(
        val word: String,
        val x: Int,
        val y: Int,
        val prob: Double
    )

    /**
     * Parse Alibaba OCR SDK response to OcrResult
     */
    private fun parseOcrResponse(response: RecognizeEduQuestionOcrResponse): OcrResult {
        Logger.OCR.d("parseOcrResponse started")

        val text = StringBuilder()
        var totalProbability = 0.0
        var count = 0

        // Access response body - data is a JSON string in body.data
        val body = response.body
        if (body == null) {
            Logger.OCR.w("Response body is null")
            return OcrResult(
                text = "",
                isComplete = false,
                hasHandwriting = false,
                handwritingRegions = null,
                confidence = 0f
            )
        }

        val dataJsonStr: String? = body.data
        if (dataJsonStr == null || dataJsonStr.isEmpty()) {
            Logger.OCR.w("Response body data is null or empty")
            return OcrResult(
                text = "",
                isComplete = false,
                hasHandwriting = false,
                handwritingRegions = null,
                confidence = 0f
            )
        }

        Logger.OCR.d("Found data JSON, length: ${dataJsonStr.length}")
        Logger.OCR.d("Data JSON preview: ${dataJsonStr.take(200)}")

        // Use JSONObject to properly parse JSON data
        try {
            val dataJson = JSONObject(dataJsonStr)

            // Extract prism_wordsInfo for structured text with line breaks
            val prismWordsInfo = dataJson.optJSONArray("prism_wordsInfo")
            if (prismWordsInfo != null && prismWordsInfo.length() > 0) {
                Logger.OCR.d("Found ${prismWordsInfo.length()} word blocks, building structured text")

                // Parse word blocks with position info
                val words = mutableListOf<WordBlock>()
                for (i in 0 until prismWordsInfo.length()) {
                    val wordInfo = prismWordsInfo.getJSONObject(i)
                    val word = wordInfo.optString("word", "")
                    val x = wordInfo.optInt("x", 0)
                    val y = wordInfo.optInt("y", 0)
                    val prob = wordInfo.optDouble("prob", 0.0)

                    if (word.isNotBlank()) {
                        words.add(WordBlock(word, x, y, prob))
                    }
                    if (prob > 0) {
                        totalProbability += prob
                        count++
                    }
                }

                // Sort words by y position (line), then x position
                words.sortWith(compareBy({ it.y }, { it.x }))

                // Group words into lines based on y position (threshold: 30 pixels)
                val lineThreshold = 30
                val lines = mutableListOf<List<WordBlock>>()
                var currentLine = mutableListOf<WordBlock>()
                var lastY = Int.MIN_VALUE

                for (word in words) {
                    if (currentLine.isEmpty() || kotlin.math.abs(word.y - lastY) <= lineThreshold) {
                        currentLine.add(word)
                        lastY = word.y
                    } else {
                        lines.add(currentLine)
                        currentLine = mutableListOf(word)
                        lastY = word.y
                    }
                }
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }

                // Build text with line breaks
                for ((lineIndex, line) in lines.withIndex()) {
                    val lineText = line.joinToString(" ") { it.word }
                    text.append(lineText)
                    if (lineIndex < lines.size - 1) {
                        text.append("\n")
                    }
                }

                Logger.OCR.d("Built ${lines.size} lines from ${words.size} words")
            } else {
                // Fallback: use content field directly
                val content = dataJson.optString("content", "")
                if (content.isNotEmpty()) {
                    Logger.OCR.d("Found content, length: ${content.length}")
                    text.append(content)
                }
            }
        } catch (e: Exception) {
            Logger.OCR.e("Failed to parse JSON data: ${e.message}")
            // Fallback: try to extract content manually
            val content = extractJsonStringField(dataJsonStr, "content")
            if (content.isNotEmpty()) {
                text.append(content)
            }
        }

        val avgProbability = if (count > 0) totalProbability / count else if (text.isNotEmpty()) 0.85 else 0.0
        Logger.OCR.d("Parsed text length: ${text.length}, avgProbability: $avgProbability, wordCount: $count")

        return OcrResult(
            text = text.toString(),
            isComplete = text.isNotEmpty(),
            hasHandwriting = false,
            handwritingRegions = null,
            confidence = avgProbability.toFloat()
        )
    }

    /**
     * Extract a string field value from a JSON string (simple parser)
     * Used as fallback when JSONObject parsing fails
     */
    private fun extractJsonStringField(json: String, fieldName: String): String {
        val pattern = "\"$fieldName\"\\s*:\\s*\""
        val index = json.indexOf(pattern)
        if (index == -1) return ""

        val startIndex = index + pattern.length
        var endIndex = startIndex
        val sb = StringBuilder()

        while (endIndex < json.length) {
            val c = json[endIndex]
            if (c == '\\' && endIndex + 1 < json.length) {
                // Escape sequence
                sb.append(json[endIndex + 1])
                endIndex += 2
            } else if (c == '"') {
                break
            } else {
                sb.append(c)
                endIndex++
            }
        }

        return sb.toString()
    }

    /**
     * Get mock OCR result for testing when network is unavailable
     */
    private fun getMockOcrResult(errorCode: String? = null, errorMessage: String? = null): OcrResult {
        Logger.OCR.w("Returning mock OCR result - errorCode: $errorCode, errorMessage: $errorMessage")
        return OcrResult(
            text = "【Mock OCR Result - OCR Service Unavailable】\n\nEndpoint: ${AlibabaOcrConfig.OCR_API_ENDPOINT}",
            isComplete = true,
            hasHandwriting = false,
            handwritingRegions = null,
            confidence = 0.5f,
            errorCode = errorCode,
            errorMessage = errorMessage
        )
    }

    /**
     * Convert bitmap to byte array, ensuring size is under 10MB for API limit
     */
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val maxSizeBytes = 10 * 1024 * 1024  // 10MB limit

        Logger.OCR.d("Original bitmap: ${bitmap.width}x${bitmap.height}")

        // First, try JPEG compression at 90% quality
        var outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        var byteArray = outputStream.toByteArray()
        Logger.OCR.d("JPEG 90%: ${byteArray.size} bytes (${byteArray.size / 1024}KB)")

        // If still over 10MB, progressively reduce quality
        var quality = 90
        while (byteArray.size > maxSizeBytes && quality > 20) {
            outputStream = ByteArrayOutputStream()
            quality -= 10
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            byteArray = outputStream.toByteArray()
            Logger.OCR.d("JPEG $quality%: ${byteArray.size} bytes (${byteArray.size / 1024}KB)")
        }

        // If still over 10MB, resize the image
        if (byteArray.size > maxSizeBytes) {
            Logger.OCR.w("Image still over 10MB after compression, will resize")
            var scale = 1.0
            var scaledBitmap = bitmap
            while (byteArray.size > maxSizeBytes && scale > 0.3) {
                scale -= 0.1
                val newWidth = (bitmap.width * scale).toInt()
                val newHeight = (bitmap.height * scale).toInt()
                scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                byteArray = outputStream.toByteArray()
                Logger.OCR.d("Resized ${(scale * 100).toInt()}%: ${byteArray.size} bytes (${byteArray.size / 1024}KB)")
            }
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
        }

        Logger.OCR.d("Final image size: ${byteArray.size} bytes (${byteArray.size / 1024}KB, ${byteArray.size / (1024 * 1024)}MB)")
        return byteArray
    }
}
