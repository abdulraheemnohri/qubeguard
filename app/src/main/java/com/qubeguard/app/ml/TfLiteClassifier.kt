package com.qubeguard.app.ml

import android.content.Context
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject

/**
 * TFLite-based classifier for detecting ads, trackers, malware, and phishing domains.
 * Uses a pre-trained TensorFlow Lite model for inference.
 */
class TfLiteClassifier @Inject constructor(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var inputTensorBuffer: TensorBuffer? = null
    private var outputTensorBuffer: TensorBuffer? = null
    private var labels: List<String> = listOf("Legitimate", "Ad", "Tracker", "Malware", "Phishing", "Analytics")

    // Thresholds for each category (adjust based on model performance)
    private val thresholds = mapOf(
        "Ad" to 0.7f,
        "Tracker" to 0.7f,
        "Malware" to 0.85f,
        "Phishing" to 0.8f,
        "Analytics" to 0.75f
    )

    /**
     * Loads the TFLite model from the assets folder.
     * @param modelFileName The name of the model file (e.g., "qubeguard_model.tflite").
     */
    fun loadModel(modelFileName: String = "qubeguard_model.tflite") {
        try {
            val modelFile = context.assets.open(modelFileName)
            val fileDescriptor = modelFile.fileDescriptor
            val startOffset = modelFile.available().toLong()

            val mappedByteBuffer = fileDescriptor.channel.map(
                FileChannel.MapMode.READ_ONLY,
                startOffset,
                FileChannel.MapMode.READ_ONLY
            )

            interpreter = Interpreter(mappedByteBuffer)

            // Initialize input and output tensors
            val inputShape = interpreter?.getInputTensorShape(0)
            val outputShape = interpreter?.getOutputTensorShape(0)

            inputTensorBuffer = TensorBuffer.createFixedSize(inputShape, DataType.FLOAT32)
            outputTensorBuffer = TensorBuffer.createFixedSize(outputShape, DataType.FLOAT32)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Runs inference on a URL/domain and returns the predicted category.
     * @param input The URL or domain to classify.
     * @return The predicted category (e.g., "Ad", "Tracker", "Malware").
     */
    fun classify(input: String): String {
        if (interpreter == null) {
            throw IllegalStateException("Model not loaded. Call loadModel() first.")
        }

        // Extract features
        val featureExtractor = FeatureExtractor()
        val features = featureExtractor.extractFeatures(input)

        // Load features into input tensor
        inputTensorBuffer?.loadArray(features)

        // Run inference
        interpreter?.run(inputTensorBuffer?.buffer, outputTensorBuffer?.buffer)

        // Get output probabilities
        val output = outputTensorBuffer?.floatArray ?: FloatArray(0)

        // Map output to labels
        val labelOutput = TensorLabel(labels, output)

        // Get the top prediction
        val topPrediction = labelOutput.mapWithFloatValue
            .maxByOrNull { it.value }?.key ?: "Legitimate"

        // Apply threshold
        val confidence = labelOutput.mapWithFloatValue
            .firstOrNull { it.key == topPrediction }?.value ?: 0f

        return if (confidence >= (thresholds[topPrediction] ?: 0.5f)) {
            topPrediction
        } else {
            "Legitimate"
        }
    }

    /**
     * Returns the confidence scores for all categories.
     * @param input The URL or domain to classify.
     * @return A map of category to confidence score.
     */
    fun getConfidenceScores(input: String): Map<String, Float> {
        if (interpreter == null) {
            throw IllegalStateException("Model not loaded. Call loadModel() first.")
        }

        // Extract features
        val featureExtractor = FeatureExtractor()
        val features = featureExtractor.extractFeatures(input)

        // Load features into input tensor
        inputTensorBuffer?.loadArray(features)

        // Run inference
        interpreter?.run(inputTensorBuffer?.buffer, outputTensorBuffer?.buffer)

        // Get output probabilities
        val output = outputTensorBuffer?.floatArray ?: FloatArray(0)

        // Map output to labels
        val labelOutput = TensorLabel(labels, output)

        return labelOutput.mapWithFloatValue.associate { it.key to it.value }
    }

    /**
     * Checks if a URL/domain is blocked based on the TFLite model.
     * @param input The URL or domain to check.
     * @return True if the input is blocked (Ad, Tracker, Malware, or Phishing).
     */
    fun isBlocked(input: String): Boolean {
        val category = classify(input)
        return category != "Legitimate" && category != "Analytics"
    }

    /**
     * Checks if a URL/domain is a tracker.
     * @param input The URL or domain to check.
     * @return True if the input is classified as a tracker.
     */
    fun isTracker(input: String): Boolean {
        return classify(input) == "Tracker"
    }

    /**
     * Checks if a URL/domain is malware.
     * @param input The URL or domain to check.
     * @return True if the input is classified as malware.
     */
    fun isMalware(input: String): Boolean {
        return classify(input) == "Malware"
    }

    /**
     * Checks if a URL/domain is a phishing site.
     * @param input The URL or domain to check.
     * @return True if the input is classified as phishing.
     */
    fun isPhishing(input: String): Boolean {
        return classify(input) == "Phishing"
    }

    /**
     * Closes the interpreter and releases resources.
     */
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
