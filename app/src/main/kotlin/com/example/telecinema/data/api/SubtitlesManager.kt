package com.example.telecinema.data.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.serialization.Serializable
import java.net.URLEncoder

@Serializable
data class SubtitleSource(
    val name: String,
    val siteName: String,
    val language: String,
    val searchUrlTemplate: String,
    val description: String,
    val iconName: String
)

@Serializable
data class MovieDownloadSource(
    val name: String,
    val siteName: String,
    val quality: String,
    val type: String, // Torrent, Direct MP4, Web Stream
    val searchUrlTemplate: String,
    val description: String
)

object SubtitlesManager {

    val subtitleSources = listOf(
        SubtitleSource(
            name = "Subdl (عربي / English)",
            siteName = "subdl.com",
            language = "عربي / متعدد",
            searchUrlTemplate = "https://subdl.com/search?query=%s",
            description = "أكبر موقع لتحميل الترجمات العربية والأجنبية المتوافقة مع جميع النسخ",
            iconName = "subdl"
        ),
        SubtitleSource(
            name = "OpenSubtitles (مفتوح)",
            siteName = "opensubtitles.org",
            language = "عربي / English",
            searchUrlTemplate = "https://www.opensubtitles.org/ar/search2/sublanguageid-ara,eng/moviename-%s",
            description = "قاعدة بيانات عالمية لترجمات الأفلام والمسلسلات بجميع الجودات",
            iconName = "opensubtitles"
        ),
        SubtitleSource(
            name = "Subscene (ترجمات عربية)",
            siteName = "subscene.best",
            language = "عربي",
            searchUrlTemplate = "https://subscene.best/subtitles/searchbytitle?query=%s",
            description = "ترجمات دقيقة واحترافية لكبار المترجمين العرب",
            iconName = "subscene"
        ),
        SubtitleSource(
            name = "YIFY Subtitles",
            siteName = "yifysubtitles.ch",
            language = "عربي / English",
            searchUrlTemplate = "https://yifysubtitles.ch/search?q=%s",
            description = "ترجمات متوافقة بدقة مع نسخ YTS و BluRay و WEB-DL",
            iconName = "yts"
        )
    )

    val movieDownloadSources = listOf(
        MovieDownloadSource(
            name = "Internet Archive (تحميل مباشر)",
            siteName = "archive.org",
            quality = "1080p / 720p",
            type = "تحميل مباشر MP4",
            searchUrlTemplate = "https://archive.org/search.php?query=%s&and[]=mediatype%%3A\"movies\"",
            description = "سيرفرات فائقة السرعة للتحميل المباشر بصيغة MP4 بدون إعلانات"
        ),
        MovieDownloadSource(
            name = "YTS / YIFY Movies (تورنت ومباشر)",
            siteName = "yts.mx",
            quality = "1080p / 720p / 4K",
            type = "تورنت سريع / مباشر",
            searchUrlTemplate = "https://yts.mx/browse-movies/%s/all/all/0/latest/0/all",
            description = "أفضل جودات بأصغر حجم ملف مع صوت نقي وترجمة متوافقة"
        ),
        MovieDownloadSource(
            name = "1337x Movies",
            siteName = "1337x.to",
            quality = "1080p / 2160p HDR",
            type = "تورنت توربو",
            searchUrlTemplate = "https://1337x.to/category-search/%s/Movies/1/",
            description = "مكتبة ضخمة لنسخ الريمكس والبلوراي الأصلية بجودات فائقة"
        ),
        MovieDownloadSource(
            name = "Google Direct MP4 Search",
            siteName = "google.com",
            quality = "Original MP4",
            type = "روابط مباشرة",
            searchUrlTemplate = "https://www.google.com/search?q=%s+filetype:mp4+OR+inurl:mkv",
            description = "بحث ذكي عن روابط التحميل المباشرة على سيرفرات سحابية مفتوحة"
        )
    )

    fun openSubtitleSearch(context: Context, movieTitle: String, source: SubtitleSource) {
        val cleanTitle = cleanSearchTitle(movieTitle)
        val encoded = URLEncoder.encode(cleanTitle, "UTF-8")
        val finalUrl = String.format(source.searchUrlTemplate, encoded)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openMovieDownloadSearch(context: Context, movieTitle: String, source: MovieDownloadSource) {
        val cleanTitle = cleanSearchTitle(movieTitle)
        val encoded = URLEncoder.encode(cleanTitle, "UTF-8")
        val finalUrl = String.format(source.searchUrlTemplate, encoded)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openTrailer(context: Context, movieTitle: String) {
        val cleanTitle = cleanSearchTitle(movieTitle)
        val query = "$cleanTitle official trailer"
        val encoded = URLEncoder.encode(query, "UTF-8")
        val appUri = Uri.parse("vnd.youtube:$encoded")
        val webUri = Uri.parse("https://www.youtube.com/results?search_query=$encoded")
        val appIntent = Intent(Intent.ACTION_VIEW, appUri).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        try {
            context.startActivity(appIntent)
        } catch (_: Exception) {
            try {
                context.startActivity(webIntent)
            } catch (_: Exception) {}
        }
    }

    private fun cleanSearchTitle(title: String): String {
        return title
            .replace(Regex("(?i)\\[.*?\\]|\\(.*?\\)"), "")
            .replace(Regex("(?i)1080p|720p|480p|bluray|web-dl|hdrip|dvdrip|x264|x265|hevc"), "")
            .replace(Regex("(?i)مترجم|مدبلج|فيلم|حصريا|جودة|عالية"), "")
            .trim()
    }
}
