package com.jan.imageclassification

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages the app state and coordinates between the UI and the classifier.
 * Handles image classification on a background thread and updates the UI state.
 */
class ClassificationViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow<ClassificationState>(ClassificationState.Idle)
    val state: StateFlow<ClassificationState> = _state.asStateFlow()

    private val classifier: TFLiteClassifier = TFLiteClassifier(application.applicationContext)

    /**
     * Shows the bottom sheet where user picks camera or gallery.
     */
    fun showImageSourceOptions() {
        _state.value = ClassificationState.Selecting
    }

    /**
     * Hides the image source picker and returns to idle.
     */
    fun hideImageSourceOptions() {
        _state.value = ClassificationState.Idle
    }

    /**
     * Runs the image through the classifier on a background thread.
     * Updates state to Processing, then ShowingResults when done.
     *
     * @param bitmap The image to classify
     */
    fun classifyImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _state.value = ClassificationState.Processing

            val (results, inferenceTime) = withContext(Dispatchers.Default) {
                classifier.classify(bitmap)
            }

            _state.value = ClassificationState.ShowingResults(
                result = results,
                bitmap = bitmap,
                inferenceTime = inferenceTime
            )
        }
    }

    /**
     * Closes the results sheet and removes the bitmap from memory.
     */
    fun clearResults() {
        val currentState = _state.value
        if (currentState is ClassificationState.ShowingResults) {
            currentState.bitmap.recycle()
        }
        _state.value = ClassificationState.Idle
    }
}
