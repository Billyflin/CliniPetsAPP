package cl.clinipets.ui.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

fun Context.createMultipartBody(uri: Uri, paramName: String = "file"): MultipartBody.Part? {
    return try {
        val contentResolver = this.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        
        // Get file name
        var fileName = "upload_${System.currentTimeMillis()}"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) {
                        fileName = name
                    }
                }
            }
        }

        // Create temp file
        val tempFile = File(this.cacheDir, fileName)
        
        // Copy stream
        contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: return null

        // Create RequestBody and MultipartBody.Part
        val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
        MultipartBody.Part.createFormData(paramName, fileName, requestBody)

    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
