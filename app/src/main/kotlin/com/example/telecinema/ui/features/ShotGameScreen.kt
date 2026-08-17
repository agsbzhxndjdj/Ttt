package com.example.telecinema.ui.features

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.telecinema.data.local.AppPreferences
import com.example.telecinema.model.Movie
import com.example.telecinema.util.Lang

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShotGameScreen(
    locale: String,
    onBack: () -> Unit
) {
    val allMovies = remember { AppPreferences.getAllMovies().filter { it.poster.isNotEmpty() } }

    var targetMovie by remember { mutableStateOf<Movie?>(null) }
    var options by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var selectedOption by remember { mutableStateOf<Movie?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var round by remember { mutableIntStateOf(1) }

    fun startNewRound() {
        if (allMovies.size >= 4) {
            val shuffled = allMovies.shuffled()
            val target = shuffled[0]
            val opts = (shuffled.take(4)).shuffled()
            targetMovie = target
            options = opts
            selectedOption = null
        }
    }

    LaunchedEffect(allMovies) {
        startNewRound()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = Lang.t("shotGame", locale),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = "النقاط: $score",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 14.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        if (allMovies.size < 4) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("تحتاج إلى إضافة 4 أفلام على الأقل للعب", color = Color.White)
            }
        } else if (targetMovie != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Lang.t("guessMovie", locale),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Cropped / Zoomed Poster Box
                Card(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = targetMovie!!.poster,
                            contentDescription = "Movie Scene",
                            contentScale = if (selectedOption != null) ContentScale.Crop else ContentScale.FillBounds,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (selectedOption == null) Modifier.padding(30.dp) else Modifier
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Options list
                options.forEach { opt ->
                    val isChosen = selectedOption == opt
                    val isCorrect = opt.id == targetMovie!!.id

                    val backgroundColor = when {
                        selectedOption == null -> MaterialTheme.colorScheme.surfaceVariant
                        isCorrect -> Color(0xFF2E7D32)
                        isChosen && !isCorrect -> Color(0xFFC62828)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = selectedOption == null) {
                                selectedOption = opt
                                if (isCorrect) {
                                    score += 10
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = backgroundColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = opt.title,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (selectedOption != null && isCorrect) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                            } else if (selectedOption != null && isChosen && !isCorrect) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (selectedOption != null) {
                    Button(
                        onClick = {
                            round++
                            startNewRound()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Lang.t("nextRound", locale), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
