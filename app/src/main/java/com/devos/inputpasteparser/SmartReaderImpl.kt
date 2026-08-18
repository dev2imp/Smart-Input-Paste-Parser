package com.devos.inputpasteparser

import android.content.Context
import android.content.ClipboardManager
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.regex.Pattern
import androidx.core.net.toUri

class SmartPasteReaderImpl(
    private val context: Context,
    private val storageDir: File = File(context.filesDir, "smartpaste")
) : SmartReader {

    private val urlPattern = Pattern.compile(
        "https?://[\\w.-]+(?:\\.[\\w\\-]+)+[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]*"
    )

    init {
        if (!storageDir.exists()) storageDir.mkdirs()
    }

    override suspend fun readClipboard(): CapturedContent = withContext(Dispatchers.IO) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip

        if (clip == null || clip.itemCount == 0) return@withContext CapturedContent.Empty

        val item = clip.getItemAt(0)

        item.uri?.let { uri ->
            return@withContext classifyUri(uri.toString())
        }
        val text = item.coerceToText(context)?.toString()
        if (text.isNullOrBlank()) return@withContext CapturedContent.Empty
        classifyText(text)
    }

    override suspend fun classifyUri(uri: String): CapturedContent = withContext(Dispatchers.IO) {
        val parsedUri = uri.toUri()
        val mimeType = resolveMimeType(parsedUri)
        val originalName = resolveFileName(parsedUri)

        when {
            mimeType?.startsWith("image/") == true -> {
                val saved = copyToSandbox(parsedUri, extensionFor(mimeType))
                    ?: return@withContext CapturedContent.Empty
                CapturedContent.Image(savedFile = saved, originalUri = uri)
            }
            mimeType?.startsWith("video/") == true -> {
                val saved = copyToSandbox(parsedUri, extensionFor(mimeType))
                    ?: return@withContext CapturedContent.Empty
                CapturedContent.Video(savedFile = saved, originalUri = uri)
            }
            else -> {
                val saved = copyToSandbox(parsedUri, extensionFor(mimeType ?: "application/octet-stream"))
                    ?: return@withContext CapturedContent.Empty
                CapturedContent.GenericFile(
                    savedFile = saved,
                    originalFileName = originalName ?: saved.name,
                    mimeType = mimeType,
                    originalUri = uri
                )
            }
        }
    }

    private fun classifyText(text: String): CapturedContent {
        val matcher = urlPattern.matcher(text)
        return if (matcher.find() && matcher.start() == 0 && matcher.end() == text.trim().length) {
            CapturedContent.Link(url = matcher.group(), rawText = text)
        } else {
            CapturedContent.Text(value = text)
        }
    }

    private fun resolveMimeType(uri: Uri): String? {
        return context.contentResolver.getType(uri)
            ?: MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(uri.toString().substringAfterLast('.', ""))
    }

    private fun resolveFileName(uri: Uri): String? {
        if (uri.scheme != "content") return uri.lastPathSegment

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) return cursor.getString(nameIndex)
            }
        } finally {
            cursor?.close()
        }
        return uri.lastPathSegment
    }

    private fun extensionFor(mimeType: String): String {
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
    }

    private fun copyToSandbox(uri: Uri, extension: String): File? {
        return try {
            val destination = File(storageDir, "${UUID.randomUUID()}.$extension")
            context.contentResolver.openInputStream(uri)?.use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destination
        } catch (e: SecurityException) {
            null
        }
    }
}