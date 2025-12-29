package com.jan.imageclassification

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import androidx.core.graphics.scale

/**
 * Handles loading and running the TensorFlow Lite image classification model.
 * Uses MobileNetV2 trained on ImageNet with 1000 classes.
 *
 * @param context Used to access the model and labels files from assets
 */
class TFLiteClassifier(private val context: Context) {
    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()

    companion object {
        // Model expects 224x224 RGB images
        private const val MODEL_PATH = "mobilenet_v2_1.0_224.tflite"
        private const val LABELS_PATH = "labels.txt"
        private const val INPUT_SIZE = 224
        private const val PIXEL_SIZE = 3 // RGB channels

        // Normalization values for MobileNetV2
        // Converts pixel values from [0, 255] to [-1, 1]
        private const val IMAGE_MEAN = 127.5f
        private const val IMAGE_STD = 127.5f

        private const val MAX_RESULTS = 5
    }

    init {
        loadModel()
        loadLabels()
    }

    /**
     * Loads the TFLite model from assets and sets up the interpreter.
     * Uses 4 threads for faster inference on multi-core devices.
     */
    private fun loadModel() {
        try {
            val modelFile = loadModelFile()
            val options = Interpreter.Options()
            options.setNumThreads(4)
            interpreter = Interpreter(modelFile, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Maps the model file directly into memory for efficient loading.
     * This avoids copying the entire file into RAM.
     *
     * @return Memory-mapped buffer containing the model
     */
    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(MODEL_PATH)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Reads the ImageNet class labels from the text file.
     * Each line corresponds to one of the 1000 output classes.
     */
    private fun loadLabels() {
        try {
            labels = BufferedReader(InputStreamReader(context.assets.open(LABELS_PATH)))
                .readLines()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Runs the image through the model and returns predictions.
     *
     * @param bitmap The image to classify (any size, will be resized)
     * @return Pair of (top 5 predictions, inference time in ms)
     */
    fun classify(bitmap: Bitmap): Pair<List<ClassificationResult>, Long> {
        if (interpreter == null || labels.isEmpty()) {
            return emptyList<ClassificationResult>() to 0L
        }

        val startTime = System.currentTimeMillis()

        // Resize to what the model expects
        val scaledBitmap = bitmap.scale(INPUT_SIZE, INPUT_SIZE)
        val inputBuffer = convertBitmapToByteBuffer(scaledBitmap)

        // Output is 1000 probabilities, one for each ImageNet class
        val output = Array(1) { FloatArray(labels.size) }

        interpreter?.run(inputBuffer, output)

        val inferenceTime = System.currentTimeMillis() - startTime

        // Sort by confidence and take top 5
        val results = output[0]
            .mapIndexed { index, confidence ->
                ClassificationResult(
                    label = if (index < labels.size) labels[index] else "Unknown",
                    confidence = confidence
                )
            }
            .sortedByDescending { it.confidence }
            .take(MAX_RESULTS)

        return results to inferenceTime
    }

    /**
     * Converts a bitmap into the format the model expects.
     * Extracts RGB values, normalizes them to [-1, 1], and packs into a ByteBuffer.
     *
     * @param bitmap The 224x224 image to convert
     * @return ByteBuffer ready to feed into the model
     */
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        // Extract RGB channels and normalize
        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val value = intValues[pixel++]

                // Red channel
                byteBuffer.putFloat(((value shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                // Green channel
                byteBuffer.putFloat(((value shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                // Blue channel
                byteBuffer.putFloat(((value and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
            }
        }

        return byteBuffer
    }

    /**
     * Releases the interpreter resources. Call this when done classifying.
     */
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
