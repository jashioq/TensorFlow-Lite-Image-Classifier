package com.jan.imageclassification

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.scale
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.common.ops.NormalizeOp
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Handles loading and running the TensorFlow Lite image classification model.
 * Uses MobileNetV2 trained on ImageNet with 1000 classes.
 *
 * @param context Used to access the model and labels files from assets
 */
class TFLiteClassifier(private val context: Context) {
    private lateinit var interpreter: Interpreter
    private lateinit var labels: List<String>

    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(IMAGE_MEAN, IMAGE_STD))
        .build()

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
     */
    private fun loadModel() {
        val model = FileUtil.loadMappedFile(context, MODEL_PATH)
        val options = Interpreter.Options()
        interpreter = Interpreter(model, options)
    }

    /**
     * Reads the ImageNet class labels from the text file.
     * Each line corresponds to one of the 1000 output classes.
     */
    private fun loadLabels() {
        labels = BufferedReader(InputStreamReader(context.assets.open(LABELS_PATH)))
            .readLines()
    }

    /**
     * Runs the image through the model and returns predictions.
     *
     * @param bitmap The image to classify
     * @return Pair of (top 5 predictions, inference time in ms)
     */
    fun classify(bitmap: Bitmap): Pair<List<ClassificationResult>, Long> {
        // Resize to what the model expects
        val scaledBitmap = bitmap.scale(INPUT_SIZE, INPUT_SIZE)

        // Load bitmap into TensorImage and apply preprocessing
        var tensorImage = TensorImage(DataType.UINT8)
        tensorImage.load(scaledBitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // Output is 1000 probabilities, one for each ImageNet class
        val output = Array(1) { FloatArray(labels.size) }

        val startTime = System.currentTimeMillis()

        interpreter.run(tensorImage.buffer, output)

        val inferenceTime = System.currentTimeMillis() - startTime

        // Sort by confidence and take top 5
        val results = output[0]
            .mapIndexed { index, confidence ->
                ClassificationResult(
                    label = labels[index],
                    confidence = confidence
                )
            }
            .sortedByDescending { it.confidence }
            .take(MAX_RESULTS)

        return results to inferenceTime
    }
}
