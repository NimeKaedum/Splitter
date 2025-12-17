package com.example.livesplitlike.ui.screens.timer

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import com.example.livesplitlike.timer.TimerEngine
import com.example.livesplitlike.data.local.model.SplitTemplateEntity
import com.example.livesplitlike.timer.ComparisonStatus

/**
 * TimerViewModel
 *
 * - Delegates all logic to TimerEngine.
 * - Exposes the engine StateFlows to Compose via hiltViewModel().
 */
@HiltViewModel
class TimerViewModel @Inject constructor(
    private val engine: TimerEngine
) : ViewModel() {

    val isRunning: StateFlow<Boolean> = engine.isRunning
    val isPaused: StateFlow<Boolean> = engine.isPaused
    val elapsedMillis: StateFlow<Long> = engine.elapsedMillis
    val templates: StateFlow<List<SplitTemplateEntity>> = engine.templates
    val selectedGroupName: StateFlow<String> = engine.selectedGroupName
    val bestCumulative: StateFlow<List<Long>> = engine.bestCumulative
    val splitBestSegments: StateFlow<List<Long?>> = engine.splitBestSegments
    val currentCumulative: StateFlow<List<Long?>> = engine.currentCumulative
    val diffs: StateFlow<List<Long?>> = engine.diffs
    val segmentDiffs: StateFlow<List<Long?>> = engine.segmentDiffs
    val lastRunCompleted: StateFlow<Boolean> = engine.lastRunCompleted
    val hasAnyCompleteRun: StateFlow<Boolean> = engine.hasAnyCompleteRun
    val comparisonItemsFlow = engine.comparisonItemsFlow

    // Delegate functions
    fun onTimerClicked() = engine.onTimerClicked()
    fun onPauseToggle() = engine.onPauseToggle()
    fun onReset() = engine.onReset()
    fun selectGroup(groupId: Long) = engine.selectGroup(groupId)
    fun resetCurrentToBaseline() = engine.resetCurrentToBaseline()
    fun formatMillis(ms: Long) = engine.formatMillis(ms)
    fun formatDiffMillis(diff: Long?) = engine.formatDiffMillis(diff)
    fun statusToColor(status: ComparisonStatus) = engine.statusToColor(status)
}