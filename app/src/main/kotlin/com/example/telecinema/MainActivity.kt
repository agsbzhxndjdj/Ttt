package com.example.telecinema

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.telecinema.data.local.AppPreferences
import com.example.telecinema.model.Movie
import com.example.telecinema.theme.TeleCinemaTheme
import com.example.telecinema.ui.channels.ChannelsScreen
import com.example.telecinema.ui.details.MovieDetailsScreen
import com.example.telecinema.ui.downloads.DownloadsScreen
import com.example.telecinema.ui.favorites.FavoritesScreen
import com.example.telecinema.ui.features.AchievementsScreen
import com.example.telecinema.ui.features.DiscoverScreen
import com.example.telecinema.ui.features.ShotGameScreen
import com.example.telecinema.ui.features.StatsScreen
import com.example.telecinema.ui.history.HistoryScreen
import com.example.telecinema.ui.home.HomeScreen
import com.example.telecinema.ui.navigation.Screen
import com.example.telecinema.ui.player.VideoPlayerScreen
import com.example.telecinema.ui.settings.SettingsScreen
import com.example.telecinema.ui.sites.SitesScreen
import com.example.telecinema.util.Lang

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val currentTheme by AppPreferences.themeFlow.collectAsState()
            val currentLocale by AppPreferences.localeFlow.collectAsState()

            TeleCinemaTheme(themeKey = currentTheme) {
                MainAppNavHost(locale = currentLocale)
            }
        }
    }
}

@Composable
fun MainAppNavHost(locale: String) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Active playing movie state (for player)
    var activePlayerMovie by remember { mutableStateOf<Movie?>(null) }

    val bottomNavItems = listOf(
        Triple(Screen.Home, Lang.t("home", locale), Icons.Default.Home),
        Triple(Screen.Sites, Lang.t("sites", locale), Icons.Default.Language),
        Triple(Screen.Discover, Lang.t("discover", locale), Icons.Default.Explore),
        Triple(Screen.Favorites, Lang.t("favorites", locale), Icons.Default.Favorite),
        Triple(Screen.Downloads, Lang.t("downloads", locale), Icons.Default.Download),
        Triple(Screen.Settings, Lang.t("settings", locale), Icons.Default.Settings)
    )

    val isTopLevelDestination = bottomNavItems.any { (screen, _, _) ->
        currentDestination?.route?.endsWith(screen::class.simpleName ?: "") == true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (isTopLevelDestination) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { (screen, label, icon) ->
                        val isSelected = currentDestination?.route?.endsWith(screen::class.simpleName ?: "") == true
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                unselectedTextColor = Color.White.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.testTag("nav_item_${screen::class.simpleName}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<Screen.Home> {
                HomeScreen(
                    locale = locale,
                    onNavigateToDetails = { movieId -> navController.navigate(Screen.Details(movieId)) },
                    onNavigateToPlayer = { movie ->
                        activePlayerMovie = movie
                        navController.navigate(Screen.Player(movie.id, movie.videoUrl, movie.title))
                    },
                    onNavigateToChannels = { navController.navigate(Screen.Channels) },
                    onNavigateToDiscover = { navController.navigate(Screen.Discover) }
                )
            }

            composable<Screen.Sites> {
                SitesScreen(
                    locale = locale,
                    onNavigateToDetails = { movieId -> navController.navigate(Screen.Details(movieId)) },
                    onPlayMovie = { movie ->
                        activePlayerMovie = movie
                        navController.navigate(Screen.Player(movie.id, movie.videoUrl, movie.title))
                    }
                )
            }

            composable<Screen.Discover> {
                DiscoverScreen(
                    locale = locale,
                    onNavigateToDetails = { movieId -> navController.navigate(Screen.Details(movieId)) },
                    onNavigateToShotGame = { navController.navigate(Screen.ShotGame) },
                    onNavigateToAchievements = { navController.navigate(Screen.Achievements) },
                    onPlayMovie = { movie ->
                        activePlayerMovie = movie
                        navController.navigate(Screen.Player(movie.id, movie.videoUrl, movie.title))
                    }
                )
            }

            composable<Screen.Favorites> {
                FavoritesScreen(
                    locale = locale,
                    onNavigateToDetails = { movieId -> navController.navigate(Screen.Details(movieId)) }
                )
            }

            composable<Screen.Downloads> {
                DownloadsScreen(
                    locale = locale,
                    onPlayOffline = { movie ->
                        activePlayerMovie = movie
                        navController.navigate(Screen.Player(movie.id, movie.videoUrl, movie.title))
                    }
                )
            }

            composable<Screen.Channels> {
                ChannelsScreen(locale = locale)
            }

            composable<Screen.Settings> {
                SettingsScreen(
                    locale = locale,
                    onNavigateToStats = { navController.navigate(Screen.Stats) }
                )
            }

            composable<Screen.Achievements> {
                AchievementsScreen(
                    locale = locale,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Screen.ShotGame> {
                ShotGameScreen(
                    locale = locale,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Screen.Stats> {
                StatsScreen(
                    locale = locale,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Screen.Details> { backStackEntry ->
                val details = backStackEntry.toRoute<Screen.Details>()
                MovieDetailsScreen(
                    movieId = details.movieId,
                    locale = locale,
                    onBack = { navController.popBackStack() },
                    onPlayMovie = { movie ->
                        activePlayerMovie = movie
                        navController.navigate(Screen.Player(movie.id, movie.videoUrl, movie.title))
                    },
                    onNavigateToDetails = { nextMovieId ->
                        navController.navigate(Screen.Details(nextMovieId))
                    }
                )
            }

            composable<Screen.Player> { backStackEntry ->
                val playerRoute = backStackEntry.toRoute<Screen.Player>()
                val movie = activePlayerMovie ?: AppPreferences.getMovieInfo(playerRoute.movieId) ?: Movie(
                    id = playerRoute.movieId,
                    title = playerRoute.title,
                    videoUrl = playerRoute.videoUrl
                )
                VideoPlayerScreen(
                    movie = movie,
                    locale = locale,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
