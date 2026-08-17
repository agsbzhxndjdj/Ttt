package com.example.telecinema.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.telecinema.util.Lang

data class FilterCriteria(
    val selectedGenre: String = "",
    val selectedQuality: String = "",
    val selectedChannel: String = "",
    val sortBy: String = "newest" // newest, oldest, title, year
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterDialog(
    currentFilter: FilterCriteria,
    availableGenres: List<String>,
    availableQualities: List<String>,
    availableChannels: List<String>,
    locale: String,
    onDismiss: () -> Unit,
    onApply: (FilterCriteria) -> Unit
) {
    var genre by remember { mutableStateOf(currentFilter.selectedGenre) }
    var quality by remember { mutableStateOf(currentFilter.selectedQuality) }
    var channel by remember { mutableStateOf(currentFilter.selectedChannel) }
    var sortBy by remember { mutableStateOf(currentFilter.sortBy) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .testTag("filter_dialog")
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = Lang.t("filter", locale),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Sort Options
                Text(
                    text = Lang.t("sort", locale),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "newest" to Lang.t("sortNewest", locale),
                        "oldest" to Lang.t("sortOldest", locale),
                        "title" to Lang.t("sortTitle", locale),
                        "year" to Lang.t("sortYear", locale)
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = sortBy == key,
                            onClick = { sortBy = key },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quality Options
                if (availableQualities.isNotEmpty()) {
                    Text(
                        text = Lang.t("quality", locale),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = quality.isEmpty(),
                            onClick = { quality = "" },
                            label = { Text(Lang.t("all", locale)) }
                        )
                        availableQualities.forEach { q ->
                            FilterChip(
                                selected = quality == q,
                                onClick = { quality = if (quality == q) "" else q },
                                label = { Text(q) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Genre Options
                if (availableGenres.isNotEmpty()) {
                    Text(
                        text = "الأنواع / Genres",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = genre.isEmpty(),
                            onClick = { genre = "" },
                            label = { Text(Lang.t("all", locale)) }
                        )
                        availableGenres.forEach { g ->
                            FilterChip(
                                selected = genre == g,
                                onClick = { genre = if (genre == g) "" else g },
                                label = { Text(g) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Channel Options
                if (availableChannels.isNotEmpty()) {
                    Text(
                        text = Lang.t("channels", locale),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = channel.isEmpty(),
                            onClick = { channel = "" },
                            label = { Text(Lang.t("all", locale)) }
                        )
                        availableChannels.forEach { ch ->
                            FilterChip(
                                selected = channel == ch,
                                onClick = { channel = if (channel == ch) "" else ch },
                                label = { Text("@$ch") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(Lang.t("cancel", locale))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onApply(
                                FilterCriteria(
                                    selectedGenre = genre,
                                    selectedQuality = quality,
                                    selectedChannel = channel,
                                    sortBy = sortBy
                                )
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(Lang.t("filter", locale), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
