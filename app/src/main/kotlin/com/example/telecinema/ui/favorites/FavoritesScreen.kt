package com.example.telecinema.ui.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telecinema.data.local.AppPreferences
import com.example.telecinema.model.Movie
import com.example.telecinema.ui.components.MovieRowItem
import com.example.telecinema.util.Lang

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    locale: String,
    onNavigateToDetails: (String) -> Unit
) {
    val dataVersion by AppPreferences.dataVersion.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Favorites, 1: Watch Later

    val favMovies = remember(dataVersion) {
        val favIds = AppPreferences.getFavoriteIds()
        favIds.mapNotNull { AppPreferences.getMovieInfo(it) }
    }

    val watchLaterMovies = remember(dataVersion) {
        val wlIds = AppPreferences.getWatchLaterIds()
        wlIds.mapNotNull { AppPreferences.getMovieInfo(it) }
    }

    val displayList = if (selectedTab == 0) favMovies else watchLaterMovies

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedTab == 0) Lang.t("favorites", locale) else "مشاهدة لاحقاً",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("المفضلة (${favMovies.size})") },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("مشاهدة لاحقاً (${watchLaterMovies.size})") },
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (displayList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.Favorite else Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedTab == 0) Lang.t("noFavorites", locale) else "لا توجد أفلام في قائمة المشاهدة لاحقاً",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayList) { movie ->
                        MovieRowItem(
                            movie = movie,
                            onClick = { onNavigateToDetails(movie.id) }
                        )
                    }
                }
            }
        }
    }
}
