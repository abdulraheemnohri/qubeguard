package com.qubeguard.app.ml

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Downloads and manages the TFLite model file from a remote server.
 * Ensures the model is up-to-date and available for the classifier.
 */
class ModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val modelUrl = "https://example.com/models/qubeguard_model.tflite" // Replace with actual URL
    private val modelFileName = "qubeguard_model.tflite"

    /**
     * Downloads the TFLite model from the remote server.
     * @return True if the download was successful.
     */
    suspend fun downloadModel(): Boolean {
        return try {
            val request = Request.Builder().url(modelUrl).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return false
            }

            val modelData = response.body?.bytes() ?: return false

            // Save the model to the app's internal storage
            val modelFile = File(context.filesDir, modelFileName)
            FileOutputStream(modelFile).use { outputStream ->
                outputStream.write(modelData)
            }

            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Checks if the model file exists locally.
     * @return True if the model file exists.
     */
    fun isModelDownloaded(): Boolean {
        val modelFile = File(context.filesDir, modelFileName)
        return modelFile.exists()
    }

    /**
     * Gets the local path to the model file.
     * @return The File object for the model, or null if it doesn't exist.
     */
    fun getModelFile(): File? {
        val modelFile = File(context.filesDir, modelFileName)
        return if (modelFile.exists()) modelFile else null
    }

    /**
     * Deletes the local model file.
     */
    fun deleteModel() {
        val modelFile = File(context.filesDir, modelFileName)
        if (modelFile.exists()) {
            modelFile.delete()
        }
    }

    /**
     * Gets the version of the local model (if available).
     * @return The version string, or null if not available.
     */
    fun getModelVersion(): String? {
        // In a real implementation, you might store the version in a separate file
        // or as metadata in the model file itself.
        return null
    }
}
