package com.oido.ui.familiar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oido.domain.Levels
import com.oido.domain.Note
import com.oido.ui.components.KeyState
import com.oido.ui.components.PianoKeyboard
import com.oido.ui.theme.AccentBlue
import com.oido.ui.theme.AccentGold
import com.oido.ui.theme.NavyBackground
import com.oido.ui.theme.NavySurface
import com.oido.ui.theme.StrokeSubtle
import com.oido.ui.theme.TextPrimary
import com.oido.ui.theme.TextSecondary

/**
 * "Modo exploración": free play where the name of every note is visible.
 * This is a complementary learning aid, not a replacement for the guessing
 * exercise.
 */
@Composable
fun FamiliarScreen(
    unlockedLevel: Int,
    onKeyPressed: (Note) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf<Note?>(null) }
    val notes = Levels.unlockedNotes(unlockedLevel)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = "MODO EXPLORACIÓN",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Toca cada tecla y escucha su sonido",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "ESCUCHA Y OBSERVA",
            color = AccentBlue,
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Current note display
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(NavySurface)
                .border(1.5.dp, StrokeSubtle, RoundedCornerShape(20.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = selected?.displayName ?: "—",
                color = if (selected == null) TextSecondary else AccentGold,
                fontSize = if (selected == null) 34.sp else 56.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = selected?.let {
                    "Frecuencia: ${"%.2f".format(it.frequency)} Hz"
                } ?: "Pulsa una tecla del teclado",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        PianoKeyboard(
            notes = notes,
            onNoteSelected = { note ->
                selected = note
                onKeyPressed(note)
            },
            enabled = true,
            keyState = { note -> if (note == selected) KeyState.HIGHLIGHT else KeyState.IDLE },
            labelsVisible = true,
            keyHeight = 220.dp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "En el entrenamiento las notas se ocultan.\nAquí puedes conocerlas sin presión.",
            color = TextSecondary.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}
