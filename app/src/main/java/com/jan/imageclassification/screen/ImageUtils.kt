package com.jan.imageclassification.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

/**
 * Creates a URI for saving camera photos using FileProvider.
 * Photos are saved to the cache directory with a timestamp.
 *
 * @param context Used to access cache directory and package name
 * @return URI that the camera app can write to
 */
fun createImageUri(context: Context): Uri {
    val imageFile = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

/**
 * Converts a URI (from camera or gallery) into a Bitmap.
 *
 * @param context Used to access content resolver
 * @param uri The image URI to load
 * @return Bitmap or null if loading failed
 */
fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
