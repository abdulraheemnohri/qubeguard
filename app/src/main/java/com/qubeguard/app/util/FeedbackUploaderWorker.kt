package com.qubeguard.app.util

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.qubeguard.app.policy.FeedbackCollector
import com.qubeguard.app.policy.FeedbackDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * WorkManager worker for uploading user feedback to the server.
 * Only uploads feedback if telemetry is enabled.
 */
@HiltWorker
class FeedbackUploaderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val feedbackCollector: FeedbackCollector,
    private val feedbackDao: FeedbackDao
) : CoroutineWorker(context, workerParams) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            // Get unuploaded feedback
            val unuploadedFeedback = feedbackCollector.getUnuploadedFeedback()
            if (unuploadedFeedback.isEmpty()) {
                return@withContext Result.success()
            }

            // Prepare feedback data for upload (anonymized)
            val feedbackData = JSONArray().apply {
                for (feedback in unuploadedFeedback) {
                    val jsonObject = JSONObject().apply {
                        put("url", feedback.url)
                        put("decision", feedback.decision)
                        put("category", feedback.category)
                        put("confidence", feedback.confidence)
                        put("timestamp", feedback.timestamp)
                        // Do NOT include qubeId or any PII
                    }
                    put(jsonObject)
                }
            }

            // Upload feedback to the server
            val requestBody = feedbackData.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(Constants.FEEDBACK_UPLOAD_URL)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful) {
                // Mark feedback as uploaded
                feedbackCollector.markAllAsUploaded()
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "FeedbackUploaderWorker"
    }
}
