package com.praytracker.ui.tasbih

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.praytracker.data.db.TasbihEntity
import com.praytracker.data.repo.TasbihRepository
import com.praytracker.data.settings.Settings
import com.praytracker.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TasbihUiState(
    val items: List<TasbihEntity> = emptyList(),
    val selectedId: Long = -1,
    val settings: Settings = Settings(),
)

class TasbihViewModel(container: AppContainer) : ViewModel() {

    private val tasbihRepository = container.tasbihRepository
    private val settingsRepository = container.settingsRepository

    val state: StateFlow<TasbihUiState> =
        combine(
            tasbihRepository.observeAll(),
            selectedId,
            settingsRepository.settings,
        ) { items, selected, settings ->
            val effectiveSelected = if (items.any { it.id == selected }) selected else items.firstOrNull()?.id ?: -1
            TasbihUiState(items = items, selectedId = effectiveSelected, settings = settings)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasbihUiState())

    private val selectedId = MutableStateFlow(-1L)

    fun select(id: Long) {
        selectedId.value = id
    }

    fun incrementSelected() {
        val current = state.value
        val entity = current.items.firstOrNull { it.id == current.selectedId } ?: return
        viewModelScope.launch {
            val count = tasbihRepository.bump(entity.id) ?: return@launch
            if (count >= entity.target && current.settings.tasbihAutoSwitchToNext) {
                autoSwitchToNext(entity.id)
            }
        }
    }

    fun resetSelected() {
        val current = state.value
        viewModelScope.launch {
            tasbihRepository.reset(current.selectedId)
        }
    }

    fun add(arabic: String, translation: String?, target: Int) {
        viewModelScope.launch {
            val id = tasbihRepository.upsert(
                TasbihEntity(
                    arabic = arabic.trim(),
                    translation = translation?.trim()?.ifBlank { null },
                    target = target.coerceIn(1, 100_000),
                    isCustom = true,
                    orderIndex = state.value.items.size,
                )
            )
            selectedId.value = id
        }
    }

    fun update(edit: TasbihEntity) {
        viewModelScope.launch {
            tasbihRepository.upsert(edit.copy(count = 0))
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            tasbihRepository.delete(id)
            if (selectedId.value == id) selectedId.value = -1
        }
    }

    private suspend fun autoSwitchToNext(id: Long) {
        val items = tasbihRepository.getAll()
        val idx = items.indexOfFirst { it.id == id }
        if (idx >= 0 && idx < items.size - 1) {
            selectedId.value = items[idx + 1].id
        } else {
            selectedId.value = items.firstOrNull()?.id ?: -1
            tasbihRepository.reset(items.firstOrNull()?.id ?: return)
        }
    }
}