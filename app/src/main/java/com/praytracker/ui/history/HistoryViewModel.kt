package com.praytracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.praytracker.data.db.PrayerRecord
import com.praytracker.data.repo.PrayerRepository
import com.praytracker.data.repo.PrayerTimesRepository
import com.praytracker.data.settings.Settings
import com.praytracker.di.AppContainer
import com.praytracker.prayer.DailyPrayerTimes
import com.praytracker.prayer.Prayer
import com.praytracker.prayer.PrayerStatus
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val settings: Settings = Settings(),
    val monthStart: LocalDate = LocalDate.now().withDayOfMonth(1),
    val records: Map<String, PrayerRecord> = emptyMap(),
    val selectedDate: LocalDate? = null,
    val selectedTimes: DailyPrayerTimes? = null,
)

class HistoryViewModel(container: AppContainer) : ViewModel() {

    private val settingsRepository = container.settingsRepository
    private val prayerRepository = container.prayerRepository
    private val timesRepository = container.prayerTimesRepository

    private val monthStart = MutableStateFlow(LocalDate.now().withDayOfMonth(1))
    private val selectedDate = MutableStateFlow<LocalDate?>(LocalDate.now())

    val state: StateFlow<HistoryUiState> =
        combine(
            settingsRepository.settings,
            monthStart,
            prayerRepository.observeAll(),
            selectedDate,
        ) { settings, month, allRecords, selected ->
            HistoryUiState(
                settings = settings,
                monthStart = month,
                records = allRecords.associateBy { it.date },
                selectedDate = selected,
                selectedTimes = selected?.let { timesRepository.compute(it, settings) },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun previousMonth() {
        monthStart.value = monthStart.value.minusMonths(1)
    }

    fun nextMonth() {
        monthStart.value = monthStart.value.plusMonths(1)
    }

    fun today() {
        monthStart.value = LocalDate.now().withDayOfMonth(1)
        selectedDate.value = LocalDate.now()
    }

    fun select(date: LocalDate) {
        selectedDate.value = date
    }

    fun closeDayDetail() {
        selectedDate.value = null
    }

    fun setStatus(date: LocalDate, prayer: Prayer, status: PrayerStatus) {
        viewModelScope.launch {
            prayerRepository.setStatus(date, prayer, status)
        }
    }

    fun nextStatus(current: PrayerStatus): PrayerStatus = when (current) {
        PrayerStatus.NOT_RECORDED -> PrayerStatus.PRAYED
        PrayerStatus.PRAYED -> PrayerStatus.DELAYED
        PrayerStatus.DELAYED -> PrayerStatus.NOT_DID
        PrayerStatus.NOT_DID -> PrayerStatus.NOT_RECORDED
    }
}