package com.praytracker.ui.today

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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val settings: Settings = Settings(),
    val dayOffset: Int = 0,
    val times: DailyPrayerTimes? = null,
    val record: PrayerRecord? = null,
)

class TodayViewModel(container: AppContainer) : ViewModel() {

    private val settingsRepository = container.settingsRepository
    private val timesRepository = container.prayerTimesRepository
    private val prayerRepository = container.prayerRepository

    private val dayOffset = MutableStateFlow(0)
    private val settingsFlow = settingsRepository.settings

    val state: StateFlow<TodayUiState> =
        combine(settingsFlow, dayOffset) { settings, offset -> Pair(settings, offset) }
            .flatMapLatest { (settings, offset) ->
                val shown = LocalDate.now().plusDays(offset.toLong())
                flow {
                    emit(
                        TodayUiState(
                            settings = settings,
                            dayOffset = offset,
                            times = timesRepository.compute(shown, settings),
                            record = prayerRepository.record(shown),
                        )
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun shiftDay(delta: Int) {
        dayOffset.value = (dayOffset.value + delta).coerceIn(-365, 30)
    }

    fun resetDay() {
        dayOffset.value = 0
    }

    fun setStatus(prayer: Prayer, status: PrayerStatus) {
        val offset = dayOffset.value
        viewModelScope.launch {
            prayerRepository.setStatus(LocalDate.now().plusDays(offset.toLong()), prayer, status)
        }
    }

    fun nextStatus(current: PrayerStatus): PrayerStatus = when (current) {
        PrayerStatus.NOT_RECORDED -> PrayerStatus.PRAYED
        PrayerStatus.PRAYED -> PrayerStatus.DELAYED
        PrayerStatus.DELAYED -> PrayerStatus.NOT_DID
        PrayerStatus.NOT_DID -> PrayerStatus.NOT_RECORDED
    }
}