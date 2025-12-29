package com.jan.imageclassification.screen

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jan.imageclassification.ClassificationResult

/**
 * Displays the classification results in a bottom sheet.
 * Shows the image, processing time, and top 5 predictions with confidence bars.
 *
 * @param bitmap The classified image to display
 * @param results List of top predictions
 * @param processingTime How long classification took in milliseconds
 */
@Composable
fun ResultsBottomSheet(
    bitmap: Bitmap,
    results: List<ClassificationResult>,
    processingTime: Long
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Classification Results",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Classified image",
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Processing time: ${processingTime}ms",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Top 5 Predictions",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        results.forEach { result ->
            PredictionItem(result)
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Shows a single prediction with label, percentage, and progress bar.
 *
 * @param result The prediction to display
 */
@Composable
private fun PredictionItem(result: ClassificationResult) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = result.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${(result.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { result.confidence },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
