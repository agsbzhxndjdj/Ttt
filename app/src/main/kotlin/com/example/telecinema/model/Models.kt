package com.example.telecinema.model

import kotlinx.serialization.Serializable

@Serializable
data class Movie(
    val id: String = "",
    val channel: String = "",
    val msgId: Int = 0,
    val title: String = "",
    val poster: String = "",
    val videoUrl: String = "",
    val description: String = "",
    val genres: List<String> = emptyList(),
    val quality: String = "",
    val size: String = "",
    val duration: String = "",
    val date: String = "",
    val alts: List<Map<String, String>> = emptyList(),
    val qualities: List<VideoQuality> = emptyList(),
    val year: Int = 0,
    val sizeMb: Double = 0.0
) {
    val durationSeconds: Int
        get() {
            val parts = duration.split(":")
            return try {
                when (parts.size) {
                    3 -> parts[0].toInt() * 3600 + parts[1].toInt() * 60 + parts[2].toInt()
                    2 -> parts[0].toInt() * 60 + parts[1].toInt()
                    else -> 0
                }
            } catch (e: Exception) {
                0
            }
        }
}

@Serializable
data class VideoQuality(
    val label: String = "",
    val url: String = "",
    val bandwidth: Long = 0
)

@Serializable
data class Channel(
    val username: String = "",
    val title: String = "",
    val avatar: String = "",
    val movieCount: Int = 0
)

@Serializable
data class SiteMovie(
    val id: String = "",
    val title: String = "",
    val year: String = "",
    val site: String = "", // archive, plex, roku, crackle
    val videoUrl: String = "",
    val poster: String = "",
    val backdrop: String = "",
    val overview: String = "",
    val rating: Double = 0.0,
    val duration: String = "",
    val genres: List<String> = emptyList(),
    val qualities: List<VideoQuality> = emptyList(),
    val hasSubtitle: Boolean = false,
    val subtitleLang: String = "",
    val tmdbId: Int? = null
) {
    fun toMovie(): Movie {
        return Movie(
            id = id,
            channel = site.uppercase(),
            msgId = 0,
            title = title,
            poster = poster,
            videoUrl = videoUrl,
            description = overview,
            genres = genres,
            quality = qualities.firstOrNull()?.label ?: "HD",
            size = "",
            duration = duration,
            date = year,
            qualities = qualities,
            year = year.filter { it.isDigit() }.toIntOrNull() ?: 0
        )
    }
}

@Serializable
data class Comment(
    val id: String = "",
    val movieId: String = "",
    val author: String = "User",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class DownloadItem(
    val id: String = "",
    val movieId: String = "",
    val title: String = "",
    val poster: String = "",
    val videoUrl: String = "",
    val filePath: String = "",
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.DOWNLOADING
)

enum class DownloadStatus {
    DOWNLOADING, PAUSED, COMPLETED, FAILED
}

@Serializable
data class UserStats(
    val secondsWatched: Long = 0L,
    val moviesCount: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastActiveDate: String = ""
)

@Serializable
data class BackupData(
    val channels: List<Channel> = emptyList(),
    val favorites: List<String> = emptyList(),
    val watchLater: List<String> = emptyList(),
    val history: List<String> = emptyList(),
    val stats: UserStats = UserStats()
)
