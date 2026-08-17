package com.example.telecinema.ui.features

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telecinema.data.local.AppPreferences
import com.example.telecinema.model.Movie
import com.example.telecinema.ui.components.MovieCard
import com.example.telecinema.ui.components.WheelDialog
import com.example.telecinema.util.Lang
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    locale: String,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToShotGame: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onPlayMovie: (Movie) -> Unit
) {
    val dataVersion by AppPreferences.dataVersion.collectAsState()
    val allMovies = remember(dataVersion) { AppPreferences.getAllMovies() }

    var selectedDurationMinutes by remember { mutableStateOf<Int?>(null) }
    var showWheelDialog by remember { mutableStateOf(false) }

    val filteredByBudget = remember(allMovies, selectedDurationMinutes) {
        if (selectedDurationMinutes == null) emptyList()
        else {
            val maxSec = selectedDurationMinutes!! * 60
            allMovies.filter { it.durationSeconds in 1..maxSec }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = Lang.t("discover", locale),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Feature Quick Tiles (Shot Game, Achievements, Wheel)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Shot game
                    Card(
                        modifier = Modifier
                            .testTag("shot_game_tile")
                            .weight(1f)
                            .height(110.dp)
                            .clickable { onNavigateToShotGame() },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE5B13D).copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(Icons.Default.MovieFilter, contentDescription = null, tint = Color(0xFFE5B13D), modifier = Modifier.size(28.dp))
                            Text(Lang.t("shotGame", locale), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }
                    }

                    // Achievements
                    Card(
                        modifier = Modifier
                            .testTag("achievements_tile")
                            .weight(1f)
                            .height(110.dp)
                            .clickable { onNavigateToAchievements() },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF9C57FF).copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFF9C57FF), modifier = Modifier.size(28.dp))
                            Text(Lang.t("achievements", locale), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }
                    }

                    // Wheel
                    Card(
                        modifier = Modifier
                            .testTag("wheel_tile")
                            .weight(1f)
                            .height(110.dp)
                            .clickable { showWheelDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0088CC).copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(Icons.Default.Casino, contentDescription = null, tint = Color(0xFF0088CC), modifier = Modifier.size(28.dp))
                            Text("عجلة الحظ", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }
                    }
                }
            }

            // Time Budget Mode ("وقتي محدود ⏱️")
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Lang.t("limitedTime", locale),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Lang.t("pickTime", locale),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(45, 90, 120).forEach { mins ->
                                val isSelected = selectedDurationMinutes == mins
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedDurationMinutes = if (isSelected) null else mins
                                    },
                                    label = { Text("أقل من $mins د") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (selectedDurationMinutes != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            if (filteredByBudget.isEmpty()) {
                                Text("لا توجد أفلام بهذه المدة", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(filteredByBudget) { m ->
                                        MovieCard(
                                            movie = m,
                                            onClick = { onNavigateToDetails(m.id) },
                                            modifier = Modifier.width(115.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Cinema Eras Section
            item {
                Text(
                    text = "حقبات سينمائية 🎞️",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                val eras = listOf(
                    "2020+" to allMovies.filter { it.year >= 2020 },
                    "2010 - 2019" to allMovies.filter { it.year in 2010..2019 },
                    "2000 - 2009" to allMovies.filter { it.year in 2000..2009 },
                    "كلاسيكيات (قبل 2000)" to allMovies.filter { it.year in 1..1999 }
                )

                eras.forEach { (eraTitle, moviesInEra) ->
                    if (moviesInEra.isNotEmpty()) {
                        Column(modifier = Modifier.padding(bottom = 12.dp)) {
                            Text(
                                text = "$eraTitle (${moviesInEra.size})",
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(moviesInEra) { movie ->
                                    MovieCard(
                                        movie = movie,
                                        onClick = { onNavigateToDetails(movie.id) },
                                        modifier = Modifier.width(110.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWheelDialog) {
        WheelDialog(
            movies = allMovies,
            locale = locale,
            onDismiss = { showWheelDialog = false },
            onMovieSelected = { onPlayMovie(it) }
        )
    }
}
