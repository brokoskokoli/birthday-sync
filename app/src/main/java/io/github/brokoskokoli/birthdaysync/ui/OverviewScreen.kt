package io.github.brokoskokoli.birthdaysync.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.brokoskokoli.birthdaysync.sync.Occurrence
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val DATE_TIME = DateTimeFormatter.ofPattern("EEE, dd.MM.yyyy, HH:mm 'Uhr'", Locale.GERMAN)
private val DAY = DateTimeFormatter.ofPattern("EEE, dd.MM.", Locale.GERMAN)

fun formatInstant(millis: Long): String = DATE_TIME.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

@Composable
fun OverviewScreen(vm: MainViewModel, modifier: Modifier, openIssues: Int, onSetup: () -> Unit, onGoto: (Destination) -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val running by vm.syncRunning.collectAsStateWithLifecycle()
    val nextSync by vm.nextPeriodicSync.collectAsStateWithLifecycle()
    val upcoming by vm.upcoming.collectAsStateWithLifecycle()
    val calendars by vm.calendars.collectAsStateWithLifecycle()
    val target = calendars.firstOrNull { it.id == settings.calendarId }

    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = ScreenSpacing) {
        if (openIssues > 0) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Einrichtung unvollständig", style = MaterialTheme.typography.titleSmall)
                            Text("$openIssues Berechtigung(en) oder Einstellung(en) fehlen für den zuverlässigen Sync.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Button(onClick = onSetup, modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)) { Text("Jetzt einrichten") }
                }
            }
        }
        if (settings.selectedSources.isEmpty() || target == null) {
            item {
                SectionCard("Erste Schritte") {
                    if (settings.selectedSources.isEmpty()) {
                        InfoText("1. Wähle die Kontaktlisten, deren Geburtstage übernommen werden sollen.")
                        OutlinedButton(onClick = { onGoto(Destination.CONTACTS) }, Modifier.padding(horizontal = 16.dp)) { Text("Kontaktlisten wählen") }
                    }
                    if (target == null) {
                        InfoText("2. Wähle den Kalender, in dem die Termine angelegt werden.")
                        OutlinedButton(onClick = { onGoto(Destination.CALENDAR) }, Modifier.padding(horizontal = 16.dp)) { Text("Zielkalender wählen") }
                    }
                }
            }
        }
        item {
            SectionCard("Synchronisation") {
                InfoText("Zielkalender: ${target?.name ?: "nicht gewählt"}")
                InfoText(
                    if (settings.autoSyncEnabled) "Automatisch ${if (settings.syncIntervalDays == 1) "täglich" else "wöchentlich"}" +
                        (if (nextSync > 0) ", nächster Lauf frühestens ${formatInstant(nextSync)}" else "")
                    else "Automatischer Sync ist aus",
                )
                if (status.lastSyncAt > 0) {
                    Text(
                        "Letzter Sync: ${formatInstant(status.lastSyncAt)}\n${status.lastSyncMessage}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (status.lastSyncOk) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                } else InfoText("Noch kein Sync durchgeführt")
                Button(onClick = vm::syncNow, enabled = !running, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    if (running) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Filled.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (running) "Synchronisiere …" else "Jetzt synchronisieren")
                }
            }
        }
        item {
            Text("Nächste Geburtstage", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        }
        if (upcoming.isEmpty()) {
            item { InfoText("Keine Geburtstage in den ausgewählten Kontaktlisten gefunden.", Modifier.padding(0.dp)) }
        }
        items(upcoming.take(30), key = { it.key }) { UpcomingRow(it) }
    }
}

@Composable
private fun UpcomingRow(o: Occurrence) {
    val days = ChronoUnit.DAYS.between(LocalDate.now(), o.date)
    val whenText = when (days) {
        0L -> "Heute"
        1L -> "Morgen"
        else -> "in $days Tagen"
    }
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(o.birthday.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    DAY.format(o.date) + (o.age?.let { " · wird $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                whenText,
                style = MaterialTheme.typography.labelLarge,
                color = if (days == 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider()
    }
}
