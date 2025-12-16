package com.example.livesplitlike.ui.screens.timer

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livesplitlike.data.local.db.GroupDao
import com.example.livesplitlike.data.local.db.RunDao
import com.example.livesplitlike.data.local.db.RunTimeDao
import com.example.livesplitlike.data.local.db.SplitTemplateDao
import com.example.livesplitlike.data.local.model.RunEntity
import com.example.livesplitlike.data.local.model.RunTimeEntity
import com.example.livesplitlike.data.local.model.SplitTemplateEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * TimerViewModel final
 *
 * - Maneja plantillas (splits) por grupo, selección de grupo por id y nombre.
 * - Calcula BEST (run con menor total), SPLITBEST (mejor segmento por índice),
 *   diffs acumulados (CurrentCumulative - BestCumulative) y segmentDiffs (CurrentSegment - SplitBest).
 * - Persiste runs completas al finalizar y evita condiciones de carrera que provocaban diffs a 0.00
 *   o cambios de color inesperados.
 *
 * Notas clave:
 * - No se crean splits extra: todas las estructuras se dimensionan exactamente al tamaño de la plantilla.
 * - Se cancela el collector anterior de SPLITBEST al cambiar de grupo para evitar collectors duplicados.
 * - Después de persistir una run, se recalculan baseline y splitbest y se recomputan diffs sin limpiar
 *   indebidamente el estado actual.
 */

data class ComparisonItem(
    val indexInGroup: Int,
    val name: String,
    val currentMillis: Long?,   // cumulative timeFromStartMillis for this split (null if not recorded)
    val bestMillis: Long,       // baseline cumulative (0 if none)
    val diffMillis: Long?,      // cumulative diff: currentCumulative - bestCumulative
    val status: ComparisonStatus
)

enum class ComparisonStatus {
    NONE,
    GOLD,
    GAIN_GAINING,   // overall gain, segment losing
    GAIN_LOSING,    // overall gain, segment gaining
    LOSS_GAINING,   // overall loss, segment gaining
    LOSS_LOSING     // overall loss, segment losing
}

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val splitTemplateDao: SplitTemplateDao,
    private val runDao: RunDao,
    private val runTimeDao: RunTimeDao,
    private val groupDao: GroupDao
) : ViewModel() {

    // Config
    private val defaultGroupId: Long = 1L

    // UI state flows
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis: StateFlow<Long> = _elapsedMillis.asStateFlow()

    private val _templates = MutableStateFlow<List<SplitTemplateEntity>>(emptyList())
    val templates: StateFlow<List<SplitTemplateEntity>> = _templates.asStateFlow()

    private val _selectedGroupName = MutableStateFlow<String>("Sin grupo")
    val selectedGroupName: StateFlow<String> = _selectedGroupName.asStateFlow()

    // Best cumulative (baseline) from best run
    private val _bestCumulative = MutableStateFlow<List<Long>>(emptyList())
    val bestCumulative: StateFlow<List<Long>> = _bestCumulative.asStateFlow()

    // SPLITBEST: best segment durations per index across all runs (nullable if no data)
    private val _splitBestSegments = MutableStateFlow<List<Long?>>(emptyList())
    val splitBestSegments: StateFlow<List<Long?>> = _splitBestSegments.asStateFlow()

    // Current run cumulative times (in-memory while running or after finish)
    private val _currentCumulative = MutableStateFlow<List<Long?>>(emptyList())
    val currentCumulative: StateFlow<List<Long?>> = _currentCumulative.asStateFlow()

    // Cumulative diffs shown in UI: currentCumulative - bestCumulative
    private val _diffs = MutableStateFlow<List<Long?>>(emptyList())
    val diffs: StateFlow<List<Long?>> = _diffs.asStateFlow()

    // Per-segment diffs vs SPLITBEST (nullable)
    private val _segmentDiffs = MutableStateFlow<List<Long?>>(emptyList())
    private val segmentDiffs: StateFlow<List<Long?>> = _segmentDiffs.asStateFlow()

    // Flag: true if last run was completed and persisted
    private val _lastRunCompleted = MutableStateFlow(false)
    val lastRunCompleted: StateFlow<Boolean> = _lastRunCompleted.asStateFlow()

    // If group has at least one complete run (used to decide GOLD behavior)
    private val _hasAnyCompleteRun = MutableStateFlow(false)
    val hasAnyCompleteRun: StateFlow<Boolean> = _hasAnyCompleteRun.asStateFlow()

    // Pending best cumulative to apply after user starts next run (to avoid current==best race)
    private val _pendingBestCumulative = MutableStateFlow<List<Long>?>(null)
    private val pendingBestCumulative: StateFlow<List<Long>?> = _pendingBestCumulative.asStateFlow()

    // Combined items for UI
    val comparisonItemsFlow: Flow<List<ComparisonItem>> = combine(
        templates,
        bestCumulative,
        currentCumulative,
        diffs,
        splitBestSegments
    ) { tpl, best, cur, diffs, splitBest ->
        val size = tpl.size
        (0 until size).map { i ->
            val name = tpl.getOrNull(i)?.name ?: "Split ${i + 1}"
            val bestVal = best.getOrNull(i) ?: 0L
            val curVal = cur.getOrNull(i)
            val diff = diffs.getOrNull(i)
            val status = determineStatus(i, best, cur, diffs, splitBest)
            ComparisonItem(i, name, curVal, bestVal, diff, status)
        }
    }

    // Timer internals
    private var tickerJob: Job? = null
    private var timerStartRealtime: Long = 0L

    // Collector job for SPLITBEST; cancel when switching groups
    private var splitBestJob: Job? = null

    init {
        Log.d("SettingsGm", "TimerViewModel init hash=${this.hashCode()}")
        viewModelScope.launch(Dispatchers.IO) {
            // Try to select default group if exists
            val defaultGroup = groupDao.getGroupById(defaultGroupId)
            if (defaultGroup != null) {
                selectGroup(defaultGroupId)
            } else {
                // initialize empty safe structures
                loadTemplateSafe(emptyList())
            }
        }
    }

    // --- Loading template and group info ---

    private suspend fun loadTemplate(groupId: Long) {
        val tpl = splitTemplateDao.getTemplatesForGroup(groupId).sortedBy { it.indexInGroup }
        loadTemplateSafe(tpl)
    }

    private fun loadTemplateSafe(tpl: List<SplitTemplateEntity>) {
        _templates.value = tpl
        val size = tpl.size
        _bestCumulative.value = List(size) { 0L }
        _currentCumulative.value = List(size) { null }
        _diffs.value = List(size) { null }
        _splitBestSegments.value = List(size) { null }
        _segmentDiffs.value = List(size) { null }
        _hasAnyCompleteRun.value = false
        _lastRunCompleted.value = false
    }

    // Observe SPLITBEST for the selected group; cancel previous collector if any
    private fun observeSplitBestSegments(groupId: Long) {
        splitBestJob?.cancel()
        splitBestJob = viewModelScope.launch(Dispatchers.IO) {
            runTimeDao.getBestSegmentTimesFlow(groupId).collect { list ->
                val size = _templates.value.size
                val arr = MutableList<Long?>(size) { null }
                list.forEach { b ->
                    if (b.indexInGroup in 0 until size) arr[b.indexInGroup] = b.bestSegmentMillis
                }
                _splitBestSegments.value = arr
                // recompute diffs after splitBest changes
                recomputeDiffs()
            }
        }
    }

    // Recompute best baseline cumulative times from DB (only complete runs)
    private suspend fun recomputeBestBaseline(groupId: Long) {
        val templateSize = _templates.value.size
        val totals = runTimeDao.getTotalsByRun(groupId)
        if (totals.isEmpty()) {
            _bestCumulative.value = List(templateSize) { 0L }
            _hasAnyCompleteRun.value = false
            recomputeDiffs()
            return
        }

        val candidates = mutableListOf<Pair<Long, Long>>()
        for (t in totals) {
            val times = runTimeDao.getTimesForRun(t.runId, groupId)
            if (times.size >= templateSize) {
                val indices = times.map { it.splitIndex }.sorted()
                val full = indices.firstOrNull() == 0 && indices.lastOrNull() == templateSize - 1 && indices.size >= templateSize
                if (full) candidates.add(Pair(t.runId, t.total))
            }
        }

        if (candidates.isEmpty()) {
            _bestCumulative.value = List(templateSize) { 0L }
            _hasAnyCompleteRun.value = false
            recomputeDiffs()
            return
        }

        _hasAnyCompleteRun.value = true
        val bestPair = candidates.minByOrNull { it.second }!!
        val bestTimes = runTimeDao.getTimesForRun(bestPair.first, groupId).sortedBy { it.splitIndex }
        val cumulative = bestTimes.map { it.timeFromStartMillis }
        _bestCumulative.value = List(templateSize) { idx -> cumulative.getOrNull(idx) ?: 0L }
        recomputeDiffs()
    }

    // --- Diff computations ---

    // Convert cumulative list to per-segment durations (null preserved)
    private fun cumulativeToSegments(cumulative: List<Long?>): List<Long?> {
        if (cumulative.isEmpty()) return emptyList()
        val segments = MutableList<Long?>(cumulative.size) { null }
        for (i in cumulative.indices) {
            val cur = cumulative[i]
            val prev = if (i == 0) 0L else cumulative[i - 1] ?: 0L
            segments[i] = cur?.let { it - prev }
        }
        return segments
    }

    // Recompute diffs:
    // - cumulative diffs = currentCumulative - bestCumulative (what UI shows)
    // - segmentDiffs = currentSegment - splitBestSegment (used to decide gaining/losing)
    private fun recomputeDiffs() {
        val bestCum = _bestCumulative.value
        val curCum = _currentCumulative.value

        val curSeg = cumulativeToSegments(curCum)
        val size = maxOf(bestCum.size, curCum.size)
        val cumulativeDiffs = MutableList<Long?>(size) { null }
        for (i in 0 until size) {
            val curC = curCum.getOrNull(i)
            val bestC = bestCum.getOrNull(i) ?: 0L
            cumulativeDiffs[i] = curC?.let { it - bestC }
        }
        _diffs.value = cumulativeDiffs

        val splitBest = _splitBestSegments.value
        val segSize = maxOf(curSeg.size, splitBest.size)
        val segDiffs = MutableList<Long?>(segSize) { null }
        for (i in 0 until segSize) {
            val cSeg = curSeg.getOrNull(i)
            val sb = splitBest.getOrNull(i)
            segDiffs[i] = if (cSeg != null && sb != null) (cSeg - sb) else null
        }
        _segmentDiffs.value = segDiffs
    }

    // Determine status for UI coloring
    private fun determineStatus(
        idx: Int,
        best: List<Long>,
        current: List<Long?>,
        diffsList: List<Long?>,
        splitBest: List<Long?>
    ): ComparisonStatus {
        val curSeg = cumulativeToSegments(current).getOrNull(idx)
        val cumulativeDiff = diffsList.getOrNull(idx)
        val segmentDiff = _segmentDiffs.value.getOrNull(idx)
        val splitBestVal = splitBest.getOrNull(idx)

        // GOLD priority:
        // - If there are no complete runs in the group, first run should show GOLD for recorded splits.
        // - Otherwise require splitBestVal != null and curSeg <= splitBestVal.
        if (curSeg != null) {
            if (!_hasAnyCompleteRun.value) return ComparisonStatus.GOLD
            if (splitBestVal != null && curSeg <= splitBestVal) return ComparisonStatus.GOLD
        }

        if (cumulativeDiff == null) return ComparisonStatus.NONE

        val overallIsGain = cumulativeDiff < 0
        val segmentIsGain = (segmentDiff != null && segmentDiff < 0)

        return when {
            overallIsGain && segmentIsGain -> ComparisonStatus.GAIN_LOSING
            overallIsGain && !segmentIsGain -> ComparisonStatus.GAIN_GAINING
            !overallIsGain && segmentIsGain -> ComparisonStatus.LOSS_GAINING
            else -> ComparisonStatus.LOSS_LOSING
        }
    }

    // --- Run lifecycle: start, record split, finalize ---

    private fun startInMemoryRun() {
        // Start a fresh in-memory run: keep arrays sized to template
        val size = _templates.value.size
        _currentCumulative.value = List(size) { null }
        _diffs.value = List(size) { null }
        _segmentDiffs.value = List(size) { null }
        _elapsedMillis.value = 0L
        timerStartRealtime = 0L
        _lastRunCompleted.value = false
    }

    private suspend fun finalizeRunAndPersist(groupId: Long) = withContext(Dispatchers.IO) {
        val cumulative = _currentCumulative.value
        if (cumulative.isEmpty()) return@withContext

        val runId = runDao.insertRun(RunEntity(groupId = groupId))
        for (i in cumulative.indices) {
            val time = cumulative[i] ?: 0L
            val rt = RunTimeEntity(
                runId = runId,
                groupId = groupId,
                splitIndex = i,
                timeFromStartMillis = time,
                recordedAtMillis = System.currentTimeMillis()
            )
            runTimeDao.insertRunTime(rt)
        }

        // Decide new BEST but do NOT apply it immediately if it is the newly inserted run.
        // Compute candidate best run (same logic as recomputeBestBaseline but we check if new run is best)
        val templateSize = _templates.value.size
        val totals = runTimeDao.getTotalsByRun(groupId)
        val candidates = mutableListOf<Pair<Long, Long>>()
        for (t in totals) {
            val times = runTimeDao.getTimesForRun(t.runId, groupId)
            if (times.size >= templateSize) {
                val indices = times.map { it.splitIndex }.sorted()
                val full = indices.firstOrNull() == 0 && indices.lastOrNull() == templateSize - 1 && indices.size >= templateSize
                if (full) candidates.add(Pair(t.runId, t.total))
            }
        }

        if (candidates.isEmpty()) {
            // no complete runs -> clear best
            _bestCumulative.value = List(templateSize) { 0L }
            _hasAnyCompleteRun.value = false
            // recompute diffs against current best (unchanged or zero)
            recomputeDiffs()
            return@withContext
        }

        val bestPair = candidates.minByOrNull { it.second }!!
        val bestTimes = runTimeDao.getTimesForRun(bestPair.first, groupId).sortedBy { it.splitIndex }
        val bestCumulativeCandidate = List(templateSize) { idx -> bestTimes.getOrNull(idx)?.timeFromStartMillis ?: 0L }

        // If the best run is the one we just inserted, store it in pending and DO NOT apply now.
        if (bestPair.first == runId) {
            _pendingBestCumulative.value = bestCumulativeCandidate
            _hasAnyCompleteRun.value = true
            // Do NOT set _bestCumulative yet; keep previous baseline so diffs don't become zero.
            // But ensure splitBest query will pick up new run (observer will update splitBestSegments).
            // Recompute diffs to keep UI consistent (diffs remain vs previous best)
            recomputeDiffs()
        } else {
            // Another run remains best: apply immediately
            _bestCumulative.value = bestCumulativeCandidate
            _hasAnyCompleteRun.value = true
            recomputeDiffs()
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("SettingsGm", "TimerViewModel onCleared hash=${this.hashCode()}")
    }

    // Public: user tapped the timer
    fun onTimerClicked() {
        Log.d("SettingsGm", "onTimerClicked invoked in TimerViewModel; isRunning=${_isRunning.value}")
        viewModelScope.launch(Dispatchers.IO) {
            val groupId = _templates.value.firstOrNull()?.groupId ?: defaultGroupId

            // If no template -> ignore
            if (_templates.value.isEmpty()) return@launch

            // Start new in-memory run if none running and last run not just completed
            if (!_isRunning.value && !_lastRunCompleted.value) {
                startInMemoryRun()
                _isRunning.value = true
                _isPaused.value = false
                startTicker()
                return@launch
            }

            // Resume from pause
            if (_isPaused.value) {
                val now = SystemClock.elapsedRealtime()
                timerStartRealtime = now - _elapsedMillis.value
                _isPaused.value = false
                startTicker()
                return@launch
            }

            // If running -> record next split
            if (_isRunning.value) {
                val currentElapsed = _elapsedMillis.value
                val nextIndex = _currentCumulative.value.indexOfFirst { it == null }.let { if (it < 0) _templates.value.size else it }

                // If nextIndex >= template size -> finalize run
                if (nextIndex >= _templates.value.size) {
                    // finalize and persist
                    finalizeRunAndPersist(groupId)
                    stopTicker()
                    _isRunning.value = false
                    _lastRunCompleted.value = true
                    return@launch
                }

                // record in-memory
                val newCurrent = _currentCumulative.value.toMutableList()
                if (nextIndex >= newCurrent.size) {
                    repeat(nextIndex - newCurrent.size + 1) { newCurrent.add(null) }
                }
                newCurrent[nextIndex] = currentElapsed
                _currentCumulative.value = newCurrent

                // recompute diffs after recording
                recomputeDiffs()

                // if run completed -> finalize
                val remaining = _currentCumulative.value.count { it == null }
                if (remaining == 0) {
                    finalizeRunAndPersist(groupId)
                    stopTicker()
                    _isRunning.value = false
                    _lastRunCompleted.value = true
                }
                return@launch
            }

            // If not running but last run was completed -> start a new run immediately
            // If not running but last run was completed (user pressed timer after finishing)
            if (!_isRunning.value && _lastRunCompleted.value) {
                // If we have a pending best (the run we just persisted was the new best), apply it now
                val pending = _pendingBestCumulative.value
                if (pending != null) {
                    _bestCumulative.value = pending
                    _pendingBestCumulative.value = null
                    // recompute diffs now that baseline changed (current will be reset immediately)
                    recomputeDiffs()
                } else {
                    // otherwise recompute baseline from DB to be safe
                    recomputeBestBaseline(groupId)
                }

                // reset in-memory and elapsed BEFORE starting ticker
                startInMemoryRun()
                _isRunning.value = true
                _isPaused.value = false
                startTicker()
                _lastRunCompleted.value = false
                return@launch
            }
        }
    }

    // Pause / resume
    fun onPauseToggle() {
        viewModelScope.launch {
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

    // Reset: pause and show baseline (do not modify DB)
    fun onReset() {
        viewModelScope.launch(Dispatchers.IO) {
            val groupId = _templates.value.firstOrNull()?.groupId ?: defaultGroupId
            recomputeBestBaseline(groupId)
            // keep current run in-memory cleared (ignore partials)
            val size = _templates.value.size
            _currentCumulative.value = List(size) { null }
            _diffs.value = List(size) { null }
            _segmentDiffs.value = List(size) { null }
            stopTicker()
            _isRunning.value = false
            _isPaused.value = false
            _elapsedMillis.value = 0L
            _lastRunCompleted.value = false
        }
    }

    // Start ticker robustly
    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = null

        val now = SystemClock.elapsedRealtime()
        timerStartRealtime = now - _elapsedMillis.value

        tickerJob = viewModelScope.launch(Dispatchers.Default) {
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
        // do not reset elapsedMillis here; caller decides
        timerStartRealtime = 0L
    }

    // Select group: load template, group name, start observing splitbest and recompute baseline
    fun selectGroup(groupId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val g = groupDao.getGroupById(groupId)
            _selectedGroupName.value = g?.name ?: "Grupo $groupId"
            loadTemplate(groupId)
            observeSplitBestSegments(groupId)
            recomputeBestBaseline(groupId)
            // keep current cleared so UI shows baseline until user starts
            resetCurrentToBaseline()
        }
    }

    // Reset current to baseline (keeps sizes exact)
    fun resetCurrentToBaseline() {
        val size = _templates.value.size
        _currentCumulative.value = List(size) { null }
        _diffs.value = List(size) { null }
        _segmentDiffs.value = List(size) { null }
        _lastRunCompleted.value = false
    }

    // Formatting helpers
    fun formatMillis(ms: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        val millis = ms % 1000
        return String.format(Locale.US, "%02d:%02d.%03d", minutes, seconds, millis)
    }

    fun formatDiffMillis(diffMillis: Long?): String? {
        if (diffMillis == null) return null
        val seconds = diffMillis.toDouble() / 1000.0
        return String.format(Locale.US, "%+.2f", seconds)
    }

    // Color helper (returns ARGB as Long)
    fun statusToColor(status: ComparisonStatus): Long {
        return when (status) {
            ComparisonStatus.GOLD -> 0xFFFFD700
            ComparisonStatus.LOSS_LOSING -> 0xFFF44336
            ComparisonStatus.LOSS_GAINING -> 0xFFB71C1C
            ComparisonStatus.GAIN_LOSING -> 0xFF2E7D32
            ComparisonStatus.GAIN_GAINING -> 0xFF4CAF50
            ComparisonStatus.NONE -> 0xFF9E9E9E
        }
    }
}