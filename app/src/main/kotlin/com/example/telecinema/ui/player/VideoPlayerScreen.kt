package com.example.telecinema.ui.player

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.telecinema.data.api.SubtitlesManager
import com.example.telecinema.data.local.AppPreferences
import com.example.telecinema.model.Movie
import com.example.telecinema.model.VideoQuality
import com.example.telecinema.util.Lang
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    movie: Movie,
    locale: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var currentUrl by remember { mutableStateOf(movie.videoUrl) }
    var currentQuality by remember { mutableStateOf(movie.quality.ifEmpty { "1080p" }) }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isUserScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableLongStateOf(0L) }

    var showControls by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var isControlsLocked by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showQualitySelector by remember { mutableStateOf(false) }
    var showSubtitlesDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    val cyanAccent = Color(0xFF00B0FF)
    val cyanLight = Color(0xFF4FC3F7)

    // Keep screen awake while playing
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        AppPreferences.markWatched(movie)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Configure resilient ExoPlayer instance
    val exoPlayer = remember(context) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20000)
            .setReadTimeoutMs(25000)
            .setDefaultRequestProperties(
                mapOf(
                    "Accept" to "*/*",
                    "Accept-Encoding" to "identity"
                )
            )

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                playWhenReady = true
            }
    }

    // Set playback source
    LaunchedEffect(currentUrl) {
        if (currentUrl.isNotEmpty()) {
            isBuffering = true
            playbackError = null
            val mediaItem = MediaItem.fromUri(Uri.parse(currentUrl))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()

            val savedSec = AppPreferences.getPosition(movie.id)
            if (savedSec > 10) {
                exoPlayer.seekTo(savedSec * 1000L)
            }
            exoPlayer.play()
        }
    }

    // Sync playback speed
    LaunchedEffect(playbackSpeed) {
        exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
    }

    // Player state listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> isBuffering = true
                    Player.STATE_READY -> {
                        isBuffering = false
                        duration = exoPlayer.duration.coerceAtLeast(0L)
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                    }
                    Player.STATE_IDLE -> {}
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                isBuffering = false
                playbackError = "تعذر تشغيل الفيديو: ${error.message}"
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            val pos = exoPlayer.currentPosition / 1000
            val dur = exoPlayer.duration / 1000
            if (pos > 10 && (dur == 0L || pos < dur - 10)) {
                AppPreferences.savePosition(movie.id, pos.toInt())
            }
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Periodic time & position tracker & auto saver
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            if (exoPlayer.isPlaying && !isUserScrubbing) {
                currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
                duration = exoPlayer.duration.coerceAtLeast(0L)

                val posSec = (currentPosition / 1000).toInt()
                if (posSec % 5 == 0 && posSec > 10) {
                    AppPreferences.savePosition(movie.id, posSec)
                }
            }
            delay(400)
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(showControls, isPlaying, isUserScrubbing, showQualitySelector, showSubtitlesDialog, showSpeedDialog) {
        if (showControls && isPlaying && !isControlsLocked && !isUserScrubbing && !showQualitySelector && !showSubtitlesDialog && !showSpeedDialog) {
            delay(4500)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .testTag("video_player_container")
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ExoPlayer Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    this.resizeMode = resizeMode
                    setOnClickListener {
                        showControls = !showControls
                    }
                }
            },
            update = { playerView ->
                playerView.resizeMode = resizeMode
                playerView.setOnClickListener {
                    showControls = !showControls
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Transparent Full-Screen Gesture Layer to ensure tap detection works
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showControls = !showControls
                }
        )

        // Buffering Spinner
        if (isBuffering && playbackError == null) {
            CircularProgressIndicator(
                color = cyanAccent,
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.Center)
            )
        }

        // Error message card
        if (playbackError != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .background(Color.Black.copy(alpha = 0.88f), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFFF3B30),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = playbackError ?: "خطأ أثناء تشغيل الفيديو",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            playbackError = null
                            isBuffering = true
                            val mediaItem = MediaItem.fromUri(Uri.parse(currentUrl))
                            exoPlayer.setMediaItem(mediaItem)
                            exoPlayer.prepare()
                            exoPlayer.play()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = cyanAccent)
                    ) {
                        Text("إعادة المحاولة", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    if (movie.qualities.size > 1 || movie.alts.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showQualitySelector = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = cyanAccent)
                        ) {
                            Text("مصدر بديل")
                        }
                    }

                    OutlinedButton(
                        onClick = onBack,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("رجوع")
                    }
                }
            }
        }

        // Overlay Controls (Top bar, Center buttons, Bottom scrubber)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.52f))
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("player_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = movie.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (currentQuality.isNotEmpty()) {
                            Text(
                                text = "الجودة: $currentQuality",
                                color = cyanLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Subtitles Button (CC)
                    IconButton(
                        onClick = { showSubtitlesDialog = true },
                        modifier = Modifier.testTag("player_subtitles_button")
                    ) {
                        Icon(Icons.Default.Subtitles, contentDescription = "Subtitles", tint = Color.White)
                    }

                    // Playback Speed Button
                    IconButton(
                        onClick = { showSpeedDialog = true },
                        modifier = Modifier.testTag("player_speed_button")
                    ) {
                        Text(
                            text = "${playbackSpeed}x",
                            color = cyanAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Quality / Server Selector
                    if (movie.qualities.size > 1 || movie.alts.isNotEmpty()) {
                        IconButton(
                            onClick = { showQualitySelector = true },
                            modifier = Modifier.testTag("quality_selector_button")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Quality", tint = cyanAccent)
                        }
                    }

                    // Aspect Ratio Button
                    IconButton(
                        onClick = {
                            resizeMode = when (resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) Icons.Default.AspectRatio else Icons.Default.CropFree,
                            contentDescription = "Resize",
                            tint = Color.White
                        )
                    }

                    // Screen Lock Button
                    IconButton(
                        onClick = { isControlsLocked = !isControlsLocked }
                    ) {
                        Icon(
                            imageVector = if (isControlsLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock",
                            tint = if (isControlsLocked) cyanAccent else Color.White
                        )
                    }
                }

                // Center Play/Pause & Seek Controls (Hidden when locked)
                if (!isControlsLocked) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(0.85f),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rewind 10s
                        IconButton(
                            onClick = {
                                val target = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(target)
                                currentPosition = target
                            },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Rewind 10s",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        // Play / Pause Button
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(cyanAccent)
                                .clickable {
                                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.Black,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        // Forward 10s
                        IconButton(
                            onClick = {
                                val target = (exoPlayer.currentPosition + 10000L).coerceAtMost(exoPlayer.duration)
                                exoPlayer.seekTo(target)
                                currentPosition = target
                            },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Forward 10s",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }

                // Bottom Timeline & Scrubber Bar
                if (!isControlsLocked) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))
                                )
                            )
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        val displayPos = if (isUserScrubbing) scrubPosition else currentPosition
                        val sliderValue = if (duration > 0) (displayPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

                        // Scrubber Slider
                        Slider(
                            value = sliderValue,
                            onValueChange = { frac ->
                                isUserScrubbing = true
                                scrubPosition = (frac * duration).toLong()
                            },
                            onValueChangeFinished = {
                                exoPlayer.seekTo(scrubPosition)
                                currentPosition = scrubPosition
                                isUserScrubbing = false
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = cyanAccent,
                                activeTrackColor = cyanAccent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Time stamps
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDurationMs(displayPos),
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = formatDurationMs(duration),
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog: Subtitles & Subtitle Downloader
    if (showSubtitlesDialog) {
        ModalBottomSheet(
            onDismissRequest = { showSubtitlesDialog = false },
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
                        text = "الترجمة والتحميل",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "تحميل وبحث عن ترجمة الفيلم مباشرة:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                SubtitlesManager.subtitleSources.forEach { source ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                SubtitlesManager.openSubtitleSearch(context, movie.title, source)
                                showSubtitlesDialog = false
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
                                Text(source.description, color = Color.White.copy(0.7f), fontSize = 11.sp)
                            }
                            Icon(Icons.Default.Download, contentDescription = null, tint = cyanAccent)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Dialog: Playback Speed
    if (showSpeedDialog) {
        ModalBottomSheet(
            onDismissRequest = { showSpeedDialog = false },
            containerColor = Color(0xFF101622)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "سرعة التشغيل",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = cyanLight
                )
                Spacer(modifier = Modifier.height(12.dp))

                val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                speeds.forEach { speed ->
                    val isSelected = playbackSpeed == speed
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                playbackSpeed = speed
                                showSpeedDialog = false
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) cyanAccent.copy(alpha = 0.2f) else Color(0xFF182335)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (speed == 1.0f) "عادي (1.0x)" else "${speed}x",
                                color = if (isSelected) cyanAccent else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = cyanAccent)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Dialog: Quality Selector
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
                Spacer(modifier = Modifier.height(12.dp))

                val allQualities = if (movie.qualities.isNotEmpty()) {
                    movie.qualities
                } else {
                    listOf(
                        VideoQuality(label = "1080p Full HD", url = movie.videoUrl),
                        VideoQuality(label = "720p HD", url = movie.videoUrl),
                        VideoQuality(label = "480p SD", url = movie.videoUrl)
                    )
                }

                allQualities.forEach { q ->
                    val isSelected = currentUrl == q.url
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                if (q.url != currentUrl) {
                                    val savedPos = exoPlayer.currentPosition
                                    currentUrl = q.url
                                    currentQuality = q.label
                                    exoPlayer.seekTo(savedPos)
                                }
                                showQualitySelector = false
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) cyanAccent.copy(alpha = 0.2f) else Color(0xFF182335)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Hd,
                                    contentDescription = null,
                                    tint = if (isSelected) cyanAccent else Color.White
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = q.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) cyanAccent else Color.White
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = cyanAccent)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

private fun formatDurationMs(ms: Long): String {
    val totalSec = ms / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
