package com.qubeguard.app.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.json.JSONObject
import java.io.File
import java.nio.LongBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device Transformer classifier for malicious URLs.
 *
 * Source: r3ddkahili/final-complete-malicious-url-model
 * Architecture: BERT sequence classification
 * Runtime: ONNX Runtime Android
 * Network inference: never
 */
@Singleton
class TransformerUrlClassifier @Inject constructor(
    private val modelDownloader: ModelDownloader
) {
    companion object {
        const val BENIGN = "Benign"
        const val DEFACEMENT = "Defacement"
        const val PHISHING = "Phishing"
        const val MALWARE = "Malware"
        private const val MAX_LENGTH = 128
    }

    private val environment: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }
    private var session: OrtSession? = null
    private var tokenizer: BertWordPieceTokenizer? = null
    private var labels: List<String> = listOf(BENIGN, DEFACEMENT, PHISHING, MALWARE)

    @Synchronized
    fun load(): Boolean {
        if (!modelDownloader.isModelReady()) return false
        if (session != null && tokenizer != null) return true

        return try {
            labels = readLabels(modelDownloader.configFile())
            require(labels.size == 4) { "Unsupported classifier label count: ${labels.size}" }

            val loadedSession = environment.createSession(
                modelDownloader.modelFile().absolutePath,
                OrtSession.SessionOptions()
            )
            require(loadedSession.inputNames.contains("input_ids"))
            require(loadedSession.inputNames.contains("attention_mask"))

            session = loadedSession
            tokenizer = BertWordPieceTokenizer(modelDownloader.vocabFile())
            true
        } catch (_: Exception) {
            session?.close()
            session = null
            tokenizer = null
            false
        }
    }

    fun isLoaded(): Boolean = session != null && tokenizer != null

    @Synchronized
    fun classify(url: String): Prediction {
        check(isLoaded()) { "Transformer model is not loaded" }

        val encoded = tokenizer!!.encode(url, MAX_LENGTH)
        val inputIds = OnnxTensor.createTensor(
            environment,
            LongBuffer.wrap(encoded.inputIds),
            longArrayOf(1, MAX_LENGTH.toLong())
        )
        val attentionMask = OnnxTensor.createTensor(
            environment,
            LongBuffer.wrap(encoded.attentionMask),
            longArrayOf(1, MAX_LENGTH.toLong())
        )
        val tokenTypeIds = OnnxTensor.createTensor(
            environment,
            LongBuffer.wrap(encoded.tokenTypeIds),
            longArrayOf(1, MAX_LENGTH.toLong())
        )

        try {
            val inputNames = session!!.inputNames
            val inputs = linkedMapOf<String, OnnxTensor>(
                "input_ids" to inputIds,
                "attention_mask" to attentionMask
            )
            if (inputNames.contains("token_type_ids")) {
                inputs["token_type_ids"] = tokenTypeIds
            }

            session!!.run(inputs).use { result ->
                val logits = extractLogits(result[0].value)
                require(logits.size == labels.size) {
                    "Model output count ${logits.size} does not match ${labels.size} labels"
                }
                val probabilities = softmax(logits)
                val index = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
                return Prediction(labels[index], probabilities[index], probabilities)
            }
        } finally {
            inputIds.close()
            attentionMask.close()
            tokenTypeIds.close()
        }
    }

    fun isBlocked(url: String): Boolean {
        val prediction = classify(url)
        return prediction.label == MALWARE ||
            prediction.label == PHISHING ||
            prediction.label == DEFACEMENT
    }

    fun close() {
        session?.close()
        session = null
        tokenizer = null
    }

    fun getLabels(): List<String> = labels

    data class Prediction(
        val label: String,
        val confidence: Float,
        val probabilities: FloatArray
    )

    private fun readLabels(configFile: File): List<String> {
        val config = JSONObject(configFile.readText())
        val id2label = config.optJSONObject("id2label") ?: return listOf(
            BENIGN, DEFACEMENT, PHISHING, MALWARE
        )
        return (0 until config.optInt("num_labels", 4)).map { index ->
            id2label.optString(index.toString(), "LABEL_$index").let { raw ->
                when (raw.uppercase()) {
                    "LABEL_0" -> BENIGN
                    "LABEL_1" -> DEFACEMENT
                    "LABEL_2" -> PHISHING
                    "LABEL_3" -> MALWARE
                    else -> raw
                }
            }
        }
    }

    private fun extractLogits(value: Any?): FloatArray = when (value) {
        is FloatArray -> value
        is Array<*> -> {
            when (val first = value.firstOrNull()) {
                is FloatArray -> first
                is Array<*> -> first.filterIsInstance<Float>().toFloatArray()
                else -> error("Unsupported ONNX output shape")
            }
        }
        else -> error("Unsupported ONNX output type: ${value?.javaClass}")
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val max = logits.maxOrNull() ?: 0f
        val exps = FloatArray(logits.size) {
            kotlin.math.exp((logits[it] - max).toDouble()).toFloat()
        }
        val sum = exps.sum().coerceAtLeast(Float.MIN_VALUE)
        return FloatArray(exps.size) { exps[it] / sum }
    }
}
