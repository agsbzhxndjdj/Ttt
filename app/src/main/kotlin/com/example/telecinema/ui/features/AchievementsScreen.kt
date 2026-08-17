package com.example.telecinema.ui.features

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telecinema.data.local.AppPreferences
import com.example.telecinema.util.Lang

data class BadgeItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isUnlocked: Boolean,
    val xpReward: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    locale: String,
    onBack: () -> Unit
) {
    val stats = remember { AppPreferences.getUserStats() }
    val favCount = remember { AppPreferences.getFavoriteIds().size }
    val channelCount = remember { AppPreferences.getChannels().size }
    val watchedCount = remember { AppPreferences.getHistoryMovies().size }

    val badges = listOf(
        BadgeItem(
            id = "first_movie",
            title = "أول رحلة سينمائية",
            description = "شاهد أول فيلم في تلي سينما",
            icon = Icons.Default.PlayCircle,
            isUnlocked = watchedCount >= 1,
            xpReward = 50
        ),
        BadgeItem(
            id = "movie_collector",
            title = "جامع التحف",
            description = "أضف 5 أفلام إلى المفضلة",
            icon = Icons.Default.Favorite,
            isUnlocked = favCount >= 5,
            xpReward = 100
        ),
        BadgeItem(
            id = "channel_master",
            title = "سيد القنوات",
            description = "أضف 3 قنوات تليجرام مختلفة",
            icon = Icons.Default.LiveTv,
            isUnlocked = channelCount >= 3,
            xpReward = 150
        ),
        BadgeItem(
            id = "movie_marathon",
            title = "ماراثون سينمائي",
            description = "شاهد أكثر من 10 ساعات محتوى",
            icon = Icons.Default.Timer,
            isUnlocked = (stats.secondsWatched / 3600) >= 10,
            xpReward = 200
        ),
        BadgeItem(
            id = "streak_legend",
            title = "أسطورة الالتزام",
            description = "حافظ على ستريك مشاهدة لمدة 3 أيام متتالية",
            icon = Icons.Default.LocalFireDepartment,
            isUnlocked = stats.currentStreak >= 3,
            xpReward = 250
        )
    )

    val totalXp = badges.filter { it.isUnlocked }.sumOf { it.xpReward } + (watchedCount * 10)
    val level = (totalXp / 100) + 1
    val progressInLevel = (totalXp % 100) / 100f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = Lang.t("achievements", locale),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Level and XP Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "المستوى $level (سينيفيلي ${if (level > 3) "محترف" else "مبتدئ"})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "$totalXp XP",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { progressInLevel },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Badges List
            item {
                Text(
                    text = "الأوسمة والإنجازات 🏅",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(badges) { badge ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (badge.isUnlocked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    if (badge.isUnlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = badge.icon,
                                contentDescription = null,
                                tint = if (badge.isUnlocked) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = badge.title,
                                fontWeight = FontWeight.Bold,
                                color = if (badge.isUnlocked) Color.White else Color.Gray,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = badge.description,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }

                        Surface(
                            color = if (badge.isUnlocked) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "+${badge.xpReward} XP",
                                color = if (badge.isUnlocked) Color.Black else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
