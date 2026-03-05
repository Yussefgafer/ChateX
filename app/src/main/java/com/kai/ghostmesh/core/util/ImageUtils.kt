package com.kai.ghostmesh.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

object ImageUtils {
    private const val MAX_IMAGE_DIMENSION = 1024
    private const val MAX_IMAGE_SIZE_BYTES = 500 * 1024
    private const val INITIAL_QUALITY = 80

    fun uriToBase64(context: Context, uri: Uri, maxSizeBytes: Int = MAX_IMAGE_SIZE_BYTES): String? {
        val mimeType = context.contentResolver.getType(uri) ?: ""

        // Handle videos or large files via Raw Base64 if needed,
        // but for now we focus on correcting the URI reading.
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            if (mimeType.startsWith("image/")) {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()

                val originalWidth = options.outWidth
                val originalHeight = options.outHeight
                if (originalWidth <= 0 || originalHeight <= 0) return null

                var sampleSize = 1
                while (originalWidth / sampleSize > MAX_IMAGE_DIMENSION * 2 ||
                       originalHeight / sampleSize > MAX_IMAGE_DIMENSION * 2) {
                    sampleSize *= 2
                }

                val decodeStream = context.contentResolver.openInputStream(uri) ?: return null
                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val bitmap = BitmapFactory.decodeStream(decodeStream, null, decodeOptions) ?: return null
                decodeStream.close()

                val scaledBitmap = if (originalWidth > MAX_IMAGE_DIMENSION || originalHeight > MAX_IMAGE_DIMENSION) {
                    val scale = minOf(
                        MAX_IMAGE_DIMENSION.toFloat() / originalWidth,
                        MAX_IMAGE_DIMENSION.toFloat() / originalHeight
                    )
                    val newWidth = (originalWidth * scale).toInt()
                    val newHeight = (originalHeight * scale).toInt()
                    Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true).also {
                        if (it != bitmap) bitmap.recycle()
                    }
                } else {
                    bitmap
                }

                var quality = INITIAL_QUALITY
                val outputStream = ByteArrayOutputStream()

                do {
                    outputStream.reset()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                    quality -= 10
                } while (outputStream.size() > maxSizeBytes && quality > 20)

                val result = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
                outputStream.close()
                scaledBitmap.recycle()
                result
            } else {
                // Non-image files: read raw bytes and encode to Base64 (limited by size)
                val rawBytes = inputStream.readBytes()
                inputStream.close()
                if (rawBytes.size > maxSizeBytes) {
                   null
                } else {
                   Base64.encodeToString(rawBytes, Base64.DEFAULT)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun base64ToBitmap(base64: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
