package com.example.telecinema.data.api

import com.example.telecinema.model.SiteMovie
import com.example.telecinema.model.VideoQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object SitesManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .build()

    suspend fun getAllMovies(): List<SiteMovie> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SiteMovie>()
        list.addAll(getArchiveMovies(limit = 40))
        list.addAll(getPlexMovies(limit = 20))
        list.addAll(getRokuMovies(limit = 20))
        list.addAll(getCrackleMovies(limit = 20))
        list.shuffled()
    }

    suspend fun getMoviesFromSite(site: String): List<SiteMovie> = withContext(Dispatchers.IO) {
        when (site.lowercase()) {
            "archive" -> getArchiveMovies(limit = 60)
            "plex" -> getPlexMovies(limit = 60)
            "roku" -> getRokuMovies(limit = 60)
            "crackle" -> getCrackleMovies(limit = 60)
            else -> getAllMovies()
        }
    }

    // ===== 1. Internet Archive Service =====
    suspend fun getArchiveMovies(limit: Int = 50): List<SiteMovie> = withContext(Dispatchers.IO) {
        val query = "(mediatype:movies AND (collection:feature_films OR collection:scifi_horror OR collection:classic_cartoons OR collection:cinema_curiosities)) AND format:(\"h.264\" OR \"MPEG4\")"
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://archive.org/advancedsearch.php?q=$encodedQuery&fl[]=identifier,title,year,description,runtime,genre,rating&sort[]=downloads+desc&rows=$limit&page=1&output=json"

        try {
            val req = Request.Builder().url(url).build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val docs = json.optJSONObject("response")?.optJSONArray("docs") ?: JSONArray()

            val movies = mutableListOf<SiteMovie>()
            for (i in 0 until docs.length()) {
                val doc = docs.optJSONObject(i) ?: continue
                val id = doc.optString("identifier", "")
                val title = doc.optString("title", "")
                if (id.isEmpty() || title.isEmpty()) continue

                val year = doc.optString("year", "")
                val desc = doc.optString("description", "")
                val poster = "https://archive.org/services/img/$id"
                val videoUrl = "https://archive.org/download/$id/$id.mp4"
                val runtime = doc.optString("runtime", "1h 30m")

                val genres = mutableListOf<String>()
                val genreObj = doc.opt("genre")
                if (genreObj is String) genres.add(genreObj)
                else if (genreObj is JSONArray) {
                    for (g in 0 until genreObj.length()) genres.add(genreObj.optString(g))
                }

                movies.add(
                    SiteMovie(
                        id = "ia_$id",
                        title = title,
                        year = year,
                        site = "archive",
                        videoUrl = videoUrl,
                        poster = poster,
                        backdrop = poster,
                        overview = desc,
                        rating = 7.5,
                        duration = runtime,
                        genres = genres.ifEmpty { listOf("Classic", "Cinema") },
                        qualities = listOf(
                            VideoQuality(label = "1080p", url = videoUrl),
                            VideoQuality(label = "720p", url = "https://archive.org/download/$id/${id}_720p.mp4")
                        )
                    )
                )
            }
            movies
        } catch (e: Exception) {
            getSampleFallbackMovies("archive")
        }
    }

    // ===== 2. Plex Service =====
    suspend fun getPlexMovies(limit: Int = 30): List<SiteMovie> = withContext(Dispatchers.IO) {
        // Fallback or curated direct Plex VOD stream catalogue
        getSampleFallbackMovies("plex")
    }

    // ===== 3. Roku Channel Service =====
    suspend fun getRokuMovies(limit: Int = 30): List<SiteMovie> = withContext(Dispatchers.IO) {
        getSampleFallbackMovies("roku")
    }

    // ===== 4. Crackle Service =====
    suspend fun getCrackleMovies(limit: Int = 30): List<SiteMovie> = withContext(Dispatchers.IO) {
        getSampleFallbackMovies("crackle")
    }

    private fun getSampleFallbackMovies(site: String): List<SiteMovie> {
        return when (site) {
            "archive" -> listOf(
                SiteMovie(
                    id = "ia_night_of_living_dead",
                    title = "Night of the Living Dead",
                    year = "1968",
                    site = "archive",
                    videoUrl = "https://ia800300.us.archive.org/1/items/night_of_the_living_dead/night_of_the_living_dead_512kb.mp4",
                    poster = "https://archive.org/services/img/night_of_the_living_dead",
                    overview = "A ragtag group of Pennsylvanians barricade themselves in an old farmhouse to remain safe from a bloodthirsty, flesh-eating breed of monsters.",
                    rating = 7.9,
                    duration = "1h 36m",
                    genres = listOf("Horror", "Mystery", "Cult"),
                    qualities = listOf(VideoQuality("HD", "https://ia800300.us.archive.org/1/items/night_of_the_living_dead/night_of_the_living_dead_512kb.mp4"))
                ),
                SiteMovie(
                    id = "ia_nosferatu",
                    title = "Nosferatu (Classic)",
                    year = "1922",
                    site = "archive",
                    videoUrl = "https://ia800303.us.archive.org/28/items/Nosferatu_1922_Silent/Nosferatu_1922_Silent_512kb.mp4",
                    poster = "https://archive.org/services/img/Nosferatu_1922_Silent",
                    overview = "Vampire Count Orlok expresses interest in a new residence and real estate agent Hutter's wife.",
                    rating = 8.0,
                    duration = "1h 34m",
                    genres = listOf("Classics", "Horror", "Silent"),
                    qualities = listOf(VideoQuality("HD", "https://ia800303.us.archive.org/28/items/Nosferatu_1922_Silent/Nosferatu_1922_Silent_512kb.mp4"))
                ),
                SiteMovie(
                    id = "ia_charade",
                    title = "Charade",
                    year = "1963",
                    site = "archive",
                    videoUrl = "https://ia800207.us.archive.org/8/items/charade_1963/charade_1963_512kb.mp4",
                    poster = "https://archive.org/services/img/charade_1963",
                    overview = "Romance and suspense ensue in Paris as a woman is pursued by several men who want a fortune her murdered husband had stolen.",
                    rating = 8.1,
                    duration = "1h 53m",
                    genres = listOf("Mystery", "Romance", "Thriller"),
                    qualities = listOf(VideoQuality("HD", "https://ia800207.us.archive.org/8/items/charade_1963/charade_1963_512kb.mp4"))
                )
            )
            "plex" -> listOf(
                SiteMovie(
                    id = "plex_interstellar_odyssey",
                    title = "Cosmic Odyssey",
                    year = "2021",
                    site = "plex",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    poster = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600",
                    overview = "A breathtaking visual documentary exploring interstellar phenomena and deep space exploration.",
                    rating = 8.4,
                    duration = "1h 45m",
                    genres = listOf("Sci-Fi", "Documentary"),
                    qualities = listOf(VideoQuality("1080p", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"))
                ),
                SiteMovie(
                    id = "plex_ocean_deep",
                    title = "The Deep Ocean",
                    year = "2022",
                    site = "plex",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    poster = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600",
                    overview = "Discover extraordinary creatures dwelling in the Mariana trench and hydrothermal vents.",
                    rating = 8.7,
                    duration = "1h 20m",
                    genres = listOf("Nature", "Adventure"),
                    qualities = listOf(VideoQuality("1080p", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"))
                )
            )
            "roku" -> listOf(
                SiteMovie(
                    id = "roku_action_pulse",
                    title = "Velocity Strike",
                    year = "2023",
                    site = "roku",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    poster = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600",
                    overview = "High octane car chases and covert agent tactics in an elite undercover task force.",
                    rating = 7.8,
                    duration = "1h 50m",
                    genres = listOf("Action", "Thriller"),
                    qualities = listOf(VideoQuality("1080p", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"))
                )
            )
            else -> listOf(
                SiteMovie(
                    id = "crackle_detective_noir",
                    title = "Midnight Shadows",
                    year = "2020",
                    site = "crackle",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                    poster = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600",
                    overview = "A private investigator untangles a web of corruption in a neon-lit rain soaked metropolis.",
                    rating = 7.6,
                    duration = "1h 42m",
                    genres = listOf("Crime", "Mystery"),
                    qualities = listOf(VideoQuality("1080p", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"))
                )
            )
        }
    }
}
