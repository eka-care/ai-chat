package com.eka.conversation.matrix.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.eka.conversation.matrix.PresignedUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** A file picked by the user, already read into memory (Matrix caps uploads at 3 MB). */
data class MatrixFile(
    val name: String,
    val mimeType: String,
    val bytes: ByteArray,
    val uri: Uri? = null,
) {
    val size: Int get() = bytes.size
    val isImage: Boolean get() = mimeType.startsWith("image/")

    override fun equals(other: Any?) = other is MatrixFile && other.name == name && other.size == size && other.uri == uri
    override fun hashCode() = (name.hashCode() * 31 + size) * 31 + (uri?.hashCode() ?: 0)

    companion object {
        const val MAX_FILE_SIZE = 3 * 1024 * 1024
        const val MAX_FILES_PER_MESSAGE = 3
        const val MAX_FILES_PER_SESSION = 5
        const val MAX_TOTAL_SIZE = 10 * 1024 * 1024
        val ALLOWED_TYPES = setOf("application/pdf", "image/jpeg", "image/png", "image/jpg")

        fun isValid(file: MatrixFile) = file.mimeType in ALLOWED_TYPES && file.size < MAX_FILE_SIZE

        /** Reads a content Uri; returns null when it cannot be opened. */
        suspend fun fromUri(context: Context, uri: Uri): MatrixFile? = withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val mime = resolver.getType(uri) ?: "application/octet-stream"
                var name = "file"
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) name = c.getString(0) ?: name
                }
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
                MatrixFile(name, mime, bytes, uri)
            } catch (_: Exception) {
                null
            }
        }
    }
}

/**
 * Port of synapse `Filemanager`: holds files between the two chat frames of the upload handshake
 * (1. client sends extensions+sizes, 2. server answers presigned urls, 3. client uploads then sends ids).
 */
class FileUploadManager(private val okHttp: OkHttpClient) {
    var pendingFiles: List<MatrixFile> = emptyList(); private set
    var pendingMessage: String = ""; private set

    fun setFilesForUpload(files: List<MatrixFile>, message: String?) {
        val valid = files.filter(MatrixFile::isValid)
        require(valid.isNotEmpty()) { "File not supported" }
        pendingFiles = valid
        pendingMessage = message?.trim().orEmpty()
    }

    val formats get() = pendingFiles.map { it.mimeType }
    val sizes get() = pendingFiles.map { it.size }

    /** Uploads each pending file to its presigned url; returns the ids that succeeded. */
    suspend fun upload(urls: List<PresignedUrl>): List<String> = withContext(Dispatchers.IO) {
        val pairs = urls.mapIndexedNotNull { i, u -> pendingFiles.getOrNull(i)?.let { u to it } }
            .filter { it.first.url.isNotBlank() }
        require(pairs.isNotEmpty()) { "No valid presigned URLs provided" }
        val ok = mutableListOf<String>()
        try {
            for ((entry, file) in pairs) {
                val request = if (entry.fields != null) {
                    val body = MultipartBody.Builder().setType(MultipartBody.FORM).apply {
                        entry.fields.forEach { (k, v) -> addFormDataPart(k, v) }
                        addFormDataPart("file", file.name, file.bytes.toRequestBody(file.mimeType.toMediaType()))
                    }.build()
                    Request.Builder().url(entry.url).post(body).build()
                } else {
                    Request.Builder().url(entry.url)
                        .header("Content-Type", file.mimeType)
                        .header("x-ms-blob-type", "BlockBlob")
                        .put(file.bytes.toRequestBody(file.mimeType.toMediaType()))
                        .build()
                }
                okHttp.newCall(request).execute().use { res ->
                    if (!res.isSuccessful) throw IllegalStateException("Failed to upload ${file.name}: ${res.code}")
                }
                ok += entry.id
            }
            ok
        } catch (e: Exception) {
            if (ok.isNotEmpty()) ok else throw e
        }
    }

    fun clear() {
        pendingFiles = emptyList()
        pendingMessage = ""
    }
}
