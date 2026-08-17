package com.example.telecinema.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Home : Screen

    @Serializable
    data object Sites : Screen

    @Serializable
    data object Favorites : Screen

    @Serializable
    data object History : Screen

    @Serializable
    data object Downloads : Screen

    @Serializable
    data object Channels : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data object Discover : Screen

    @Serializable
    data object Achievements : Screen

    @Serializable
    data object ShotGame : Screen

    @Serializable
    data object Stats : Screen

    @Serializable
    data class Details(val movieId: String) : Screen

    @Serializable
    data class Player(val movieId: String, val videoUrl: String = "", val title: String = "") : Screen
}
