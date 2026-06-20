package com.topscore.errornotebook.core.ocr

/**
 * Alibaba Cloud OCR Configuration
 *
 * To obtain AccessKey credentials:
 * 1. Go to Alibaba Cloud RAM console (https://ram.console.aliyun.com)
 * 2. Create an AccessKey or use an existing one
 * 3. For production, use RAM user with appropriate OCR permissions
 *
 * For OCR service, enable the following in Alibaba Cloud Visual Intelligence Platform:
 * - General text OCR (通用文字识别) - RecognizeCharacter API
 *
 * @see <a href="https://help.aliyun.com/zh/viapi/use-cases/general-character-recognition">Alibaba OCR SDK Documentation</a>
 */
object AlibabaOcrConfig {
    // TODO: Replace with actual AccessKey credentials
    // Get your AccessKey from: https://ram.console.aliyun.com/manage/ak
    const val ACCESS_KEY_ID = "YOUR_ACCESS_KEY_ID_PLACEHOLDER"
    const val ACCESS_KEY_SECRET = "YOUR_ACCESS_KEY_SECRET_PLACEHOLDER"

    // OCR API Endpoint for SDK
    // Use the region-specific endpoint for the OCR API
    // Refer to: https://help.aliyun.com/zh/ocr/developer-reference/regions-and-endpoints
    const val OCR_API_ENDPOINT = "ocr-api.cn-hangzhou.aliyuncs.com"
}
