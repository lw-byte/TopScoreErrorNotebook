# 阿里云 OCR SDK 调试总结

## 时间
2026年6月

## 调试过程关键点

### 1. SDK 版本选择

#### 最初错误选择
- 使用了 `com.aliyun:ocr20191230` (版本 2.0.3)
- 这是旧版 API（2019-12-30），**已被阿里云弃用**

#### 问题表现
```
Unable to resolve host "ocr.cn-hangzhou.aliyuncs.com": No address associated with hostname
```

#### 最终选择
- 改用 `com.aliyun:ocr_api20210707` (版本 3.1.3)
- 这是阿里云当前推荐的 OCR API 版本

### 2. Endpoint 地址

| SDK 版本 | Endpoint | 状态 |
|---------|----------|------|
| ocr20191230 | `ocr.cn-hangzhou.aliyuncs.com` | ❌ 已弃用，DNS无法解析 |
| ocr_api20210707 | `ocr-api.cn-hangzhou.aliyuncs.com` | ✅ 正确 |

### 3. API 接口选择

使用 **教育试卷OCR单题识别** 接口：
- 接口名：`RecognizeEduQuestionOcr`
- 适用场景：扫描/拍照场景的单题题目识别
- 方法：`client.recognizeEduQuestionOcrWithOptions()`

### 4. 请求参数

```kotlin
val request = RecognizeEduQuestionOcrRequest().apply {
    body = ByteArrayInputStream(imageBytes)  // 图片二进制，最大10MB
    needRotate = true  // 启用自动旋转
}
```

**注意**：`body` 参数接受 InputStream，不是 base64 字符串。

### 5. 图片大小限制

阿里云 OCR API 限制：
- 最大 10MB（教育试卷单题识别）
- 推荐压缩到 4MB 以下以提高传输效率

#### 压缩策略
```kotlin
private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
    val maxSizeBytes = 4 * 1024 * 1024  // 4MB

    // 1. JPEG 85% 质量压缩
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)

    // 2. 逐步降低质量 (75%, 65%...)
    while (byteArray.size > maxSizeBytes && quality > 20) {
        quality -= 10
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    }

    // 3. 如仍超标，缩小图片尺寸 (90%, 80%...)
    while (byteArray.size > maxSizeBytes && scale > 0.3) {
        scale -= 0.1
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
```

### 6. 响应解析

#### 响应结构
```kotlin
val response: RecognizeEduQuestionOcrResponse = client.recognizeEduQuestionOcrWithOptions(request, runtime)
// response.body.data 是 JSON 字符串
```

#### JSON 数据结构
```json
{
  "content": "识别出的文字内容",
  "prism_wordsInfo": [
    {
      "word": "文字块内容",
      "prob": 99.0,
      "rectangle": {...}
    }
  ],
  "height": 3509,
  "width": 2512
}
```

### 7. JSON 解析问题

#### 问题表现
日志显示 `content` 有内容，但 `Parsed text length: 0`

#### 原因
简单的字符串解析器无法处理转义字符，如 `\\frac`

#### 解决方案
使用 `org.json.JSONObject` 正确解析：
```kotlin
val dataJson = JSONObject(dataJsonStr)
val content = dataJson.optString("content", "")
val prismWordsInfo = dataJson.optJSONArray("prism_wordsInfo")
```

### 8. 错误信息展示

#### OcrResult 数据类
```kotlin
data class OcrResult(
    val text: String,
    val isComplete: Boolean,
    val hasHandwriting: Boolean,
    val handwritingRegions: List<Rect>? = null,
    val confidence: Float,
    val errorCode: String? = null,    // 如 "ClientError.413"
    val errorMessage: String? = null   // 如 "Request Entity Too Large"
)
```

#### 界面显示格式
```
识别失败:
<Code>ClientError.413</Code>
<Message>Request Entity Too Large (5130418f5fbd9e3e15d06226892aa26c)</Message>
```

### 9. 错误处理

```kotlin
catch (e: TeaException) {
    // 阿里云 SDK 业务异常
    val errorCode = e.code ?: "Unknown"
    val requestId = e.data?.get("requestId")?.toString() ?: ""
    // 返回带错误信息的 OcrResult
}

catch (e: UnknownHostException) {
    // DNS 解析失败，网络问题
    Result.success(getMockOcrResult(...))
}
```

## 关键配置

### build.gradle.kts
```kotlin
implementation("com.aliyun:ocr_api20210707:3.1.3")
```

### AlibabaOcrConfig.kt
```kotlin
object AlibabaOcrConfig {
    const val ACCESS_KEY_ID = "YOUR_ACCESS_KEY_ID"
    const val ACCESS_KEY_SECRET = "YOUR_ACCESS_KEY_SECRET"
    const val OCR_API_ENDPOINT = "ocr-api.cn-hangzhou.aliyuncs.com"
}
```

## 服务开通要求

使用教育试卷OCR单题识别接口需要：
1. 开通阿里云教育场景识别服务
2. 购买题目识别资源包（API有免费额度）
3. RAM授权：`AliyunOCRFullAccess`

## 日志标签

使用 `Logger.OCR` 打印调试日志：
```kotlin
Logger.OCR.d("OCR识别成功，置信度: ${ocrResult.confidence}")
Logger.OCR.e("OCR失败: code=${e.code}, message=${e.message}")
```

## 参考文档

- [教育试卷OCR单题识别 API文档](https://help.aliyun.com/zh/ocr/developer-reference/api-ocr-api-2021-07-07-recognizeeduquestionocr)
- [阿里云 OCR SDK Java](https://github.com/aliyun/alibabacloud-java-sdk)
- [服务接入点各地域的公网与VPC接入地址](https://help.aliyun.com/zh/ocr/developer-reference/api-ocr-api-2021-07-07-endpoint)
