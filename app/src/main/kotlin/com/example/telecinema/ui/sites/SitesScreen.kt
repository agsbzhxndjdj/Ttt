package com.example.telecinema.ui.sites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telecinema.data.api.SitesManager
import com.example.telecinema.data.api.SubtitlesManager
import com.example.telecinema.data.local.AppPreferences
import com.example.telecinema.model.Movie
import com.example.telecinema.model.SiteMovie
import com.example.telecinema.ui.components.MovieCard
import com.example.telecinema.util.Lang
import kotlinx.coroutines.launch

data class MainSiteTab(val key: String, val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SitesScreen(
    locale: String,
    onNavigateToDetails: (String) -> Unit = {},
    onPlayMovie: (Movie) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeCategory by remember { mutableStateOf("movies") } // "movies", "subtitles", "download_sites"
    var selectedSiteFilter by remember { mutableStateOf("all") }
    var movies by remember { mutableStateOf<List<SiteMovie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var subtitleSearchQuery by remember { mutableStateOf("") }

    val cyanAccent = Color(0xFF00B0FF)
    val cyanLight = Color(0xFF4FC3F7)
    val bgDark = Color(0xFF0A0E17)

    val mainTabs = listOf(
        MainSiteTab("movies", "أفلام وسيرفرات", Icons.Default.Movie),
        MainSiteTab("subtitles", "مواقع الترجمة", Icons.Default.Subtitles),
        MainSiteTab("download_sites", "مواقع التحميل", Icons.Default.Download)
    )

    fun loadMovies(site: String) {
        scope.launch {
            isLoading = true
            selectedSiteFilter = site
            try {
                movies = SitesManager.getMoviesFromSite(site)
                // Cache them in AppPreferences so details screen finds them
                movies.forEach { siteMovie ->
                    AppPreferences.saveMovieInfo(siteMovie.toMovie())
                }
            } catch (_: Exception) {
                movies = emptyList()
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadMovies("all")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "مواقع الأفلام والترجمات والتحميل",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                },
                actions = {
                    IconButton(
                        onClick = { loadMovies(selectedSiteFilter) },
                        modifier = Modifier.testTag("sites_refresh_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = cyanAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgDark)
            )
        },
        containerColor = bgDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Primary Category Tabs (Movies vs Subtitles vs Download Portals)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mainTabs.forEach { tab ->
                    val isSelected = activeCategory == tab.key
                    Surface(
                        color = if (isSelected) cyanAccent else Color(0xFF141D2B),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { activeCategory = tab.key }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.Black else Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.label,
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            when (activeCategory) {
                // ================= TAB 1: MOVIES CATALOG =================
                "movies" -> {
                    // Site Filter Chips
                    val siteFilters = listOf(
                        "all" to "الكل",
                        "archive" to "Archive (مباشر)",
                        "plex" to "Plex VOD",
                        "roku" to "Roku HD",
                        "crackle" to "Crackle"
                    )

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(siteFilters) { (key, label) ->
                            val isSelected = selectedSiteFilter == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { loadMovies(key) },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = cyanAccent,
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF16202E),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }

                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = cyanAccent)
                        }
                    } else if (movies.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد أفلام حالياً، اضغط على زر التحديث",
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        val movieRows = remember(movies) { movies.map { it.toMovie() }.chunked(3) }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 80.dp, top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(movieRows) { rowMovies ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (m in rowMovies) {
                                        MovieCard(
                                            movie = m,
                                            onClick = {
                                                AppPreferences.saveMovieInfo(m)
                                                onNavigateToDetails(m.id)
                                            },
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

                // ================= TAB 2: SUBTITLE DOWNLOAD SITES =================
                "subtitles" -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            // Subtitle Direct Search Box
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF141D2B))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "🔍 بحث سريع عن ترجمة أي فيلم",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = subtitleSearchQuery,
                                        onValueChange = { subtitleSearchQuery = it },
                                        placeholder = { Text("اكتب اسم الفيلم بالإنجليزية أو العربية...", fontSize = 13.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = cyanAccent,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val q = subtitleSearchQuery.ifEmpty { "Captain America" }
                                                SubtitlesManager.openSubtitleSearch(context, q, SubtitlesManager.subtitleSources[0])
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = cyanAccent),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("بحث في Subdl", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Button(
                                            onClick = {
                                                val q = subtitleSearchQuery.ifEmpty { "Captain America" }
                                                SubtitlesManager.openSubtitleSearch(context, q, SubtitlesManager.subtitleSources[1])
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D2C42)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("OpenSubtitles", color = cyanLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "مواقع ترجمة الأفلام المعتمدة:",
                                color = cyanLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )
                        }

                        items(SubtitlesManager.subtitleSources) { source ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val q = subtitleSearchQuery.ifEmpty { "Inception" }
                                        SubtitlesManager.openSubtitleSearch(context, q, source)
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF141D2B))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(source.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = cyanAccent.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = source.language,
                                                    color = cyanAccent,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = source.description,
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = source.siteName,
                                            color = cyanLight,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    FilledIconButton(
                                        onClick = {
                                            val q = subtitleSearchQuery.ifEmpty { "Inception" }
                                            SubtitlesManager.openSubtitleSearch(context, q, source)
                                        },
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = cyanAccent)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.Black)
                                    }
                                }
                            }
                        }
                    }
                }

                // ================= TAB 3: MOVIE DOWNLOAD SITES =================
                "download_sites" -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "مواقع وسيرفرات تحميل الأفلام المجانية:",
                                color = cyanLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        items(SubtitlesManager.movieDownloadSources) { source ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        SubtitlesManager.openMovieDownloadSearch(context, "Captain America", source)
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF141D2B))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(source.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = Color(0xFF00E676).copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = source.type,
                                                    color = Color(0xFF00E676),
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = source.description,
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "الجودة المتوفرة: ${source.quality}",
                                            color = cyanLight,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    FilledIconButton(
                                        onClick = {
                                            SubtitlesManager.openMovieDownloadSearch(context, "Captain America", source)
                                        },
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = cyanAccent)
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = "Open", tint = Color.Black)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
