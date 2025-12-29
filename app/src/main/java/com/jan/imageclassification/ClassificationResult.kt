package com.jan.imageclassification

/**
 * A single prediction result from the image classifier.
 *
 * @param label The class name (e.g., "golden retriever", "coffee mug")
 * @param confidence Probability between 0 and 1, where 1 means completely confident
 */
data class ClassificationResult(
    val label: String,
    val confidence: Float
)
