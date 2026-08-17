package com.example.telecinema.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.telecinema.model.*
import com.example.telecinema.util.AppJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import java.text.SimpleDateFormat
import java.util.*

object AppPreferences {
    private const val PREFS_NAME = "tele_cinema_prefs"
    private lateinit var prefs: SharedPreferences

    private val _dataVersion = MutableStateFlow(0)
    val dataVersion = _dataVersion.asStateFlow()

    private val _themeFlow = MutableStateFlow("gold")
    val themeFlow = _themeFlow.asStateFlow()

    private val _localeFlow = MutableStateFlow("ar")
    val localeFlow = _localeFlow.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _themeFlow.value = prefs.getString("theme", "gold") ?: "gold"
        _localeFlow.value = prefs.getString("locale", "ar") ?: "ar"
        // Add default popular channels if empty
        if (getChannels().isEmpty()) {
            val defaults = listOf(
                Channel(username = "c1nema", title = "تلي سينما", avatar = ""),
                Channel(username = "filmaraby", title = "أفلام عربي", avatar = ""),
                Channel(username = "cinema_movies", title = "عالم الأفلام", avatar = "")
            )
            saveChannels(defaults)
        }
    }

    private fun notifyChanged() {
        _dataVersion.value = _dataVersion.value + 1
    }

    // ===== Locale & Theme =====
    var locale: String
        get() = prefs.getString("locale", "ar") ?: "ar"
        set(value) {
            prefs.edit().putString("locale", value).apply()
            _localeFlow.value = value
            notifyChanged()
        }

    var theme: String
        get() = prefs.getString("theme", "gold") ?: "gold"
        set(value) {
            prefs.edit().putString("theme", value).apply()
            _themeFlow.value = value
            notifyChanged()
        }

    var viewMode: String
        get() = prefs.getString("view_mode", "grid") ?: "grid"
        set(value) {
            prefs.edit().putString("view_mode", value).apply()
            notifyChanged()
        }

    var hideWatched: Boolean
        get() = prefs.getBoolean("hide_watched", false)
        set(value) {
            prefs.edit().putBoolean("hide_watched", value).apply()
            notifyChanged()
        }

    var incognito: Boolean
        get() = prefs.getBoolean("incognito", false)
        set(value) {
            prefs.edit().putBoolean("incognito", value).apply()
            notifyChanged()
        }

    var kidsMode: Boolean
        get() = prefs.getBoolean("kids_mode", false)
        set(value) {
            prefs.edit().putBoolean("kids_mode", value).apply()
            notifyChanged()
        }

    // ===== Channels =====
    fun getChannels(): List<Channel> {
        val raw = prefs.getString("channels_json", null) ?: return emptyList()
        return try {
            AppJson.decodeFromString(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveChannels(channels: List<Channel>) {
        val raw = AppJson.encodeToString(channels)
        prefs.edit().putString("channels_json", raw).apply()
        notifyChanged()
    }

    fun addChannel(channel: Channel) {
        val list = getChannels().toMutableList()
        if (list.none { it.username.equals(channel.username, ignoreCase = true) }) {
            list.add(channel)
            saveChannels(list)
        }
    }

    fun removeChannel(username: String) {
        val list = getChannels().filterNot { it.username.equals(username, ignoreCase = true) }
        saveChannels(list)
        // Also remove cached movies for this channel
        prefs.edit().remove("movies_${username.lowercase()}").apply()
        notifyChanged()
    }

    // ===== Movies per Channel =====
    fun getMoviesForChannel(channel: String): List<Movie> {
        val raw = prefs.getString("movies_${channel.lowercase()}", null) ?: return emptyList()
        return try {
            AppJson.decodeFromString(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveMoviesForChannel(channel: String, movies: List<Movie>) {
        val raw = AppJson.encodeToString(movies)
        prefs.edit().putString("movies_${channel.lowercase()}", raw).apply()
        // update count in channel
        val chs = getChannels().map {
            if (it.username.equals(channel, ignoreCase = true)) it.copy(movieCount = movies.size) else it
        }
        saveChannels(chs)
        notifyChanged()
    }

    fun getAllMovies(): List<Movie> {
        val all = mutableListOf<Movie>()
        for (c in getChannels()) {
            all.addAll(getMoviesForChannel(c.username))
        }
        return all.distinctBy { it.id.ifEmpty { "${it.channel}_${it.msgId}" } }
    }

    // ===== Favorites =====
    fun getFavoriteIds(): Set<String> {
        return prefs.getStringSet("favorites", emptySet()) ?: emptySet()
    }

    fun isFavorite(movieId: String): Boolean {
        return getFavoriteIds().contains(movieId)
    }

    fun toggleFavorite(movie: Movie) {
        val set = getFavoriteIds().toMutableSet()
        if (set.contains(movie.id)) {
            set.remove(movie.id)
        } else {
            set.add(movie.id)
            // also ensure movie cached in favorites_store
            saveMovieInfo(movie)
        }
        prefs.edit().putStringSet("favorites", set).apply()
        notifyChanged()
    }

    // ===== Watch Later =====
    fun getWatchLaterIds(): Set<String> {
        return prefs.getStringSet("watch_later", emptySet()) ?: emptySet()
    }

    fun isWatchLater(movieId: String): Boolean {
        return getWatchLaterIds().contains(movieId)
    }

    fun toggleWatchLater(movie: Movie) {
        val set = getWatchLaterIds().toMutableSet()
        if (set.contains(movie.id)) {
            set.remove(movie.id)
        } else {
            set.add(movie.id)
            saveMovieInfo(movie)
        }
        prefs.edit().putStringSet("watch_later", set).apply()
        notifyChanged()
    }

    // Single movie lookup cache
    fun saveMovieInfo(movie: Movie) {
        val raw = AppJson.encodeToString(movie)
        prefs.edit().putString("movie_cache_${movie.id}", raw).apply()
    }

    fun getMovieInfo(movieId: String): Movie? {
        val fromAll = getAllMovies().firstOrNull { it.id == movieId }
        if (fromAll != null) return fromAll
        val raw = prefs.getString("movie_cache_$movieId", null) ?: return null
        return try {
            AppJson.decodeFromString(raw)
        } catch (e: Exception) {
            null
        }
    }

    // ===== Playback Position & History =====
    fun getPosition(movieId: String): Int {
        return prefs.getInt("pos_$movieId", 0)
    }

    fun savePosition(movieId: String, seconds: Int) {
        prefs.edit().putInt("pos_$movieId", seconds).apply()
        notifyChanged()
    }

    fun markWatched(movie: Movie) {
        if (incognito) return
        saveMovieInfo(movie)
        val historySet = prefs.getStringSet("history_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        historySet.add(movie.id)
        prefs.edit().putStringSet("history_ids", historySet).apply()

        // update timestamp
        prefs.edit().putLong("hist_time_${movie.id}", System.currentTimeMillis()).apply()

        // update stats & streak
        updateStatsOnWatch()
        notifyChanged()
    }

    fun getHistoryMovies(): List<Movie> {
        val ids = prefs.getStringSet("history_ids", emptySet()) ?: emptySet()
        return ids.mapNotNull { getMovieInfo(it) }
            .sortedByDescending { prefs.getLong("hist_time_${it.id}", 0L) }
    }

    fun clearHistory() {
        prefs.edit().remove("history_ids").apply()
        notifyChanged()
    }

    // ===== Ratings & Comments =====
    fun getRating(movieId: String): Int {
        return prefs.getInt("rating_$movieId", 0)
    }

    fun setRating(movieId: String, rating: Int) {
        prefs.edit().putInt("rating_$movieId", rating).apply()
        notifyChanged()
    }

    fun getComments(movieId: String): List<Comment> {
        val raw = prefs.getString("comments_$movieId", null) ?: return emptyList()
        return try {
            AppJson.decodeFromString(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addComment(movieId: String, text: String, author: String = "أنا") {
        val list = getComments(movieId).toMutableList()
        list.add(
            Comment(
                id = UUID.randomUUID().toString(),
                movieId = movieId,
                author = author,
                text = text,
                timestamp = System.currentTimeMillis()
            )
        )
        prefs.edit().putString("comments_$movieId", AppJson.encodeToString(list)).apply()
        notifyChanged()
    }

    // ===== Stats & Streak =====
    fun getUserStats(): UserStats {
        val raw = prefs.getString("user_stats", null) ?: return UserStats()
        return try {
            AppJson.decodeFromString(raw)
        } catch (e: Exception) {
            UserStats()
        }
    }

    private fun updateStatsOnWatch() {
        val cur = getUserStats()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        var streak = cur.currentStreak
        if (cur.lastActiveDate.isNotEmpty()) {
            val lastDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(cur.lastActiveDate)
            val nowDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(today)
            if (lastDate != null && nowDate != null) {
                val diffDays = ((nowDate.time - lastDate.time) / (1000 * 60 * 60 * 24)).toInt()
                if (diffDays == 1) {
                    streak++
                } else if (diffDays > 1) {
                    streak = 1
                }
            }
        } else {
            streak = 1
        }

        val updated = cur.copy(
            moviesCount = cur.moviesCount + 1,
            secondsWatched = cur.secondsWatched + 3600, // standard credit
            currentStreak = streak,
            bestStreak = maxOf(cur.bestStreak, streak),
            lastActiveDate = today
        )
        prefs.edit().putString("user_stats", AppJson.encodeToString(updated)).apply()
    }

    // ===== Secret Vault =====
    var vaultPin: String
        get() = prefs.getString("vault_pin", "") ?: ""
        set(value) {
            prefs.edit().putString("vault_pin", value).apply()
            notifyChanged()
        }

    var isVaultUnlocked: Boolean = false

    fun getHiddenChannels(): Set<String> {
        return prefs.getStringSet("hidden_channels", emptySet()) ?: emptySet()
    }

    fun toggleChannelHidden(channel: String) {
        val set = getHiddenChannels().toMutableSet()
        if (set.contains(channel.lowercase())) {
            set.remove(channel.lowercase())
        } else {
            set.add(channel.lowercase())
        }
        prefs.edit().putStringSet("hidden_channels", set).apply()
        notifyChanged()
    }

    // ===== External Sources Enabled =====
    fun isSiteSourceEnabled(site: String): Boolean {
        return prefs.getBoolean("site_enabled_$site", true)
    }

    fun setSiteSourceEnabled(site: String, enabled: Boolean) {
        prefs.edit().putBoolean("site_enabled_$site", enabled).apply()
        notifyChanged()
    }

    // ===== Backup & Restore =====
    fun exportAllJson(): String {
        val backup = BackupData(
            channels = getChannels(),
            favorites = getFavoriteIds().toList(),
            watchLater = getWatchLaterIds().toList(),
            history = prefs.getStringSet("history_ids", emptySet())?.toList().orEmpty(),
            stats = getUserStats()
        )
        return AppJson.encodeToString(backup)
    }

    fun importAllJson(jsonString: String): Boolean {
        return try {
            val backup = AppJson.decodeFromString<BackupData>(jsonString)
            if (backup.channels.isNotEmpty()) {
                saveChannels(backup.channels)
            }
            if (backup.favorites.isNotEmpty()) {
                prefs.edit().putStringSet("favorites", backup.favorites.toSet()).apply()
            }
            if (backup.watchLater.isNotEmpty()) {
                prefs.edit().putStringSet("watch_later", backup.watchLater.toSet()).apply()
            }
            if (backup.history.isNotEmpty()) {
                prefs.edit().putStringSet("history_ids", backup.history.toSet()).apply()
            }
            prefs.edit().putString("user_stats", AppJson.encodeToString(backup.stats)).apply()
            notifyChanged()
            true
        } catch (e: Exception) {
            false
        }
    }
}
