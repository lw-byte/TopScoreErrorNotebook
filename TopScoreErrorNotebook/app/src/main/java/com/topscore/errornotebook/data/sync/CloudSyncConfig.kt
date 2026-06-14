package com.topscore.errornotebook.data.sync

/**
 * Alibaba Cloud Sync Configuration
 *
 * To obtain server credentials:
 * 1. Go to Alibaba Cloud RAM console (https://ram.console.aliyun.com)
 * 2. Create an AccessKey or use an existing one for your backend server
 * 3. Set up your own backend server that interfaces with Alibaba Cloud services
 *
 * For production, recommended architecture:
 * - Mobile app calls your backend server
 * - Backend server calls Alibaba Cloud services using AccessKey
 * - This keeps AccessKey secure on the server side
 *
 * @see <a href="https://help.aliyun.com/zh/ram/use-cases/best-practices-for-programmatic-access-to-alibaba-cloud">Alibaba Cloud AccessKey Best Practices</a>
 */
object CloudSyncConfig {
    // TODO: Replace with your actual backend server URL
    // This should be your own backend server that interfaces with Alibaba Cloud
    const val SYNC_SERVER_URL = "https://api.topscore.example.com"

    // API endpoints
    const val ENDPOINT_SYNC = "/v1/sync"
    const val ENDPOINT_QUESTIONS = "/v1/questions"
    const val ENDPOINT_IMAGES = "/v1/images"
    const val ENDPOINT_USER = "/v1/user"

    // Sync configuration
    const val SYNC_TIMEOUT_SECONDS = 30L
    const val MAX_RETRY_ATTEMPTS = 3
    const val RETRY_DELAY_MILLIS = 1000L

    // Sync modes
    const val MODE_INCREMENTAL = "incremental"
    const val MODE_FULL = "full"

    // Placeholder AccessKey - in production, this would be on your server
    const val ACCESS_KEY_ID = "YOUR_ACCESS_KEY_ID_PLACEHOLDER"
    const val ACCESS_KEY_SECRET = "YOUR_ACCESS_KEY_SECRET_PLACEHOLDER"
}