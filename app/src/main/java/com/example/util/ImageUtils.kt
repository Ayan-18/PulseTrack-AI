package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUtils {

    fun scaleBitmap(bitmap: Bitmap, maxDimension: Int = 1024): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt().coerceAtLeast(1)
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt().coerceAtLeast(1)
        }
        val scaled = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        if (scaled !== bitmap && !bitmap.isRecycled) {
            bitmap.recycle()
        }
        return scaled
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun uriToScaledBitmap(context: Context, uri: Uri, maxDimension: Int = 1024): Bitmap? {
        return try {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(boundsOptions, maxDimension, maxDimension)
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val sampled = BitmapFactory.decodeStream(stream, null, decodeOptions) ?: return null
                scaleBitmap(sampled, maxDimension)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun uriToScaledBitmapAsync(context: Context, uri: Uri, maxDimension: Int = 1024): Bitmap? =
        withContext(Dispatchers.IO) {
            uriToScaledBitmap(context, uri, maxDimension)
        }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun bitmapToBase64Async(bitmap: Bitmap): String =
        withContext(Dispatchers.IO) {
            bitmapToBase64(bitmap)
        }

    fun base64ToBitmap(base64: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun loadPhotoBitmapAsync(context: Context, pathOrBase64: String?): Bitmap? =
        withContext(Dispatchers.IO) {
            if (pathOrBase64.isNullOrBlank()) return@withContext null
            try {
                val file = File(pathOrBase64)
                if (file.exists() && file.isFile) {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, bounds)
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = calculateInSampleSize(bounds, 1024, 1024)
                    }
                    BitmapFactory.decodeFile(file.absolutePath, opts)
                } else {
                    base64ToBitmap(pathOrBase64)
                }
            } catch (e: Exception) {
                null
            }
        }

    suspend fun saveBase64ImageToInternalStorage(
        context: Context,
        base64OrPath: String?,
        prefix: String = "kinesi"
    ): String? = withContext(Dispatchers.IO) {
        if (base64OrPath.isNullOrBlank()) return@withContext null
        val existing = File(base64OrPath)
        if (existing.exists() && existing.isFile) {
            return@withContext existing.absolutePath
        }
        try {
            val dir = File(context.filesDir, "kinesitherapy_photos").apply {
                if (!exists()) mkdirs()
            }
            val targetFile = File(dir, "${prefix}_${UUID.randomUUID()}.jpg")
            val bytes = Base64.decode(base64OrPath, Base64.DEFAULT)
            FileOutputStream(targetFile).use { fos ->
                fos.write(bytes)
                fos.flush()
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteInternalPhotoIfExists(path: String?) = withContext(Dispatchers.IO) {
        if (path.isNullOrBlank()) return@withContext
        try {
            val file = File(path)
            if (file.exists() && file.isFile && file.absolutePath.contains("kinesitherapy_photos")) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }

    // Creates a stylized mock image of a clinic kinesitherapy program card
    fun createSampleKinesitherapyProgramCard(): Bitmap {
        val width = 600
        val height = 400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply { color = Color.parseColor("#F1F5F9") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Header bar
        val headerPaint = Paint().apply { color = Color.parseColor("#0F766E") }
        canvas.drawRect(0f, 0f, width.toFloat(), 60f, headerPaint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 24f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText("КАРТА КИНЕЗИОТЕРАПИИ: ПРОГРАММА МТБ", 24f, 40f, textPaint)

        val itemPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 18f
            isAntiAlias = true
        }

        val exercises = listOf(
            "1. Тяга блока снизу лежа (декомпрессия) — 25 кг x 3 x 15",
            "2. Тяга верхнего блока к груди узким хватом — 30 кг x 3 x 15",
            "3. Гиперэкстензия с отягощением — 5 кг x 3 x 15",
            "4. Пуловер на горизонтальной скамье — 15 кг x 3 x 15",
            "5. Отведение ноги в петле на четвереньках — 15 кг x 3 x 18",
            "6. Приведение бедра сидя на МТБ — 12.5 кг x 3 x 15",
            "7. Скручивания на фитболе (выдох «Хаа») — 3 x 20"
        )

        var y = 100f
        for (ex in exercises) {
            canvas.drawText(ex, 24f, y, itemPaint)
            y += 40f
        }

        return bitmap
    }
}
