package com.oido.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oido.ui.theme.AccentBlue
import com.oido.ui.theme.AccentGold
import com.oido.ui.theme.CorrectGreen
import com.oido.ui.theme.NavyBackground
import com.oido.ui.theme.NavySurface
import com.oido.ui.theme.NavySurfaceElevated
import com.oido.ui.theme.StrokeSubtle
import com.oido.ui.theme.TextPrimary
import com.oido.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun HomeScreen(
    state: HomeViewModel.HomeUiState,
    onStartTraining: (level: Int, length: Int) -> Unit,
    onStartFamiliar: () -> Unit,
    onSessionLengthSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SoundWaveVisual(modifier = Modifier.fillMaxWidth().height(96.dp))

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "OIDO",
            color = TextPrimary,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 10.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Entrena tu oído musical",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Session length: 10 / 20 / 30 questions.
        Text(
            text = "DURACIÓN DE LA SESIÓN",
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(10, 20, 30).forEach { length ->
                val selected = state.sessionLength == length
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) AccentGold.copy(alpha = 0.16f) else NavySurface)
                        .border(
                            1.5.dp,
                            if (selected) AccentGold else StrokeSubtle,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSessionLengthSelected(length) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$length",
                        color = if (selected) AccentGold else TextSecondary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Daily progress
        StatCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PROGRESO DE HOY",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${state.todayExercises} de ${HomeViewModel.HomeUiState.DAILY_GOAL} ejercicios",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = "${(state.dailyGoalProgress * 100).toInt()}%",
                    color = AccentGold,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(NavySurfaceElevated)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(state.dailyGoalProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.horizontalGradient(listOf(AccentGold, AccentBlue)))
                )
            }
            if (state.todayExercises > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Precisión de hoy: ${percent(state.todayAccuracy)}",
                    color = if (state.todayAccuracy >= 0.75) CorrectGreen else TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Stats grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MiniStat(
                value = "${state.totalSessions}",
                label = "Sesiones",
                modifier = Modifier.weight(1f)
            )
            MiniStat(
                value = percent(state.accuracy),
                label = "Precisión",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MiniStat(
                value = "${state.dailyStreak}",
                label = "Racha diaria",
                modifier = Modifier.weight(1f)
            )
            MiniStat(
                value = "${state.unlockedNotes.size}/7",
                label = "Notas desbloq.",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Level selector
        Text(
            text = "NIVEL ${state.unlockedLevel}",
            color = AccentBlue,
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = state.levelConfigDescription(),
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onStartTraining(state.unlockedLevel, state.sessionLength) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGold,
                contentColor = NavyBackground
            )
        ) {
            Text(
                text = "ENTRENAR",
                style = MaterialTheme.typography.labelLarge,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onStartFamiliar,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, StrokeSubtle)
        ) {
            Text(
                text = "MODO EXPLORACIÓN",
                color = TextSecondary,
                style = MaterialTheme.typography.labelLarge
            )
        }

        if (state.totalSessions == 0) {
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Escucha una nota y elige la tecla que sonó.\nCon la práctica reconocerás cada nota de oído.",
                color = TextSecondary.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

private fun HomeViewModel.HomeUiState.levelConfigDescription(): String = when (unlockedLevel) {
    1 -> "Dos notas: DO y SOL"
    2 -> "Cuatro notas: DO, RE, MI y SOL"
    else -> "Las siete notas naturales"
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(NavySurface)
            .border(1.dp, StrokeSubtle, RoundedCornerShape(18.dp))
            .padding(18.dp),
        content = content
    )
}

@Composable
private fun MiniStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(NavySurface)
            .border(1.dp, StrokeSubtle, RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = TextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center
        )
    }
}

private fun percent(value: Double): String = "${(value * 100).toInt()}%"

/**
 * Subtle animated sine wave: a musical visual without childish graphics.
 */
@Composable
private fun SoundWaveVisual(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val midY = size.height / 2f
            val colors = listOf(AccentGold, AccentBlue, CorrectGreen)
            colors.forEachIndexed { index, color ->
                val amplitude = size.height * (0.34f - index * 0.08f)
                val frequency = 1.6f + index * 0.7f
                val offset = phase + index * 0.7f
                val step = 4f
                var previousY = midY
                var x = 0f
                while (x <= size.width) {
                    val normalized = x / size.width
                    val envelope = sin(normalized * PI).toFloat()
                    val y = midY + amplitude * envelope *
                        sin(normalized * frequency * 2f * PI + offset).toFloat()
                    if (x > 0f) {
                        drawLine(
                            color = color.copy(alpha = 0.55f - index * 0.12f),
                            start = Offset(x - step, previousY),
                            end = Offset(x, y),
                            strokeWidth = 3f - index * 0.6f
                        )
                    }
                    previousY = y
                    x += step
                }
            }
        }
    }
}
