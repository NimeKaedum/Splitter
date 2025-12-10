package com.example.livesplitlike.ui.screens.groups

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * ViewModel para la pantalla de Runs de un grupo.
 *
 * Exposiciones:
 * - groupName: nombre del grupo
 * - templates: lista de SplitTemplateEntity (ordenada)
 * - bestCumulative: cumulativos de la run BEST (por total)
 * - splitBestSegments: mejor segmento por índice (SPLITBEST)
 * - theoreticalBestCumulative: cumulativos construidos a partir de splitBestSegments
 * - runsWithTimes: lista de pares (RunEntity, List<RunTimeEntity>) ordenada por runId descendente (más reciente arriba)
 */
@HiltViewModel
class ViewRunsViewModel @Inject constructor(
    private val runDao: RunDao,
    private val runTimeDao: RunTimeDao,
    private val splitTemplateDao: SplitTemplateDao,
    private val groupDao: GroupDao
) : ViewModel() {

    private val _groupName = MutableStateFlow("Grupo")
    val groupName: StateFlow<String> = _groupName.asStateFlow()

    private val _templates = MutableStateFlow<List<SplitTemplateEntity>>(emptyList())
    val templates: StateFlow<List<SplitTemplateEntity>> = _templates.asStateFlow()

    private val _bestCumulative = MutableStateFlow<List<Long>>(emptyList())
    val bestCumulative: StateFlow<List<Long>> = _bestCumulative.asStateFlow()

    private val _splitBestSegments = MutableStateFlow<List<Long?>>(emptyList())
    val splitBestSegments: StateFlow<List<Long?>> = _splitBestSegments.asStateFlow()

    private val _theoreticalBestCumulative = MutableStateFlow<List<Long>>(emptyList())
    val theoreticalBestCumulative: StateFlow<List<Long>> = _theoreticalBestCumulative.asStateFlow()

    // List of runs with their times (each list ordered by splitIndex asc)
    private val _runsWithTimes = MutableStateFlow<List<Pair<RunEntity, List<RunTimeEntity>>>>(emptyList())
    val runsWithTimes: StateFlow<List<Pair<RunEntity, List<RunTimeEntity>>>> = _runsWithTimes.asStateFlow()

    /**
     * Carga toda la información necesaria para mostrar la tabla de runs.
     * groupId: id del grupo a mostrar
     * groupName: nombre para mostrar en la cabecera
     */
    fun loadGroupRuns(groupId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val group = groupDao.getGroupById(groupId)
            _groupName.value = group?.name ?: "Grupo $groupId"

            val tpl = splitTemplateDao.getTemplatesForGroup(groupId).sortedBy { it.indexInGroup }
            _templates.value = tpl

            // BEST
            recomputeBestBaseline(groupId, tpl.size)

            // SPLITBEST
            val bestSegList = runTimeDao.getBestSegmentTimesOnce(groupId)
            val splitBestArr = MutableList<Long?>(tpl.size) { null }
            bestSegList.forEach { b ->
                if (b.indexInGroup in 0 until tpl.size) splitBestArr[b.indexInGroup] = b.bestSegmentMillis
            }
            _splitBestSegments.value = splitBestArr

            // Teórica
            _theoreticalBestCumulative.value = computeTheoreticalBestCumulative(splitBestArr)

            // Runs (más recientes primero)
            val totals = runTimeDao.getTotalsByRun(groupId).sortedByDescending { it.runId }
            val runsPairs = mutableListOf<Pair<RunEntity, List<RunTimeEntity>>>()
            for (t in totals) {
                val run = runDao.getRunById(t.runId)
                if (run != null) {
                    val times = runTimeDao.getTimesForRun(t.runId, groupId).sortedBy { it.splitIndex }
                    runsPairs.add(run to times)
                }
            }
            _runsWithTimes.value = runsPairs
        }
    }

    private suspend fun recomputeBestBaseline(groupId: Long, templateSize: Int) = withContext(Dispatchers.IO) {
        val totals = runTimeDao.getTotalsByRun(groupId)
        if (totals.isEmpty()) {
            _bestCumulative.value = List(templateSize) { 0L }
            return@withContext
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
            return@withContext
        }

        val bestPair = candidates.minByOrNull { it.second }!!
        val bestTimes = runTimeDao.getTimesForRun(bestPair.first, groupId).sortedBy { it.splitIndex }
        val cumulative = bestTimes.map { it.timeFromStartMillis }
        _bestCumulative.value = List(templateSize) { idx -> cumulative.getOrNull(idx) ?: 0L }
    }

    private fun computeTheoreticalBestCumulative(splitBest: List<Long?>): List<Long> {
        val cumul = mutableListOf<Long>()
        var acc = 0L
        for (seg in splitBest) {
            val segVal = seg ?: 0L
            acc += segVal
            cumul.add(acc)
        }
        return cumul
    }

    // Helpers para formatear tiempos (reutilizables en UI)
    fun formatMillis(ms: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        val millis = ms % 1000
        return String.format(Locale.US, "%02d:%02d.%03d", minutes, seconds, millis)
    }


    fun buildPbShareMessage(): String {
        val tpl = templates.value
        val best = bestCumulative.value
        val sb = StringBuilder()
        sb.appendLine("PB - ${groupName.value}")
        tpl.forEachIndexed { i, split ->
            val t = best.getOrNull(i) ?: 0L
            sb.appendLine("${split.name}: ${formatMillis(t)}")
        }
        return sb.toString().trim()
    }

    fun buildBestPossibleShareMessage(): String {
        val tpl = templates.value
        val theoretical = theoreticalBestCumulative.value
        val sb = StringBuilder()
        sb.appendLine("Best Possible Time - ${groupName.value}")
        tpl.forEachIndexed { i, split ->
            val t = theoretical.getOrNull(i) ?: 0L
            sb.appendLine("${split.name}: ${formatMillis(t)}")
        }
        return sb.toString().trim()
    }

    fun buildRunShareMessage(runIndex: Int): String {
        val tpl = templates.value
        val pairs = runsWithTimes.value
        if (runIndex !in pairs.indices) return "Run inválida"
        val (run, times) = pairs[runIndex]
        val ordered = times.sortedBy { it.splitIndex }
        val sb = StringBuilder()
        sb.appendLine("Run ${run.id} - ${groupName.value}")
        tpl.forEachIndexed { i, split ->
            val t = ordered.getOrNull(i)?.timeFromStartMillis ?: 0L
            sb.appendLine("${split.name}: ${formatMillis(t)}")
        }
        return sb.toString().trim()
    }
}