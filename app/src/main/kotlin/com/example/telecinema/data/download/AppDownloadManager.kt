package com.example.telecinema.data.download

import android.content.Context
import com.example.telecinema.model.DownloadItem
import com.example.telecinema.model.DownloadStatus
import com.example.telecinema.model.Movie
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

object AppDownloadManager {
    private lateinit var appContext: Context
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = OkHttpClient()

    private val downloadsMap = ConcurrentHashMap<String, DownloadItem>()
    private val jobsMap = ConcurrentHashMap<String, Job>()

    private val _downloadsFlow = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloadsFlow = _downloadsFlow.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun startDownload(movie: Movie, onStarted: (() -> Unit)? = null) {
        if (downloadsMap.containsKey(movie.id) && downloadsMap[movie.id]?.status == DownloadStatus.DOWNLOADING) {
            return
        }

        val moviesDir = File(appContext.filesDir, "downloads").apply { mkdirs() }
        val sanitizedTitle = movie.title.replace(Regex("[^a-zA-Z0-9._\\- ]"), "_").take(30)
        val file = File(moviesDir, "${movie.id}_$sanitizedTitle.mp4")

        val item = DownloadItem(
            id = movie.id,
            movieId = movie.id,
            title = movie.title,
            poster = movie.poster,
            videoUrl = movie.videoUrl,
            filePath = file.absolutePath,
            progress = 0f,
            status = DownloadStatus.DOWNLOADING
        )

        downloadsMap[movie.id] = item
        _downloadsFlow.value = downloadsMap.values.toList()
        onStarted?.invoke()

        val job = scope.launch {
            try {
                val request = Request.Builder()
                    .url(movie.videoUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "*/*")
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body
                if (!response.isSuccessful || body == null) {
                    downloadsMap[movie.id] = item.copy(status = DownloadStatus.FAILED)
                    _downloadsFlow.value = downloadsMap.values.toList()
                    return@launch
                }

                val totalLength = body.contentLength()
                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(file)

                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (!isActive) {
                        outputStream.close()
                        inputStream.close()
                        return@launch
                    }
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    val progress = if (totalLength > 0) totalBytesRead.toFloat() / totalLength else 0.5f
                    downloadsMap[movie.id] = item.copy(
                        progress = progress,
                        downloadedBytes = totalBytesRead,
                        totalBytes = totalLength,
                        status = DownloadStatus.DOWNLOADING
                    )
                    _downloadsFlow.value = downloadsMap.values.toList()
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                downloadsMap[movie.id] = item.copy(
                    progress = 1.0f,
                    downloadedBytes = totalBytesRead,
                    totalBytes = totalBytesRead,
                    status = DownloadStatus.COMPLETED
                )
                _downloadsFlow.value = downloadsMap.values.toList()
            } catch (e: Exception) {
                if (isActive) {
                    downloadsMap[movie.id] = item.copy(status = DownloadStatus.FAILED)
                    _downloadsFlow.value = downloadsMap.values.toList()
                }
            }
        }

        jobsMap[movie.id] = job
    }

    fun cancelDownload(id: String) {
        jobsMap[id]?.cancel()
        jobsMap.remove(id)
        val item = downloadsMap[id]
        if (item != null) {
            try {
                File(item.filePath).delete()
            } catch (_: Exception) {}
        }
        downloadsMap.remove(id)
        _downloadsFlow.value = downloadsMap.values.toList()
    }

    fun deleteDownloadedFile(id: String) {
        cancelDownload(id)
    }

    fun isDownloaded(movieId: String): Boolean {
        val item = downloadsMap[movieId]
        return item != null && item.status == DownloadStatus.COMPLETED && File(item.filePath).exists()
    }
}
