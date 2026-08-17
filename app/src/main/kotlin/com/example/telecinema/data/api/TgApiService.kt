package com.example.telecinema.data.api

import com.example.telecinema.model.Movie
import com.example.telecinema.model.VideoQuality
import com.example.telecinema.util.Lang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object TgApiService {
    private const val BASE_URL = "http://13.49.41.150:5000"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun cleanUsername(input: String): String {
        return input.trim()
            .replace("https://t.me/", "")
            .replace("http://t.me/", "")
            .replace("t.me/", "")
            .replace("t.me/+", "")
            .replace("t.me/joinchat/", "")
            .replace("@", "")
            .replace("/", "")
            .trim()
    }

    data class ChannelPageResult(
        val title: String,
        val avatar: String,
        val movies: List<Movie>
    )

    suspend fun fetchChannelPage(username: String, before: Int? = null): ChannelPageResult = withContext(Dispatchers.IO) {
        val clean = cleanUsername(username)
        val url = if (before != null) "$BASE_URL/channel/$clean?before=$before" else "$BASE_URL/channel/$clean"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "TeleCinema/1.0 (Android)")
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext ChannelPageResult(clean, "", emptyList())
            }

            val json = JSONObject(body)
            val title = json.optString("title", clean)
            val avatar = json.optString("avatar", "")
            val items = json.optJSONArray("movies") ?: JSONArray()

            val movies = mutableListOf<Movie>()
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val msgId = item.optInt("msg_id", item.optInt("id", 0))
                val movieTitle = item.optString("title", "فيلم $msgId")
                val posterUrl = item.optString("poster", "$BASE_URL/poster/$clean/$msgId")
                val videoUrl = item.optString("stream_url", "$BASE_URL/stream/$clean/$msgId")
                val desc = item.optString("description", "")
                val quality = item.optString("quality", "HD")
                val duration = item.optString("duration", "")
                val size = item.optString("size", "")
                val date = item.optString("date", "")
                val year = item.optInt("year", 0)

                val genresList = mutableListOf<String>()
                val genresJson = item.optJSONArray("genres")
                if (genresJson != null) {
                    for (g in 0 until genresJson.length()) {
                        genresList.add(genresJson.optString(g))
                    }
                }

                val qualitiesList = mutableListOf<VideoQuality>()
                qualitiesList.add(VideoQuality(label = quality.ifEmpty { "1080p" }, url = videoUrl))

                val altsJson = item.optJSONArray("alts")
                val altsMapList = mutableListOf<Map<String, String>>()
                if (altsJson != null) {
                    for (a in 0 until altsJson.length()) {
                        val altObj = altsJson.optJSONObject(a) ?: continue
                        val q = altObj.optString("q", "720p")
                        val u = altObj.optString("url", "")
                        if (u.isNotEmpty()) {
                            altsMapList.add(mapOf("q" to q, "url" to u))
                            qualitiesList.add(VideoQuality(label = q, url = u))
                        }
                    }
                }

                movies.add(
                    Movie(
                        id = "${clean}_$msgId",
                        channel = clean,
                        msgId = msgId,
                        title = movieTitle,
                        poster = posterUrl,
                        videoUrl = videoUrl,
                        description = desc,
                        genres = genresList,
                        quality = quality,
                        size = size,
                        duration = duration,
                        date = date,
                        alts = altsMapList,
                        qualities = qualitiesList,
                        year = year
                    )
                )
            }

            ChannelPageResult(title = title, avatar = avatar, movies = movies)
        } catch (e: Exception) {
            ChannelPageResult(clean, "", emptyList())
        }
    }
}
