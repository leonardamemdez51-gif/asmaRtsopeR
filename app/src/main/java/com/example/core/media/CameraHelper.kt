package com.example.core.media

import android.content.Context
import android.net.Uri
import com.example.core.logger.AppLogger
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CameraHelper {

    fun createImageFile(context: Context, prefix: String = "RAMA_IMG_"): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "${prefix}${timeStamp}_"
        val storageDir = context.getExternalFilesDir("rama_photos") ?: context.filesDir
        return File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        ).also {
            AppLogger.d("Created temp photo file: ${it.absolutePath}", tag = "CameraHelper")
        }
    }

    fun isPhotoValid(photoUri: String?): Boolean {
        if (photoUri.isNullOrBlank()) return false
        return try {
            val uri = Uri.parse(photoUri)
            uri != null
        } catch (e: Exception) {
            false
        }
    }
}
