package com.jan.imageclassification.screen

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jan.imageclassification.ClassificationState
import com.jan.imageclassification.ClassificationViewModel

/**
 * Main screen for the image classification app.
 * Shows a camera button, handles image capture/selection, and displays results.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassificationScreen(viewModel: ClassificationViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri?.let { uri ->
                val bitmap = uriToBitmap(context, uri)
                bitmap?.let { viewModel.classifyImage(it) }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bitmap = uriToBitmap(context, it)
            bitmap?.let { bmp -> viewModel.classifyImage(bmp) }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createImageUri(context)
            photoUri = uri
            cameraLauncher.launch(uri)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        FilledTonalButton(
            onClick = { viewModel.showImageSourceOptions() },
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Camera",
                modifier = Modifier.size(40.dp)
            )
        }
    }

    if (state is ClassificationState.Selecting) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideImageSourceOptions() },
            sheetState = rememberModalBottomSheetState()
        ) {
            ImageSourceBottomSheet(
                onTakePhoto = {
                    viewModel.hideImageSourceOptions()
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onChooseFromGallery = {
                    viewModel.hideImageSourceOptions()
                    galleryLauncher.launch("image/*")
                }
            )
        }
    }

    if (state is ClassificationState.Processing) {
        ModalBottomSheet(
            onDismissRequest = { },
            sheetState = rememberModalBottomSheetState()
        ) {
            ProcessingBottomSheet()
        }
    }

    if (state is ClassificationState.ShowingResults) {
        val resultsState = state as ClassificationState.ShowingResults
        ModalBottomSheet(
            onDismissRequest = { viewModel.clearResults() },
            sheetState = rememberModalBottomSheetState()
        ) {
            ResultsBottomSheet(
                bitmap = resultsState.bitmap,
                results = resultsState.result,
                processingTime = resultsState.processingTime
            )
        }
    }
}
