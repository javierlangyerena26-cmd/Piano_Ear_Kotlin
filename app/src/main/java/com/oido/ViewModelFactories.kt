package com.oido

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.oido.audio.AudioEngine
import com.oido.data.ProgressRepository
import com.oido.ui.familiar.FamiliarViewModel
import com.oido.ui.home.HomeViewModel
import com.oido.ui.training.TrainingViewModel

/**
 * Minimal manual dependency injection. The app has no DI framework on purpose:
 * three factories keep the object graph explicit and testable.
 */
@Suppress("UNCHECKED_CAST")
class HomeViewModelFactory(
    private val repository: ProgressRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        HomeViewModel(repository) as T
}

@Suppress("UNCHECKED_CAST")
class TrainingViewModelFactory(
    private val repository: ProgressRepository,
    private val audio: AudioEngine
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        TrainingViewModel(repository, audio) as T
}

@Suppress("UNCHECKED_CAST")
class FamiliarViewModelFactory(
    private val audio: AudioEngine
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        FamiliarViewModel(audio) as T
}
