package com.oido

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oido.audio.PianoAudioEngine
import com.oido.data.ProgressRepository
import com.oido.ui.familiar.FamiliarScreen
import com.oido.ui.familiar.FamiliarViewModel
import com.oido.ui.home.HomeScreen
import com.oido.ui.home.HomeViewModel
import com.oido.ui.navigation.Screen
import com.oido.ui.results.ResultsScreen
import com.oido.ui.theme.OidoTheme
import com.oido.ui.training.TrainingScreen
import com.oido.ui.training.TrainingViewModel

/**
 * Single activity of the app. It only wires navigation and the ViewModels;
 * all the logic lives in dedicated classes (see the architecture notes in
 * README.md).
 */
class MainActivity : ComponentActivity() {

    private val audioEngine: PianoAudioEngine by lazy { PianoAudioEngine() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = ProgressRepository(applicationContext)

        setContent {
            OidoTheme {
                AppNavigation(
                    repository = repository,
                    audioEngine = audioEngine
                )
            }
        }
    }

    override fun onStop() {
        // Never leave a note ringing in the background.
        audioEngine.stop()
        super.onStop()
    }

    override fun onDestroy() {
        audioEngine.release()
        super.onDestroy()
    }
}

@Composable
private fun AppNavigation(
    repository: ProgressRepository,
    audioEngine: PianoAudioEngine
) {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var lastUnlockedLevel by remember { mutableStateOf(1) }

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(repository)
    )
    val trainingViewModel: TrainingViewModel = viewModel(
        factory = TrainingViewModelFactory(repository, audioEngine)
    )
    val familiarViewModel: FamiliarViewModel = viewModel(
        factory = FamiliarViewModelFactory(audioEngine)
    )

    when (val current = screen) {
        is Screen.Home -> {
            val state by homeViewModel.uiState.collectAsState()
            HomeScreen(
                state = state,
                onStartTraining = { level, length ->
                    lastUnlockedLevel = state.unlockedLevel
                    trainingViewModel.startSession(level, length)
                    screen = Screen.Training(level, length)
                },
                onStartFamiliar = {
                    familiarViewModel.initialize()
                    screen = Screen.Familiar
                },
                onSessionLengthSelected = { homeViewModel.setSessionLength(it) }
            )
        }

        is Screen.Training -> {
            val state by trainingViewModel.uiState.collectAsState()
            TrainingScreen(
                state = state,
                onPlay = { trainingViewModel.playCurrent() },
                onReplay = { trainingViewModel.replay() },
                onAnswer = { trainingViewModel.answer(it) },
                onContinue = { trainingViewModel.continueManually() },
                onExit = {
                    trainingViewModel.shutdown()
                    screen = Screen.Home
                }
            )

            if (state.finished) {
                // Results replace the training screen as soon as the session ends.
                androidx.compose.runtime.LaunchedEffect(state.finished) {
                    screen = Screen.Results
                }
            }
        }

        is Screen.Results -> {
            val trainingState by trainingViewModel.uiState.collectAsState()
            val homeState by homeViewModel.uiState.collectAsState()
            val snapshot = trainingState.sessionSnapshot
            if (snapshot != null) {
                val finishedLevel = trainingState.level
                ResultsScreen(
                    snapshot = snapshot,
                    unlockedLevel = homeState.unlockedLevel,
                    previousUnlockedLevel = lastUnlockedLevel,
                    onRetry = {
                        lastUnlockedLevel = homeState.unlockedLevel
                        trainingViewModel.startSession(
                            finishedLevel,
                            homeState.sessionLength
                        )
                        screen = Screen.Training(finishedLevel, homeState.sessionLength)
                    },
                    onContinue = {
                        lastUnlockedLevel = homeState.unlockedLevel
                        trainingViewModel.startSession(
                            homeState.unlockedLevel,
                            homeState.sessionLength
                        )
                        screen = Screen.Training(homeState.unlockedLevel, homeState.sessionLength)
                    },
                    onHome = { screen = Screen.Home }
                )
            } else {
                // Defensive: never leave the user on a blank screen.
                LaunchedEffect(Unit) { screen = Screen.Home }
            }
        }

        is Screen.Familiar -> {
            FamiliarScreen(
                unlockedLevel = familiarLevel(homeViewModel),
                onKeyPressed = { familiarViewModel.onKeyPressed(it) },
                onBack = {
                    familiarViewModel.stop()
                    screen = Screen.Home
                }
            )
        }
    }
}

@Composable
private fun familiarLevel(homeViewModel: HomeViewModel): Int =
    homeViewModel.uiState.collectAsState().value.unlockedLevel
