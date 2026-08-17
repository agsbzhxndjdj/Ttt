package com.example.telecinema.ui.channels

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.telecinema.data.api.TgApiService
import com.example.telecinema.data.local.AppPreferences
import com.example.telecinema.model.Channel
import com.example.telecinema.util.Lang
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(
    locale: String
) {
    val scope = rememberCoroutineScope()
    val dataVersion by AppPreferences.dataVersion.collectAsState()
    val channels = remember(dataVersion) { AppPreferences.getChannels() }

    var showAddDialog by remember { mutableStateOf(false) }
    var newChannelInput by remember { mutableStateOf("") }
    var isAdding by remember { mutableStateOf(false) }
    var addErrorMsg by remember { mutableStateOf("") }

    var channelToDelete by remember { mutableStateOf<Channel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = Lang.t("channels", locale),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    newChannelInput = ""
                    addErrorMsg = ""
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black,
                modifier = Modifier.testTag("add_channel_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Channel")
            }
        }
    ) { innerPadding ->
        if (channels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = Lang.t("noChannels", locale),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(channels) { channel ->
                    Card(
                        modifier = Modifier
                            .testTag("channel_item_${channel.username}")
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (channel.avatar.isNotEmpty()) {
                                    AsyncImage(
                                        model = channel.avatar,
                                        contentDescription = channel.title,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = channel.title.ifEmpty { "@${channel.username}" },
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "@${channel.username} • ${channel.movieCount} فيلم",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }

                            IconButton(
                                onClick = { channelToDelete = channel },
                                modifier = Modifier.testTag("delete_channel_${channel.username}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Channel Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { if (!isAdding) showAddDialog = false },
            title = { Text(Lang.t("addChannel", locale)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newChannelInput,
                        onValueChange = { newChannelInput = it },
                        placeholder = { Text(Lang.t("addChannelHint", locale)) },
                        singleLine = true,
                        modifier = Modifier
                            .testTag("channel_input_field")
                            .fillMaxWidth()
                    )
                    if (addErrorMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = addErrorMsg, color = Color.Red, fontSize = 12.sp)
                    }
                    if (isAdding) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("جاري الفحص وجلب الأفلام...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = TgApiService.cleanUsername(newChannelInput)
                        if (clean.isEmpty()) {
                            addErrorMsg = "الرجاء إدخال اسم القناة"
                            return@Button
                        }
                        isAdding = true
                        addErrorMsg = ""
                        scope.launch {
                            try {
                                val result = TgApiService.fetchChannelPage(clean)
                                if (result.movies.isNotEmpty()) {
                                    val newChannel = Channel(
                                        username = clean,
                                        title = result.title,
                                        avatar = result.avatar,
                                        movieCount = result.movies.size
                                    )
                                    AppPreferences.addChannel(newChannel)
                                    AppPreferences.saveMoviesForChannel(clean, result.movies)
                                    showAddDialog = false
                                } else {
                                    // Fallback: save channel anyway so it will retry
                                    val newChannel = Channel(
                                        username = clean,
                                        title = clean,
                                        avatar = "",
                                        movieCount = 0
                                    )
                                    AppPreferences.addChannel(newChannel)
                                    showAddDialog = false
                                }
                            } catch (e: Exception) {
                                addErrorMsg = Lang.t("channelNotFound", locale)
                            }
                            isAdding = false
                        }
                    },
                    enabled = !isAdding,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("confirm_add_channel_button")
                ) {
                    Text(Lang.t("addChannel", locale), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    enabled = !isAdding
                ) {
                    Text(Lang.t("cancel", locale))
                }
            }
        )
    }

    // Delete confirmation dialog
    if (channelToDelete != null) {
        AlertDialog(
            onDismissRequest = { channelToDelete = null },
            title = { Text(Lang.t("deleteChannel", locale)) },
            text = { Text(Lang.t("deleteConfirm", locale)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        AppPreferences.removeChannel(channelToDelete!!.username)
                        channelToDelete = null
                    }
                ) {
                    Text(Lang.t("delete", locale), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { channelToDelete = null }) {
                    Text(Lang.t("cancel", locale))
                }
            }
        )
    }
}
