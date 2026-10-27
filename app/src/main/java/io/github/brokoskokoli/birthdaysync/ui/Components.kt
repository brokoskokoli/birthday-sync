package io.github.brokoskokoli.birthdaysync.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, subtitle: String? = null, content: @Composable () -> Unit) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
            if (subtitle != null) {
                Text(
                    subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            Spacer(Modifier.padding(top = 4.dp))
            content()
        }
    }
}

@Composable
private fun RowText(title: String, subtitle: String?, modifier: Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, subtitle: String? = null, enabled: Boolean = true) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Switch) { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowText(title, subtitle, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
fun CheckRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, subtitle: String? = null, enabled: Boolean = true, indent: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Checkbox) { onChange(!checked) }
            .padding(start = if (indent) 40.dp else 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Spacer(Modifier.width(12.dp))
        RowText(title, subtitle, Modifier.weight(1f))
    }
}

/** Eine Zeile mit aktuellem Wert; antippen öffnet eine Auswahlliste. */
@Composable
fun <T> ChoiceRow(title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, enabled: Boolean = true) {
    var open by remember { mutableStateOf(false) }
    val label = options.firstOrNull { it.first == selected }?.second ?: selected.toString()
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled) { open = true }.padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        RowText(title, label, Modifier.weight(1f))
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    options.forEach { (value, text) ->
                        Row(
                            Modifier.fillMaxWidth()
                                .selectable(value == selected, role = Role.RadioButton) { onSelect(value); open = false }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = value == selected, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(text)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Abbrechen") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeRow(title: String, minuteOfDay: Int, onChange: (Int) -> Unit, enabled: Boolean = true) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled) { open = true }.padding(horizontal = 16.dp, vertical = 10.dp)) {
        RowText(title, formatTime(minuteOfDay) + " Uhr", Modifier.weight(1f))
    }
    if (open) {
        val state = rememberTimePickerState(minuteOfDay / 60, minuteOfDay % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = { TimePicker(state) },
            confirmButton = { TextButton(onClick = { onChange(state.hour * 60 + state.minute); open = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Abbrechen") } },
        )
    }
}

@Composable
fun InfoText(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

val ScreenSpacing = Arrangement.spacedBy(12.dp)

fun formatTime(minuteOfDay: Int) = "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)

fun formatDuration(minutes: Int): String = when {
    minutes < 60 -> "$minutes Minuten"
    minutes % 60 == 0 -> if (minutes == 60) "1 Stunde" else "${minutes / 60} Stunden"
    else -> "${minutes / 60} h ${minutes % 60} min"
}
