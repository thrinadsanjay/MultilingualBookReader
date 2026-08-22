package com.multilingualbookreader.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multilingualbookreader.domain.model.LibraryBook
import com.multilingualbookreader.domain.model.VoiceProfile
import com.multilingualbookreader.domain.repository.VoiceRepository
import com.multilingualbookreader.domain.usecase.GetContinueReadingUseCase
import com.multilingualbookreader.network.ConnectivityObserver
import com.multilingualbookreader.update.AppUpdateManager
import com.multilingualbookreader.update.UpdateUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val continueReading: LibraryBook? = null,
    val voice: VoiceProfile? = null,
    val online: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    continueReading: GetContinueReadingUseCase,
    voices: VoiceRepository,
    connectivity: ConnectivityObserver,
    val updates: AppUpdateManager,
) : ViewModel() {
    val state: StateFlow<HomeUiState> = combine(
        flow { emit(continueReading()) },
        voices.observeProfiles(),
        connectivity.observe(),
    ) { latest, profiles, online ->
        HomeUiState(
            continueReading = latest,
            voice = profiles.firstOrNull(),
            online = online,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    val updateState: StateFlow<UpdateUiState> = updates.state

    init {
        viewModelScope.launch { updates.check() }
    }

    fun checkUpdate() {
        viewModelScope.launch { updates.check() }
    }

    fun downloadUpdate() {
        viewModelScope.launch { updates.download() }
    }
}
