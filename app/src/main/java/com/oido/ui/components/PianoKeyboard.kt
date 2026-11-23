package com.oido.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oido.domain.Note
import com.oido.ui.theme.AccentBlue
import com.oido.ui.theme.CorrectGreen
import com.oido.ui.theme.KeyWhite
import com.oido.ui.theme.KeyWhitePressed
import com.oido.ui.theme.KeyWhiteText
import com.oido.ui.theme.NavyBackground
import com.oido.ui.theme.StrokeSubtle
import com.oido.ui.theme.WrongRed

/** Visual state of one key. */
enum class KeyState { IDLE, PRESSED, CORRECT, WRONG, HIGHLIGHT }

/**
 * Reusable seven white key piano keyboard (DO, RE, MI, FA, SOL, LA, SI).
 *
 * @param notes keys to display, in order.
 * @param onNoteSelected callback when the user taps a key.
 * @param enabled when false the keys ignore input (used after answering).
 * @param keyState state per note, used for pressed / correct / wrong feedback.
 * @param labelsVisible must be false while the note is hidden (training) and
 *        true in the familiarization mode.
 * @param revealCorrectNote when true the correct key is highlighted for the user.
 * @param highlightedNotes extra notes to highlight (e.g. the correct answer).
 */
@Composable
fun PianoKeyboard(
    notes: List<Note>,
    onNoteSelected: (Note) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyState: (Note) -> KeyState = { KeyState.IDLE },
    labelsVisible: Boolean = true,
    keyHeight: Dp = 200.dp
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(keyHeight),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        notes.forEach { note ->
            PianoKey(
                note = note,
                state = keyState(note),
                enabled = enabled,
                labelsVisible = labelsVisible,
                onClick = { onNoteSelected(note) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun PianoKey(
    note: Note,
    state: KeyState,
    enabled: Boolean,
    labelsVisible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val effectiveState = when {
        state == KeyState.IDLE && pressed -> KeyState.PRESSED
        else -> state
    }

    val shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 10.dp, bottomEnd = 10.dp)

    val background by animateColorAsState(
        targetValue = when (effectiveState) {
            KeyState.PRESSED -> KeyWhitePressed
            KeyState.CORRECT -> CorrectGreen
            KeyState.WRONG -> WrongRed
            KeyState.HIGHLIGHT -> AccentBlue
            KeyState.IDLE -> KeyWhite
        },
        animationSpec = tween(durationMillis = 180),
        label = "keyBackground"
    )

    val scale by animateFloatAsState(
        targetValue = when (effectiveState) {
            KeyState.PRESSED -> 0.97f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 120),
        label = "keyScale"
    )

    // After answering we must not reveal anything about the target before the
    // answer: descriptions only mention the key position until then.
    val description = when {
        !labelsVisible && state == KeyState.IDLE -> "Tecla ${notesIndexOf(note) + 1} de 7"
        state == KeyState.CORRECT -> "Tecla ${note.displayName}, respuesta correcta"
        state == KeyState.WRONG -> "Tecla ${note.displayName}, incorrecta"
        labelsVisible -> "Tecla ${note.displayName}"
        else -> "Tecla ${notesIndexOf(note) + 1} de 7"
    }

    val borderColor = when (effectiveState) {
        KeyState.CORRECT -> CorrectGreen
        KeyState.WRONG -> WrongRed
        KeyState.HIGHLIGHT -> AccentBlue
        else -> StrokeSubtle
    }

    Box(
        modifier = modifier
            .shadow(elevation = 4.dp, shape = shape, ambientColor = NavyBackground)
            .clip(shape)
            .background(background)
            .border(width = 1.5.dp, color = borderColor, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .semantics {
                role = Role.Button
                contentDescription = description
            }
            .padding(horizontal = 2.dp, vertical = 10.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (labelsVisible) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = note.displayName,
                    color = when (effectiveState) {
                        KeyState.CORRECT -> NavyBackground
                        KeyState.WRONG -> NavyBackground
                        else -> KeyWhiteText
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${note.frequency.toInt()} Hz",
                    color = when (effectiveState) {
                        KeyState.CORRECT -> NavyBackground.copy(alpha = 0.75f)
                        KeyState.WRONG -> NavyBackground.copy(alpha = 0.75f)
                        else -> KeyWhiteText.copy(alpha = 0.45f)
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun notesIndexOf(note: Note): Int = Note.entries.indexOf(note)

/** Maps an answered exercise to the visual state of every key. */
fun keyStatesFor(
    selected: Note?,
    target: Note?,
    answerState: com.oido.domain.AnswerState
): (Note) -> KeyState = when (answerState) {
    com.oido.domain.AnswerState.WAITING -> {
        { KeyState.IDLE }
    }

    com.oido.domain.AnswerState.CORRECT -> { note ->
        if (note == target) KeyState.CORRECT else KeyState.IDLE
    }

    com.oido.domain.AnswerState.WRONG -> { note ->
        when {
            note == target -> KeyState.CORRECT
            note == selected -> KeyState.WRONG
            else -> KeyState.IDLE
        }
    }
}
