package com.example.livesplitlike.ui.screens.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livesplitlike.data.local.db.GroupDao
import com.example.livesplitlike.data.local.model.GroupEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val groupDao: GroupDao
) : ViewModel() {

    val groups = groupDao.getAllGroupsFlow()
        .map { it.sortedBy { g -> g.id } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun deleteGroup(group: GroupEntity) {
        viewModelScope.launch {
            try {
                groupDao.deleteGroup(group)
            } catch (_: Exception) {
                // swallow to avoid crash in UI; you can log if needed
            }
        }
    }
}