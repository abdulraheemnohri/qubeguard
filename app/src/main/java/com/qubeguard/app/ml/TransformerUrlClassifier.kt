package com.qubeguard.app.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.nio.LongBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device Transformer classifier for malicious URLs.
 *
 * Model source: r3ddkahili/final-complete-malicious-url-model
 * Architecture: BERT sequence classification, four classes.
 * Runtime: ONNX Runtime Android. No network inference is performed.
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
        private val LABELS = arrayOf(BENIGN, DEFACEMENT, PHISHING, MALWARE)
    }

    private val environment: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }
    private var session: OrtSession? = null
    private var tokenizer: BertWordPieceTokenizer? = null

    @Synchronized
    fun load(): Boolean {
        if (!modelDownloader.isModelReady()) return false
        if (session != null && tokenizer != null) return true

        return try {
            session = environment.createSession(
                modelDownloader.modelFile().absolutePath,
                OrtSession.SessionOptions()
            )
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
            val inputs = linkedMapOf<String, OnnxTensor>()
            val names = session!!.inputNames.toList()
            inputs[names.firstOrNull { it == "input_ids" } ?: names[0]] = inputIds
            inputs[names.firstOrNull { it == "attention_mask" } ?: names.getOrElse(1) { names[0] }] = attentionMask
            if (names.contains("token_type_ids")) inputs["token_type_ids"] = tokenTypeIds

            session!!.run(inputs).use { result ->
                val logits = extractLogits(result[0].value)
                val probabilities = softmax(logits)
                val index = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
                return Prediction(LABELS.getOrElse(index) { BENIGN }, probabilities[index], probabilities)
            }
        } finally {
            inputIds.close()
            attentionMask.close()
            tokenTypeIds.close()
        }
    }

    fun isBlocked(url: String): Boolean {
        val prediction = classify(url)
        return prediction.label == MALWARE || prediction.label == PHISHING || prediction.label == DEFACEMENT
    }

    fun close() {
        session?.close()
        session = null
        tokenizer = null
    }

    data class Prediction(
        val label: String,
        val confidence: Float,
        val probabilities: FloatArray
    )

    private fun extractLogits(value: Any?): FloatArray {
        return when (value) {
            is FloatArray -> value
            is Array<*> -> {
                val first = value.firstOrNull()
                when (first) {
                    is FloatArray -> first
                    is Array<*> -> first.filterIsInstance<Float>().toFloatArray()
                    else -> error("Unsupported ONNX output shape")
                }
            }
            else -> error("Unsupported ONNX output type: ${value?.javaClass}")
        }
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val max = logits.maxOrNull() ?: 0f
        val exps = FloatArray(logits.size) { kotlin.math.exp((logits[it] - max).toDouble()).toFloat() }
        val sum = exps.sum().coerceAtLeast(Float.MIN_VALUE)
        return FloatArray(exps.size) { exps[it] / sum }
    }
}
