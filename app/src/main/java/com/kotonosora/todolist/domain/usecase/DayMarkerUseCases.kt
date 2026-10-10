package com.kotonosora.todolist.domain.usecase

import com.kotonosora.todolist.domain.model.DayMarkers
import com.kotonosora.todolist.domain.model.dayKeyToDate
import com.kotonosora.todolist.domain.repository.DayMarkerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Calendar day-marker reads. Writes happen inside the data layer
 * ([DayMarkerRepository.refreshDays] from repository impls + sync paths —
 * same precedent as the FTS index); ViewModels only observe and repair.
 */
class ObserveDayMarkersUseCase(private val repository: DayMarkerRepository) {
    operator fun invoke(): Flow<Map<LocalDate, DayMarkers>> =
        repository.observeMarkers().map { markers ->
            markers.mapKeys { (date, _) -> dayKeyToDate(date) }
        }
}

class RebuildDayMarkersIfEmptyUseCase(private val repository: DayMarkerRepository) {
    suspend operator fun invoke() = repository.rebuildIfEmpty()
}

data class DayMarkerUseCases(
    val observeDayMarkers: ObserveDayMarkersUseCase,
    val rebuildIfEmpty: RebuildDayMarkersIfEmptyUseCase
) {
    companion object {
        fun from(repository: DayMarkerRepository): DayMarkerUseCases = DayMarkerUseCases(
            observeDayMarkers = ObserveDayMarkersUseCase(repository),
            rebuildIfEmpty = RebuildDayMarkersIfEmptyUseCase(repository)
        )
    }
}
