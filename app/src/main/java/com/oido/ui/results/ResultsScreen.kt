package com.oido.ui.results

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oido.domain.SessionSnapshot
import com.oido.ui.theme.AccentBlue
import com.oido.ui.theme.AccentGold
import com.oido.ui.theme.CorrectGreen
import com.oido.ui.theme.NavyBackground
import com.oido.ui.theme.NavySurface
import com.oido.ui.theme.StrokeSubtle
import com.oido.ui.theme.TextPrimary
import com.oido.ui.theme.TextSecondary
import com.oido.ui.theme.WrongRed

@Composable
fun ResultsScreen(
    snapshot: SessionSnapshot,
    unlockedLevel: Int,
    previousUnlockedLevel: Int,
    onRetry: () -> Unit,
    onContinue: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val levelUp = unlockedLevel > previousUnlockedLevel

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = AccuracyCenterHorizontally
    ) {
        Text(
            text = "RESULTADOS",
            color = TextPrimary,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = when {
                snapshot.accuracy >= 0.9 -> "¡Excelente oído!"
                snapshot.accuracy >= 0.75 -> "¡Muy buen trabajo!"
                else -> "Sigue practicando, mejora con la escucha"
            },
            color = TextSecondary,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Headline accuracy
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(NavySurface)
                .border(1.5.dp, StrokeSubtle, RoundedCornerShape(20.dp))
                .padding(vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${(snapshot.accuracy * 100).toInt()}%",
                color = if (snapshot.accuracy >= 0.75) CorrectGreen else AccentGold,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Precisión de la sesión",
                color = TextSecondary,
                style = MaterialTheme.typography.labelMedium
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ResultStat("Aciertos", "${snapshot.correct}", CorrectGreen, Modifier.weight(1f))
            ResultStat("Fallos", "${snapshot.incorrect}", WrongRed, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ResultStat(
                "Preguntas",
                "${snapshot.totalQuestions}",
                TextPrimary,
                Modifier.weight(1f)
            )
            ResultStat(
                "Mejor racha",
                "${snapshot.bestStreak}",
                AccentGold,
                Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        ResultStat(
            label = "Tiempo medio de respuesta",
            value = formatMs(snapshot.averageResponseMs),
            color = AccentBlue,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Notes recognized
        ResultsSectionCard(title = "NOTAS RECONOCIDAS") {
            if (snapshot.recognizedNotes.isEmpty()) {
                Text(
                    text = "Ninguna esta vez. ¡A la próxima!",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    snapshot.recognizedNotes.forEach { note ->
                        NoteChip(note.displayName, CorrectGreen)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Notes to improve
        ResultsSectionCard(title = "A MEJORAR") {
            if (snapshot.notesToImprove.isEmpty()) {
                Text(
                    text = "¡Sin fallos! Tu oído está afinado.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    snapshot.notesToImprove.forEach { note ->
                        NoteChip(note.displayName, WrongRed)
                    }
                }
            }
        }

        if (levelUp) {
            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NavySurface)
                    .border(1.5.dp, AccentGold, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "¡NIVEL $unlockedLevel DESBLOQUEADO!",
                    color = AccentGold,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ahora puedes practicar con más notas.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGold,
                contentColor = NavyBackground
            )
        ) {
            Text(text = "REINTENTAR SESIÓN", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, StrokeSubtle)
        ) {
            Text(text = "CONTINUAR ENTRENANDO", color = TextSecondary,
                style = MaterialTheme.typography.labelLarge)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onHome,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, StrokeSubtle)
        ) {
            Text(text = "VOLVER AL INICIO", color = TextSecondary,
                style = MaterialTheme.typography.labelLarge)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

private val AccuracyCenterHorizontally = Alignment.CenterHorizontally

@Composable
private fun ResultStat(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
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
            color = color,
            fontSize = 26.sp,
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

@Composable
private fun ResultsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NavySurface)
            .border(1.dp, StrokeSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun NoteChip(name: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = name,
        color = color,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

private fun formatMs(ms: Long): String {
    val seconds = ms / 1000.0
    return if (seconds < 10) String.format("%.1f s", seconds)
    else "${seconds.toInt()} s"
}
