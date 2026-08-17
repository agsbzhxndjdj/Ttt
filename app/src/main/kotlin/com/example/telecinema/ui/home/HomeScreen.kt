package com.example.telecinema.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.telecinema.data.api.TgApiService
import com.example.telecinema.data.local.AppPreferences
import com.example.telecinema.model.Movie
import com.example.telecinema.ui.components.*
import com.example.telecinema.util.Lang
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    locale: String,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToPlayer: (Movie) -> Unit,
    onNavigateToChannels: () -> Unit,
    onNavigateToDiscover: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }

    var showWheelDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var filterCriteria by remember { mutableStateOf(FilterCriteria()) }

    val dataVersion by AppPreferences.dataVersion.collectAsState()

    // Trigger initial channel loads if empty
    LaunchedEffect(Unit) {
        val chs = AppPreferences.getChannels()
        if (AppPreferences.getAllMovies().isEmpty() && chs.isNotEmpty()) {
            isRefreshing = true
            for (ch in chs) {
                try {
                    val res = TgApiService.fetchChannelPage(ch.username)
                    if (res.movies.isNotEmpty()) {
                        AppPreferences.saveMoviesForChannel(ch.username, res.movies)
                    }
                } catch (_: Exception) {}
            }
            isRefreshing = false
        }
    }

    val refreshAllChannels = {
        scope.launch {
            isRefreshing = true
            for (ch in AppPreferences.getChannels()) {
                try {
                    val res = TgApiService.fetchChannelPage(ch.username)
                    if (res.movies.isNotEmpty()) {
                        AppPreferences.saveMoviesForChannel(ch.username, res.movies)
                    }
                } catch (_: Exception) {}
            }
            isRefreshing = false
        }
    }

    val allMovies = remember(dataVersion, isRefreshing) { AppPreferences.getAllMovies() }
    val continueWatchingList = remember(allMovies, dataVersion) {
        allMovies.filter {
            val pos = AppPreferences.getPosition(it.id)
            val dur = it.durationSeconds
            pos > 15 && (dur == 0 || pos < dur * 0.95)
        }
    }

    // Filter & sort movies
    val filteredMovies = remember(allMovies, searchQuery, filterCriteria, dataVersion) {
        var list = allMovies

        if (searchQuery.isNotBlank()) {
            val normQuery = Lang.normalizeArabic(searchQuery)
            list = list.filter {
                Lang.normalizeArabic(it.title).contains(normQuery) ||
                Lang.normalizeArabic(it.description).contains(normQuery) ||
                it.channel.contains(searchQuery, ignoreCase = true)
            }
        }

        if (filterCriteria.selectedGenre.isNotEmpty()) {
            list = list.filter { it.genres.any { g -> g.equals(filterCriteria.selectedGenre, ignoreCase = true) } }
        }

        if (filterCriteria.selectedQuality.isNotEmpty()) {
            list = list.filter { it.quality.equals(filterCriteria.selectedQuality, ignoreCase = true) }
        }

        if (filterCriteria.selectedChannel.isNotEmpty()) {
            list = list.filter { it.channel.equals(filterCriteria.selectedChannel, ignoreCase = true) }
        }

        if (AppPreferences.hideWatched) {
            val histIds = AppPreferences.getHistoryMovies().map { it.id }.toSet()
            list = list.filterNot { histIds.contains(it.id) }
        }

        when (filterCriteria.sortBy) {
            "oldest" -> list.reversed()
            "title" -> list.sortedBy { it.title }
            "year" -> list.sortedByDescending { it.year }
            else -> list // newest
        }
    }

    val availableGenres = remember(allMovies) {
        allMovies.flatMap { it.genres }.distinct().filter { it.isNotBlank() }
    }
    val availableQualities = remember(allMovies) {
        allMovies.map { it.quality }.distinct().filter { it.isNotBlank() }
    }
    val availableChannels = remember(allMovies) {
        allMovies.map { it.channel }.distinct().filter { it.isNotBlank() }
    }

    val heroMovie = remember(allMovies) {
        allMovies.firstOrNull { it.poster.isNotEmpty() } ?: allMovies.firstOrNull()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchExpanded) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(Lang.t("search", locale), fontSize = 14.sp) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .testTag("search_text_field")
                                .fillMaxWidth()
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = Lang.t("appName", locale),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isSearchExpanded = !isSearchExpanded
                            if (!isSearchExpanded) searchQuery = ""
                        },
                        modifier = Modifier.testTag("search_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }

                    IconButton(
                        onClick = { showWheelDialog = true },
                        modifier = Modifier.testTag("wheel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "Wheel of Fortune",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { showFilterDialog = true },
                        modifier = Modifier.testTag("filter_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter"
                        )
                    }

                    IconButton(
                        onClick = {
                            AppPreferences.viewMode = if (AppPreferences.viewMode == "grid") "list" else "grid"
                        },
                        modifier = Modifier.testTag("view_mode_button")
                    ) {
                        Icon(
                            imageVector = if (AppPreferences.viewMode == "grid") Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "View Mode"
                        )
                    }

                    IconButton(
                        onClick = { refreshAllChannels() },
                        enabled = !isRefreshing,
                        modifier = Modifier.testTag("refresh_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (allMovies.isEmpty() && !isRefreshing) {
                // Empty state - Prompt to add channel
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LiveTv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = Lang.t("noChannels", locale),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = Lang.t("noChannelsHint", locale),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onNavigateToChannels,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("add_channel_prompt_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Lang.t("addChannel", locale), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // 1. Hero Spotlight Banner (if not searching)
                    if (searchQuery.isBlank() && heroMovie != null) {
                        item {
                            Card(
                                modifier = Modifier
                                    .testTag("hero_banner")
                                    .fillMaxWidth()
                                    .height(230.dp)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clickable { onNavigateToDetails(heroMovie.id) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (heroMovie.poster.isNotEmpty()) {
                                        AsyncImage(
                                            model = heroMovie.poster,
                                            contentDescription = heroMovie.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color.Black.copy(alpha = 0.5f),
                                                        Color.Black.copy(alpha = 0.95f)
                                                    )
                                                )
                                            )
                                    )
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(16.dp)
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "🔥 " + Lang.t("popular", locale),
                                                color = Color.Black,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = heroMovie.title,
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { onNavigateToPlayer(heroMovie) },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                                modifier = Modifier.testTag("hero_play_button")
                                            ) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(Lang.t("play", locale), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                            OutlinedButton(
                                                onClick = { onNavigateToDetails(heroMovie.id) },
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                            ) {
                                                Text("التفاصيل", color = Color.White, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Continue Watching Shelf
                    if (searchQuery.isBlank() && continueWatchingList.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "▶ " + Lang.t("continueWatching", locale),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(continueWatchingList) { movie ->
                                        MovieCard(
                                            movie = movie,
                                            onClick = { onNavigateToPlayer(movie) },
                                            modifier = Modifier.width(135.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Movies Header
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) "نتائج البحث (${filteredMovies.size})" else Lang.t("movies", locale),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (filterCriteria.selectedGenre.isNotEmpty() || filterCriteria.selectedQuality.isNotEmpty()) {
                                    TextButton(onClick = { filterCriteria = FilterCriteria() }) {
                                        Text("إلغاء الفلتر", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // 4. Movies Items Grid / List
                    if (filteredMovies.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = Lang.t("noMovies", locale),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (AppPreferences.viewMode == "list") {
                        items(filteredMovies) { movie ->
                            MovieRowItem(
                                movie = movie,
                                onClick = { onNavigateToDetails(movie.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    } else {
                        // Chunk for 3-column grid inside LazyColumn
                        val rows = filteredMovies.chunked(3)
                        items(rows) { rowMovies ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (movie in rowMovies) {
                                    MovieCard(
                                        movie = movie,
                                        onClick = { onNavigateToDetails(movie.id) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                for (i in 0 until (3 - rowMovies.size)) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Wheel of Fortune Dialog
    if (showWheelDialog) {
        WheelDialog(
            movies = allMovies,
            locale = locale,
            onDismiss = { showWheelDialog = false },
            onMovieSelected = { onNavigateToPlayer(it) }
        )
    }

    // Filter Dialog
    if (showFilterDialog) {
        FilterDialog(
            currentFilter = filterCriteria,
            availableGenres = availableGenres,
            availableQualities = availableQualities,
            availableChannels = availableChannels,
            locale = locale,
            onDismiss = { showFilterDialog = false },
            onApply = { filterCriteria = it }
        )
    }
}
