package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID

data class StoredDocumentResult(
    val filePath: String,
    val thumbnailPath: String?,
    val fileSizeByte: Long,
    val checksumSha256: String,
    val width: Int,
    val height: Int
)

object DocumentStorageManager {

    private const val SECURE_DOC_DIR = "secure_expedientes"
    private const val SECURE_THUMB_DIR = "secure_thumbnails"

    fun saveDocumentImage(
        context: Context,
        sourceUri: Uri,
        typeCode: String,
        clientId: Long,
        isOfficialId: Boolean = false
    ): StoredDocumentResult? {
        return try {
            val inputStream: InputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) return null

            // Correct orientation from EXIF if available
            val rotatedBitmap = try {
                context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
                    rotateBitmapIfNeeded(originalBitmap, orientation)
                } ?: originalBitmap
            } catch (_: Exception) {
                originalBitmap
            }

            // Dimension limits & Compression Quality
            val maxDimension = if (isOfficialId) 1920 else 1280
            val quality = if (isOfficialId) 85 else 80

            val scaledBitmap = scaleBitmapToMaxDimension(rotatedBitmap, maxDimension)

            // Prepare secure storage directories
            val secureDir = context.getDir(SECURE_DOC_DIR, Context.MODE_PRIVATE)
            if (!secureDir.exists()) secureDir.mkdirs()

            val thumbDir = context.getDir(SECURE_THUMB_DIR, Context.MODE_PRIVATE)
            if (!thumbDir.exists()) thumbDir.mkdirs()

            // Unique filename naming convention
            val uniqueSuffix = UUID.randomUUID().toString().take(8)
            val fileName = "doc_${typeCode}_cli_${clientId}_${System.currentTimeMillis()}_$uniqueSuffix.jpg"
            val targetFile = File(secureDir, fileName)

            val outputStream = FileOutputStream(targetFile)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.flush()
            outputStream.close()

            // Generate Thumbnail (150x150)
            val thumbName = "thumb_${fileName}"
            val thumbFile = File(thumbDir, thumbName)
            val thumbBitmap = Bitmap.createScaledBitmap(scaledBitmap, 150, (150f * scaledBitmap.height / scaledBitmap.width).toInt().coerceAtLeast(100), true)
            val thumbOut = FileOutputStream(thumbFile)
            thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 70, thumbOut)
            thumbOut.flush()
            thumbOut.close()

            val fileSize = targetFile.length()
            val checksum = calculateSha256(targetFile)

            StoredDocumentResult(
                filePath = targetFile.absolutePath,
                thumbnailPath = thumbFile.absolutePath,
                fileSizeByte = fileSize,
                checksumSha256 = checksum,
                width = scaledBitmap.width,
                height = scaledBitmap.height
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getTempImageUriForCamera(context: Context): Pair<Uri, File>? {
        return try {
            val cacheDir = context.cacheDir
            val tempFile = File.createTempFile("camera_capture_", ".jpg", cacheDir)
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, tempFile)
            Pair(uri, tempFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleBitmapToMaxDimension(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int

        if (width > height) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).toInt()
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun calculateSha256(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val inputStream = file.inputStream()
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
            inputStream.close()
            val hashBytes = digest.digest()
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    fun deleteFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }
}
