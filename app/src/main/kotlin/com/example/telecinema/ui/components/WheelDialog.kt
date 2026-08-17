package com.example.telecinema.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.telecinema.model.Movie
import com.example.telecinema.util.Lang
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun WheelDialog(
    movies: List<Movie>,
    locale: String,
    onDismiss: () -> Unit,
    onMovieSelected: (Movie) -> Unit
) {
    if (movies.isEmpty()) return

    val sampleMovies = remember { movies.shuffled().take(8) }
    var isSpinning by remember { mutableStateOf(false) }
    var selectedMovie by remember { mutableStateOf<Movie?>(null) }
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .testTag("wheel_dialog")
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Lang.t("wheelOfFortune", locale),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Wheel graphic with pointer
                Box(
                    modifier = Modifier.size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val colors = listOf(
                        Color(0xFFE5B13D),
                        Color(0xFF0088CC),
                        Color(0xFF9C57FF),
                        Color(0xFF4CAF50),
                        Color(0xFFE53935),
                        Color(0xFF00BCD4),
                        Color(0xFFFF9800),
                        Color(0xFF3F51B5)
                    )

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(rotation.value)
                    ) {
                        val anglePerSlice = 360f / sampleMovies.size
                        sampleMovies.forEachIndexed { index, _ ->
                            drawArc(
                                color = colors[index % colors.size],
                                startAngle = index * anglePerSlice,
                                sweepAngle = anglePerSlice,
                                useCenter = true
                            )
                        }

                        drawCircle(
                            color = Color(0xFF151B23),
                            radius = size.minDimension / 6
                        )
                        drawCircle(
                            color = Color(0xFFE5B13D),
                            radius = size.minDimension / 6,
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }

                    // Center indicator pin
                    Canvas(modifier = Modifier.size(30.dp)) {
                        val path = Path().apply {
                            moveTo(size.width / 2, 0f)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(path, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedMovie != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🎉 " + selectedMovie!!.title,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    onMovieSelected(selectedMovie!!)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(Lang.t("play", locale), color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            if (!isSpinning) {
                                isSpinning = true
                                selectedMovie = null
                                val targetSpins = 5 + Random.nextInt(5)
                                val randomIdx = Random.nextInt(sampleMovies.size)
                                val targetAngle = targetSpins * 360f + (randomIdx * (360f / sampleMovies.size))
                                scope.launch {
                                    rotation.animateTo(
                                        targetValue = rotation.value + targetAngle,
                                        animationSpec = tween(durationMillis = 3000, easing = FastOutSlowInEasing)
                                    )
                                    selectedMovie = sampleMovies[randomIdx]
                                    isSpinning = false
                                }
                            }
                        },
                        enabled = !isSpinning,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .testTag("spin_wheel_button")
                            .fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Casino, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Lang.t("spin", locale), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss) {
                    Text(Lang.t("cancel", locale), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
