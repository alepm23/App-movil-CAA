package com.pictofly.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object ImageDownloader {
    suspend fun downloadAndSaveImage(
        context: Context,
        imageUrl: String,
        fileName: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val imagesDir = File(context.filesDir, "local_images")
            if (!imagesDir.exists()) imagesDir.mkdirs()

            val outputFile = File(imagesDir, fileName)
            if (outputFile.exists()) {
                return@withContext "local_images/$fileName"
            }

            val url = URL(imageUrl)
            val connection = when {
                imageUrl.startsWith("https") -> url.openConnection() as HttpsURLConnection
                else -> url.openConnection() as HttpURLConnection
            }

            connection.apply {
                connectTimeout = 10000
                readTimeout = 10000
                doInput = true
                instanceFollowRedirects = true
            }

            connection.connect()

            if (connection.responseCode in 200..299) {
                val inputStream = connection.inputStream
                val contentLength = connection.contentLength.toLong()
                val options = BitmapFactory.Options().apply {
                    inSampleSize = calculateInSampleSize(contentLength)
                }

                val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()
                connection.disconnect()

                if (bitmap != null) {
                    val outputStream = FileOutputStream(outputFile)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    outputStream.flush()
                    outputStream.close()
                    bitmap.recycle()

                    "local_images/$fileName"
                } else {
                    null
                }
            } else {
                connection.disconnect()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(fileSize: Long): Int {
        return when {
            fileSize > 2 * 1024 * 1024 -> 4 // > 2MB
            fileSize > 1 * 1024 * 1024 -> 2 // > 1MB
            else -> 1
        }
    }
}