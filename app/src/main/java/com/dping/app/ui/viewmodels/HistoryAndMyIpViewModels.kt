package com.dping.app.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dping.app.data.model.DiagnosticRecord
import com.dping.app.data.repository.DiagnosticRepository
import com.dping.app.network.discovery.MyIpInfo
import com.dping.app.network.discovery.NetworkInfoProvider
import com.dping.app.utils.ExportManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HistoryUiState(
    val records: List<DiagnosticRecord> = emptyList(),
    val filteredRecords: List<DiagnosticRecord> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: String = "ALL", // "ALL", "PING", "SPEED", "TRACEROUTE"
    val isLoading: Boolean = false,
    val newestFirst: Boolean = true
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val repository: DiagnosticRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _newestFirst = MutableStateFlow(true)
    private val _selectedFilter = MutableStateFlow("ALL")

    val uiState: StateFlow<HistoryUiState> = combine(
        _searchQuery,
        _selectedFilter,
        repository.preferences.newestFirstFlow
    ) { query, filter, prefNewestFirst ->
        Triple(query, filter, prefNewestFirst)
    }.flatMapLatest { (query, filter, newestFirst) ->
        val flow = if (query.isBlank()) {
            repository.getHistory(newestFirst)
        } else {
            repository.searchHistory(query)
        }
        flow.map { records ->
            val filtered = if (filter == "ALL") {
                records
            } else {
                records.filter { rec ->
                    when (filter) {
                        "PING" -> rec.testType.contains("PING", ignoreCase = true)
                        "SPEED" -> rec.testType.contains("SPEED", ignoreCase = true)
                        "TRACEROUTE" -> rec.testType.contains("TRACE", ignoreCase = true)
                        else -> true
                    }
                }
            }
            HistoryUiState(
                records = records,
                filteredRecords = filtered,
                searchQuery = query,
                selectedFilter = filter,
                newestFirst = newestFirst
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterChange(filter: String) {
        _selectedFilter.value = filter
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteRecord(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun exportToCsv(context: Context) {
        val records = uiState.value.filteredRecords.ifEmpty { uiState.value.records }
        if (records.isNotEmpty()) {
            val csv = ExportManager.exportHistoryToCsv(records)
            ExportManager.shareCsvFile(context, csv)
        }
    }

    fun exportToJson(context: Context) {
        val records = uiState.value.filteredRecords.ifEmpty { uiState.value.records }
        if (records.isNotEmpty()) {
            val json = ExportManager.exportHistoryToJson(records)
            ExportManager.shareJsonFile(context, json)
        }
    }
}

data class MyIpUiState(
    val info: MyIpInfo = MyIpInfo(),
    val isLoading: Boolean = false,
    val selectedTab: String = "IP"
)

class MyIpViewModel(
    private val context: Context
) : ViewModel() {

    private val networkInfoProvider = NetworkInfoProvider(context)
    private val _uiState = MutableStateFlow(MyIpUiState(isLoading = true))
    val uiState: StateFlow<MyIpUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun selectTab(tab: String) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val ipInfo = networkInfoProvider.getMyIpInfo()
            _uiState.update { it.copy(info = ipInfo, isLoading = false) }
        }
    }
}
