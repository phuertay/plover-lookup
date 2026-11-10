package com.phuertay.ploverlookup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DictionariesScreen(
    viewModel: LookupViewModel,
    onBack: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dictionaries by viewModel.dictionaries.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val importing = importState is ImportUi.Running
    Column(modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("Back") }
            Button(onClick = onImport, enabled = !importing) { Text("Import JSON") }
        }
        Text(
            "Dictionaries",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            "Enabled dictionaries are searched together. The sample stays on the device. Personal dictionaries are not part of this project.",
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyColumn(Modifier.weight(1f)) {
            items(dictionaries, key = { it.id }) { dictionary ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(dictionary.name, style = MaterialTheme.typography.titleMedium)
                        val kind = if (dictionary.builtIn) "Built in · " else ""
                        Text(
                            kind + formatCount(dictionary.entryCount) + " entries",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!dictionary.builtIn) {
                        TextButton(onClick = { viewModel.deleteImported(dictionary.id) }) {
                            Text("Remove")
                        }
                    }
                    Switch(
                        checked = dictionary.enabled,
                        onCheckedChange = { viewModel.setEnabled(dictionary.id, it) },
                    )
                }
            }
        }
        Text(
            "Copyright 2026 Pedro Huerta. CC BY-NC-SA 4.0.",
            modifier = Modifier.padding(vertical = 12.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
