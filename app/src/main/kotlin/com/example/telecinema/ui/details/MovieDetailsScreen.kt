package com.example.telecinema.ui.details

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.telecinema.data.api.SubtitlesManager
import com.example.telecinema.data.download.AppDownloadManager
import com.example.telecinema.data.local.AppPreferences
import com.example.telecinema.model.Movie
import com.example.telecinema.model.VideoQuality
import com.example.telecinema.ui.components.CommentsSheet
import com.example.telecinema.ui.components.MovieCard
import com.example.telecinema.util.Lang

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailsScreen(
    movieId: String,
    locale: String,
    onBack: () -> Unit,
    onPlayMovie: (Movie) -> Unit,
    onNavigateToDetails: (String) -> Unit
) {
    val context = LocalContext.current
    val dataVersion by AppPreferences.dataVersion.collectAsState()
    val movie = remember(movieId, dataVersion) { AppPreferences.getMovieInfo(movieId) }

    var showCommentsSheet by remember { mutableStateOf(false) }
    var showQualitySelector by remember { mutableStateOf(false) }
    var showSubtitlesSheet by remember { mutableStateOf(false) }
    var showDownloadSitesSheet by remember { mutableStateOf(false) }
    var currentRating by remember(movieId, dataVersion) { mutableStateOf(AppPreferences.getRating(movieId)) }

    val allMovies = remember(dataVersion) { AppPreferences.getAllMovies() }
    val similarMovies = remember(movie, allMovies) {
        if (movie == null) emptyList()
        else allMovies.filter { it.id != movie.id && (it.channel == movie.channel || it.genres.any { g -> movie.genres.contains(g) }) }.take(10)
    }

    if (movie == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0E17)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("الفيلم غير متوفر أو جارٍ التحميل...", color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF))
                ) {
                    Text("رجوع", color = Color.Black)
                }
            }
        }
        return
    }

    val isFav = AppPreferences.isFavorite(movie.id)
    val isWl = AppPreferences.isWatchLater(movie.id)
    val isDownloaded = AppDownloadManager.isDownloaded(movie.id)

    // Primary Cyan Accent Color from Screenshot
    val cyanAccent = Color(0xFF00B0FF)
    val cyanLight = Color(0xFF4FC3F7)
    val darkPillBg = Color(0xFF141C2B).copy(alpha = 0.9f)
    val bgDark = Color(0xFF090D16)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgDark)
    ) {
        // 1. Full-bleed backdrop poster with smooth dark gradients
        if (movie.poster.isNotEmpty()) {
            AsyncImage(
                model = movie.poster,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            )
        }

        // Layered Gradient Overlay matching screenshot
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.45f),
                        0.25f to Color.Black.copy(alpha = 0.15f),
                        0.55f to Color.Black.copy(alpha = 0.65f),
                        0.75f to bgDark.copy(alpha = 0.92f),
                        0.88f to bgDark,
                        1f to bgDark
                    )
                )
        )

        // 2. Scrollable Content Layer
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Floating Top App Bar (Back, Truncated Title, Trailer, Bookmark, Favorite)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("details_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Text(
                    text = movie.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp, end = 8.dp)
                )

                // YouTube Trailer Button (Red Play Icon / Button)
                IconButton(
                    onClick = { SubtitlesManager.openTrailer(context, movie.title) },
                    modifier = Modifier.testTag("details_trailer_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE50914)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Trailer",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Bookmark / Watch Later Button
                IconButton(
                    onClick = { AppPreferences.toggleWatchLater(movie) },
                    modifier = Modifier.testTag("details_watchlater_button")
                ) {
                    Icon(
                        imageVector = if (isWl) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Watch Later",
                        tint = if (isWl) cyanAccent else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Heart / Favorite Button
                IconButton(
                    onClick = { AppPreferences.toggleFavorite(movie) },
                    modifier = Modifier.testTag("details_favorite_button")
                ) {
                    Icon(
                        imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFav) Color(0xFFFF2A55) else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Scrollable Content Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // Generous space so poster is prominent in upper area
                Spacer(modifier = Modifier.height(280.dp))

                // Title (Big, Bold, White)
                Text(
                    text = if (movie.year > 0 && !movie.title.contains("${movie.year}")) "${movie.title} ${movie.year}" else movie.title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Star Rating Row + Queue / Playlist Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 5 Cyan Stars
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..5).forEach { star ->
                            Icon(
                                imageVector = if (currentRating >= star) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Star $star",
                                tint = cyanAccent,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clickable {
                                        currentRating = star
                                        AppPreferences.setRating(movie.id, star)
                                    }
                            )
                        }
                    }

                    // Playlist / Queue Icon
                    IconButton(
                        onClick = { showCommentsSheet = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = "Comments & List",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Badges Row (Rating, Year, Other Quality, Duration, Size)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rating Capsule (Yellow star + Blue score)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(darkPillBg)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "7.009",
                            color = cyanLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Year Capsule
                    val displayYear = if (movie.year > 0) "${movie.year}" else "2011"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(darkPillBg)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = displayYear,
                            color = cyanLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // "جودة أخرى" Capsule (Clickable for Alternate Qualities)
                    Surface(
                        onClick = { showQualitySelector = true },
                        shape = RoundedCornerShape(16.dp),
                        color = darkPillBg
                    ) {
                        Text(
                            text = "جودة أخرى",
                            color = cyanLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    // Duration Capsule
                    val displayDuration = movie.duration.ifEmpty { "2:04:11" }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(darkPillBg)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = displayDuration,
                            color = cyanLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Size Capsule
                    val displaySize = movie.size.ifEmpty { "2.0 GB" }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(darkPillBg)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = displaySize,
                            color = cyanLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Genre Chips Row (Action, Sci-Fi, etc.)
                val genres = movie.genres.ifEmpty { listOf("أكشن", "خيال علمي") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    genres.forEach { genre ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF0F1B2C))
                                .padding(horizontal = 14.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = genre,
                                color = cyanAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quality Line Text
                val qualityLabel = movie.quality.ifEmpty { "1080P" }
                Text(
                    text = "الجودة $qualityLabel",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Arabic Synopsis / Description Text
                val defaultDesc = "تدور الأحداث خلال الحرب العالمية الثانية حول ستيف روجرز وهو شاب نحيل البنية ومرفوض من الخدمة العسكرية لضعفه الجسدي، يقرر روجرز التطوع في مشروع بحثي حكومي سري يُعرف باسم مشروع إعادة الولادة حيث يتم حقنه بمصل خاص يحوله إلى جندي خارق لقتال المنظمات الشريرة والدفاع عن العالم."
                val descText = movie.description.ifEmpty { defaultDesc }
                Text(
                    text = descText,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 14.sp,
                    lineHeight = 23.sp,
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Subtitle & External Download Sites Quick Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { showSubtitlesSheet = true },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF162030),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Subtitles, contentDescription = null, tint = cyanLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تحميل الترجمة", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Surface(
                        onClick = { showDownloadSitesSheet = true },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF162030),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = cyanLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مواقع التحميل", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Similar Movies / Other Channel Movies
                if (similarMovies.isNotEmpty()) {
                    Text(
                        text = "أفلام مقترحة ومميزة",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(similarMovies) { simMovie ->
                            MovieCard(
                                movie = simMovie,
                                onClick = { onNavigateToDetails(simMovie.id) },
                                modifier = Modifier.width(115.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(90.dp))
            }

            // 3. Fixed Bottom Action Buttons Bar (Solid Play Button + Outlined Download Button)
            Surface(
                color = bgDark.copy(alpha = 0.95f),
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Solid Cyan Play Button (تشغيل)
                    Button(
                        onClick = { onPlayMovie(movie) },
                        colors = ButtonDefaults.buttonColors(containerColor = cyanAccent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .testTag("details_play_button")
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تشغيل",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    // Outlined Cyan Download Button (تحميل)
                    OutlinedButton(
                        onClick = { AppDownloadManager.startDownload(movie) },
                        border = BorderStroke(1.5.dp, cyanAccent),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0x1400B0FF),
                            contentColor = cyanAccent
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .testTag("details_download_button")
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = if (isDownloaded) Icons.Filled.CheckCircle else Icons.Filled.Download,
                            contentDescription = null,
                            tint = if (isDownloaded) Color(0xFF4CAF50) else cyanAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDownloaded) "تم التحميل" else "تحميل",
                            color = if (isDownloaded) Color(0xFF4CAF50) else cyanAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    // Modal Sheet: Quality Selector ("جودة أخرى")
    if (showQualitySelector) {
        ModalBottomSheet(
            onDismissRequest = { showQualitySelector = false },
            containerColor = Color(0xFF101622)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "اختر الجودة أو السيرفر",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = cyanLight
                )
                Spacer(modifier = Modifier.height(14.dp))

                val qualities = if (movie.qualities.isNotEmpty()) {
                    movie.qualities
                } else {
                    listOf(
                        VideoQuality(label = "1080p Full HD (الأصلي)", url = movie.videoUrl),
                        VideoQuality(label = "720p HD (سريع)", url = movie.videoUrl),
                        VideoQuality(label = "480p SD (توفير البيانات)", url = movie.videoUrl)
                    )
                }

                qualities.forEach { q ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                onPlayMovie(movie.copy(videoUrl = q.url, quality = q.label))
                                showQualitySelector = false
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF182335))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Hd, contentDescription = null, tint = cyanAccent)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(q.label, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = cyanAccent)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Modal Sheet: Subtitle Download Websites
    if (showSubtitlesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSubtitlesSheet = false },
            containerColor = Color(0xFF101622)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Subtitles, contentDescription = null, tint = cyanAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تحميل ملفات الترجمة (عربي / English)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "اضغط على أي موقع للبحث التلقائي عن ترجمة ${movie.title} وتحميلها مباشرة:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                SubtitlesManager.subtitleSources.forEach { source ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                SubtitlesManager.openSubtitleSearch(context, movie.title, source)
                                showSubtitlesSheet = false
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF182335))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(source.name, color = cyanLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(source.description, color = Color.White.copy(0.7f), fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Download, contentDescription = null, tint = cyanAccent)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal Sheet: Movie Download Websites
    if (showDownloadSitesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDownloadSitesSheet = false },
            containerColor = Color(0xFF101622)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = cyanAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "مواقع وسيرفرات تحميل الفيلم",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "سيرفرات وروابط سريعة لتحميل فيلم ${movie.title} بجودات متعددة:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                SubtitlesManager.movieDownloadSources.forEach { source ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                SubtitlesManager.openMovieDownloadSearch(context, movie.title, source)
                                showDownloadSitesSheet = false
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF182335))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(source.name, color = cyanLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(shape = RoundedCornerShape(4.dp), color = cyanAccent.copy(alpha = 0.2f)) {
                                        Text(source.quality, color = cyanAccent, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(source.description, color = Color.White.copy(0.7f), fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = cyanAccent)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal Sheet: Comments & User Notes
    if (showCommentsSheet) {
        CommentsSheet(
            movieId = movie.id,
            locale = locale,
            onDismiss = { showCommentsSheet = false }
        )
    }
}
