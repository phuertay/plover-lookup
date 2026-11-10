package com.phuertay.ploverlookup

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ImportUi {
    data object Idle : ImportUi
    data class Running(val name: String, val entries: Int) : ImportUi
    data class Finished(val name: String, val entries: Int) : ImportUi
    data class Failed(val message: String) : ImportUi
}

@OptIn(FlowPreview::class)
class LookupViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PloverLookupApp
    private val repository = app.repository
    private val contentResolver = application.contentResolver

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _results = MutableStateFlow(LookupResults.empty)
    val results: StateFlow<LookupResults> = _results

    private val _dictionaries = MutableStateFlow<List<Dictionary>>(emptyList())
    val dictionaries: StateFlow<List<Dictionary>> = _dictionaries

    private val _import = MutableStateFlow<ImportUi>(ImportUi.Idle)
    val importState: StateFlow<ImportUi> = _import

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.ensureSample()
                _dictionaries.value = repository.dictionaries()
            }
            _ready.value = true
            _query.debounce(120).collect { text ->
                _results.value = withContext(Dispatchers.IO) { repository.search(text) }
            }
        }
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setEnabled(id, enabled)
            _dictionaries.value = repository.dictionaries()
            _results.value = repository.search(_query.value)
        }
    }

    fun deleteImported(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteImported(id)
            _dictionaries.value = repository.dictionaries()
            _results.value = repository.search(_query.value)
        }
    }

    fun dismissImportMessage() {
        if (_import.value is ImportUi.Running) return
        _import.value = ImportUi.Idle
    }

    fun importUri(uri: Uri) {
        if (_import.value is ImportUi.Running) return
        _import.value = ImportUi.Running("Dictionary", 0)
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) { displayName(uri) }
            _import.value = ImportUi.Running(name, 0)
            try {
                val count = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { input ->
                        repository.importStream(name, input, builtIn = false) { entries ->
                            _import.value = ImportUi.Running(name, entries)
                        }
                    } ?: throw IllegalStateException("Could not open that file")
                }
                _import.value = ImportUi.Finished(name, count)
                withContext(Dispatchers.IO) {
                    _dictionaries.value = repository.dictionaries()
                    _results.value = repository.search(_query.value)
                }
            } catch (error: Exception) {
                val message = error.message?.takeIf { it.isNotBlank() } ?: "Import failed"
                _import.value = ImportUi.Failed(message)
            }
        }
    }

    private fun displayName(uri: Uri): String {
        val cursor = contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    val raw = it.getString(index)
                    if (!raw.isNullOrBlank()) {
                        return raw.removeSuffix(".json").removeSuffix(".JSON")
                    }
                }
            }
        }
        return "Dictionary"
    }
}
