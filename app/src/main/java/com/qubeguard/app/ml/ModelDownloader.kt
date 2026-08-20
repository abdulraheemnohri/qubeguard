package com.qubeguard.app.ml

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads the mobile-optimized ONNX export of the QubeGuard malicious URL
 * Transformer. The training/source checkpoint is:
 * r3ddkahili/final-complete-malicious-url-model
 *
 * The source Hub repository is a 438 MB safetensors BERT checkpoint. Android
 * does not execute safetensors directly, so the build pipeline exports it to
 * ONNX and publishes the mobile artifact to the runtime repository below.
 */
@Singleton
class ModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val SOURCE_MODEL = "r3ddkahili/final-complete-malicious-url-model"
        const val RUNTIME_MODEL = "abdulraheemnohri/qubeguard-transformer-model"
        const val DEFAULT_REVISION = "main"

        private const val MODEL_FILE = "model.onnx"
        private const val VOCAB_FILE = "vocab.txt"
        private const val CONFIG_FILE = "config.json"
        private const val MANIFEST_FILE = "manifest.json"

        private const val MODEL_BASE_URL =
            "https://huggingface.co/$RUNTIME_MODEL/resolve/$DEFAULT_REVISION"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .build()

    private val modelDir = File(context.filesDir, "models/qubeguard-transformer")

    suspend fun ensureModel(): Boolean = withContext(Dispatchers.IO) {
        modelDir.mkdirs()
        if (isModelReady()) return@withContext true
        downloadRuntimeFiles()
    }

    suspend fun updateModel(): Boolean = withContext(Dispatchers.IO) {
        modelDir.mkdirs()
        downloadRuntimeFiles()
    }

    fun isModelReady(): Boolean =
        File(modelDir, MODEL_FILE).let { it.exists() && it.length() > 1_000_000L } &&
            File(modelDir, VOCAB_FILE).exists()

    fun modelFile(): File = File(modelDir, MODEL_FILE)
    fun vocabFile(): File = File(modelDir, VOCAB_FILE)
    fun configFile(): File = File(modelDir, CONFIG_FILE)
    fun manifestFile(): File = File(modelDir, MANIFEST_FILE)

    fun deleteModel() {
        modelDir.deleteRecursively()
    }

    private fun downloadRuntimeFiles(): Boolean {
        return try {
            val manifest = download("$MODEL_BASE_URL/$MANIFEST_FILE")
                ?: return false
            writeAtomically(manifestFile(), manifest)

            val manifestText = manifest.toString(Charsets.UTF_8)
            val modelSha = Regex("\\\"sha256\\\"\\s*:\\s*\\\"([a-fA-F0-9]{64})\\\"")
                .find(manifestText)?.groupValues?.get(1)
            val expectedVersion = Regex("\\\"version\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
                .find(manifestText)?.groupValues?.get(1)

            val currentVersion = readVersion()
            if (currentVersion == expectedVersion && isModelReady()) return true

            val model = download("$MODEL_BASE_URL/$MODEL_FILE") ?: return false
            if (modelSha != null && sha256(model) != modelSha.lowercase()) return false
            writeAtomically(modelFile(), model)

            val vocab = download("$MODEL_BASE_URL/$VOCAB_FILE") ?: return false
            writeAtomically(vocabFile(), vocab)

            download("$MODEL_BASE_URL/$CONFIG_FILE")?.let { writeAtomically(configFile(), it) }
            writeVersion(expectedVersion ?: sha256(model))
            isModelReady()
        } catch (_: Exception) {
            false
        }
    }

    private fun download(url: String): ByteArray? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "QubeGuard-Android")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.bytes()
        }
    }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        val temp = File(target.parentFile, "${target.name}.part")
        FileOutputStream(temp).use { it.write(bytes) }
        if (target.exists()) target.delete()
        check(temp.renameTo(target)) { "Unable to install ${target.name}" }
    }

    private fun readVersion(): String? =
        File(modelDir, "version.txt").takeIf { it.exists() }?.readText()?.trim()

    private fun writeVersion(version: String) {
        File(modelDir, "version.txt").writeText(version)
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
}
