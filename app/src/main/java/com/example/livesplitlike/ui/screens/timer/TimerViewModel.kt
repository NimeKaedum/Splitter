package com.example.livesplitlike.ui.screens.timer

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livesplitlike.domain.mapper.toDomain
import com.example.livesplitlike.data.repositories.RunRepository
import com.example.livesplitlike.data.repositories.SplitRepository
import com.example.livesplitlike.domain.usecase.FinishRunUseCase
import com.example.livesplitlike.domain.usecase.RecordSplitUseCase
import com.example.livesplitlike.domain.usecase.ResetRunUseCase
import com.example.livesplitlike.domain.usecase.StartRunUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val runRepository: RunRepository,
    private val splitRepository: SplitRepository,
    private val startRunUseCase: StartRunUseCase,
    private val recordSplitUseCase: RecordSplitUseCase,
    private val finishRunUseCase: FinishRunUseCase,
    private val resetRunUseCase: ResetRunUseCase
) : ViewModel() {

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis: StateFlow<Long> = _elapsedMillis.asStateFlow()

    private val _currentRunId = MutableStateFlow<Long?>(null)
    val currentRunId: StateFlow<Long?> = _currentRunId.asStateFlow()

    val splitsFlow: Flow<List<com.example.livesplitlike.domain.models.Split>> =
        _currentRunId.filterNotNull().flatMapLatest { id ->
            splitRepository.getSplitsForRunFlow(id).map { list -> list.map { it.toDomain() } }
        }

    private var tickerJob: Job? = null
    private var timerStartRealtime: Long = 0L

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val runs = runRepository.getAllRunsFlow().firstOrNull() ?: emptyList()
            if (runs.isNotEmpty()) _currentRunId.value = runs.first().id
        }
    }

    fun onTimerClicked() {
        viewModelScope.launch(Dispatchers.IO) {
            val runId = _currentRunId.value
            if (runId == null) {
                val newRunId = startRunUseCase.invoke(null)
                _currentRunId.value = newRunId
                startTicker()
                _isRunning.value = true
                _isPaused.value = false
                return@launch
            }

            if (!_isRunning.value) {
                startRunUseCase.invoke(runId)
                startTicker()
                _isRunning.value = true
                _isPaused.value = false
                return@launch
            }

            if (_isPaused.value) {
                return@launch
            }

            recordSplitUseCase.invoke(runId, _elapsedMillis.value)

            val remaining = splitRepository.getSplitsForRun(runId).count { it.timeFromStartMillis < 0 }
            if (remaining <= 0) {
                finishRunUseCase.invoke(runId)
                stopTicker()
                _isRunning.value = false
            }
        }
    }

    fun onPauseToggle() {
        viewModelScope.launch(Dispatchers.Default) {
            if (!_isRunning.value) return@launch
            if (_isPaused.value) {
                val now = SystemClock.elapsedRealtime()
                timerStartRealtime = now - _elapsedMillis.value
                _isPaused.value = false
                startTicker()
            } else {
                _isPaused.value = true
                stopTicker()
            }
        }
    }

    fun onReset() {
        viewModelScope.launch(Dispatchers.IO) {
            val runId = _currentRunId.value ?: return@launch
            resetRunUseCase.invoke(runId)
            stopTicker()
            _isRunning.value = false
            _isPaused.value = false
            _elapsedMillis.value = 0L
        }
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = viewModelScope.launch(Dispatchers.Default) {
            val now = SystemClock.elapsedRealtime()
            if (timerStartRealtime == 0L) timerStartRealtime = now
            else timerStartRealtime = now - _elapsedMillis.value

            while (_isRunning.value && !_isPaused.value) {
                val n = SystemClock.elapsedRealtime()
                _elapsedMillis.value = n - timerStartRealtime
                kotlinx.coroutines.delay(50)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
        timerStartRealtime = 0L
    }

    fun formatMillis(ms: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        val millis = ms % 1000
        return String.format("%02d:%02d.%03d", minutes, seconds, millis)
    }
}