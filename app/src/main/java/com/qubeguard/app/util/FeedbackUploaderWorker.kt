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
import java.util.concurrent.TimeUnit

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
        try {
            val feedback = feedbackCollector.getUnuploadedFeedback()
            if (feedback.isEmpty()) return@withContext Result.success()
            val payload = JSONArray().apply {
                feedback.forEach { item ->
                    put(JSONObject().apply {
                        put("url", item.url)
                        put("decision", item.decision)
                        put("category", item.category)
                        put("confidence", item.confidence)
                        put("timestamp", item.timestamp)
                    })
                }
            }
            val request = Request.Builder()
                .url(Constants.FEEDBACK_UPLOAD_URL)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    feedbackCollector.markAllAsUploaded()
                    Result.success()
                } else Result.retry()
            }
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object { const val WORK_NAME = "FeedbackUploaderWorker" }
}
