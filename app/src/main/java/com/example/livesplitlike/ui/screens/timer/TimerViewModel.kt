package com.example.livesplitlike.ui.screens.timer

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livesplitlike.data.local.db.RunDao
import com.example.livesplitlike.data.local.db.RunTimeDao
import com.example.livesplitlike.data.local.db.SplitTemplateDao
import com.example.livesplitlike.data.local.model.RunEntity
import com.example.livesplitlike.data.local.model.RunTimeEntity
import com.example.livesplitlike.data.local.model.SplitTemplateEntity
import com.example.livesplitlike.data.local.model.BestSegmentLocal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject


data class ComparisonItem(
    val indexInGroup: Int,
    val name: String,
    val currentMillis: Long?,   // cumulative timeFromStartMillis for this split (null if not recorded)
    val bestMillis: Long,       // baseline cumulative (0 if none)
    val diffMillis: Long?,      // currentSegment - bestSegment (null if no current)
    val status: ComparisonStatus
)

// Reemplaza la enum actual por esta
enum class ComparisonStatus {
    NONE,
    GOLD,           // cumple SPLITBEST (prioridad)
    GAIN_GAINING,   // current < best (acumulado), pero segmentDiff > 0 (perdiendo en este split)
    GAIN_LOSING,    // current < best (acumulado), segmentDiff < 0 (ganando en este split)
    LOSS_GAINING,   // current > best (acumulado), segmentDiff < 0 (ganando en este split aunque global pierda)
    LOSS_LOSING     // current > best (acumulado), segmentDiff > 0 (pierde y además pierde en este split)
}

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val splitTemplateDao: SplitTemplateDao,
    private val runDao: RunDao,
    private val runTimeDao: RunTimeDao
) : ViewModel() {

    private val defaultGroupId: Long = 1L

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis: StateFlow<Long> = _elapsedMillis.asStateFlow()

    private val _templates = MutableStateFlow<List<SplitTemplateEntity>>(emptyList())
    val templates: StateFlow<List<SplitTemplateEntity>> = _templates.asStateFlow()

    // Baseline cumulative times (best run complete)
    private val _bestCumulative = MutableStateFlow<List<Long>>(emptyList())
    val bestCumulative: StateFlow<List<Long>> = _bestCumulative.asStateFlow()

    // Best segment per index across all runs (SPLITBEST) -> segment durations (not cumulative)
    private val _splitBestSegments = MutableStateFlow<List<Long?>>(emptyList())
    val splitBestSegments: StateFlow<List<Long?>> = _splitBestSegments.asStateFlow()

    private val _currentCumulative = MutableStateFlow<List<Long?>>(emptyList())
    val currentCumulative: StateFlow<List<Long?>> = _currentCumulative.asStateFlow()

    private val _diffs = MutableStateFlow<List<Long?>>(emptyList())
    val diffs: StateFlow<List<Long?>> = _diffs.asStateFlow()

    private val _lastRunCompleted = MutableStateFlow(false)
    val lastRunCompleted: StateFlow<Boolean> = _lastRunCompleted.asStateFlow()

    // per-segment diffs vs SPLITBEST (nullable)
    private val _segmentDiffs = MutableStateFlow<List<Long?>>(emptyList())
    private val segmentDiffs: StateFlow<List<Long?>> = _segmentDiffs.asStateFlow()

    val comparisonItemsFlow: Flow<List<ComparisonItem>> = combine(
        templates,
        bestCumulative,
        currentCumulative,
        diffs,
        splitBestSegments
    ) { tpl, best, cur, diffs, splitBest ->
        val count = maxOf(tpl.size, best.size, cur.size, diffs.size, splitBest.size)
        (0 until count).map { i ->
            val name = tpl.getOrNull(i)?.name ?: "Split ${i + 1}"
            val bestVal = best.getOrNull(i) ?: 0L
            val curVal = cur.getOrNull(i)
            val diff = diffs.getOrNull(i)
            val status = determineStatus(i, best, cur, diffs, splitBest)
            ComparisonItem(i, name, curVal, bestVal, diff, status)
        }
    }

    private var tickerJob: Job? = null
    private var timerStartRealtime: Long = 0L

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadTemplate(defaultGroupId)
            observeSplitBestSegments(defaultGroupId)
            recomputeBestBaseline(defaultGroupId)
            resetCurrentToBaseline()
        }
    }

    private suspend fun loadTemplate(groupId: Long) {
        val tpl = splitTemplateDao.getTemplatesForGroup(groupId).sortedBy { it.indexInGroup }
        _templates.value = tpl
        val size = tpl.size
        _bestCumulative.value = List(size) { 0L }
        _currentCumulative.value = List(size) { null }
        _diffs.value = List(size) { null }
        _splitBestSegments.value = List(size) { null }
        _splitBestSegments.value = List(size) { null }
        _segmentDiffs.value = List(size) { null }
    }

    // Observa SPLITBEST (mejor segmento por índice) de forma reactiva
    private fun observeSplitBestSegments(groupId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            runTimeDao.getBestSegmentTimesFlow(groupId).collect { list ->
                // list: List<BestSegmentLocal(indexInGroup, bestSegmentMillis)>
                val size = maxOf(_templates.value.size, list.size)
                val arr = MutableList<Long?>(size) { null }
                list.forEach { b ->
                    if (b.indexInGroup >= 0 && b.indexInGroup < size) arr[b.indexInGroup] = b.bestSegmentMillis
                }
                _splitBestSegments.value = arr
            }
        }
    }

    private suspend fun recomputeBestBaseline(groupId: Long) {
        val templateSize = _templates.value.size
        val totals = runTimeDao.getTotalsByRun(groupId)
        if (totals.isEmpty()) {
            _bestCumulative.value = List(templateSize) { 0L }
            return
        }

        val candidates = mutableListOf<Pair<Long, Long>>()
        for (t in totals) {
            val times = runTimeDao.getTimesForRun(t.runId, groupId)
            if (times.size >= templateSize) {
                val indices = times.map { it.splitIndex }.sorted()
                val full = indices.size >= templateSize && indices.firstOrNull() == 0 && indices.lastOrNull() == templateSize - 1
                if (full) candidates.add(Pair(t.runId, t.total))
            }
        }

        if (candidates.isEmpty()) {
            _bestCumulative.value = List(templateSize) { 0L }
            return
        }

        val bestPair = candidates.minByOrNull { it.second }!!
        val bestTimes = runTimeDao.getTimesForRun(bestPair.first, groupId).sortedBy { it.splitIndex }
        val cumulative = bestTimes.map { it.timeFromStartMillis }
        val size = maxOf(templateSize, cumulative.size)
        _bestCumulative.value = MutableList(size) { idx -> cumulative.getOrNull(idx) ?: 0L }
    }

    private fun resetCurrentToBaseline() {
        val size = _templates.value.size
        _currentCumulative.value = List(size) { null }
        _diffs.value = List(size) { null }
        _lastRunCompleted.value = false
    }

    fun onTimerClicked() {
        viewModelScope.launch(Dispatchers.IO) {
            val groupId = _templates.value.firstOrNull()?.groupId ?: defaultGroupId

            if (!_isRunning.value && !_lastRunCompleted.value) {
                startInMemoryRun()
                startTicker()
                _isRunning.value = true
                _isPaused.value = false
                return@launch
            }

            if (_isPaused.value) {
                val now = SystemClock.elapsedRealtime()
                timerStartRealtime = now - _elapsedMillis.value
                _isPaused.value = false
                startTicker()
                return@launch
            }

            if (_isRunning.value) {
                val currentElapsed = _elapsedMillis.value
                val nextIndex = _currentCumulative.value.indexOfFirst { it == null }.let { if (it < 0) _templates.value.size else it }

                if (nextIndex >= _templates.value.size) {
                    finalizeRunAndPersist(groupId)
                    stopTicker()
                    _isRunning.value = false
                    _lastRunCompleted.value = true
                    return@launch
                }

                val newCurrent = _currentCumulative.value.toMutableList()
                if (nextIndex >= newCurrent.size) {
                    repeat(nextIndex - newCurrent.size + 1) { newCurrent.add(null) }
                }
                newCurrent[nextIndex] = currentElapsed
                _currentCumulative.value = newCurrent

                recomputeDiffs()

                val remaining = _currentCumulative.value.count { it == null }
                if (remaining == 0) {
                    finalizeRunAndPersist(groupId)
                    stopTicker()
                    _isRunning.value = false
                    _lastRunCompleted.value = true
                }
                return@launch
            }

            if (!_isRunning.value && _lastRunCompleted.value) {
                // recompute baseline (includes the just persisted run)
                recomputeBestBaseline(groupId)

                // reset in-memory and elapsed BEFORE starting ticker
                startInMemoryRun()
                _elapsedMillis.value = 0L

                // start ticker robustly
                _isRunning.value = true
                _isPaused.value = false
                startTicker()

                _lastRunCompleted.value = false
                return@launch
            }
        }
    }

    private fun startInMemoryRun() {
        resetCurrentToBaseline()
        _elapsedMillis.value = 0L
        timerStartRealtime = 0L
    }

    private suspend fun finalizeRunAndPersist(groupId: Long) = withContext(Dispatchers.IO) {
        val runId = runDao.insertRun(RunEntity(groupId = groupId))
        val cumulative = _currentCumulative.value
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
        recomputeBestBaseline(groupId)
    }

    private fun cumulativeToSegments(cumulative: List<Long?>): List<Long?> {
        if (cumulative.isEmpty()) return emptyList()
        val segments = mutableListOf<Long?>()
        for (i in cumulative.indices) {
            val cur = cumulative[i]
            val prev = if (i == 0) 0L else cumulative[i - 1] ?: 0L
            segments.add(cur?.let { it - prev })
        }
        return segments
    }

    // Reemplaza recomputeDiffs() por esta versión que calcula diff acumulado (Current - Best)
    private fun recomputeDiffs() {
        // best cumulative -> best per-segment durations (from the best run)
        val bestCum = _bestCumulative.value
        val bestSeg = cumulativeToSegments(bestCum.map { it }) // List<Long?> where each is segment millis (or null -> treated as 0)

        // current cumulative -> current per-segment durations
        val curCum = _currentCumulative.value
        val curSeg = cumulativeToSegments(curCum) // List<Long?> (null if not recorded)

        val size = maxOf(bestCum.size, curCum.size)
        val cumulativeDiffs = MutableList<Long?>(size) { null } // acumulado: currentCumulative - bestCumulative
        val segmentDiffs = MutableList<Long?>(size) { null }    // por-segmento: currentSegment - splitBestSegment (ya calculado en _splitBestSegments)

        // compute cumulative diffs
        for (i in 0 until size) {
            val curC = curCum.getOrNull(i)
            val bestC = bestCum.getOrNull(i) ?: 0L
            cumulativeDiffs[i] = curC?.let { it - bestC }
        }

        // compute segment diffs vs SPLITBEST (we keep them in a separate flow if needed)
        val splitBest = _splitBestSegments.value
        val curSegList = curSeg
        for (i in 0 until size) {
            val cSeg = curSegList.getOrNull(i)
            val sb = splitBest.getOrNull(i)
            segmentDiffs[i] = cSeg?.let { if (sb != null) it - sb else null }
        }

        // store cumulative diffs in _diffs (this is what UI shows as Diff = Current-Best acumulado)
        _diffs.value = cumulativeDiffs

        // store segment diffs in a separate state so determineStatus can use them (we'll keep a private flow)
        _segmentDiffs.value = segmentDiffs
    }

    // Reemplaza determineStatus(...) por esta versión que usa diff acumulado y segmentDiff
    private fun determineStatus(
        idx: Int,
        best: List<Long>,
        current: List<Long?>,
        diffsList: List<Long?>,
        splitBest: List<Long?>
    ): ComparisonStatus {
        // current cumulative segment duration and per-segment duration
        val curSeg = cumulativeToSegments(current).getOrNull(idx) // per-segment duration for current
        val cumulativeDiff = diffsList.getOrNull(idx) // currentCumulative - bestCumulative (nullable)
        val segmentDiff = _segmentDiffs.value.getOrNull(idx) // currentSegment - splitBestSegment (nullable)

        // GOLD priority: if current exists and is <= SPLITBEST (or SPLITBEST null -> first run)
        val splitBestVal = splitBest.getOrNull(idx)
        if (curSeg != null) {
            if (splitBestVal == null || (segmentDiff != null && curSeg <= splitBestVal)) {
                return ComparisonStatus.GOLD
            }
        }

        // If no current cumulative diff, no status
        if (cumulativeDiff == null) return ComparisonStatus.NONE

        // Signs: negative cumulativeDiff => current < best (GAIN overall), positive => LOSS overall
        val overallIsGain = cumulativeDiff < 0
        val segmentIsGain = segmentDiff != null && segmentDiff < 0

        return when {
            overallIsGain && segmentIsGain -> ComparisonStatus.GAIN_LOSING /* actually both gain: overall gain, segment gain */
            overallIsGain && !segmentIsGain -> ComparisonStatus.GAIN_GAINING /* overall gain, segment losing */
            !overallIsGain && segmentIsGain -> ComparisonStatus.LOSS_GAINING /* overall loss, segment gain */
            else -> ComparisonStatus.LOSS_LOSING
        }
    }

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

    fun onReset() {
        viewModelScope.launch(Dispatchers.IO) {
            val groupId = _templates.value.firstOrNull()?.groupId ?: defaultGroupId
            recomputeBestBaseline(groupId)
            resetCurrentToBaseline()
            stopTicker()
            _isRunning.value = false
            _isPaused.value = false
            _elapsedMillis.value = 0L
            _lastRunCompleted.value = false
        }
    }

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
        timerStartRealtime = 0L
    }

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

    /**
     * Construye la run teórica más eficiente (cumulativos) a partir de SPLITBEST (segment durations).
     * Retorna lista de cumulativos (index 0 = first split cumulative).
     */
    fun computeTheoreticalBestCumulative(): List<Long> {
        val splitBest = _splitBestSegments.value
        if (splitBest.isEmpty()) return emptyList()
        val cumul = mutableListOf<Long>()
        var acc = 0L
        for (seg in splitBest) {
            val segVal = seg ?: 0L
            acc += segVal
            cumul.add(acc)
        }
        return cumul
    }

}