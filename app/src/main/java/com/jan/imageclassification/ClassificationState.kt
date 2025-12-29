package com.jan.imageclassification

import android.graphics.Bitmap

/**
 * Represents the different states the classification screen can be in.
 */
sealed class ClassificationState {
    data object Idle : ClassificationState()

    data object Selecting : ClassificationState()

    data object Processing : ClassificationState()

    /**
     * Results are ready to display.
     *
     * @param result List of top predictions with confidence scores
     * @param bitmap The classified image to show in the results sheet
     * @param inferenceTime How long the model took to classify in milliseconds
     */
    data class ShowingResults(
        val result: List<ClassificationResult>,
        val bitmap: Bitmap,
        val inferenceTime: Long
    ) : ClassificationState()
}
