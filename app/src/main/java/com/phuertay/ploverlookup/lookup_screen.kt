package com.phuertay.ploverlookup

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.NumberFormat
import java.util.Locale

private enum class LookupPage {
    Search,
    Dictionaries,
}

@Composable
fun LookupApp(viewModel: LookupViewModel) {
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    var page by remember { mutableStateOf(LookupPage.Search) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.importUri(uri)
    }
    BackHandler(enabled = page == LookupPage.Dictionaries) {
        page = LookupPage.Search
    }
    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
            ) {
                ImportBanner(importState, onDismiss = viewModel::dismissImportMessage)
                if (!ready) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else when (page) {
                    LookupPage.Search -> LookupScreen(
                        viewModel = viewModel,
                        onOpenDictionaries = { page = LookupPage.Dictionaries },
                        modifier = Modifier.weight(1f),
                    )
                    LookupPage.Dictionaries -> DictionariesScreen(
                        viewModel = viewModel,
                        onBack = { page = LookupPage.Search },
                        modifier = Modifier.weight(1f),
                        onImport = {
                            launcher.launch(
                                arrayOf(
                                    "application/json",
                                    "text/plain",
                                    "application/octet-stream",
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportBanner(state: ImportUi, onDismiss: () -> Unit) {
    when (state) {
        ImportUi.Idle -> Unit
        is ImportUi.Running -> {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text("Importing ${state.name}… ${formatCount(state.entries)} entries")
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        }
        is ImportUi.Finished -> {
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Imported ${formatCount(state.entries)} entries into ${state.name}.")
                TextButton(onClick = onDismiss) { Text("Dismiss") }
            }
        }
        is ImportUi.Failed -> {
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Dismiss") }
            }
        }
    }
}

@Composable
private fun LookupScreen(
    viewModel: LookupViewModel,
    onOpenDictionaries: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val dictionaries by viewModel.dictionaries.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    Column(modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Plover lookup", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onOpenDictionaries) { Text("Dictionaries") }
        }
        SearchField(query, viewModel::setQuery, focus)
        if (query.isBlank()) {
            Text(
                "Type an outline or a translation. A small sample dictionary is ready, and you can import your own Plover JSON from Dictionaries.",
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            return
        }
        val anyEnabled = dictionaries.any { it.enabled }
        if (!anyEnabled) {
            Text(
                "Turn a dictionary on to search.",
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            return
        }
        LazyColumn(Modifier.weight(1f).padding(top = 8.dp)) {
            if (results.strokeHits.isNotEmpty()) {
                item { SectionTitle("Stroke lookup") }
                itemsIndexed(
                    results.strokeHits,
                    key = { index, hit -> "stroke:$index:${hit.dictionaryName}:${hit.outline}" },
                ) { _, hit ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(hit.outline, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleMedium)
                        Text(hit.translation, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            hit.dictionaryName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (results.translations.isNotEmpty()) {
                item { SectionTitle("Translations") }
                items(
                    results.translations,
                    key = { group -> "text:${group.dictionaryName}\u0000${group.translation}" },
                ) { group ->
                    TranslationBlock(group)
                }
            }
            if (results.strokeHits.isEmpty() && results.translations.isEmpty()) {
                item {
                    Text(
                        "No matches.",
                        modifier = Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            if (results.truncated) {
                item {
                    Text(
                        "Showing the first matches. Type more to narrow them.",
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit, focus: FocusManager) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        placeholder = { Text("Enter definition or stroke to lookup") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
    )
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun TranslationBlock(group: TranslationGroup) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(group.translation, style = MaterialTheme.typography.titleMedium)
        Text(
            group.dictionaryName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        group.outlines.forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    row.outline,
                    modifier = Modifier.weight(1f),
                    fontFamily = FontFamily.Monospace,
                )
                val label = if (row.strokeCount == 1) "1 stroke" else "${row.strokeCount} strokes"
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(Modifier.padding(top = 8.dp))
    }
}

internal fun formatCount(value: Int): String {
    return NumberFormat.getIntegerInstance(Locale.US).format(value)
}
