package com.example.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object ReceiptStorage {
    suspend fun copyReceiptToInternalStorage(context: Context, sourceUri: Uri): String? =
        withContext(Dispatchers.IO) {
            try {
                val receiptsDir = File(context.filesDir, "receipts").apply { mkdirs() }
                val destFile = File(receiptsDir, "receipt_${UUID.randomUUID()}.jpg")
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext null
                destFile.absolutePath
            } catch (_: Exception) {
                null
            }
        }
}
