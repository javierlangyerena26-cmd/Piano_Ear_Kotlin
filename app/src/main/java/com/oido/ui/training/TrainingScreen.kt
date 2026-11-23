package com.oido.ui.training

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oido.domain.AnswerState
import com.oido.domain.Levels
import com.oido.domain.Note
import com.oido.ui.components.KeyState
import com.oido.ui.components.PianoKeyboard
import com.oido.ui.components.keyStatesFor
import com.oido.ui.theme.AccentBlue
import com.oido.ui.theme.AccentGold
import com.oido.ui.theme.CorrectGreen
import com.oido.ui.theme.CorrectGreenSurface
import com.oido.ui.theme.NavyBackground
import com.oido.ui.theme.NavySurface
import com.oido.ui.theme.StrokeSubtle
import com.oido.ui.theme.TextPrimary
import com.oido.ui.theme.TextSecondary
import com.oido.ui.theme.WrongRed
import com.oido.ui.theme.WrongRedSurface

/**
 * The training screen. Nothing on it can reveal the target note before the
 * user answers: no label, no description, no color.
 */
@Composable
fun TrainingScreen(
    state: TrainingUiState,
    onPlay: () -> Unit,
    onReplay: () -> Unit,
    onAnswer: (Note) -> Unit,
    onContinue: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val answered = state.answerState != AnswerState.WAITING
    val notes = Levels.forLevel(state.level).activeNotes

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header: exercise counter + progress + exit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EJERCICIO ${state.exerciseIndex} / ${state.totalExercises}",
                color = TextSecondary,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = "NIVEL ${state.level}",
                color = AccentBlue,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = "SALIR",
                color = TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NavySurface)
                    .border(1.dp, StrokeSubtle, RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onExit
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LinearProgressIndicator(
            progress = { state.exerciseIndex.toFloat() / state.totalExercises.coerceAtLeast(1) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = AccentGold,
            trackColor = NavySurface
        )

        Spacer(modifier = Modifier.height(26.dp))

        Text(
            text = if (answered) "RESULTADO" else "ESCUCHA LA NOTA",
            color = if (answered) TextSecondary else TextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (answered) "¿Reconociste la tecla?" else "¿Qué tecla sonó?",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Play / replay controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val playScale by animateFloatAsState(
                targetValue = if (state.notePlayed) 1f else 1.06f,
                animationSpec = tween(600),
                label = "playScale"
            )
            Button(
                onClick = onPlay,
                enabled = state.canPlay && (state.replaysRemaining > 0 || !state.notePlayed),
                modifier = Modifier
                    .fillMaxWidth(if (state.notePlayed) 0.72f else 0.78f)
                    .height(64.dp)
                    .graphicsLayer {
                        scaleX = playScale
                        scaleY = playScale
                    },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGold,
                    contentColor = NavyBackground,
                    disabledContainerColor = NavySurface,
                    disabledContentColor = TextSecondary
                ),
                border = if (state.notePlayed) null else BorderStroke(2.dp, AccentGold.copy(alpha = 0.5f))
            ) {
                Text(
                    text = if (state.notePlayed) "VOLVER A ESCUCHAR" else "ESCUCHAR NOTA",
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onReplay,
                enabled = state.canReplay,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, StrokeSubtle)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Repetir nota",
                    modifier = Modifier.size(18.dp),
                    tint = if (state.canReplay) {
                        TextPrimary
                    } else {
                        TextSecondary.copy(alpha = 0.5f)
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REPETIR",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            if (state.replaysRemaining != Int.MAX_VALUE) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Intentos: ${state.replaysRemaining}",
                    color = TextMutedOrSecondary(),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (state.notePlayed) "Pulsa una tecla para responder" else "Pulsa ESCUCHAR para empezar",
            color = TextSecondary.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Feedback banner
        AnimatedVisibility(
            visible = answered,
            enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.96f, animationSpec = tween(180)),
            exit = fadeOut(tween(120))
        ) {
            FeedbackBanner(state)
        }

        Spacer(modifier = Modifier.height(18.dp))

        // The keyboard: labels are the seven note names, always the same, and
        // never change with the target, so they cannot leak the answer.
        PianoKeyboard(
            notes = notes,
            onNoteSelected = onAnswer,
            enabled = state.canPlay && state.notePlayed,
            keyState = keyStatesFor(
                selected = state.selectedNote,
                target = state.targetNote,
                answerState = state.answerState
            ),
            labelsVisible = true,
            keyHeight = 190.dp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        AnimatedVisibility(
            visible = answered,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(120))
        ) {
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue,
                    contentColor = NavyBackground
                )
            ) {
                Text(
                    text = if (state.exerciseIndex >= state.totalExercises) "VER RESULTADOS" else "CONTINUAR",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FeedbackBanner(state: TrainingUiState) {
    val correct = state.answerState == AnswerState.CORRECT
    val background = if (correct) CorrectGreenSurface else WrongRedSurface
    val accent = if (correct) CorrectGreen else WrongRed

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .border(1.5.dp, accent, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (correct) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (correct) "¡Correcto!" else "La nota era ${state.targetNote?.displayName}",
                color = accent,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = when {
                correct -> "Sigue entrenando tu oído"
                state.selectedNote != null ->
                    "Tú elegiste ${state.selectedNote.displayName}. Escucha bien la diferencia."
                else -> ""
            },
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TextMutedOrSecondary(): Color = TextSecondary.copy(alpha = 0.75f)
