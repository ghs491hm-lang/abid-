package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

data class PdfDocumentInfo(
    val name: String,
    val sizeBytes: Long,
    val pageCount: Int,
    val bytes: ByteArray,
    val base64Data: String,
    val previewBitmap: Bitmap?
) {
    val formattedSize: String
        get() = when {
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> String.format("%.1f KB", sizeBytes / 1024.0)
            else -> String.format("%.2f MB", sizeBytes / (1024.0 * 1024.0))
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PdfDocumentInfo
        return name == other.name && sizeBytes == other.sizeBytes
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + sizeBytes.hashCode()
        return result
    }
}

object PdfHelper {

    suspend fun parsePdfUri(context: Context, uri: Uri): Result<PdfDocumentInfo> = withContext(Dispatchers.IO) {
        try {
            var fileName = "Document.pdf"
            var fileSize = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: "Document.pdf"
                    }
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(Exception("Could not read file data"))

            if (fileSize == 0L) {
                fileSize = bytes.size.toLong()
            }

            val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)

            // Let's render page count and page 1 preview via PdfRenderer
            var pageCount = 1
            var previewBitmap: Bitmap? = null

            try {
                val tempFile = File.createTempFile("pdf_preview", ".pdf", context.cacheDir)
                FileOutputStream(tempFile).use { it.write(bytes) }

                val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                pageCount = renderer.pageCount

                if (pageCount > 0) {
                    val page = renderer.openPage(0)
                    val width = 400
                    val height = ((400.0 / page.width) * page.height).toInt().coerceIn(200, 600)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    previewBitmap = bitmap
                }
                renderer.close()
                pfd.close()
                tempFile.delete()
            } catch (e: Exception) {
                // If PdfRenderer fails (e.g. password protected or malformed), still preserve bytes & info
            }

            Result.success(
                PdfDocumentInfo(
                    name = fileName,
                    sizeBytes = fileSize,
                    pageCount = pageCount,
                    bytes = bytes,
                    base64Data = base64String,
                    previewBitmap = previewBitmap
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
