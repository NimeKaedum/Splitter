package com.example.livesplitlike.ui.screens.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livesplitlike.data.local.db.GroupDao
import com.example.livesplitlike.data.local.db.SplitTemplateDao
import com.example.livesplitlike.data.local.model.GroupEntity
import com.example.livesplitlike.data.local.model.SplitTemplateEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val groupDao: GroupDao,
    private val splitTemplateDao: SplitTemplateDao
) : ViewModel() {

    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> = _groupName

    private val _splitNames = MutableStateFlow<List<String>>(emptyList())
    val splitNames: StateFlow<List<String>> = _splitNames

    // validations / errors
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun initDefaults(defaultCount: Int = 5) {
        if (_splitNames.value.isEmpty()) {
            _splitNames.value = List(defaultCount) { idx -> "Split ${idx + 1}" }
        }
    }

    fun setGroupName(name: String) { _groupName.value = name }

    fun setSplitName(index: Int, name: String) {
        val list = _splitNames.value.toMutableList()
        if (index in list.indices) {
            list[index] = name
            _splitNames.value = list
        }
    }

    fun setSplitCount(count: Int) {
        val safe = count.coerceIn(1, 50)
        val list = _splitNames.value.toMutableList()
        if (list.size < safe) {
            val start = list.size
            repeat(safe - list.size) { i -> list.add("Split ${start + i + 1}") }
        } else if (list.size > safe) {
            while (list.size > safe) list.removeAt(list.lastIndex)
        }
        _splitNames.value = list
    }

    /**
     * Create group and templates. Returns created groupId via callback.
     * Minimal validations: name not blank (fallback), at least 1 split.
     */
    fun createGroup(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            try {
                val name = _groupName.value.trim().ifEmpty { "Grupo" }
                val splits = _splitNames.value
                if (splits.isEmpty()) {
                    _error.value = "Debe haber al menos 1 split"
                    return@launch
                }
                val gid = groupDao.insertGroup(GroupEntity(name = name))
                splits.forEachIndexed { idx, s ->
                    val safeName = s.trim().ifEmpty { "Split ${idx + 1}" }
                    splitTemplateDao.insertTemplate(
                        SplitTemplateEntity(
                            groupId = gid,
                            indexInGroup = idx,
                            name = safeName
                        )
                    )
                }
                _error.value = null
                onCreated(gid)
            } catch (e: Exception) {
                _error.value = "Error al crear grupo"
            }
        }
    }
}