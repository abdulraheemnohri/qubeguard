package com.qubeguard.app.ml

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads the mobile ONNX export of the selected Hugging Face Transformer.
 *
 * Source model:
 * r3ddkahili/final-complete-malicious-url-model
 *
 * Android executes the exported ONNX artifact locally with ONNX Runtime.
 * The source repository remains the canonical model/metadata source; the
 * runtime repository contains the Android-ready ONNX artifact.
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
        private const val TOKENIZER_FILE = "tokenizer.json"
        private const val TOKENIZER_CONFIG_FILE = "tokenizer_config.json"
        private const val SPECIAL_TOKENS_FILE = "special_tokens_map.json"
        private const val CONFIG_FILE = "config.json"
        private const val MANIFEST_FILE = "manifest.json"
        private const val VERSION_FILE = "version.txt"

        private const val MODEL_BASE_URL =
            "https://huggingface.co/$RUNTIME_MODEL/resolve/$DEFAULT_REVISION"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
        .callTimeout(15, TimeUnit.MINUTES)
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
            File(modelDir, VOCAB_FILE).exists() &&
            File(modelDir, TOKENIZER_FILE).exists() &&
            File(modelDir, CONFIG_FILE).exists() &&
            File(modelDir, MANIFEST_FILE).exists()

    fun modelFile(): File = File(modelDir, MODEL_FILE)
    fun vocabFile(): File = File(modelDir, VOCAB_FILE)
    fun tokenizerFile(): File = File(modelDir, TOKENIZER_FILE)
    fun configFile(): File = File(modelDir, CONFIG_FILE)
    fun manifestFile(): File = File(modelDir, MANIFEST_FILE)

    fun deleteModel() {
        modelDir.deleteRecursively()
    }

    private fun downloadRuntimeFiles(): Boolean {
        return try {
            val manifestBytes = download("$MODEL_BASE_URL/$MANIFEST_FILE") ?: return false
            val manifest = JSONObject(manifestBytes.toString(Charsets.UTF_8))
            require(manifest.optString("source_model") == SOURCE_MODEL)
            require(manifest.optString("runtime") == "onnxruntime-android")
            require(manifest.optString("model_file") == MODEL_FILE)

            val expectedVersion = manifest.optString("version").takeIf { it.isNotBlank() }
            val modelSha = manifest.optString("sha256").takeIf { it.matches(Regex("[a-fA-F0-9]{64}")) }
            val tokenizerSha = manifest.optString("tokenizer_sha256")
                .takeIf { it.matches(Regex("[a-fA-F0-9]{64}")) }

            if (isModelReady() && readVersion() == expectedVersion) return true

            val model = download("$MODEL_BASE_URL/$MODEL_FILE") ?: return false
            if (modelSha != null && sha256(model) != modelSha.lowercase()) return false

            val vocab = download("$MODEL_BASE_URL/$VOCAB_FILE") ?: return false
            val tokenizer = download("$MODEL_BASE_URL/$TOKENIZER_FILE") ?: return false
            if (tokenizerSha != null && sha256(tokenizer) != tokenizerSha.lowercase()) return false

            val config = download("$MODEL_BASE_URL/$CONFIG_FILE") ?: return false
            val tokenizerConfig = download("$MODEL_BASE_URL/$TOKENIZER_CONFIG_FILE") ?: return false
            val specialTokens = download("$MODEL_BASE_URL/$SPECIAL_TOKENS_FILE") ?: return false

            // Install only after every required artifact has been downloaded and verified.
            writeAtomically(modelFile(), model)
            writeAtomically(vocabFile(), vocab)
            writeAtomically(tokenizerFile(), tokenizer)
            writeAtomically(configFile(), config)
            writeAtomically(File(modelDir, TOKENIZER_CONFIG_FILE), tokenizerConfig)
            writeAtomically(File(modelDir, SPECIAL_TOKENS_FILE), specialTokens)
            writeAtomically(manifestFile(), manifestBytes)
            writeVersion(expectedVersion ?: sha256(model))

            isModelReady()
        } catch (_: Exception) {
            false
        }
    }

    private fun download(url: String): ByteArray? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "QubeGuard-Android/1.0")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.bytes()
        }
    }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, "${target.name}.part")
        FileOutputStream(temp).use { it.write(bytes) }
        if (target.exists()) target.delete()
        check(temp.renameTo(target)) { "Unable to install ${target.name}" }
    }

    private fun readVersion(): String? =
        File(modelDir, VERSION_FILE).takeIf { it.exists() }?.readText()?.trim()

    private fun writeVersion(version: String) {
        writeAtomically(File(modelDir, VERSION_FILE), version.toByteArray())
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
}
